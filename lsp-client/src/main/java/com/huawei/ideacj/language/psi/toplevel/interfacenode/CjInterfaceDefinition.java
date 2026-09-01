/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.interfacenode;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.toplevel.CjTypeParameters;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import javax.swing.Icon;

/**
 * Variabledeclaration
 *
 * @since 2021-09-08
 */
public class CjInterfaceDefinition extends CangjieBaseNode implements CangjieGetID {
    public CjInterfaceDefinition(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return CjTypeParameters
     *
     * @return String
     */
    public String getCjTypeParameters() {
        int count = PsiTreeUtil.countChildrenOfType(this, CjTypeParameters.class);
        if (count == 0) {
            return "";
        }
        CjTypeParameters cjTypeParameters = PsiTreeUtil.getRequiredChildOfType(this, CjTypeParameters.class);
        return cjTypeParameters.getText();
    }

    /**
     * Return identifier psi element
     *
     * @return PsiElement
     */
    @Override
    public PsiElement getIdentifier() {
        return PsiTreeUtil.getChildOfType(this, CjIdentifier.class);
    }

    /**
     * Return Element Icon
     *
     * @param flags int
     * @return Icon
     */
    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_INTERFACE;
    }

    @Override
    @Nullable
    public TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.INTERFACE);
    }
}
