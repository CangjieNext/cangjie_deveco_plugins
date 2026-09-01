/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

import org.jetbrains.annotations.Nullable;

/**
 * The interface Scope node.
 *
 * @since 2024/03/20
 */
public interface ScopeNode extends PsiElement {
    /**
     * Resolve psi element.
     *
     * @param element the element
     * @return the psi element
     */
    @Nullable
    PsiElement resolve(PsiNamedElement element);

    @Nullable
    @Override // alter return type
    ScopeNode getContext();
}
