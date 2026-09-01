/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.variabledeclaration;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.CangjieNamedElement;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Variabledeclaration
 *
 * @since 2021-09-08
 */
public class CjVariableDeclaration extends CangjieNamedElement implements CangjieGetID {
    public CjVariableDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (!"patternsMaybeIrrefutable".equals(childElement.getNode().getElementType().toString())) {
                continue;
            }
            PsiElement[] elementsOfPatterns = childElement.getChildren();
            for (PsiElement childOfPattern : elementsOfPatterns) {
                if ("varBindingPattern".equals(childOfPattern.getNode().getElementType().toString())) {
                    return childOfPattern.getFirstChild().getText();
                }
            }
        }
        return "";
    }

    /**
     * Return VariableType
     *
     * @return String
     */
    public String getVariableType() {
        return "";
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_VARIABLE;
    }
}
