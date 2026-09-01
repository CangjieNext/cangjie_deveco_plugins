/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.language.psi.CangjieNamedElement;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassMemberDeclaration
 *
 * @since 2021-07-31
 */
public class CjClassMemberDeclaration extends CangjieNamedElement {
    public CjClassMemberDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return Psi of CjClassPrimaryInit
     *
     * @return List<CjClassPrimaryInit>
     */
    public List<CjClassPrimaryInit> getPrimaryInitMember() {
        List<CjClassPrimaryInit> primaryInits = new ArrayList<>();
        PsiElement element = this.getFirstChild();
        if (element instanceof CjClassPrimaryInit) {
            primaryInits.add((CjClassPrimaryInit) element);
        }
        return primaryInits;
    }
}
