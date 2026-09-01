/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.bracematcher.selection;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 鼠标双击高亮大括号显示大括号里面的内容
 *
 * @since 2026-01-19
 */
public class CjBraceSelectionHandler implements ExtendWordSelectionHandler {
    @Override
    public boolean canSelect(@NotNull PsiElement element) {
        Language language = element.getLanguage();
        if (!(language.isKindOf(CangJieLanguage.INSTANCE))) {
            return false;
        }
        String text = element.getText();
        return "{".equals(text) || "}".equals(text);
    }

    @Override
    public List<TextRange> select(@NotNull PsiElement element, @NotNull CharSequence editorText, int cursorOffset,
            @NotNull Editor editor) {
        // 只处理花括号
        String text = element.getText();
        if (!"{".equals(text) && !"}".equals(text)) {
            return Collections.emptyList();
        }

        int startOffset = element.getTextRange().getStartOffset();
        int endOffset = -1;

        if ("{".equals(text)) {
            // 向下找匹配的 }
            endOffset = findMatchingBrace(editorText, startOffset, true);
        } else {
            // 向上找匹配的 {
            // 注意：如果是点击 }，通常建议返回其对应的整个块范围
            int pairStart = findMatchingBrace(editorText, startOffset, false);
            if (pairStart != -1) {
                return Collections.singletonList(new TextRange(pairStart, startOffset + 1));
            }
        }

        if (endOffset != -1) {
            return Collections.singletonList(new TextRange(startOffset, endOffset));
        }

        return Collections.emptyList();
    }

    private int findMatchingBrace(CharSequence text, int offset, boolean forward) {
        int balance = 0;
        int step = forward ? 1 : -1;
        int i = offset;

        while (i >= 0 && i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                if (forward) {
                    balance++;
                } else {
                    balance--;
                    if (balance == 0) {
                        return i;
                    }
                }
            }
            if (c == '}') {
                if (forward) {
                    balance--;
                    if (balance == 0) {
                        return i + 1;
                    }
                } else {
                    balance++;
                }
            }
            i += step;
        }
        return -1; // 未找到匹配
    }
}