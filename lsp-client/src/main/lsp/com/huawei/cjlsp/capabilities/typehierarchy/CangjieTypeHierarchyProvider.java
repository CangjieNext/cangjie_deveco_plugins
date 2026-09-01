/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.typehierarchy;

import com.huawei.cjlsp.capabilities.lsp.CangjieGotoDeclaration;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjUserType;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;

import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type Cangjie type hierarchy provider.
 *
 * @since 2022-01-19
 */
public class CangjieTypeHierarchyProvider implements HierarchyProvider {
    @Nullable
    @Override
    public PsiElement getTarget(@NotNull DataContext dataContext) {
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (editor != null && file != null) {
            element = file.findElementAt(editor.getCaretModel().getOffset());
            if (element == null) {
                return null;
            }
            if ((element instanceof PsiWhiteSpace
                    || "<:".equals(element.getText())
                    || "{".equals(element.getText()))
                    && editor.getCaretModel().getOffset() > 0) {
                element = file.findElementAt(editor.getCaretModel().getOffset() - 1);
            }
            if (element == null
                    || !(element.getParent() instanceof CjIdentifier)
                    && !"<:".equals(element.getText()) && !"{".equals(element.getText())) {
                return null;
            }
            element = new CangjieGotoDeclaration().getDeclarationTarget(element);
        }
        // filter not CjIdentifier
        if (element == null || !(element.getParent() instanceof CjIdentifier)) {
            return null;
        }
        PsiElement aimElement = element.getParent().getParent();
        if (checkSupportElement(aimElement)) {
            return element;
        }
        return null;
    }

    private boolean checkSupportElement(PsiElement aimElement) {
        return (aimElement instanceof CjUserType
                || aimElement instanceof CjInterfaceDefinition
                || aimElement instanceof CjClassDefinition
                || aimElement instanceof CjStructDefinition
                || aimElement instanceof CjEnumDefinition);
    }

    @NotNull
    @Override
    public HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        return new CangjieTypeHierarchyBrowser(target);
    }

    @Override
    public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
        if (hierarchyBrowser instanceof CangjieTypeHierarchyBrowser cangjieBrowser) {
            cangjieBrowser.changeView("Subtypes of {0}");
        }
    }
}
