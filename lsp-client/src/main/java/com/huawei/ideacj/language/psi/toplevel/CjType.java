/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

/**
 * Type
 *
 * @since 2021-07-31
 */
public class CjType extends CangjieBaseNode implements CangjieGetID {
    public CjType(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.CLASS);
    }
}
