/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.enumnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * CjEnumBody
 *
 * @since 2022-02-18
 */
public class CjEnumBody extends CJPsiNode {
    public CjEnumBody(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return Members
     *
     * @return List<ANTLRPsiNode>
     */
    public List<CJPsiNode> getMember() {
        PsiElement[] children = this.getChildren();
        List<CJPsiNode> members = new ArrayList<>();
        for (PsiElement child : children) {
            if ((child instanceof CjEnumCaseBody
                    || child instanceof CjFunctionDefinition
                    || child instanceof CjMacroExpression)
                && child instanceof CJPsiNode) {
                members.add((CJPsiNode) child);
            }
        }
        return members;
    }
}
