/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import com.intellij.ui.PopupHandler;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTree;

/**
 * create call hierarchy browser window
 *
 * @since 2021-04-26
 */
public class CangjieCallHierarchyBrowser extends CallHierarchyBrowserBase {
    private PsiElement leafElement;

    /**
     * Instantiates a new Cpp call hierarchy browser.
     *
     * @param project     the project
     * @param method      the method
     * @param leafElement the leaf element
     */
    public CangjieCallHierarchyBrowser(@NotNull Project project, @NotNull PsiElement method, PsiElement leafElement) {
        super(project, method);
        this.leafElement = leafElement;
    }

    @Nullable
    @Override
    protected PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
        return null;
    }

    @Override
    protected void createTrees(@NotNull Map<? super String, ? super JTree> trees) {
        ActionManager manager = ActionManager.getInstance();
        AnAction groupCallAction = manager.getAction(IdeActions.GROUP_CALL_HIERARCHY_POPUP);
        if (!(groupCallAction instanceof ActionGroup)) {
            return;
        }

        ActionGroup group = (ActionGroup) groupCallAction;
        final JTree tree1 = createTree(false);
        PopupHandler.installPopupMenu(tree1, group, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP);
        final BaseOnThisMethodAction baseOnThisMethodAction =
            new BaseOnThisMethodAction();
        AnAction callAction = manager.getAction(IdeActions.ACTION_CALL_HIERARCHY);
        baseOnThisMethodAction.registerCustomShortcutSet(callAction.getShortcutSet(), tree1);
        trees.put(getCalleeType(), tree1);

        final JTree tree2 = createTree(false);
        PopupHandler.installPopupMenu(tree2, group, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP);
        baseOnThisMethodAction.registerCustomShortcutSet(callAction.getShortcutSet(), tree2);
        trees.put(getCallerType(), tree2);
    }

    @Override
    protected boolean isApplicableElement(@NotNull PsiElement element) {
        return true;
    }

    @Nullable
    @Override
    protected Comparator<NodeDescriptor<?>> getComparator() {
        return AlphaComparator.getInstance();
    }

    @Nullable
    @Override
    protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String type,
        @NotNull PsiElement psiElement) {
        if (getCallerType().equals(type)) {
            return new CangjieCallerHierarchyTreeStructure(myProject, psiElement, this.leafElement);
        } else if (getCalleeType().equals(type)) {
            return new CangjieCalleeHierarchyTreeStructure(myProject, psiElement, this.leafElement);
        } else {
            return null;
        }
    }

    @Override
    protected void prependActions(@NotNull DefaultActionGroup actionGroup) {
        actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.caller.methods.hierarchy"),
                IdeBundle.message("action.caller.methods.hierarchy"),
                AllIcons.Hierarchy.Supertypes, getCallerType()));
        actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.callee.methods.hierarchy"),
                IdeBundle.message("action.callee.methods.hierarchy"),
                AllIcons.Hierarchy.Subtypes, getCalleeType()));
    }

    private final class ChangeViewTypeActionBase extends ToggleAction {
        @Nls
        private final String myTypeName;

        private ChangeViewTypeActionBase(@NlsActions.ActionText String shortDescription,
                                         @NlsActions.ActionDescription String longDescription,
                                         Icon icon,
                                         @Nls String typeName) {
            super(shortDescription, longDescription, icon);
            myTypeName = typeName;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
            return myTypeName.equals(getCurrentViewType());
        }

        @Override
        @NotNull
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean flag) {
            if (flag) {
                // invokeLater is called to update state of button before long tree building operation
                ApplicationManager.getApplication().invokeLater(() -> changeView(myTypeName));
            }
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            super.update(event);
            setEnabled(true);
        }
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
