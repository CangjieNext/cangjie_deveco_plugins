/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi;

import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieBaseNode
 *
 * @since 2024/05/20
 */
public class CangjieBaseNode extends CangjieNamedElement {
    public CangjieBaseNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                return childElement.getText().trim();
            }
        }
        return "";
    }

    /**
     * Return identifier psi element
     *
     * @return PsiElement
     */
    @Nullable
    public PsiElement getIdentifier() {
        return PsiTreeUtil.getChildOfType(this, CjIdentifier.class);
    }
}
