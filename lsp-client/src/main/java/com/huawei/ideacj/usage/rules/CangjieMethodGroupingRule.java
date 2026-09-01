/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.usage.rules;

import com.huawei.ace.language.usages.rules.UsageRuleUtils;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.usage.CangjieUsageUtil;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.navigation.NavigationItemFileStatus;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.SingleParentUsageGroupingRule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.psi.LSPPsiElement;

import java.util.Objects;

import javax.swing.Icon;

/**
 * CangjieMethodGroupingRule 按方法进行Usage分组
 *
 * @since 2025-10-29
 */
public class CangjieMethodGroupingRule extends SingleParentUsageGroupingRule {
    @Nullable
    @Override
    protected UsageGroup getParentGroupFor(@NotNull Usage usage, UsageTarget @NotNull [] targets) {
        // 未选中File structure按钮或者usage不是PsiElementUsage
        if (!(usage instanceof PsiElementUsage psiElementUsage)) {
            return null;
        }
        PsiElement psiElement = psiElementUsage.getElement();
        if (psiElement == null || psiElement.getContainingFile() == null) {
            return null;
        }
        // 从structure图标方法里面获取Function和Method的情况
        CangjieBaseNode methodNode = PsiTreeUtil.getParentOfType(psiElement,
                CjFunctionDefinition.class, CjOperatorFunctionDefinition.class,
                CjMainDefinition.class, CjStructInit.class, CjClassInit.class,
                CjClassPrimaryInit.class, CjPropertyDefinition.class);
        if (methodNode == null && psiElement instanceof LSPPsiElement) {
            int startOffset = ((LSPPsiElement) psiElement).start;
            int endOffset = ((LSPPsiElement) psiElement).end;
            PsiElement realPsiElement = PsiTreeUtil.findElementOfClassAtRange(psiElement.getContainingFile(),
                    startOffset, endOffset, PsiElement.class);
            methodNode = PsiTreeUtil.getParentOfType(realPsiElement,
                    CjFunctionDefinition.class, CjOperatorFunctionDefinition.class,
                    CjMainDefinition.class, CjStructInit.class, CjClassInit.class,
                    CjClassPrimaryInit.class, CjPropertyDefinition.class);
        }
        if (methodNode == null) {
            return null;
        }
        String text = CangjieUsageUtil.getPresentableText(methodNode);
        Icon icon = CangjieIcons.CANGJIE_FUNCTION;
        return new MethodUsageGroup(icon, text, methodNode);
    }

    private static class MethodUsageGroup implements UsageGroup, DataProvider {
        private final Icon myIcon;

        private final String myName;

        private final CangjieNamedElement currentPsiElement;

        private final Project myProject;

        private final SmartPsiElementPointer<PsiElement> myMethodPointer;

        /**
         * 构造方法
         *
         * @param methodIcon 图标
         * @param methodText 标题
         * @param psiElement psi节点
         */
        public MethodUsageGroup(@Nullable Icon methodIcon, @Nullable String methodText,
                                @NotNull CangjieNamedElement psiElement) {
            myIcon = methodIcon;
            myName = methodText;
            currentPsiElement = psiElement;
            myProject = psiElement.getProject();
            myMethodPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(psiElement);
        }

        @Override
        @Nullable
        public FileStatus getFileStatus() {
            if (myProject.isDisposed()) {
                return null;
            }
            PsiFile file = currentPsiElement.getContainingFile();
            return file == null ? null : NavigationItemFileStatus.get(file);
        }

        @Override
        public boolean isValid() {
            return currentPsiElement.isValid();
        }

        @Override
        public void navigate(boolean requestFocus) {
            // jump to source按钮
            if (!canNavigate()) {
                return;
            }
            PsiElement identifyingElement = currentPsiElement.getOriginalElement();
            if (identifyingElement == null) {
                return;
            }
            Navigatable navigatable = PsiNavigationSupport.getInstance()
                    .createNavigatable(myProject, currentPsiElement.getContainingFile().getVirtualFile(),
                            identifyingElement.getTextRange().getStartOffset());
            navigatable.navigate(requestFocus);
        }

        @Override
        public boolean canNavigate() {
            return isValid();
        }

        @Override
        public boolean canNavigateToSource() {
            // 对应页面上的jump to source按钮
            return canNavigate();
        }

        @Override
        @Nullable
        public Icon getIcon() {
            return myIcon;
        }

        @Override
        @NlsContexts.ListItem
        @NotNull
        public String getPresentableGroupText() {
            return myName;
        }

        @Override
        @Nullable
        public Object getData(@NotNull String dataId) {
            return UsageRuleUtils.getData(dataId, currentPsiElement);
        }

        @Override
        public int compareTo(@NotNull UsageGroup usageGroup) {
            // 影响排序
            return getPresentableGroupText().compareToIgnoreCase(usageGroup.getPresentableGroupText());
        }

        @Override
        public boolean equals(Object object) {
            // 重写equals防止元素重复加载
            if (!(object instanceof MethodUsageGroup group)) {
                return false;
            }
            return Objects.equals(myName, group.myName) && SmartPointerManager.getInstance(myProject)
                    .pointToTheSameElement(myMethodPointer, group.myMethodPointer);
        }

        @Override
        public int hashCode() {
            return myName.hashCode();
        }
    }
}
