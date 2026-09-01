/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.huawei.deveco.res.ohos.reference.base.ResourceReferencePsiElement;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiBinaryFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.PsiElementRenameHandler;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.rename.LSPRenameHandler;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

/**
 * CangjieLSPRenameHandler
 *
 * @since 2023-02-01
 */
public class CangjieLSPRenameHandler extends LSPRenameHandler {
    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (file == null) {
            return false;
        }
        PsiElement element = PsiElementRenameHandler.getElement(dataContext);
        if (element == null) {
            return false;
        }
        if (element instanceof ResourceReferencePsiElement
                || element instanceof PsiBinaryFile
                || !(element.getLanguage() instanceof CangJieLanguage)) {
            return false;
        }
        Language lang = file.getLanguage();
        if (lang instanceof CangJieLanguage) {
            return super.isAvailableOnDataContext(dataContext);
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        EditorEventManager editorEventManager = EditorEventManagerBase.forEditor(editor);
        if (editorEventManager == null) {
            return;
        }
        editorEventManager.prepareRename();
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
        invoke(project, dataContext.getData(CommonDataKeys.EDITOR), dataContext.getData(CommonDataKeys.PSI_FILE),
                dataContext);
    }
}
