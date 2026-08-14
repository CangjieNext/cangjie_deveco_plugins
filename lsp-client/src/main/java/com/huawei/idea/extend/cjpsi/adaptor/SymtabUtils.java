/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.extend.cjpsi.adaptor;

import com.huawei.idea.extend.cjpsi.adaptor.psi.ScopeNode;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;

import java.util.Optional;

/**
 * The type Symtab utils.
 *
 * @since 2024/03/20
 */
public class SymtabUtils {
    /**
     * Gets context for.
     *
     * @param element the element
     * @return the context for
     */
    public static Optional<ScopeNode> getContextFor(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        if (parent instanceof ScopeNode) {
            return Optional.of((ScopeNode) parent);
        }
        if (parent instanceof PsiErrorElement) {
            return Optional.empty();
        }
        PsiElement context = parent.getContext();
        if (context instanceof ScopeNode) {
            return Optional.of((ScopeNode) context);
        } else {
            return Optional.empty();
        }
    }
}
