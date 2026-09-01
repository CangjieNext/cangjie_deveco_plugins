/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.edit;

import com.huawei.ideacj.filetypes.CangjieCodeFile;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * pair completion in preprocess command
 *
 * @since 2022-09-27
 */
public class QuotePairCompletion extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result beforeCharTyped(char character, @NotNull Project project, @NotNull Editor editor,
                                  @NotNull PsiFile file, @NotNull FileType fileType) {
        if (!(fileType instanceof CangjieCodeFile)) {
            return Result.CONTINUE;
        }
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE || "\"'".indexOf(character) == -1) {
            return Result.CONTINUE;
        }
        int offset = editor.getCaretModel().getOffset();
        if (offset == 0) {
            return Result.CONTINUE;
        }
        if (!(editor instanceof EditorEx)) {
            return Result.CONTINUE;
        }
        if (character == '"' || character == '\'') {
            return handleInQuote(character, editor, offset);
        }
        editor.getDocument().insertString(offset, getPairedStr(character));
        editor.getCaretModel().moveToOffset(offset + 1);
        return Result.STOP;
    }

    private Result handleInQuote(char character, @NotNull Editor editor, int offset) {
        Document document = editor.getDocument();
        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        if (offset > document.getTextLength() || lineStartOffset < 0) {
            return Result.CONTINUE;
        }
        if (offset == document.getTextLength()) {
            editor.getDocument().insertString(offset, getPairedStr(character));
            editor.getCaretModel().moveToOffset(offset + 1);
            return Result.STOP;
        }
        char afterChar = document.getText(new TextRange(offset, offset + 1)).charAt(0);
        if (afterChar == character) {
            editor.getCaretModel().moveToOffset(offset + 1);
            return Result.STOP;
        }
        editor.getDocument().insertString(offset, getPairedStr(character));
        editor.getCaretModel().moveToOffset(offset + 1);
        return Result.STOP;
    }

    /**
     * getPairedStr
     *
     * @param character char
     * @return char
     */
    private static String getPairedStr(final char character) {
        switch (character) {
            case '"':
                return "\"\"";
            case '\'':
                return "''";
            default:
                return "";
        }
    }
}
