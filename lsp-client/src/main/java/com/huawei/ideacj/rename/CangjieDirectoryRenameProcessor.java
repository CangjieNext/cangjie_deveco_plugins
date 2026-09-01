/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import static com.huawei.ideacj.lsp.utils.LanguageManager.CANGJIE_EXTENSION;

import com.huawei.cangjie.projectmgmt.utils.CangjieModulePathType;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.language.psi.compileunitnode.CjPreamble;
import com.huawei.ideacj.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.ideacj.language.psi.importnode.CjImportList;
import com.huawei.ideacj.language.psi.packagenode.CjPackageHeader;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenameDialog;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Cangjie directory rename processor.
 *
 * @since 2025-08-12
 */
public class CangjieDirectoryRenameProcessor extends RenamePsiElementProcessor {
    private CangjieRenameDirectoryInfo renameInfo;

    /**
     * Is cangjie module directory optional.
     *
     * @param element the element
     * @return the optional
     */
    public static Optional<CangjieRenameDirectoryInfo> getCangjieModuleDirectory(@NotNull PsiElement element) {
        if (!(element instanceof PsiDirectory psiDirectory)) {
            return Optional.empty();
        }
        VirtualFile renameFile = psiDirectory.getVirtualFile();
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(element);
        if (!(module instanceof OhosModuleModel ohosModuleModel) || !LspConfigUtils.isCangjieModule(ohosModuleModel)) {
            return Optional.empty();
        }
        Project project = element.getProject();
        Optional<CangjieRenameDirectoryInfo> mainRenameInfoOpt =
            getRenameInfo(project, ohosModuleModel, CangjieModulePathType.MAIN, renameFile);
        if (mainRenameInfoOpt.isPresent()) {
            return mainRenameInfoOpt;
        }
        Optional<CangjieRenameDirectoryInfo> ohosTestRenameInfoOpt =
            getRenameInfo(project, ohosModuleModel, CangjieModulePathType.OHOS_TEST, renameFile);
        if (ohosTestRenameInfoOpt.isPresent()) {
            return ohosTestRenameInfoOpt;
        }
        return getRenameInfo(project, ohosModuleModel, CangjieModulePathType.LOCAL_TEST, renameFile);
    }

    private static Optional<CangjieRenameDirectoryInfo> getRenameInfo(Project project, OhosModuleModel ohosModuleModel,
        CangjieModulePathType pathType, VirtualFile renameFile) {
        Optional<Path> srcDirPathOpt = LspConfigUtils.getSourceAbsolutePath(project, ohosModuleModel, pathType);
        if (srcDirPathOpt.isPresent() && LspConfigUtils.isChildFile(srcDirPathOpt.get(), renameFile)) {
            CangjieRenameDirectoryInfo renameDirectoryInfo = new CangjieRenameDirectoryInfo();
            renameDirectoryInfo.setBelongModuleModel(ohosModuleModel);
            renameDirectoryInfo.setRootPath(srcDirPathOpt.get());
            renameDirectoryInfo.setRootPackageName(
                LspConfigUtils.getCangjieModuleName(project, ohosModuleModel, pathType));
            return Optional.of(renameDirectoryInfo);
        }
        return Optional.empty();
    }

