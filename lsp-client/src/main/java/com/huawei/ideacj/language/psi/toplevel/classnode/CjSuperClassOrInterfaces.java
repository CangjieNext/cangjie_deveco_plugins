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

import java.util.ArrayList;
import java.util.List;

/**
 * SuperClassOrInterfaces
 *
 * @since 2021-07-31
 */
public class CjSuperClassOrInterfaces extends CJPsiNode {
    public CjSuperClassOrInterfaces(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @return PsiElement
     */
    public List<PsiElement> getIDPsi() {
        List<PsiElement> psiElements = new ArrayList<>();
        PsiElement[] children = this.getChildren();
        for (PsiElement child : children) {
            if (child instanceof CjSuperClass) {
                PsiElement insert = null;
                if (((CjSuperClass) child).getIDPsi().isPresent()) {
                    insert = ((CjSuperClass) child).getIDPsi().get();
                }
                if (insert == null) {
                    continue;
                }
                psiElements.add(insert);
                continue;
            }
            if (child instanceof CjSuperInterfaces) {
                List<PsiElement> insert = ((CjSuperInterfaces) child).getIDPsi();
                if (insert != null) {
                    psiElements.addAll(insert);
                }
            }
        }
        return psiElements;
    }
}
