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
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.lsp.utils.CommonUtils;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import javax.swing.Icon;

/**
 * TypeAlias
 *
 * @since 2022-02-15
 */
public class CjPostfixExpression extends CJPsiNode implements CangjieGetID {
    public CjPostfixExpression(@NotNull ASTNode node) {
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
            if (childElement instanceof CjCallSuffix) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
            }
        }
        if (this.getParent() instanceof CjPostfixExpression) {
            for (PsiElement childElement : this.getParent().getChildren()) {
                if (childElement instanceof CjCallSuffix) {
                    return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
                }
            }
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }

    @Override
    public String getName() {
        if (!Character.isUnicodeIdentifierStart(this.getText().charAt(0))) {
            return "";
        }
        if (this.getText().contains(" ") || this.getText().contains(";")
                || this.getText().contains("\n") || this.getText().contains("(")) {
            return CommonUtils.subLetterFromString(this.getText()).trim();
        }
        return this.getText().trim();
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        PsiElement child = this.getFirstChild();
        if (child == null) {
            return CangjieIcons.CANGJIE_FUNCTION;
        }
        PsiElement next = CjPsiUtils.getValidNextToken(child);
        if (next instanceof CjCallSuffix) {
            PsiElement gChild = child.getFirstChild();
            if (gChild == null) {
                return CangjieIcons.CANGJIE_FUNCTION;
            }
            PsiElement gNext = CjPsiUtils.getValidNextToken(gChild);
            if (gNext == null) {
                return CangjieIcons.CANGJIE_FUNCTION;
            }
            if (gChild.getLastChild() instanceof CjLambdaExpression) {
                return CangjieIcons.CANGJIE_FUNCTION;
            }
            if (gNext.getText().equals(".")) {
                return CangjieIcons.CANGJIE_VARIABLE;
            }
            return CangjieIcons.CANGJIE_FUNCTION;
        }
        if (next != null && next.getText().equals(".")) {
            if (CjPsiUtils.getValidNextToken(this) instanceof CjCallSuffix) {
                return CangjieIcons.CANGJIE_FUNCTION;
            }
            return CangjieIcons.CANGJIE_VARIABLE;
        }
        return CangjieIcons.CANGJIE_FUNCTION;
    }
}
