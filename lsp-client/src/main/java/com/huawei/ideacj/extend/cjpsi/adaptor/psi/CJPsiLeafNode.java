/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.psi;

import com.huawei.ideacj.extend.cjpsi.adaptor.SymtabUtils;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A leaf node you can use as a superclass for your PSI trees.
 * You don't have to use it of course, but it gives you basic
 * simple scoping behavior via getContext().
 * <p>
 * I recommends creating a subclass for identifiers, such as
 * MyLanguageIDNode. To enable rename, find usages, etc... that
 * node will need to implement PsiNamedElement.
 *
 * @since 2024/03/20
 */
public class CJPsiLeafNode extends LeafPsiElement implements PsiNamedElement {
    /**
     * Instantiates a new Antlr psi leaf node.
     *
     * @param type the type
     * @param text the text
     */
    public CJPsiLeafNode(IElementType type, CharSequence text) {
        super(type, text);
    }

    /** We're a leaf node so must start looking at parent node for a scope.
     *  This assumes a reasonable getContext() implementation for your
     *  internal, non-leaf PSI nodes. It's easiest to use {@link CJPsiNode}
     *  subclasses for your internal notes.
     *
     * @return PsiElement
     */
    @Override
    public PsiElement getContext() {
        Optional<ScopeNode> nodeOptional = SymtabUtils.getContextFor(this);
        if (nodeOptional.isEmpty()) {
            return super.getContext();
        } else {
            return nodeOptional.get();
        }
    }

    @Override
    public PsiElement setName(@NlsSafe @NotNull String s) throws IncorrectOperationException {
        return null;
    }
}
