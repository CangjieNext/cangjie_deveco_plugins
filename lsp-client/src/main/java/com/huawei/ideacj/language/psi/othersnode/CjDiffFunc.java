/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.othersnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CjDiffFunc
 *
 * @since 2022-02-15
 */
public class CjDiffFunc extends CJPsiNode implements CangjieGetID {
    private static final long serialVersionUID = 1168813106028856655L;

    public CjDiffFunc(@NotNull ASTNode node) {
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
        List<PsiElement> aimElement = new ArrayList<>();
        Collections.addAll(aimElement, this.getChildren());
        return aimElement;
    }
}
