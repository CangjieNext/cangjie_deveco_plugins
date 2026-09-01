/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.psi;

import com.intellij.lang.DefaultASTFactoryImpl;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieASTFactory
 *
 * @since 2025/8/14
 */
public class CangjieASTFactory extends DefaultASTFactoryImpl {
    @Override
    @NotNull
    public LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
        ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(type.getLanguage());
        if (parserDefinition != null && (parserDefinition.getCommentTokens().contains(type)
                || parserDefinition.getStringLiteralElements().contains(type))) {
            return super.createLeaf(type, text);
        } else {
            return new CJPsiLeafNode(type, text);
        }
    }
}
