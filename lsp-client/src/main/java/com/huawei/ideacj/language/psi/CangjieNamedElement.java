/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieNamedElement
 *
 * @since 2024/11/15
 */
public class CangjieNamedElement extends CJPsiNode implements PsiNameIdentifierOwner {
    /**
     * Instantiates a new Antlr psi node.
     *
     * @param node the node
     */
    public CangjieNamedElement(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public PsiElement getNameIdentifier() {
        return PsiTreeUtil.findChildOfType(this, CjIdentifier.class);
    }

    @Override
    public PsiElement setName(@NlsSafe @NotNull String name) throws IncorrectOperationException {
        return null;
    }
}
