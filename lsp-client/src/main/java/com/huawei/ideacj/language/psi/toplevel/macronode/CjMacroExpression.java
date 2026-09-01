/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.macronode;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;

/**
 * CjMacroExpression
 *
 * @since 2022-10-13
 */
public class CjMacroExpression extends CangjieBaseNode implements CangjieGetID {
    private static final long serialVersionUID = 1L;

    public CjMacroExpression(@NotNull ASTNode node) {
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
            if (!CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                continue;
            }
            var idText = childElement.getText();
            List<String> keyWordList = Arrays.asList("vjp", "grad", "valWithGrad", "adjointOf");
            if (keyWordList.contains(idText)) {
                return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.KEYWORD);
            }
        }
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.FUNCTION);
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_MACRO;
    }
}
