/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage;

import com.huawei.ace.lsp.extensions.CheckCppElementResult;
import com.huawei.ace.lsp.extensions.client.CrossLanguageRenameProvider;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjieCrossRenameProvider
 *
 * @since 2025/09/03
 */
public class CangjieCrossRenameProvider implements CrossLanguageRenameProvider {
    private static final Logger LOG = Logger.getInstance(CangjieCrossRenameProvider.class);
    private static final int REGISTER_TYPE_INTEROP = 1;
    private static final int REGISTER_TYPE_REGISTER = 2;
    private static final Pattern QUOTES_PATTERN = Pattern.compile("\"([^\"]*)\"");

    @Override
    public boolean isTargetElement(PsiElement source, CheckCppElementResult result, Project project) {
        if (result == null || StringUtils.isEmpty(result.getDtsPath())) {
            return false;
        }
        return result.getDtsPath().contains("src/main/cangjie");
    }

    @Override
    public void rename(PsiElement source, String oldName, String newName, CheckCppElementResult elementResult,
                       Project project) {
        List<CrossLanguageRegisterItem> registerItems = CrossLanguageUtils.getRenameItem(source, elementResult,
                project);
        if (registerItems.isEmpty()) {
            return;
        }
        Map<String, Integer> paramsMap = new HashMap<>();
        for (CrossLanguageRegisterItem item : registerItems) {
            if (item.getRegisterType() == REGISTER_TYPE_INTEROP) {
                renameInteropName(project, item, newName, paramsMap);
            } else if (item.getRegisterType() == REGISTER_TYPE_REGISTER) {
                renameRegisterName(source, project, item, oldName, newName);
            } else {
                LOG.warn("rename unknown register type");
            }
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.openFile(source.getContainingFile().getVirtualFile(), true);
    }

    private String replaceInQuotes(String str, String oldName, String newName) {
        Matcher matcher = QUOTES_PATTERN.matcher(str);
        StringBuilder stringBuffer = new StringBuilder();
        while (matcher.find()) {
            String captured = matcher.group(1);
            if (captured.equals(oldName)) {
                matcher.appendReplacement(stringBuffer, "\"" + newName + "\"");
            } else {
                matcher.appendReplacement(stringBuffer, matcher.group(0));
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private int getOffsetFromRange(Editor editor, Range range) {
        Document document = editor.getDocument();
        int startLine = range.getStart().getLine();
        int startColumn = range.getStart().getCharacter();
        int lineStartOffset = document.getLineStartOffset(startLine);
        int lineEndOffset = document.getLineEndOffset(startLine);
        int column = Math.min(startColumn, lineEndOffset - lineStartOffset);
        return lineStartOffset + column;
    }

    private void renameInteropName(Project project, @NotNull CrossLanguageRegisterItem registerItem,
                                   String newName, Map<String, Integer> paramsMap) {
        if (registerItem.getDefinition() == null) {
            return;
        }
        Optional<VirtualFile> virtualFile = getVirtualFileFromRegisterItem(registerItem);
        if (virtualFile.isEmpty() || !virtualFile.get().isValid()) {
            return;
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.openFile(virtualFile.get(), false);
        Editor editor = editorManager.getSelectedTextEditor();
        EditorEventManager editorEventManager = EditorEventManagerBase.forEditor(editor);
        if (editor == null || editorEventManager == null) {
            return;
        }
        int offSet = getOffsetFromRange(editor, registerItem.getDefinition().getRange());
        if (isDuplicateFunction(virtualFile.get(), editor, offSet, paramsMap)) {
            return;
        }
        editorEventManager.rename(newName, offSet);
    }

    private boolean isDuplicateFunction(VirtualFile virtualFile, Editor editor, int offSet,
                                        Map<String, Integer> paramsMap) {
        PsiFile psiFile = PsiManager.getInstance(Objects.requireNonNull(editor.getProject())).findFile(virtualFile);
        if (psiFile == null) {
            return false;
        }
        PsiElement elementAt = psiFile.findElementAt(offSet);
        if (elementAt == null) {
            return false;
        }
        CjFunctionDefinition cjFunction = PsiTreeUtil.getParentOfType(elementAt, CjFunctionDefinition.class);
        if (cjFunction == null) {
            return false;
        }
        FunctionDefinitionInfo info = cjFunction.getFunctionDefinitionInfo();
        List<String> params = info.getTypeParams();
        String paramsKey = buildParameters(params);
        if (paramsMap.containsKey(paramsKey) && paramsMap.get(paramsKey) == params.size()) {
            return true;
        }
        paramsMap.put(paramsKey, params.size());
        return false;
    }

    private String buildParameters(List<String> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return "";
        }
        return String.join(",", paramsList);
    }

    private void renameRegisterName(PsiElement source, Project project, CrossLanguageRegisterItem registerItem,
                                    String oldName, String newName) {
        if (registerItem.getDeclaration() == null) {
            return;
        }
        Optional<VirtualFile> virtualFile = getVirtualFileFromRegisterItem(registerItem);
        if (virtualFile.isEmpty() || !virtualFile.get().isValid()) {
            return;
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.openFile(virtualFile.get(), false);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile.get());
        if (psiFile == null) {
            return;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        Position startPosition = registerItem.getDeclaration().getRange().getStart();
        Position endPosition = registerItem.getDeclaration().getRange().getEnd();
        int oldNameStartOffSet = document.getLineStartOffset(startPosition.getLine()) + startPosition.getCharacter();
        int oldNameEndOffSet = document.getLineStartOffset(endPosition.getLine()) + endPosition.getCharacter();
        if (oldNameStartOffSet == 0 && oldNameEndOffSet == 0) {
            editorManager.openFile(source.getContainingFile().getVirtualFile(), true);
            return;
        }
        // 根据注册方式判断注册类型
        // exports ["testCJ"] = runtime.function(hello).toJSValue()方式注册
        // clazz.addMethod(runtime.string("setId"), runtime.function(setDataId))方式注册
        TextRange range = new TextRange(oldNameStartOffSet, oldNameEndOffSet);
        String text = document.getText(range);
        String replaceText = replaceInQuotes(text, oldName, newName);
        Runnable r = () -> document.replaceString(
                oldNameStartOffSet,
                oldNameEndOffSet,
                replaceText);
        WriteCommandAction.runWriteCommandAction(project, r);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }

    private Optional<VirtualFile> getVirtualFileFromRegisterItem(@NotNull CrossLanguageRegisterItem registerItem) {
        Location definition = registerItem.getDefinition();
        if (definition == null) {
            return Optional.empty();
        }
        String uri = definition.getUri();
        if (uri == null || uri.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            URI uriObj = URI.create(uri);
            Path path = Paths.get(uriObj);
            String pathStr = path.toString();
            if (pathStr.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(pathStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
