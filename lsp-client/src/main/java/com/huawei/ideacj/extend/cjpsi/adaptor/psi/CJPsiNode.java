/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.psi;

import com.huawei.ideacj.extend.cjpsi.adaptor.SymtabUtils;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The type Cj psi node.
 *
 * @since 2024/03/20
 */
public class CJPsiNode extends ASTWrapperPsiElement {
    /**
     * Instantiates a new Antlr psi node.
     *
     * @param node the node
     */
    public CJPsiNode(@NotNull ASTNode node) {
        super(node);
    }

    /** For some reason, default impl of this only returns rule refs
     *  (composite nodes in jetbrains speak) but we want ALL children.
     *  Well, we don't want hidden channel stuff.
     *
     * @return PsiElement[]
     */
    @Override
    @NotNull
    public PsiElement[] getChildren() {
        return Trees.getChildren(this);
    }

    /** For this internal PSI node, look upward for our enclosing scope.
     *  Start looking for a scope at our parent node so getContext()
     *  returns the enclosing scope (context) when this is a ScopeNode.
     *
     *  From the return to scope node, you typically look for a declaration
     *  by looking at its children.
     *
     * @return ScopeNode
     */
    @Override
    public ScopeNode getContext() {
        Optional<ScopeNode> nodeOptional = SymtabUtils.getContextFor(this);
        if (nodeOptional.isEmpty()) {
            return null;
        } else {
            return nodeOptional.get();
        }
    }
}
