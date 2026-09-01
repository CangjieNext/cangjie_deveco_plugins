/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ace.constants.ArkTSConstants.RAW_FILE_DIR;
import static com.huawei.ace.constants.ArkTSConstants.RIGHT_SQUARE_BRACKET_AND_DOT;
import static com.huawei.ace.constants.ArkTSConstants.LEFT_SQUARE_BRACKET;
import static com.huawei.ideacj.resource.CangjieResourceUtil.PATH_REGEX;
import static com.huawei.ideacj.resource.CangjieResourceUtil.RAW_FILE;
import static com.huawei.ideacj.resource.CangjieResourceUtil.getResourceInput;

import com.huawei.ace.ohos.contributors.RawFileReferenceBase;
import com.huawei.ace.rawfileref.CrossModuleResourceManager;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.ohos.common.HarmonyConstants;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.deveco.res.ohos.utils.ProductTargetUtil;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.history.core.Paths;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.Locale;
import java.util.Collections;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CangjieRawFileReferenceProvider
 *
 * @since 2024/10/18
 */
public class CangjieRawFileReferenceProvider extends PsiReferenceProvider {
    /**
     * rawfile path
     */
    private static final List<String> RAW_FILE_DIRS_PATH = new ArrayList<>();

    /**
     * rawfile dir name
     */
    private static final List<PsiFileSystemItem> RAW_FILE_DIRS = new ArrayList<>();

