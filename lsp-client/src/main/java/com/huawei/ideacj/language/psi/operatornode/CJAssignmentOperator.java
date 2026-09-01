/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.operatornode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CJAssignmentOperator
 *
 * @since 2025/12/16
 */
public class CJAssignmentOperator extends CJPsiNode implements CangjieGetID {
    public CJAssignmentOperator(@NotNull ASTNode node) {
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
        return CjPsiUtils.getIDPsi(this);
    }
}
