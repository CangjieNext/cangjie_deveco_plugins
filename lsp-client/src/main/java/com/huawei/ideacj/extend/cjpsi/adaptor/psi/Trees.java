/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.psi;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.RuleIElementType;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.TokenElementType;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Trees.
 *
 * @since 2024/03/20
 */
public class Trees {
    private Trees() {}

    /**
     * From collection of nodes, make a map from the text of the node to the
     * node.
     *
     * @param nodes the nodes
     * @return the map
     */
    public static Map<String, PsiElement> toMap(Collection<? extends PsiElement> nodes) {
        HashMap<String, PsiElement> elementHashMap = new HashMap<>();

        for (PsiElement node : nodes) {
            elementHashMap.put(node.getText(), node);
        }
        return elementHashMap;
    }

    /**
     * Find all token nodes collection.
     *
     * @param element     the t
     * @param ttype the ttype
     * @return the collection
     */
    public static Collection<PsiElement> findAllTokenNodes(PsiElement element, int ttype) {
        return findAllNodes(element, ttype, true);
    }

    /**
     * Find all rule nodes collection.
     *
     * @param element         the t
     * @param ruleIndex the rule index
     * @return the collection
     */
    public static Collection<PsiElement> findAllRuleNodes(PsiElement element, int ruleIndex) {
        return findAllNodes(element, ruleIndex, false);
    }

    /**
     * Find all nodes list.
     *
     * @param element the t
     * @param index the index
     * @param isFindTokens the find tokens
     * @return List<PsiElement> the list
     */
    public static List<PsiElement> findAllNodes(PsiElement element, int index, boolean isFindTokens) {
        List<PsiElement> nodes = new ArrayList<>();
        findAllNodes(element, index, isFindTokens, nodes);
        return nodes;
    }

    /**
     * Find all nodes.
     *
     * @param element          the t
     * @param index      the index
     * @param isFindTokens the find tokens
     * @param nodes      the nodes
     */
    public static void findAllNodes(
            PsiElement element, int index, boolean isFindTokens, List<? super PsiElement> nodes) {
        // check this node (the root) first
        if (isFindTokens && element instanceof LeafPsiElement) {
            LeafPsiElement tnode = (LeafPsiElement) element;
            IElementType elType = tnode.getNode().getElementType();
            if (elType instanceof TokenElementType) {
                if (((TokenElementType) elType).getANTLRTokenType() == index) {
                    nodes.add(element);
                }
            }
        }
        if (!isFindTokens && element instanceof CJPsiNode) {
            CJPsiNode ctx = (CJPsiNode) element;
            IElementType elType = ctx.getNode().getElementType();
            if (elType instanceof RuleIElementType) {
                if (((RuleIElementType) elType).getRuleIndex() == index) {
                    nodes.add(element);
                }
            }
        }
        // check children
        for (PsiElement c : element.getChildren()) {
            findAllNodes(c, index, isFindTokens, nodes);
        }
    }

    /**
     * Gets descendants.
     *
     * @param element the t
     * @return the descendants
     */

    /* * Get all descendents; includes t itself. */
    public static List<PsiElement> getDescendants(PsiElement element) {
        List<PsiElement> nodes = new ArrayList<>();
        nodes.add(element);

        for (PsiElement c : element.getChildren()) {
            nodes.addAll(getDescendants(c));
        }
        return nodes;
    }

    /**
     * Get children psi element [ ].
     *
     * @param psiElement the t
     * @return the psi element [ ]
     */
    /* * Get all non-WS, non-Comment children of t */
    @NotNull
    public static PsiElement[] getChildren(PsiElement psiElement) {
        if (psiElement == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        PsiElement psiChild = psiElement.getFirstChild();
        if (psiChild == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        List<PsiElement> result = new ArrayList<>();
        while (psiChild != null) {
            if (!(psiChild instanceof PsiComment) && !(psiChild instanceof PsiWhiteSpace)) {
                result.add(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
        return PsiUtilCore.toPsiElementArray(result);
    }
}
