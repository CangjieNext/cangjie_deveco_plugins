/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.descriptorNavigate;

import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.eclipse.lsp4j.CallHierarchyItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * a descriptor for caller item
 *
 * @since 2021-04-26
 */
public class CangjieCallerHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private boolean isMyBase = false;

    private final CallHierarchyItem fromItem;

    private String nameText;

    private int callTimes = 1;

    /**
     * Instantiates a new Cangjie call hierarchy node descriptor.
     *
     * @param project          the project
     * @param parentDescriptor the parent descriptor
     * @param element          the element
     * @param isBase           the is base
     * @param itemText         the item text
     * @param item             the item
     */
    public CangjieCallerHierarchyNodeDescriptor(@NotNull Project project,
                                                @Nullable HierarchyNodeDescriptor parentDescriptor,
                                                @NotNull PsiElement element, boolean isBase,
                                                String itemText, CallHierarchyItem item) {
        super(project, parentDescriptor, element, isBase);
        this.nameText = itemText;
        this.fromItem = item;
        this.isMyBase = isBase;
    }

    /**
     * Instantiates a new Cangjie caller hierarchy node descriptor.
     *
     * @param project          the project
     * @param parentDescriptor the parent descriptor
     * @param element          the element
     * @param itemText         the item text
     * @param item             the item
     * @param callTimes        the call times
     */
    public CangjieCallerHierarchyNodeDescriptor(@NotNull Project project,
                                                @Nullable HierarchyNodeDescriptor parentDescriptor,
                                                @NotNull PsiElement element, String itemText,
                                                CallHierarchyItem item, int callTimes) {
        super(project, parentDescriptor, element, false);
        this.nameText = itemText;
        this.fromItem = item;
        this.callTimes = callTimes;
    }

    /**
     * get the element
     *
     * @return PsiElement the element
     */
    @Nullable
    public final PsiElement getEnclosingElement() {
        PsiElement element = getPsiElement();
        return element == null ? null : PsiTreeUtil.getNonStrictParentOfType(element, CjFunctionDefinition.class);
    }

    /**
     * get the target element
     *
     * @return PsiElement the element
     */
    public final PsiElement getTargetElement() {
        return getPsiElement();
    }

    /**
     * get the target element
     *
     * @return String get the element name
     */
    public String getNameText() {
        return this.nameText;
    }

    /**
     * setNameText
     *
     * @param nameText nameText
     */
    public void setNameText(String nameText) {
        this.nameText = nameText;
    }

    /**
     * is this descriptor is a base descriptor
     *
     * @return true is base, false is not base
     */
    public Boolean isBase() {
        return this.isMyBase;
    }

    /**
     * Gets from item.
     *
     * @return the from item
     */
    public CallHierarchyItem getFromItem() {
        return this.fromItem;
    }

    @Override
    public final boolean update() {
        final PsiElement enclosingElement = getTargetElement();
        if (enclosingElement == null) {
            return invalidElement();
        }
        myHighlightedText = new CompositeAppearance();
        myName = this.nameText;
        if (StringUtil.isEmpty(myName)) {
            myName = enclosingElement.getText();
        }
        myHighlightedText.getEnding().addText(myName);
        if (this.callTimes > 1) {
            myHighlightedText.getEnding()
                .addText(IdeBundle.message("node.call.hierarchy.N.usages", this.callTimes),
                    HierarchyNodeDescriptor.getUsageCountPrefixAttributes());
        }
        return super.update();
    }

    @Override
    public void navigate(boolean isRequestFocus) {
        PsiElement element = getPsiElement();
        if (element instanceof Navigatable && ((Navigatable) element).canNavigate()) {
            descriptorNavigate(element, isRequestFocus);
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

    public CompositeAppearance getMyHighlightedText() {
        return myHighlightedText;
    }

    /**
     * setMyHighlightedText
     *
     * @param name name
     */
    public void setMyHighlightedText(String name) {
        myHighlightedText = new CompositeAppearance();
        myHighlightedText.getEnding().addText(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CangjieCallerHierarchyNodeDescriptor targetObj)) {
            return false;
        }
        String sourceNameText = this.getNameText();
        String targetNameText = targetObj.getNameText();
        return CangjieCallHierarchyUtils.isSameHierarchyNodeDescriptor(targetObj, targetNameText, this, sourceNameText);
    }

    @Override
    public int hashCode() {
        if (this.getContainingFile() == null || this.getContainingFile().getVirtualFile() == null) {
            return Objects.hash(nameText);
        }
        return Objects.hash(nameText, this.getContainingFile().getVirtualFile().toString());
    }
}
