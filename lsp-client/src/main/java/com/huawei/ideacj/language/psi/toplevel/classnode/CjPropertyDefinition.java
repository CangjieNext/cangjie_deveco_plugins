/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * PropertyDefinition
 *
 * @since 2021-07-31
 */
public class CjPropertyDefinition extends CangjieBaseNode implements CangjieGetID {
    private String propName = "";

    public CjPropertyDefinition(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @return List<PsiElement>
     */
    @Override
    @Nullable
    public String getName() {
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                return childElement.getText();
            }
        }
        return propName;
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_PROPERTY;
    }
}
