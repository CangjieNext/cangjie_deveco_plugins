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
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.usage.CangjieUsageUtil;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.navigation.NavigationItemFileStatus;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.VirtualFile;
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

import java.util.Map;
import java.util.Objects;

import javax.swing.Icon;

/**
 * CangjieClassGroupingRule 按类进行Usage分组
 *
 * @since 2025-10-29
 */
public class CangjieClassGroupingRule extends SingleParentUsageGroupingRule {
    private static final Map<Class<? extends CangjieBaseNode>, Icon> ICON_MAP = Map.of(
            CjClassDefinition.class, CangjieIcons.CANGJIE_CLASS,
            CjStructDefinition.class, CangjieIcons.CANGJIE_STRUCT,
            CjInterfaceDefinition.class, CangjieIcons.CANGJIE_INTERFACE,
            CjEnumDefinition.class, CangjieIcons.CANGJIE_ENUM,
            CjExtendDefinition.class, CangjieIcons.CANGJIE_EXTEND,
            CjMacroDefinition.class, CangjieIcons.CANGJIE_MACRO
    );

    @Override
    @Nullable
    protected UsageGroup getParentGroupFor(@NotNull Usage usage, UsageTarget @NotNull [] usageTargets) {
        // 未选中File structure按钮或者usage不是PsiElementUsage
        if (!(usage instanceof PsiElementUsage psiElementUsage)) {
            return null;
        }
        PsiElement psiElement = psiElementUsage.getElement();
        if (psiElement == null || psiElement.getContainingFile() == null) {
            return null;
        }
        CangjieBaseNode classNode = PsiTreeUtil.getParentOfType(psiElement,
                CjClassDefinition.class, CjInterfaceDefinition.class,
                CjStructDefinition.class, CjEnumDefinition.class,
                CjExtendDefinition.class, CjMacroDefinition.class);
        if (classNode == null && psiElement instanceof LSPPsiElement) {
            int startOffset = ((LSPPsiElement) psiElement).start;
            int endOffset = ((LSPPsiElement) psiElement).end;
            PsiElement realPsiElement = PsiTreeUtil.findElementOfClassAtRange(psiElement.getContainingFile(),
                    startOffset, endOffset, PsiElement.class);
            classNode = PsiTreeUtil.getParentOfType(realPsiElement,
                    CjClassDefinition.class, CjInterfaceDefinition.class,
                    CjStructDefinition.class, CjEnumDefinition.class,
                    CjExtendDefinition.class, CjMacroDefinition.class);
        }
        if (classNode == null) {
            return null;
        }
        String text = CangjieUsageUtil.getPresentableText(classNode);
        Icon icon = getIcon(classNode);
        return new ClassUsageGroup(icon, text, classNode);
    }

    @Nullable
    private static Icon getIcon(CangjieBaseNode classNode) {
        return classNode != null ? ICON_MAP.getOrDefault(classNode.getClass(), null) : null;
    }

    private static class ClassUsageGroup implements UsageGroup, DataProvider {
        private final Icon myIcon;

        private final String myName;

        private final SmartPsiElementPointer<PsiElement> myClassPointer;

        private final CangjieNamedElement classPsiElement;

        private final Project myProject;

        /**
         * 构造方法
         *
         * @param icon       图标
         * @param text       标题
         * @param psiElement psi节点
         */
        public ClassUsageGroup(@Nullable Icon icon, @NotNull String text, @NotNull CangjieNamedElement psiElement) {
            myIcon = icon;
            myName = text;
            classPsiElement = psiElement;
            myProject = psiElement.getProject();
            myClassPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(psiElement);
        }

        @Override
        @Nullable
        public Icon getIcon() {
            return myIcon;
        }

        @Override
        @NotNull
        @NlsContexts.ListItem
        public String getPresentableGroupText() {
            return myName;
        }

        @Override
        @Nullable
        public FileStatus getFileStatus() {
            if (myProject.isDisposed()) {
                return null;
            }
            PsiFile file = classPsiElement.getContainingFile();
            return file == null ? null : NavigationItemFileStatus.get(file);
        }

        @Override
        public boolean isValid() {
            return classPsiElement.isValid();
        }

        @Override
        public void navigate(boolean requestFocus) {
            if (!canNavigate()) {
                return;
            }
            PsiElement identifyingElement = classPsiElement.getOriginalElement();
            if (identifyingElement == null) {
                return;
            }
            VirtualFile virtualFile = classPsiElement.getContainingFile().getVirtualFile();
            int startOffset = identifyingElement.getTextRange().getStartOffset();
            Navigatable navigatable = PsiNavigationSupport.getInstance()
                    .createNavigatable(myProject, virtualFile, startOffset);
            navigatable.navigate(requestFocus);
        }

        @Override
        public boolean canNavigateToSource() {
            // 对应页面上的jump to source按钮
            return canNavigate();
        }

        @Override
        public boolean canNavigate() {
            return isValid();
        }

        @Override
        public int compareTo(@NotNull UsageGroup usage) {
            // 影响排序
            return getPresentableGroupText().compareToIgnoreCase(usage.getPresentableGroupText());
        }

        @Override
        public int hashCode() {
            return myName.hashCode();
        }

        @Override
        @Nullable
        public Object getData(@NotNull String dataId) {
            return UsageRuleUtils.getData(dataId, classPsiElement);
        }

        @Override
        public boolean equals(Object object) {
            // 重写equals防止元素重复加载
            if (!(object instanceof ClassUsageGroup group)) {
                return false;
            }
            return Objects.equals(myName, group.myName) && SmartPointerManager.getInstance(myProject)
                    .pointToTheSameElement(myClassPointer, group.myClassPointer);
        }
    }
}
