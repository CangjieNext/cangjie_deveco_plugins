/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.descriptorNavigate;

import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassName;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructName;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;

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
 * a descriptor for callee item
 *
 * @since 2021-04-26
 */
public class CangjieCalleeHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private int myUsageCount = 1;
    private final CallHierarchyItem toItem;
    private boolean isMyBase = false;
    private String nameText;

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
    public CangjieCalleeHierarchyNodeDescriptor(@NotNull Project project,
                                                @Nullable HierarchyNodeDescriptor parentDescriptor,
                                                @NotNull PsiElement element, boolean isBase,
                                                String itemText, CallHierarchyItem item) {
        super(project, parentDescriptor, element, isBase);
        this.nameText = itemText;
        this.toItem = item;
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
    public CangjieCalleeHierarchyNodeDescriptor(@NotNull Project project,
                                                @Nullable HierarchyNodeDescriptor parentDescriptor,
                                                @NotNull PsiElement element, String itemText,
                                                CallHierarchyItem item, int callTimes) {
        super(project, parentDescriptor, element, false);
        this.nameText = itemText;
        this.toItem = item;
        this.myUsageCount = callTimes;
    }

    /**
     * get the element
     *
     * @return PsiElement the element
     */
    @Nullable
    public final PsiElement getEnclosingElement() {
        PsiElement element = getPsiElement();
        if (element == null) {
            return null;
        }
        if (element.getParent() instanceof CjClassInit) {
            return PsiTreeUtil.getNonStrictParentOfType(element, CjClassInit.class);
        }
        if (element.getParent().getParent() instanceof CjClassName) {
            return PsiTreeUtil.getNonStrictParentOfType(element, CjClassPrimaryInit.class);
        }
        if (element.getParent().getParent() instanceof CjStructName) {
            return PsiTreeUtil.getNonStrictParentOfType(element, CjStructPrimaryInit.class);
        }
        if (element.getParent() instanceof CjStructInit) {
            return PsiTreeUtil.getNonStrictParentOfType(element, CjStructInit.class);
        }
        if (element.getParent().getParent() instanceof CjOperatorFunctionDefinition) {
            return PsiTreeUtil.getNonStrictParentOfType(element, CjOperatorFunctionDefinition.class);
        }
        return PsiTreeUtil.getNonStrictParentOfType(element, CjFunctionDefinition.class);
    }

    /**
     * change element use times
     */
    public final void incrementUsageCount() {
        myUsageCount++;
    }

    /**
     * Gets target element.
     *
     * @return the target element
     */
    public final PsiElement getTargetElement() {
        return getPsiElement();
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
    public CallHierarchyItem getToItem() {
        return this.toItem;
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
     * setNameText
     *
     * @param nameText nameText
     */
    public void setNameText(String nameText) {
        this.nameText = nameText;
    }

    @Override
    public final boolean isValid() {
        return getEnclosingElement() != null;
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
        if (myUsageCount > 1) {
            myHighlightedText.getEnding()
                .addText(IdeBundle.message("node.call.hierarchy.N.usages", myUsageCount),
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
        if (!(obj instanceof CangjieCalleeHierarchyNodeDescriptor targetObj)) {
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
