/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import com.huawei.cjlsp.capabilities.lsp.CangjieGotoDeclaration;
import com.huawei.ideacj.language.psi.othersnode.CjCallSuffix;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassName;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructName;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * access for call hierarchy in navigate option
 *
 * @since 2024-04-16
 */
public class CangjieCallHierarchyProvider implements HierarchyProvider {
    private PsiElement leafElement;

    @Nullable
    @Override
    public PsiElement getTarget(@NotNull final DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return null;
        }
        // 防御性检查：如果 IDE 正在索引（哑模式），禁止执行任何可能触发索引的操作
        if (DumbService.isDumb(project)) {
            return null;
        }

        leafElement = null;
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (editor != null && file != null) {
            element = ApplicationManager.getApplication().runReadAction((Computable<PsiElement>) () -> {
                int offset = editor.getCaretModel().getOffset();
                PsiElement el = file.findElementAt(offset);

                if (el == null) {
                    return null;
                }

                if ((el instanceof PsiWhiteSpace || "<".equals(el.getText())) && offset > 0) {
                    el = file.findElementAt(offset - 1);
                }

                if (el == null || el instanceof PsiWhiteSpace) {
                    return null;
                }

                return new CangjieGotoDeclaration().getDeclarationTarget(el);
            });
            leafElement = file.findElementAt(editor.getCaretModel().getOffset());
        }
        if (element == null || element.getParent() == null) {
            return null;
        }
        PsiElement aimElement = element.getParent().getParent();
        if ("init".equals(element.getText()) || "super".equals(element.getText()) || isMatchType(aimElement)) {
            return element;
        }
        return null;
    }

    private static boolean isMatchType(PsiElement aimElement) {
        return aimElement instanceof CjFunctionDefinition
                || aimElement instanceof CjOperatorFunctionDefinition
                || (aimElement instanceof CjPostfixExpression
                && aimElement.getParent().getLastChild() instanceof CjCallSuffix)
                || aimElement instanceof CjClassName
                || aimElement instanceof CjStructName;
    }

    @Override
    @NotNull
    public HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        return new CangjieCallHierarchyBrowser(target.getProject(), target, leafElement);
    }

    @Override
    public void browserActivated(@NotNull final HierarchyBrowser hierarchyBrowser) {
        if (hierarchyBrowser instanceof CangjieCallHierarchyBrowser) {
            ((CangjieCallHierarchyBrowser) hierarchyBrowser).changeView(CallHierarchyBrowserBase.getCallerType());
        }
    }
}