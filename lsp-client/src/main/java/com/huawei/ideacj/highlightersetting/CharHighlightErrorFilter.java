/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.highlightersetting;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.psi.PsiErrorElement;

import org.jetbrains.annotations.NotNull;

/**
 *  the HighlightErrorFilter can make all error about ANTLR g4 token wrong error disappear
 *
 * @author t30009182
 * @since 2021-04-13
 */
public class CharHighlightErrorFilter extends HighlightErrorFilter {
    @Override
    public boolean shouldHighlightErrorElement(@NotNull PsiErrorElement element) {
        return element.getContainingFile().getLanguage() != CangJieLanguage.INSTANCE;
    }
}
