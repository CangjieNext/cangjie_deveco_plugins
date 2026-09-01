/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieUsageInfo
 *
 * @since 2022/10/28
 */
public class CangjieUsageInfo extends UsageInfo {
    public CangjieUsageInfo(@NotNull PsiReference reference) {
        super(reference);
    }

    /**
     * Returns the text range of the usage relative to the start of the file.
     * fixed: when text at end of file, return the range from start but length reduce one
     *
     * @return {@link Segment}
     */
    @Override
    public Segment getNavigationRange() {
        Segment range = super.getNavigationRange();
        PsiFile file = getFile();
        if (range == null || file == null) {
            return range;
        }

        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
        if (document != null && range.getEndOffset() == document.getTextLength()) {
            return TextRange.from(range.getStartOffset(), range.getEndOffset() - range.getStartOffset() - 1);
        }
        return range;
    }
}
