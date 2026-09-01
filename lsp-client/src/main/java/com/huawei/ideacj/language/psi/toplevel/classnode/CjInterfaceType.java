/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InterfaceType
 *
 * @since 2021-07-31
 */
public class CjInterfaceType extends CJPsiNode {
    public CjInterfaceType(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @return PsiElement
     */
    @Nullable
    public PsiElement getIDPsi() {
        PsiElement identifier = this.getFirstChild();
        if (identifier instanceof CjClassType) {
            return ((CjClassType) identifier).getIDPsi();
        }
        return null;
    }
}
