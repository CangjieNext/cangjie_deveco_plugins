/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.othersnode;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.resource.CangjieResourceUtil;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TypeAlias
 *
 * @since 2022-02-15
 */
public class CjQuoteToken extends CangjieBaseNode implements CangjieGetID {
    public CjQuoteToken(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @param myElement PsiElement
     * @return List<PsiElement>
     */
    @Override
    public List<PsiElement> getIDPsi(PsiElement myElement) {
        List<PsiElement> aimElement = new ArrayList<>();
        Collections.addAll(aimElement, this.getChildren());
        return aimElement;
    }

    @Override
    @Nullable
    public TextAttributesKey getColor() {
        PsiElement[] children = this.getChildren();
        if (children.length < 1) {
            return null;
        }
        PsiElement child = children[0];
        if (!(child instanceof LeafPsiElement)) {
            return null;
        }
        if (((LeafPsiElement) child).getElementType() != CangJieTypes.IDENTIFIER) {
            return null;
        }
        String input = CangjieResourceUtil.getResourceInput(child, false);
        if (StringUtil.isEmpty(input)) {
            return null;
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }
}
