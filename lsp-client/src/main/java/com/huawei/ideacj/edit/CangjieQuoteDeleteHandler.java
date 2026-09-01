/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.edit;

import static org.wso2.lsp4intellij.utils.ApplicationUtils.writeAction;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * delete "\"" when deleting "\"" in Cangjie file.
 *
 * @since 2024-4-5
 */
public class CangjieQuoteDeleteHandler extends BackspaceHandlerDelegate {
    private static final char QUOTE = '"';

    private static final char SINGLE_QUOTE = '\'';

    @Override
    public void beforeCharDeleted(char character, @NotNull PsiFile file, Editor editor) {
        // do nothing
    }

    @Override
    public boolean charDeleted(char character, @NotNull PsiFile psiFile, @NotNull Editor editor) {
        if (fileIsCangjieLanguage(psiFile) && (character == QUOTE || character == SINGLE_QUOTE)) {
            final int offset = editor.getCaretModel().getOffset();
            CharSequence charSequence = editor.getDocument().getCharsSequence();
            if (charSequence.length() < offset + 1) {
                return false;
            }
            if (charSequence.charAt(offset) == character) {
                writeAction(() -> {
                    editor.getSelectionModel().setSelection(offset, offset + 1);
                    EditorModificationUtil.deleteSelectedText(editor);
                });
                return true;
            }
        }
        return false;
    }

    private boolean fileIsCangjieLanguage(@Nullable PsiFile psiFile) {
        return psiFile != null && psiFile.getLanguage() instanceof CangJieLanguage;
    }
}