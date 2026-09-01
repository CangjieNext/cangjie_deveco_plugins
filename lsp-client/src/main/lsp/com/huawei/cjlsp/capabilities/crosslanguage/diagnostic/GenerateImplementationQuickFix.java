/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage.diagnostic;

import com.huawei.ace.constants.ArkCommonBundle;
import com.huawei.ace.lsp.extensions.CheckCppElementResult;
import com.huawei.cjlsp.capabilities.crosslanguage.CrossLanguageUtils;
import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.compileunitnode.CjPreamble;
import com.huawei.ideacj.language.psi.importnode.CjImportList;
import com.huawei.ideacj.language.psi.packagenode.CjPackageNameIdentifier;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * GenerateImplementationQuickFix
 *
 * @since 2025/12/23
 */
class GenerateImplementationQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private static final String IMPORT_ARK_INTEROP = "ohos.ark_interop.*";

    private static final String IMPORT_ARK_INTEROP_MACRO = "ohos.ark_interop_macro.*";

    private final Project project;

    private final PsiElement source;

    private final CheckCppElementResult result;

    private List<String> parameterTypes;

    private String returnType;

    private String indexCJDirectory;

    private String indexCJPath;

    private PsiFile indexCJPsiFile;

    /**
     * generate native function implementation
     *
     * @param source checked psiElement
     * @param cppResult CheckCppElementResult information
     * @param project project
     * @param parameterTypes parameter type list
     * @param returnType return type
     */
    protected GenerateImplementationQuickFix(@NotNull PsiElement source, @NotNull CheckCppElementResult cppResult,
        @NotNull Project project, @Nullable List<String> parameterTypes, @NotNull String returnType) {
        super(source);
        this.source = source;
        this.result = cppResult;
        this.project = project;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor,
        @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        generate();
    }

    @Override
    @NotNull
    public String getText() {
        return ArkCommonBundle.message("cross.language.function.quick.fix.display.name");
    }

    @Override
    @NotNull
    @IntentionFamilyName
    public String getFamilyName() {
        return ArkCommonBundle.message("cross.language.function.quick.fix.family.name");
    }

    /**
     * generate native function
     */
    public void generate() {
        String dtsPath = result.getDtsPath();
        this.indexCJDirectory = getBeforeTypesPath(dtsPath);
        this.indexCJPath = this.indexCJDirectory + File.separator + "index.cj";
        Optional<Editor> editor = getEditor();
        if (editor.isEmpty()) {
            return;
        }
        if (!isParameterListValid(parameterTypes)) {
            String msg = CangjieBundle.message("cross.language.unsupported.param.type");
            HintManager.getInstance().showErrorHint(editor.get(), msg);
            return;
        }

        if (!isReturnTypeValid(returnType)) {
            String msg = CangjieBundle.message("cross.language.unsupported.return.type");
            // 使用 invokeLater 确保在 UI 事件循环的下一轮执行
            ApplicationManager.getApplication().invokeLater(() -> {
                Editor ed = editor.get();
                if (!ed.isDisposed()) {
                    HintManager.getInstance().showErrorHint(ed, msg);
                }
            }, ModalityState.defaultModalityState());
            return;
        }
        initIndexCJFile();
        insertPackageNameAndImport();
        insertCJFuncContent2CJFile();
    }

    private Optional<Editor> getEditor() {
        PsiFile psiFile = source.getContainingFile();
        if (psiFile == null) {
            return Optional.empty();
        }

        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            return Optional.empty();
        }

        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor fileEditor = editorManager.getSelectedEditor(virtualFile);
        if (fileEditor instanceof TextEditor) {
            return Optional.of(((TextEditor) fileEditor).getEditor());
        }
        return Optional.empty();
    }

    private boolean isParameterListValid(List<String> parameterTypes) {
        TS2CangjieUtils utils = new TS2CangjieUtils();
        if (parameterTypes.isEmpty()) {
            return true;
        }
        return utils.isParameterListValid(parameterTypes);
    }

    private boolean isReturnTypeValid(String returnType) {
        TS2CangjieUtils utils = new TS2CangjieUtils();
        return utils.isReturnTypeValid(returnType);
    }

    private void initIndexCJFile() {
        File indexCJFile = new File(indexCJPath);
        if (!indexCJFile.exists()) {
            createCJFile(indexCJFile);
        }
        VirtualFile cjVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(indexCJPath);
        if (cjVirtualFile == null) {
            LOG.warn("Path Error: " + indexCJPath);
            return;
        }
        indexCJPsiFile = PsiManager.getInstance(project).findFile(cjVirtualFile);
    }

    private void createCJFile(File initFile) {
        try {
            if (!initFile.createNewFile()) {
                return;
            }
        } catch (IOException e) {
            return;
        }

        try (FileWriter writer = new FileWriter(initFile)) {
            writer.write("package " + CrossLanguageUtils.extractPackageName(result.getPackageName()));
        } catch (IOException e) {
            return;
        }

        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(indexCJDirectory);
        if (virtualFile != null) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null) {
                psiFile = fileFactory.createFileFromText("index.cj", CangJieLanguage.INSTANCE,
                    "package " + CrossLanguageUtils.extractPackageName(result.getPackageName()) + "\n");
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile);
                PsiFile finalPsiFile = psiFile;
                Runnable r = () -> {
                    assert psiDirectory != null;
                    psiDirectory.add(finalPsiFile);
                };
                WriteCommandAction.runWriteCommandAction(project, r);
            }
        }
    }

    private void insertCJFuncContent2CJFile() {
        String content = generateGlobalFunction(result.getInterfaceName());
        Document document = PsiDocumentManager.getInstance(project).getDocument(indexCJPsiFile);
        modifyDocument(document.getTextLength(), content);
        FileEditorManager.getInstance(project).openFile(indexCJPsiFile.getVirtualFile(), true);
    }

    private void insertPackageNameAndImport() {
        String packageName = CrossLanguageUtils.extractPackageName(result.getPackageName());
        List<CjPreamble> preambleList = new ArrayList<>(
            PsiTreeUtil.findChildrenOfType(indexCJPsiFile, CjPreamble.class));
        Set<String> existingImports = new HashSet<>();
        if (preambleList.isEmpty()) {
            String preamble = "package " + packageName + "\n" + "import " + IMPORT_ARK_INTEROP + ";" + "\n" + "import "
                + IMPORT_ARK_INTEROP_MACRO + ";" + "\n";
            modifyDocument(0, preamble);
            return;
        }

        List<CjPackageNameIdentifier> packageNameLists = new ArrayList<>(
            PsiTreeUtil.findChildrenOfType(preambleList.get(0), CjPackageNameIdentifier.class));
        if (packageNameLists.isEmpty()) {
            String packageNameContent = "package " + packageName + "\n";
            modifyDocument(0, packageNameContent);
        }

        List<CjImportList> importLists = new ArrayList<>(
            PsiTreeUtil.findChildrenOfType(preambleList.get(0), CjImportList.class));
        for (CjImportList importList : importLists) {
            // 获取该 import list 下的所有 import 语句
            for (PsiElement child : importList.getChildren()) {
                if (child instanceof CJPsiNode) {
                    String importText = child.getText().trim();
                    String importsStr = importText.replace(";", "").trim();
                    existingImports.add(importsStr);
                }
            }
        }

        String insertImportItem = "";
        if (!existingImports.contains(IMPORT_ARK_INTEROP)) {
            insertImportItem += "import " + IMPORT_ARK_INTEROP + "\n";
        }
        if (!existingImports.contains(IMPORT_ARK_INTEROP_MACRO)) {
            insertImportItem += "import " + IMPORT_ARK_INTEROP_MACRO + "\n";
        }
        String finalInsertImportItem = insertImportItem;
        if (importLists.isEmpty()) {
            List<CjPackageNameIdentifier> newList = new ArrayList<>(
                PsiTreeUtil.findChildrenOfType(preambleList.get(0), CjPackageNameIdentifier.class));
            CjPackageNameIdentifier lastItem = newList.get(newList.size() - 1);
            int packageItemOffset = lastItem.getTextRange().getEndOffset();
            modifyDocument(packageItemOffset + 1, finalInsertImportItem);
        } else {
            CjImportList lastImportList = importLists.get(importLists.size() - 1);
            int insertOffset = lastImportList.getChildren()[lastImportList.getChildren().length - 2].getTextRange()
                .getEndOffset();
            modifyDocument(insertOffset + 1, finalInsertImportItem);
        }
    }

    private String generateGlobalFunction(String funcName) {
        TS2CangjieUtils utils = new TS2CangjieUtils();
        utils.updateTSFuncSignature(parameterTypes, returnType, funcName);
        return utils.changeToCangjieGlobalFunc();
    }

    private String getBeforeTypesPath(String dtsPath) {
        if (dtsPath == null || dtsPath.isEmpty()) {
            return "";
        }

        Path path = Paths.get(dtsPath);
        Path filePath = null;

        for (Path part : path) {
            if ("types".equals(part.toString())) {
                return (filePath == null) ? (path.isAbsolute() ? File.separator : "") : filePath.toString();
            }
            filePath = (filePath == null)
                    ? (path.isAbsolute() ? path.getRoot().resolve(part) : part) : filePath.resolve(part);
        }
        return "";
    }

    private void modifyDocument(int offSet, String content) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(indexCJPsiFile);
        if (document == null || offSet > document.getTextLength()) {
            return;
        }
        Runnable r = () -> document.insertString(offSet, content);
        WriteCommandAction.runWriteCommandAction(project, r);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }
}