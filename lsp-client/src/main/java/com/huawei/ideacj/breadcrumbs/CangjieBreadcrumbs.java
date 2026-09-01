/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.breadcrumbs;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.othersnode.CjCallSuffix;
import com.huawei.ideacj.language.psi.othersnode.CjLambdaExpression;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.toplevel.CjTypeAlias;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumCaseBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjLambdaParam;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.ide.ui.UISettings;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

/**
 * Provider Breadcrumbs
 *
 * @since 2024-02-06
 */
public class CangjieBreadcrumbs implements BreadcrumbsProvider {
    private static final Language[] OUR_LANGUAGES = {CangJieLanguage.INSTANCE};

    private final Map typeInfoMap = new ConcurrentHashMap<CJPsiNode, Pair<String, ElementInfo>>();

    private boolean isSuffix = true;

    CangjieBreadcrumbs() {
        addTypeInfo();
    }

    interface ElementInfo {
        /**
         * ElementInfo
         *
         * @param node ANTLRPsiNode
         * @return String ElementInfo
         */
        String getElementInfo(CJPsiNode node);
    }

    @Override
    public Language[] getLanguages() {
        return OUR_LANGUAGES.clone();
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement element) {
        if (element instanceof CjPostfixExpression) {
            PsiElement nextSibling = CjPsiUtils.getValidNextToken(element);
            if (nextSibling != null && nextSibling.getText().equals(".")) {
                isSuffix = false;
            }
            PsiElement firstChild = element.getFirstChild();
            if (firstChild != null) {
                PsiElement next = CjPsiUtils.getValidNextToken(firstChild);
                // e.g: a.b() (identify a[next call suffix] and b[next dot])
                // e.g: aa().bb() (identify aa[next call suffix])
                return next instanceof CjCallSuffix || (isSuffix && next != null && next.getText().equals("."));
            }
            return false;
        }

        if (!typeInfoMap.containsKey(element.getClass())) {
            return false;
        }

        if (element instanceof CJPsiNode cjPsiNode) {
            String name = cjPsiNode.getName();
            return name != null && !name.isEmpty();
        }

        return false;
    }

    @NotNull
    @Override
    public String getElementInfo(@NotNull PsiElement element) {
        isSuffix = true;
        Pair<String, ElementInfo> info = (Pair<String, ElementInfo>) typeInfoMap.get(element.getClass());
        if (info == null) {
            return "";
        }

        if (element instanceof CJPsiNode node) {
            return info.second.getElementInfo(node);
        }

        return "";
    }

    @Nullable
    @Override
    public Icon getElementIcon(@NotNull PsiElement element) {
        return element.getIcon(0);
    }

    @Nullable
    @Override
    public String getElementTooltip(@NotNull PsiElement element) {
        return ((Pair<String, ElementInfo>) typeInfoMap.get(element.getClass())).first;
    }

    @Override
    public boolean isShownByDefault() {
        return !UISettings.getInstance().getShowNavigationBar();
    }

    private void addTypeInfo() {
        ElementInfo normal = PsiElementBase::getName;
        ElementInfo function = (CJPsiNode node) -> node.getName() + "()";
        ElementInfo call = (CJPsiNode node) -> {
            PsiElement child = node.getFirstChild();
            if (child == null) {
                return "";
            }
            PsiElement next = CjPsiUtils.getValidNextToken(child);
            if (next instanceof CjCallSuffix) {
                String suffix = "()";
                PsiElement gChild = child.getFirstChild();
                if (gChild == null) {
                    return node.getName() + suffix;
                }
                PsiElement gNext = CjPsiUtils.getValidNextToken(gChild);
                if (gNext == null) {
                    return node.getName() + suffix;
                }
                if (gNext.getText().equals(".")) {
                    suffix = "";
                }
                if (gChild.getLastChild() instanceof CjLambdaExpression) {
                    suffix = "()";
                }
                return node.getName() + suffix;
            }
            if (next != null && next.getText().equals(".")) {
                String suffix = "";
                if (CjPsiUtils.getValidNextToken(node) instanceof CjCallSuffix) {
                    suffix = "()";
                }
                return node.getLastChild().getText() + suffix;
            }
            return "";
        };
        typeInfoMap.put(CjInterfaceDefinition.class, new Pair<>("interface", normal));
        typeInfoMap.put(CjClassDefinition.class, new Pair<>("class", normal));
        typeInfoMap.put(CjVariableDeclaration.class, new Pair<>("variable", normal));
        typeInfoMap.put(CjStructDefinition.class, new Pair<>("struct", normal));
        typeInfoMap.put(CjMacroDefinition.class, new Pair<>("macro", normal));
        typeInfoMap.put(CjEnumDefinition.class, new Pair<>("enum", normal));
        typeInfoMap.put(CjExtendDefinition.class, new Pair<>("extend", normal));
        typeInfoMap.put(CjFunctionDefinition.class, new Pair<>("function", function));
        typeInfoMap.put(CjClassInit.class, new Pair<>("init", function));
        typeInfoMap.put(CjClassPrimaryInit.class, new Pair<>("primary init", function));
        typeInfoMap.put(CjEnumCaseBody.class, new Pair<>("enum case", normal));
        typeInfoMap.put(CjStructPrimaryInit.class, new Pair<>("primary init", function));
        typeInfoMap.put(CjStructInit.class, new Pair<>("init", function));
        typeInfoMap.put(CjPropertyDefinition.class, new Pair<>("prop", normal));
        typeInfoMap.put(CjMainDefinition.class, new Pair<>("main", function));
        typeInfoMap.put(CjTypeAlias.class, new Pair<>("typeAlias", normal));
        typeInfoMap.put(CjOperatorFunctionDefinition.class, new Pair<>("operator", function));
        typeInfoMap.put(CjLambdaParam.class, new Pair<>("lambda-param", function));
        typeInfoMap.put(CjPostfixExpression.class, new Pair<>("call", call));
    }
}
