/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.functionnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.CangjieNamedElement;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * CjLambdaDefinition
 *
 * @since 2024-05-17
 */
public class CjLambdaDefinition extends CangjieNamedElement implements CangjieGetID {
    public CjLambdaDefinition(@NotNull ASTNode node) {
        super(node);
    }


    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_FUNCTION;
    }

    @Override
    public String getName() {
        String lamdaDefinitionName = "";
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                lamdaDefinitionName = childElement.getText().trim();
            }
        }
        return lamdaDefinitionName;
    }
}
