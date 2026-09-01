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
public class CjAtomicExpression extends CJPsiNode implements CangjieGetID {
    public CjAtomicExpression(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return need HighLight color
     *
     * @return TextAttributesKey
     */
    @Override
    public TextAttributesKey getColor() {
        if (this.getParent() == null) {
            return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
        }

        boolean curEleValid = this.getNextSibling() != null && this.getNextSibling().getText().equals(".");
        boolean parentEleValid = this.getParent().getNextSibling() != null
                && this.getParent().getNextSibling().getText().equals(".");
        if (curEleValid || parentEleValid) {
            return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
        }
        PsiElement[] pElements = this.getParent().getChildren();
        for (PsiElement childElement : pElements) {
            if (childElement instanceof CjLambdaExpression) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
            }
        }

        if (this.getParent().getParent() == null) {
            return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
        }

        PsiElement[] elements = this.getParent().getParent().getChildren();
        for (PsiElement childElement : elements) {
            if (childElement.getFirstChild() != null
                    && CjPsiUtils.INSTANCE.isAimString(childElement.getFirstChild(), "'('")) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
            }
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }

    @Override
    public String getName() {
        if (!Character.isLetter(this.getText().charAt(0))) {
            return "";
        }
        if (this.getText().contains(" ") || this.getText().contains(";") || this.getText().contains("\n")) {
            return CommonUtils.subLetterFromString(this.getText()).trim();
        }
        return this.getText().trim();
    }

    @Override
    @Nullable
    public Icon getIcon(int flag) {
        return CangjieIcons.CANGJIE_FUNCTION;
    }
}
