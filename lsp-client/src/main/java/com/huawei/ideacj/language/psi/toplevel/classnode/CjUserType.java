/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.toplevel.CjTypeArguments;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

/**
 * UserType
 *
 * @since 2021-07-31
 */
public class CjUserType extends CJPsiNode implements CangjieGetID {
    public CjUserType(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return need HighLight color
     *
     * @return TextAttributesKey
     */
    @Override
    public TextAttributesKey getColor() {
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (childElement instanceof CjTypeArguments) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.CLASS);
            }
            if (CjPsiUtils.INSTANCE.isAimString(childElement, "'.'")) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
            }
        }
        if (this.getParent() == null) {
            return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.CLASS);
        }
        PsiElement[] pElements = this.getParent().getChildren();
        for (PsiElement childElement : pElements) {
            if (CjPsiUtils.INSTANCE.isAimString(childElement, "'.'")) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
            }
        }
        if (this.getParent() != null
                && this.getParent().getParent() != null
                && this.getParent().getParent().getParent() != null) {
            PsiElement[] grandparentElements = this.getParent().getParent().getParent().getChildren();
            for (PsiElement childElement : grandparentElements) {
                if (CjPsiUtils.INSTANCE.isAimString(childElement, "'.'")) {
                    return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
                }
            }
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.CLASS);
    }
}
