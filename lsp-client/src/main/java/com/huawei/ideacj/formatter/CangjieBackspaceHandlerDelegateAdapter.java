/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import static com.huawei.ideacj.formatter.CangjieEnterHandlerDelegateAdapter.countLeadingSpaces;
import static com.huawei.ideacj.formatter.CangjieEnterHandlerDelegateAdapter.getLineText;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.codeInsight.editorActions.SmartBackspaceMode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieBackspaceHandlerDelegateAdapter
 *
 * @since 2026/01/05
 */
public class CangjieBackspaceHandlerDelegateAdapter extends BackspaceHandlerDelegate {
    @Override
    public void beforeCharDeleted(char c, @NotNull PsiFile psiFile, @NotNull Editor editor) {
    }

    @Override
    public boolean charDeleted(char c, @NotNull PsiFile psiFile, @NotNull Editor editor) {
        SmartBackspaceMode mode = CodeInsightSettings.getInstance().getBackspaceMode();
        if (mode != SmartBackspaceMode.AUTOINDENT) {
            return false;
        }
        if (c != ' ' && c != '\t') {
            return false;
        }
        if (!(psiFile.getLanguage() instanceof CangJieLanguage)) {
            return false;
        }
        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(offset);
        int currLineStartOffset = doc.getLineStartOffset(line);
        if (line <= 0) {
            return false;
        }
        String curr = getLineText(doc, line);
        int baseIndentCurr = countLeadingSpaces(curr, doc, psiFile.getProject());
        if (baseIndentCurr != offset - currLineStartOffset) {
            return false;
        }
        int preLineEndOffset = doc.getLineEndOffset(line - 1);
        editor.getDocument().deleteString(preLineEndOffset, offset);
        editor.getCaretModel().moveToOffset(preLineEndOffset);
        return true;
    }
}