    private static final String FORMAT_RES_RAW_FILE = "/%s/%s/";

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement,
                                                           @NotNull ProcessingContext processingContext) {
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(psiElement);
        if (module == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        String input = getResourceInput(psiElement, true);
        if (StringUtil.isEmpty(input) || PATH_REGEX.matcher(input).matches()) {
            return PsiReference.EMPTY_ARRAY;
        }
        RAW_FILE_DIRS_PATH.clear();
        RAW_FILE_DIRS.clear();
        setRawFile(getMultiResourceContext(psiElement, module));
        return new RawFileReferenceSet(input, psiElement).getAllReferences();
    }

    private void setRawFile(@NotNull Collection<PsiFileSystemItem> items) {
        for (PsiFileSystemItem item : items) {
            String parentName = Optional.ofNullable(item.getParent()).map(PsiFileSystemItem::getName).orElse("");
            RAW_FILE_DIRS_PATH.add(String.format(Locale.ROOT, FORMAT_RES_RAW_FILE, parentName, RAW_FILE));
            RAW_FILE_DIRS.add(item);
        }
    }

    private Collection<PsiFileSystemItem> getMultiResourceContext(@NotNull PsiElement element,
                                                                         @NotNull ModuleModel module) {
        List<String> resources = new ArrayList<>(ProductTargetUtil.getValidResourceUrlForCurrentModule(module));
        String appScopeResources = getAppScopeResources(module.getProjectModel());
        // AppScope下的资源也支持rawfile，这里不区分模块类型，hsp/hap/har都支持引用AppScope下的资源
        if (StringUtil.isNotEmpty(appScopeResources)) {
            resources.add(appScopeResources);
        }
        String modulePath = module.getModulePath();
        if (StringUtil.isEmpty(modulePath)) {
            return Collections.emptyList();
        }

        // get all dependency har
        List<OhosModuleModel> dependencyHarModules = ModuleUtils.getAllDependenciesModuleModel(module);
        Set<String> dependencyHarResourceDirs = dependencyHarModules.stream()
                .map(ProductTargetUtil::getValidResourceUrlForCurrentModule)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        resources.addAll(dependencyHarResourceDirs);

        Collection<PsiFileSystemItem> result = new ArrayList<>();
        resources.stream()
                .filter(StringUtil::isNotEmpty)
                .map(path -> FileUtil.isAbsolute(path) ? path : Paths.appended(modulePath, path))
                .forEach(resource -> addRawFileDir(element, result, resource));
        return result;
    }

    private void addRawFileDir(@NotNull PsiElement element, @NotNull Collection<PsiFileSystemItem> result,
                                      @NotNull String resource) {
        Optional.of(Paths.appended(resource, RAW_FILE))
                .map(path -> LocalFileSystem.getInstance().findFileByPath(path))
                .map(vf -> element.getManager().findDirectory(vf))
                .map(result::add);
    }

    @NotNull
    private List<PsiFileSystemItem> getRawFileDirs() {
        return new ArrayList<>(RAW_FILE_DIRS);
    }

    @NotNull
    private List<String> getRawFileDirsPath() {
        return new ArrayList<>(RAW_FILE_DIRS_PATH);
    }

    /**
     * getAppScopeResources
     *
     * @param projectModel ProjectModel
     * @return app scope resources path
     */
    private String getAppScopeResources(@Nullable ProjectModel projectModel) {
        if (projectModel == null) {
            return StringUtils.EMPTY;
        }
        String path = java.nio.file.Paths.get(projectModel.getProjectPath(), HarmonyConstants.FD_APP_SCOPE,
                HarmonyConstants.FD_RES).toString();
        return FileUtil.toSystemIndependentName(path);
    }

    private class RawFileReferenceSet extends FileReferenceSet {
        /**
         * $rawfile reference set
         *
         * @param str path
         * @param element element
         */
        public RawFileReferenceSet(@NotNull String str, @NotNull PsiElement element) {
            super(str, element, 1, null, false, false);
        }

        @Override
        public FileReference createFileReference(TextRange range, int index, String text) {
            return new RawFileReference(this, range, index, text);
        }

        @Override
        @NotNull
        public Collection<PsiFileSystemItem> computeDefaultContexts() {
            return getRawFileDirs();
        }

        /**
         * rawfile case sensitive
         *
         * @return true case sensitive
         */
        @Override
        public boolean isCaseSensitive() {
            return true;
        }
    }

    /**
     * RawFileReference
     *
     * @since 2025/10/11
     */
    public static class RawFileReference extends RawFileReferenceBase {
        public RawFileReference(@NotNull FileReferenceSet referenceSet, TextRange range, int index, String text) {
            super(referenceSet, range, index, text);
        }

        @Override
        protected Object createLookupItem(PsiElement candidate) {
            return candidate;
        }

        @Override
        public PsiFileSystemItem resolve() {
            ResolveResult[] results = super.multiResolve(false);
            return results.length > 0 ? getPsiFileSystemItem(results) : null;
        }

        @Override
        protected PsiElement rename(String newName) throws IncorrectOperationException {
            PsiElement curElement = getElement();
            // 获取到移动的目的位置
            VirtualFile destFile = VfsUtilCore
                    .findRelativeFile(newName, curElement.getContainingFile().getVirtualFile());
            if (destFile == null) {
                return curElement;
            }
            Project project = curElement.getProject();
            ModuleModel destModule = ModuleUtils.findModuleModelByVirtualFile(project, destFile);
            if (destModule == null) {
                return curElement;
            }
            ModuleModel curModule = ModuleUtils.findModuleModelByPsiElement(curElement);
            if (!(curModule instanceof OhosModuleModel curOhosModule)) {
                // 把当前的module移动到一个新的目录中时，获取到的当前module为空
                return curElement;
            }
            if (Objects.equals(destModule, curModule) || StringUtil.equals(destModule.getModuleName(),
                    HarmonyConstants.FD_APP_SCOPE) || destModule.isHarLibrary()) {
                return handlerMoveSelfModule(curElement, destFile, destModule);
            } else if (CrossModuleResourceManager.isHspModule(destModule)) {
                // hsp移动到 other hsp
                return handleMoveOtherHsp(curElement, destFile, destModule);
            } else {
                return curElement;
            }
        }

        @Nullable
        private PsiElement handlerMoveSelfModule(PsiElement curElement, VirtualFile destFile, ModuleModel destModule) {
            String selfModuleNewName = getRawFileDirRelativePath(destModule, destFile);
            if (selfModuleNewName == null) {
                // 移动到非raw file目录下或非resources目录下
                return curElement;
            }
            return handleNewContent(curElement, selfModuleNewName);
        }

        @Nullable
        private PsiElement handleNewContent(PsiElement curElement, String newName) {
            if (curElement instanceof CjMacroTokens) {
                return new MyManipulator()
                        .handleContentChange(curElement.getParent().getParent(), super.getRangeInElement(), newName);
            }
            return super.handleElementRename(newName);
        }

        @Nullable
        private static String getRawFileDirRelativePath(ModuleModel destModule, VirtualFile destFile) {
            List<String> resourceDir = ProductTargetUtil.getValidResourceUrlForCurrentModule(destModule);
            for (String dir : resourceDir) {
                VirtualFile resourceVf = VirtualFileManager.getInstance().findFileByNioPath(Path.of(dir));
                if (resourceVf == null) {
                    continue;
                }
                boolean isAncestor = VfsUtilCore.isAncestor(resourceVf, destFile, true);
                if (!isAncestor) {
                    continue;
                }
                VirtualFile rawFileDirVf = resourceVf.findChild(RAW_FILE_DIR);
                if (rawFileDirVf == null) {
                    return null;
                }
                return VfsUtilCore.getRelativePath(destFile, rawFileDirVf);
            }
            return null;
        }
        @Nullable
        private PsiElement handleMoveOtherHsp(PsiElement curElement, VirtualFile destFile, ModuleModel destModule) {
            String rawFileDirRelativePath = getRawFileDirRelativePath(destModule, destFile);
            if (rawFileDirRelativePath == null) {
                // 移动到非raw file目录下或非resources目录下
                return curElement;
            }
            String newCrossName = getCrossRawFileNewName(destModule.getModuleName(), rawFileDirRelativePath);
            return handleNewContent(curElement, newCrossName);
        }

        @NotNull
        private static String getCrossRawFileNewName(@NotNull String hspModuleSource, @NotNull String newRelativePath) {
            return LEFT_SQUARE_BRACKET + hspModuleSource + RIGHT_SQUARE_BRACKET_AND_DOT + newRelativePath;
        }

        private static class MyManipulator extends AbstractElementManipulator<PsiElement> {
            @Override
            @Nullable
            public PsiElement handleContentChange(@NotNull PsiElement element,
                                                  @NotNull TextRange range,
                                                  String newContent) throws IncorrectOperationException {
                String originalContent = element.getText();
                String replacement = String.format(
                        Locale.ROOT,
                        "%s%s%s",
                        StringUtils.substring(originalContent, 0, 10),
                        newContent,
                        StringUtils.substring(originalContent, originalContent.length() - 2)
                );
                PsiFile dummyFile = PsiFileFactory.getInstance(element.getProject())
                        .createFileFromText(CangJieLanguage.INSTANCE, replacement);
                return element.replace(dummyFile.getFirstChild());
            }
        }
    }

    private static PsiFileSystemItem getPsiFileSystemItem(@NotNull ResolveResult[] results) {
        return Optional.of(results[0])
                .map(ResolveResult::getElement)
                .map(element -> ObjectUtils.tryCast(element, PsiFileSystemItem.class))
                .orElse(null);
    }
}
