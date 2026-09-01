/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.typehierarchy;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;

import com.intellij.icons.AllIcons;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;

import org.eclipse.lsp4j.TypeHierarchyItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import javax.swing.Icon;

/**
 * The type Cangjie type hierarchy node descriptor.
 *
 * @since 2022-01-19
 */
public class CangjieTypeHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private final String nameText;
    private final TypeHierarchyItem fromItem;
    private boolean isMyBase = false;

    /**
     * Instantiates a new Cpp type hierarchy node descriptor.
     *
     * @param parentDescriptor the parent descriptor
     * @param element          the element
     * @param isBase           the is base
     * @param itemText         the item text
     * @param item             the item
     */
    protected CangjieTypeHierarchyNodeDescriptor(
            @Nullable HierarchyNodeDescriptor parentDescriptor, @NotNull PsiElement element, boolean isBase,
            String itemText, TypeHierarchyItem item) {
        super(element.getProject(), parentDescriptor, element, isBase);
        this.nameText = itemText;
        this.fromItem = item;
        this.isMyBase = isBase;
    }

    /**
     * Gets name text.
     *
     * @return the name text
     */
    public String getNameText() {
        return this.nameText;
    }

    /**
     * Gets from item.
     *
     * @return the from item
     */
    public TypeHierarchyItem getFromItem() {
        return this.fromItem;
    }

    /**
     * Is base boolean.
     *
     * @return the boolean
     */
    public boolean isBase() {
        return this.isMyBase;
    }

    /**
     * get the target element
     *
     * @return PsiElement the element
     */
    @Nullable
    public final PsiElement getTargetElement() {
        return getPsiElement();
    }

    @Nullable
    @Override
    protected Icon getIcon(@NotNull PsiElement element) {
        if (element.getParent() == null) {
            return AllIcons.Nodes.Class;
        }
        if (element.getParent().getParent() instanceof CjInterfaceDefinition) {
            return AllIcons.Nodes.Interface;
        }
        if (element.getParent().getParent() instanceof CjStructDefinition) {
            return AllIcons.Nodes.Record;
        }
        return AllIcons.Nodes.Class;
    }

    @Override
    protected Icon getBaseMarkerIcon(@Nullable Icon sourceIcon) {
        return super.getBaseMarkerIcon(sourceIcon);
    }

    @Override
    public boolean update() {
        final PsiElement psiElement = getTargetElement();
        if (psiElement == null) {
            return invalidElement();
        }
        myHighlightedText = new CompositeAppearance();
        if (this.isMyBase) {
            setIcon(getBaseMarkerIcon(getIcon()));
        }
        myName = this.nameText;
        if (psiElement instanceof CJPsiNode) {
            String nodeName = ((CJPsiNode) psiElement).getName();
            if (Objects.isNull(nodeName)) {
                return false;
            }
            myName = nodeName;
        }
        if (Objects.isNull(myName)) {
            myName = psiElement.getText();
        }
        String fileName = psiElement.getContainingFile().getName();
        myHighlightedText.getEnding().addText(myName + " (" + fileName + ")");
        return super.update();
    }

    @Override
    public void navigate(boolean requestFocus) {
        PsiElement element = getPsiElement();
        if (element instanceof Navigatable && ((Navigatable) element).canNavigate()) {
            ((Navigatable) element).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        PsiElement element = getPsiElement();
        return element instanceof Navigatable && ((Navigatable) element).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }
}
