/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.completion;

import com.huawei.ideacj.language.psi.othersnode.CjCharContent;
import com.huawei.ideacj.language.psi.othersnode.CjLineStringContent;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * Cangjie Typed handler
 *
 * @author xiewen
 * @since 2021-05-12
 */
public class CangjieTypedHandler extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor,
        @NotNull PsiFile file) {
        if (file.getLanguage() instanceof CangJieLanguage) {
            VirtualFile virtualFile = file.getVirtualFile();
            boolean isCangjieFile = "cj".equalsIgnoreCase(virtualFile.getExtension());
            final PsiElement previousPsiElement = file.findElementAt(editor.getCaretModel().getOffset() - 1);
            if (previousPsiElement != null) {
                if (isCangjieFile && (Character.isUnicodeIdentifierStart(charTyped)
                        || (charTyped == '/' && (previousPsiElement.getParent() instanceof CjLineStringContent
                        || previousPsiElement.getParent() instanceof CjCharContent)))) {
                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
                    return Result.STOP;
                }
            }
        }
        return Result.CONTINUE;
    }
}