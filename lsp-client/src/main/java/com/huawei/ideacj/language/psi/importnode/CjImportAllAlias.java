/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.importnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PackageHeader
 *
 * @since 2021-06-24
 */
public class CjImportAllAlias extends CJPsiNode implements CangjieGetID {
    public CjImportAllAlias(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @param myElement PsiElement
     * @return List<PsiElement>
     */
    @Override
    public List<PsiElement> getIDPsi(PsiElement myElement) {
        List<PsiElement> psiElements = new ArrayList<>();
        PsiElement[] children = this.getChildren();
        for (PsiElement child : children) {
            if (!CjPsiUtils.INSTANCE.isAimString(child, "'as'")) {
                psiElements.add(child);
            }
        }
        return psiElements;
    }
}
