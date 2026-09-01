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
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SuperInterfaces
 *
 * @since 2021-07-31
 */
public class CjSuperInterfaces extends CJPsiNode {
    public CjSuperInterfaces(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @return PsiElement
     */
    public List<PsiElement> getIDPsi() {
        List<PsiElement> psiElements = new ArrayList<>();
        List<CjInterfaceType> element = PsiTreeUtil.getChildrenOfTypeAsList(this, CjInterfaceType.class);
        for (CjInterfaceType e : element) {
            PsiElement identifier = e.getIDPsi();
            if (identifier != null) {
                psiElements.add(identifier);
            }
        }
        return psiElements;
    }
}
