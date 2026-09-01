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
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

/**
 * TypeAlias
 *
 * @since 2022-02-15
 */
public class CjItemAfterQuest extends CJPsiNode implements CangjieGetID {
    public CjItemAfterQuest(@NotNull ASTNode node) {
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

        PsiElement[] elements = this.getParent().getChildren();
        for (PsiElement childElement : elements) {
            if (childElement.getFirstChild() != null
                    && CjPsiUtils.INSTANCE.isAimString(childElement.getFirstChild(), "'('")) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
            }
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }
}
