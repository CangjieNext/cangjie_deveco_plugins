/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.bracematcher;

import com.huawei.ideacj.language.CangJieTypes;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supports highlighting of matched bracket
 *
 * @author s30009628
 * @since 2021-06-08
 */
public class CangjieBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = {new BracePair(CangJieTypes.LPAREN, CangJieTypes.RPAREN, true),
        new BracePair(CangJieTypes.LSQUARE, CangJieTypes.RSQUARE, true),
        new BracePair(CangJieTypes.LINE_STR_EXPR_START, CangJieTypes.RCURL, true),
        new BracePair(CangJieTypes.MULTI_LINE_STR_EXPR_START, CangJieTypes.RCURL, true),
        new BracePair(CangJieTypes.LCURL, CangJieTypes.RCURL, true),
        new BracePair(CangJieTypes.CHARACTER_STR_EXPR_START, CangJieTypes.RCURL, true)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
        @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
