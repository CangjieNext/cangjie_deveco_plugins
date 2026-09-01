/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.extendnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * CjExtend
 *
 * @since 2021-09-08
 */
public class CjExtendDefinition extends CangjieBaseNode {
    public CjExtendDefinition(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        CJPsiNode element = PsiTreeUtil.getChildOfType(this, CjExtendType.class);
        if (element != null) {
            return element.getName();
        }
        return "";
    }

    @Nullable
    @Override
    public PsiElement getIdentifier() {
        PsiElement extendType = PsiTreeUtil.getChildOfType(this, CjExtendType.class);
        if (extendType != null) {
            return extendType.getFirstChild();
        }
        return null;
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_EXTEND;
    }
}
