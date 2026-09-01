/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.visitor;

import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

/**
 * CangjieBasePsiVisitor
 *
 * @since 2024/10/21
 */
public class CangjieBasePsiVisitor extends PsiElementVisitor {
    /**
     * visitMacroTokens
     *
     * @param cjElement macroTokens node
     */
    public void visitMacroTokens(CjMacroTokens cjElement) {
        visitChildren(cjElement);
    }

    /**
     * visitChildren
     *
     * @param element target element
     */
    public void visitChildren(PsiElement element) {
        if (element == null) {
            return;
        }
        for (PsiElement child : element.getChildren()) {
            child.accept(this);
        }
    }
}
