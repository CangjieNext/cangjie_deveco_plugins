/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.othersnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.List;

/**
 * Mode specifier attached to a data type. Its whole range is colored as a keyword.
 *
 * @since 2026-09-10
 */
public class CjModeSpecifier extends CJPsiNode implements CangjieGetID {
    private static final long serialVersionUID = 1L;

    public CjModeSpecifier(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public List<PsiElement> getIDPsi(PsiElement myElement) {
        return List.of(this);
    }

    @Override
    @Nullable
    public TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.KEYWORD);
    }
}