    @Override
    public boolean canProcessElement(@NotNull PsiElement psiElement) {
        Optional<CangjieRenameDirectoryInfo> renameDirectoryInfoOpt = getCangjieModuleDirectory(psiElement);
        if (renameDirectoryInfoOpt.isPresent()) {
            renameInfo = renameDirectoryInfoOpt.get();
            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public RenameDialog createRenameDialog(@NotNull Project project, @NotNull PsiElement element,
        PsiElement nameSuggestionContext, Editor editor) {
        return new CangjieRenameWithOptionalDialog(project, element, nameSuggestionContext, editor);
    }

    /**
     * 是否对注释里的标识符也进行Rename(用户可在Rename选项框中勾选)
     *
     * @param element 发起Rename请求的元素
     * @return true代表用户勾选了Search in comments and strings
     */
    @Override
    public boolean isToSearchInComments(@NotNull PsiElement element) {
        // 默认设置rename弹出框SearchInComments选项不勾选
        return false;
    }

    @Override
    @NotNull
    public Collection<PsiReference> findReferences(@NotNull PsiElement element, @NotNull SearchScope searchScope,
        boolean isSearchInCommentsAndStrings) {
        if (!getSearchForReferences(element) || renameInfo == null || !(element instanceof PsiDirectory directory)) {
            return new ArrayList<>();
        }
        Project project = directory.getProject();
        // 收集所有引用
        List<PsiReference> references = new ArrayList<>();
        // 查找模块内的所有 .cj 文件中的引用
        ReadAction.computeBlocking(() -> {
            Collection<CangjiePackageImportReference> importRefs = doFindReferences(project, directory);
            references.addAll(importRefs);
            return null;
        });
        references.addAll(super.findReferences(element, searchScope, isSearchInCommentsAndStrings));
        return references;
    }

    @Override
    public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName,
        @NotNull Map<PsiElement, String> allRenames) {
        super.prepareRenaming(element, newName, allRenames);
    }

    @Override
    public void renameElement(@NotNull PsiElement element, @NotNull String newName, UsageInfo @NotNull [] usages,
        @Nullable RefactoringElementListener listener) {
        // 1. 筛选出 CangjiePackageImportReference 类型的用法并按文件分组
        Map<PsiFile, List<UsageInfo>> usagesByFile = Arrays.stream(usages).filter(
                usage -> usage.getReference() instanceof CangjiePackageImportReference && usage.getElement() != null)
            .collect(Collectors.groupingBy(usage -> usage.getElement().getContainingFile()));
        if (usagesByFile.isEmpty()) {
            super.renameElement(element, newName, usages, listener);
            return;
        }
        // 2. 处理每个文件
        usagesByFile.forEach((file, fileUsages) -> processSingleFile(file, fileUsages, element, newName));
        super.renameElement(element, newName, usages, listener);
    }

    /**
     * 查找 .cj 文件中的 import 引用
     *
     * @param project the project
     * @param directory the directory
     * @return the list
     */
    private List<CangjiePackageImportReference> doFindReferences(@NotNull Project project,
        @NotNull PsiDirectory directory) {
        final List<String> packageDeclares = List.copyOf(getPackagePath(directory));
        List<CangjiePackageImportReference> references = new ArrayList<>();
        if (CollectionUtils.isEmpty(packageDeclares)) {
            return references;
        }
        references.addAll(findPackageReferencesInFile(project, directory, packageDeclares));
        references.addAll(findImportReferencesInFile(project, directory, packageDeclares));
        return references;
    }

    /**
     * 在单个文件中查找 package 引用
     *
     * @param project the project
     * @param directory the directory
     * @param packageDeclares the package declares
     * @return the list
     */
    private List<CangjiePackageImportReference> findPackageReferencesInFile(@NotNull Project project,
        @NotNull PsiDirectory directory, List<String> packageDeclares) {
        // 提前返回空结果，避免不必要的文件搜索
        if (packageDeclares == null || packageDeclares.isEmpty()) {
            return Collections.emptyList();
        }
        GlobalSearchScope directorySearchScope =
            GlobalSearchScopes.directoriesScope(project, true, directory.getVirtualFile());
        // 缓存常用对象
        PsiManager psiManager = PsiManager.getInstance(project);
        return FilenameIndex.getAllFilesByExt(project, CANGJIE_EXTENSION, directorySearchScope).parallelStream()
            .flatMap(cjFile -> getCangjiePackageReferenceStream(directory, packageDeclares, cjFile, psiManager))
            .collect(Collectors.toList());
    }

    /**
     * Gets cangjie package reference stream.
     *
     * @param directory the directory
     * @param packageDeclares the package declares
     * @param cjFile the cj file
     * @param psiManager the psi manager
     * @return the cangjie package reference stream
     */
    private Stream<CangjiePackageImportReference> getCangjiePackageReferenceStream(PsiDirectory directory,
        List<String> packageDeclares, VirtualFile cjFile, PsiManager psiManager) {
        // 跳过无效文件
        if (!cjFile.isValid()) {
            return Stream.empty();
        }
        // 预计算目标前缀大小，避免重复计算
        final int targetPrefixSize = packageDeclares.size();
        return ReadAction.compute(() -> {
            Optional<CjPreamble> cjPreambleOpt = getCjPreambleFromFile(psiManager, cjFile);
            if (cjPreambleOpt.isEmpty()) {
                return Stream.empty();
            }
            CjPreamble cjPreamble = cjPreambleOpt.get();
            // 使用getChildrenOfType只遍历一次PSI树
            CjPackageHeader[] packageHeaders = PsiTreeUtil.getChildrenOfType(cjPreamble, CjPackageHeader.class);
            if (packageHeaders == null) {
                return Stream.empty();
            }
            List<CangjiePackageImportReference> fileRefs = new ArrayList<>();
            // 通常一个文件只有一个package声明，所以大多数情况下这个循环只执行一次
            for (CjPackageHeader header : packageHeaders) {
                Optional<PsiElement> matchingPackageOpt =
                    CangjiePackageImportMatcher.getMatchingPackage(header, packageDeclares, targetPrefixSize);
                // 创建引用对象
                matchingPackageOpt.ifPresent(element -> addReference(directory, header, element, fileRefs));
            }
            return fileRefs.stream();
        });
    }

    /**
     * 在单个文件中查找 import 引用
     *
     * @param project the project
     * @param directory the directory
     * @param packageDeclares the package declares
     * @return the list
     */
    private List<CangjiePackageImportReference> findImportReferencesInFile(@NotNull Project project,
        @NotNull PsiDirectory directory, List<String> packageDeclares) {
        // 提前返回空结果，避免不必要的文件搜索
        if (packageDeclares == null || packageDeclares.isEmpty()) {
            return Collections.emptyList();
        }
        // 获取目录所在的模块
        Optional<GlobalSearchScope> searchScopeOpt = getGlobalSearchScope(project);
        if (searchScopeOpt.isEmpty()) {
            return Collections.emptyList();
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        Pattern importPattern = createImportPattern(Strings.join(packageDeclares, "."));
        return FilenameIndex.getAllFilesByExt(project, CANGJIE_EXTENSION, searchScopeOpt.get()).parallelStream()
            .flatMap(cjFile -> getCangjieFileImportReferenceStream(directory, packageDeclares, cjFile, psiManager,
                importPattern)).collect(Collectors.toList());
    }

    /**
     * Gets cangjie file import reference stream.
     *
     * @param directory the directory
     * @param packageDeclares the package declares
     * @param cjFile the cj file
     * @param psiManager the psi manager
     * @param importPattern pattern
     * @return the cangjie file import reference stream
     */
    private Stream<CangjiePackageImportReference> getCangjieFileImportReferenceStream(@NotNull PsiDirectory directory,
        List<String> packageDeclares, VirtualFile cjFile, PsiManager psiManager, Pattern importPattern) {
        // 跳过无效文件
        if (!cjFile.isValid()) {
            return Stream.empty();
        }
        return ReadAction.compute(() -> {
            Document document = FileDocumentManager.getInstance().getDocument(cjFile);
            boolean isContains = false;
            if (document != null) {
                Matcher matcher = importPattern.matcher(document.getText());
                isContains = matcher.find();
            }
            if (!isContains) {
                return Stream.empty();
            }
            Optional<CjPreamble> cjPreambleOpt = getCjPreambleFromFile(psiManager, cjFile);
            if (cjPreambleOpt.isEmpty()) {
                return Stream.empty();
            }
            CjPreamble cjPreamble = cjPreambleOpt.get();
            // 查找文件中的 import 引用
            List<CangjiePackageImportReference> fileRefs = new ArrayList<>();
            CjImportList[] cjImportLists = PsiTreeUtil.getChildrenOfType(cjPreamble, CjImportList.class);
            traverseImportLists(directory, packageDeclares, cjImportLists, fileRefs);
            return fileRefs.stream();
        });
    }

    /**
     * 创建导入模式匹配器，支持精确匹配和前缀匹配
     *
     * @param targetPackage the target package
     * @return the pattern
     */
    private Pattern createImportPattern(String targetPackage) {
        // 分割目标包路径
        String[] packageParts = targetPackage.split("\\.");
        // 构建完整的正则表达式
        StringBuilder patternBuilder = new StringBuilder();
        // 导入语句开始
        patternBuilder.append("\\s*import\\s+");
        // 开始匹配主体部分（使用非捕获组）
        patternBuilder.append("(?:");
        // 添加直接导入匹配模式
        appendDirectImportPattern(patternBuilder, packageParts);
        // 添加花括号导入匹配模式
        patternBuilder.append("|");
        appendBracketImportPatterns(patternBuilder, packageParts);
        patternBuilder.append(")");
        // 结束部分（分号或行尾）
        patternBuilder.append("\\s*(?:;|$)");
        return Pattern.compile(patternBuilder.toString(), Pattern.UNICODE_CHARACTER_CLASS | Pattern.MULTILINE);
    }

    /**
     * 添加直接导入匹配模式
     *
     * @param patternBuilder the pattern builder
     * @param packageParts the package parts
     */
    private void appendDirectImportPattern(StringBuilder patternBuilder, String[] packageParts) {
        if (packageParts.length == 0) {
            return;
        }
        // 创建全路径部分的正则表达式
        for (int i = 0; i < packageParts.length; i++) {
            if (i > 0) {
                patternBuilder.append("\\s*\\.\\s*");
            }
            patternBuilder.append(Pattern.quote(packageParts[i]));
        }
        // 匹配后缀部分
        patternBuilder.append("(?:");
        // 通配符导入
        patternBuilder.append("\\s*\\.\\s*\\*|");
        // 子包/子类导入
        patternBuilder.append("\\s*\\.\\s*[^;]*|");
        // 或者没有后缀（完全匹配）
        patternBuilder.append(")");
    }

    /**
     * 处理花括号导入模式
     *
     * @param patternBuilder the pattern builder
     * @param packageParts the package parts
     */
    private void appendBracketImportPatterns(StringBuilder patternBuilder, String[] packageParts) {
        if (packageParts.length == 0) {
            return;
        }
        // 处理每个可能的分割点
        for (int splitIndex = 0; splitIndex <= packageParts.length; splitIndex++) {
            if (splitIndex > 0) {
                patternBuilder.append("|");
            }
            // 前缀部分（花括号前的包路径）
            for (int i = 0; i < splitIndex; i++) {
                if (i > 0) {
                    patternBuilder.append("\\s*\\.\\s*");
                }
                patternBuilder.append(Pattern.quote(packageParts[i]));
            }
            // 添加花括号
            if (splitIndex > 0) {
                patternBuilder.append("\\s*\\.\\s*");
            }
            patternBuilder.append("\\{\\s*");
            // 构建花括号内的目标路径
            String[] remainingParts = Arrays.copyOfRange(packageParts, splitIndex, packageParts.length);
            if (remainingParts.length == 0) {
                patternBuilder.append("[^}]*\\}");
                continue;
            }
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < remainingParts.length; i++) {
                if (i > 0) {
                    pathBuilder.append("\\s*\\.\\s*");
                }
                pathBuilder.append(Pattern.quote(remainingParts[i]));
            }
            String pathPattern = pathBuilder.toString();
            // 预查确保花括号内包含目标路径（允许继续以 .xxx 扩展）
            patternBuilder.append("(?=[^}]*");
            patternBuilder.append(pathPattern);
            patternBuilder.append("(?:\\s*\\.\\s*.*?)?");
            patternBuilder.append(")");
            // 真正消费花括号到右括号
            patternBuilder.append("[^}]*\\}");
        }
    }

    private void traverseImportLists(@NotNull PsiDirectory directory, List<String> packageDeclares,
        CjImportList[] cjImportLists, List<CangjiePackageImportReference> references) {
        if (cjImportLists == null) {
            return;
        }
        for (CjImportList cjImport : cjImportLists) {
            List<PsiElement> matchingImports =
                CangjiePackageImportMatcher.getMatchingImports(cjImport, packageDeclares, packageDeclares.size());
            if (CollectionUtils.isEmpty(matchingImports)) {
                continue;
            }
            // 创建引用对象
            for (PsiElement psiElement : matchingImports) {
                addReference(directory, cjImport, psiElement, references);
            }
        }
    }

    private Optional<CjPreamble> getCjPreambleFromFile(PsiManager psiManager, VirtualFile virtualFile) {
        PsiFile psiFile = psiManager.findFile(virtualFile);
        if (psiFile == null) {
            return Optional.empty();
        }
        CjTranslationUnit cjTranslationUnit = PsiTreeUtil.getChildOfType(psiFile, CjTranslationUnit.class);
        if (cjTranslationUnit == null) {
            return Optional.empty();
        }
        CjPreamble cjPreamble = PsiTreeUtil.getChildOfType(cjTranslationUnit, CjPreamble.class);
        return cjPreamble == null ? Optional.empty() : Optional.of(cjPreamble);
    }

    private void addReference(@NotNull PsiDirectory directory, PsiElement parentElement, PsiElement matchingPackage,
        List<CangjiePackageImportReference> references) {
        TextRange parentTextRange = parentElement.getTextRange();
        TextRange childTextRange = matchingPackage.getTextRange();
        TextRange matchRelativeRange = new TextRange(childTextRange.getStartOffset() - parentTextRange.getStartOffset(),
            childTextRange.getEndOffset() - parentTextRange.getStartOffset());
        references.add(new CangjiePackageImportReference(parentElement, matchRelativeRange, directory));
    }

    private List<String> getPackagePath(@NotNull PsiDirectory directory) {
        String targetDir = directory.getVirtualFile().getPath();
        List<String> result = new ArrayList<>();
        Path rootPath = renameInfo.getRootPath();
        if (rootPath == null) {
            return result;
        }
        String[] packages = rootPath.normalize().relativize(Paths.get(targetDir).normalize()).toString()
            .split(Pattern.quote(File.separator));
        result.add(renameInfo.getRootPackageName());
        Collections.addAll(result, packages);
        return result;
    }

    private void processSingleFile(PsiFile file, List<UsageInfo> fileUsages, PsiElement originalElement,
        String newName) {
        if (file == null) {
            return;
        }
        // 过滤并倒序
        List<UsageInfo> sortedUsages = fileUsages.stream().filter(
                usage -> usage.getReference() instanceof CangjiePackageImportReference && usage.getElement() != null
                    && usage.getElement().isValid())
            .sorted((u1, u2) -> Integer.compare(getAbsoluteStartOffset(u2), getAbsoluteStartOffset(u1))).toList();
        if (sortedUsages.isEmpty()) {
            return;
        }
        String oldName = getElementName(originalElement);
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
            if (document == null) {
                return;
            }
            // 文件内倒序更新内容
            for (UsageInfo usage : sortedUsages) {
                PsiElement element = usage.getElement();
                PsiReference reference = usage.getReference();
                if (element == null || reference == null) {
                    continue;
                }
                TextRange rangeInElement = reference.getRangeInElement();
                int startOffset = element.getTextRange().getStartOffset() + rangeInElement.getStartOffset();
                int endOffset = element.getTextRange().getStartOffset() + rangeInElement.getEndOffset();
                if (startOffset >= endOffset || startOffset < 0 || endOffset > document.getTextLength()) {
                    continue;
                }
                String currentText = document.getText(new TextRange(startOffset, endOffset));
                String newText = currentText.replace(oldName, newName);
                if (!currentText.equals(newText)) {
                    document.replaceString(startOffset, endOffset, newText);
                }
            }
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        });
    }

    private String getElementName(PsiElement element) {
        // PsiFileSystemItem 有 getName() 方法
        if (element instanceof PsiFileSystemItem fileSystemItem) {
            return fileSystemItem.getName();
        }
        // 使用 PsiUtilCore 获取 VirtualFile
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (virtualFile != null) {
            return virtualFile.getName();
        }
        // 从包含的文件获取VirtualFile
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
            VirtualFile vf = containingFile.getVirtualFile();
            if (vf != null) {
                return vf.getName();
            }
        }
        // 最后的兜底方案：使用文本内容
        String text = element.getText();
        return text != null ? text : "unknown";
    }

    private Optional<GlobalSearchScope> getGlobalSearchScope(@NotNull Project project) {
        List<VirtualFile> scopeFiles = new ArrayList<>();
        getScopeVirtualFile(project, scopeFiles, CangjieModulePathType.MAIN);
        getScopeVirtualFile(project, scopeFiles, CangjieModulePathType.OHOS_TEST);
        getScopeVirtualFile(project, scopeFiles, CangjieModulePathType.LOCAL_TEST);
        if (CollectionUtils.isEmpty(scopeFiles)) {
            return Optional.empty();
        }
        GlobalSearchScope searchScope =
            GlobalSearchScopes.directoriesScope(project, true, scopeFiles.toArray(VirtualFile[]::new));
        return Optional.of(searchScope);
    }

    private void getScopeVirtualFile(Project project, List<VirtualFile> virtualFiles, CangjieModulePathType pathType) {
        Optional<Path> sourcePathOpt =
            LspConfigUtils.getSourceAbsolutePath(project, renameInfo.getBelongModuleModel(), pathType);
        if (sourcePathOpt.isEmpty()) {
            return;
        }
        VirtualFile sourceVF = LocalFileSystem.getInstance().findFileByPath(sourcePathOpt.get().toString());
        if (sourceVF == null) {
            return;
        }
        virtualFiles.add(sourceVF);
    }

    private boolean getSearchForReferences(PsiElement element) {
        return element instanceof PsiDirectory
            ? RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY
            : RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE;
    }

    private int getAbsoluteStartOffset(UsageInfo usage) {
        PsiElement element = usage.getElement();
        if (element == null || !element.isValid()) {
            return Integer.MAX_VALUE; // 放到排序的末尾
        }
        TextRange elementRange = element.getTextRange();
        if (elementRange == null) {
            return Integer.MAX_VALUE;
        }

        PsiReference reference = usage.getReference();
        if (reference == null) {
            return Integer.MAX_VALUE;
        }
        TextRange rangeInElement = reference.getRangeInElement();
        return elementRange.getStartOffset() + rangeInElement.getStartOffset();
    }
}
