/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.macronode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.visitor.CangjieBasePsiVisitor;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import javax.swing.Icon;

/**
 * CjMacroTokens
 *
 * @since 2024/10/10
 */
public class CjMacroTokens extends CJPsiNode implements CangjieGetID {
    public CjMacroTokens(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_TYPEALIAS;
    }

    @Override
    public PsiReference getReference() {
        return Optional.of(ReferenceProvidersRegistry.getReferencesFromProviders(this))
                .filter(psiReferences -> psiReferences.length > 0)
                .map(psiReferences -> psiReferences[0])
                .orElse(null);
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof CangjieBasePsiVisitor) {
            ((CangjieBasePsiVisitor) visitor).visitMacroTokens(this);
        } else {
            super.accept(visitor);
        }
    }
}
