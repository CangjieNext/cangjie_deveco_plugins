/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.typehierarchy;

import com.intellij.ide.hierarchy.HierarchyBrowserManager;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JTree;

/**
 * The type Cangjie type hierarchy browser.
 *
 * @since 2022-01-19
 */
public class CangjieTypeHierarchyBrowser extends TypeHierarchyBrowserBase {
    /**
     * Instantiates a new Cangjie type hierarchy browser.
     *
     * @param target the target
     */
    public CangjieTypeHierarchyBrowser(@NotNull PsiElement target) {
        super(target.getProject(), target);
    }

    @Override
    protected boolean isInterface(@NotNull PsiElement psiElement) {
        return true;
    }

    @Override
    protected boolean canBeDeleted(PsiElement psiElement) {
        return Objects.nonNull(psiElement);
    }

    @Override
    protected String getQualifiedName(PsiElement psiElement) {
        if (Objects.nonNull(psiElement)) {
            String name = psiElement.getText();
            if (Objects.nonNull(name)) {
                return name;
            }
        }
        return "";
    }

    @Nullable
    @Override
    protected PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
        if (descriptor instanceof CangjieTypeHierarchyNodeDescriptor) {
            return descriptor.getPsiElement();
        }
        return null;
    }

    @Override
    protected void createTrees(@NotNull Map<? super String, ? super JTree> trees) {
        createTreeAndSetupCommonActions(trees, IdeActions.GROUP_TYPE_HIERARCHY_POPUP);
    }

    @Nullable
    @Override
    protected JPanel createLegendPanel() {
        return null;
    }

    @Override
    protected boolean isApplicableElement(@NotNull PsiElement element) {
        return element instanceof PsiElement;
    }

    @Nullable
    @Override
    protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String type,
        @NotNull PsiElement psiElement) {
        switch (type) {
            case "Supertypes of {0}":
                return new CangjieSupertypesHierarchyTreeStructure(psiElement.getProject(), psiElement);
            case "Subtypes of {0}":
                return new CangjieSubtypesHierarchyTreeStructure(psiElement.getProject(), psiElement);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    protected Comparator<NodeDescriptor<?>> getComparator() {
        HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
        if (state == null) {
            throw new IllegalArgumentException("Fail to get Type Hierarchy State");
        }
        if (state.SORT_ALPHABETICALLY) {
            return AlphaComparator.getInstance();
        }
        return Comparator.comparingInt(NodeDescriptor::getIndex);
    }

    @Nullable
    @Override
    protected @NlsContexts.TabTitle
        String getContentDisplayName(@Nls @NotNull String typeName,
                                     @NotNull PsiElement element) {
        var name = element.getText();
        if (name == null) {
            return null;
        }
        return MessageFormat.format(typeName, name);
    }
}
