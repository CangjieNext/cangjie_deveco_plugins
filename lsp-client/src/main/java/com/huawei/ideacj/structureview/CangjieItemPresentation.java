/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.structureview;

import static com.huawei.ideacj.usage.CangjieUsageUtil.getClassPrimaryInitString;
import static com.huawei.ideacj.usage.CangjieUsageUtil.getClassStructInitString;
import static com.huawei.ideacj.usage.CangjieUsageUtil.getFuncDefString;
import static com.huawei.ideacj.usage.CangjieUsageUtil.getOperatorFuncDefString;
import static com.huawei.ideacj.usage.CangjieUsageUtil.getStructPrimaryInitString;
import static com.huawei.ideacj.usage.CangjieUsageUtil.getMacroDefString;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.language.psi.othersnode.CjModifier;
import com.huawei.ideacj.language.psi.toplevel.CjType;
import com.huawei.ideacj.language.psi.toplevel.CjTypeAlias;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.Icon;

/**
 * CangjieItemPresentation
 *
 * @since 2024-02-06
 */
public class CangjieItemPresentation implements ItemPresentation {
    // 静态初始化查找表
    private static final Map<Class<?>, Function<Object, String>> PRESENTATION_HANDLERS = new HashMap<>();

    private static final Map<Class<?>, Function<Object, Icon>> MODIFIER_ICON_PROVIDERS = new HashMap<>();

    static {
        PRESENTATION_HANDLERS.put(CangJiePsiFileRoot.class, element ->
                (element instanceof CangJiePsiFileRoot elm) ? elm.getViewProvider().getVirtualFile().getName() : "");

        PRESENTATION_HANDLERS.put(CjClassDefinition.class, element ->
                (element instanceof CjClassDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTATION_HANDLERS.put(CjFunctionDefinition.class, element ->
                (element instanceof CjFunctionDefinition elm) ? getFuncDefString(elm) : "");

        PRESENTATION_HANDLERS.put(CjVariableDeclaration.class, element ->
                (element instanceof CjVariableDeclaration elm) ? getCjVarName(elm) : "");

        PRESENTATION_HANDLERS.put(CjClassPrimaryInit.class, element ->
                (element instanceof CjClassPrimaryInit elm) ? getClassPrimaryInitString(elm) : "");

        PRESENTATION_HANDLERS.put(CjClassInit.class, element ->
                (element instanceof CjClassInit elm) ? getClassStructInitString(elm.getFunctionDefinitionInfo()) : "");

        PRESENTATION_HANDLERS.put(CjStructInit.class, element ->
                (element instanceof CjStructInit elm) ? getClassStructInitString(elm.getFunctionDefinitionInfo()) : "");

        PRESENTATION_HANDLERS.put(CjStructPrimaryInit.class, element ->
                (element instanceof CjStructPrimaryInit elm) ? getStructPrimaryInitString(elm) : "");

        PRESENTATION_HANDLERS.put(CjExtendDefinition.class, element ->
                (element instanceof CjExtendDefinition elm) ? elm.getName() : "");

        PRESENTATION_HANDLERS.put(CjEnumDefinition.class, element ->
                (element instanceof CjEnumDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTATION_HANDLERS.put(CjInterfaceDefinition.class, element ->
                (element instanceof CjInterfaceDefinition elm) ? elm.getName() + elm.getCjTypeParameters() : "");

        PRESENTATION_HANDLERS.put(CjStructDefinition.class, element ->
                (element instanceof CjStructDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTATION_HANDLERS.put(CjPropertyDefinition.class, element ->
                (element instanceof CjPropertyDefinition elm) ? elm.getName() : "");

        PRESENTATION_HANDLERS.put(CjMacroDefinition.class, element ->
                (element instanceof CjMacroDefinition elm) ? getMacroDefString(elm) : "");

        PRESENTATION_HANDLERS.put(CjOperatorFunctionDefinition.class, element ->
                (element instanceof CjOperatorFunctionDefinition elm) ? getOperatorFuncDefString(elm) : "");

        PRESENTATION_HANDLERS.put(CjMainDefinition.class, element ->
                (element instanceof CjMainDefinition elm) ? elm.getName() + "()" : "");

        PRESENTATION_HANDLERS.put(CjTypeAlias.class, element ->
                (element instanceof CjTypeAlias elm) ? elm.getName() : "");
    }

    static {
        Function<Object, Icon> simpleModifierProvider =
                e -> e instanceof PsiElement element ? getModifierIcon(element) : AllIcons.Nodes.PackageLocal;
        MODIFIER_ICON_PROVIDERS.put(CangJiePsiFileRoot.class, e -> null);

        MODIFIER_ICON_PROVIDERS.put(CjExtendDefinition.class, e -> AllIcons.Nodes.PackageLocal);
        MODIFIER_ICON_PROVIDERS.put(CjMacroDefinition.class, e -> AllIcons.Nodes.Public);
        MODIFIER_ICON_PROVIDERS.put(CjMainDefinition.class, e -> AllIcons.Nodes.Public);

        MODIFIER_ICON_PROVIDERS.put(CjInterfaceDefinition.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjClassDefinition.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjFunctionDefinition.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjVariableDeclaration.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjEnumDefinition.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjStructDefinition.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjClassPrimaryInit.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjClassInit.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjStructInit.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjStructPrimaryInit.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjTypeAlias.class, simpleModifierProvider);
        MODIFIER_ICON_PROVIDERS.put(CjOperatorFunctionDefinition.class, simpleModifierProvider);
    }

    /**
     * PsiElement element
     */
    protected final PsiElement element;

    public CangjieItemPresentation(PsiElement element) {
        this.element = element;
    }

    @Nullable
    public String getLocationString() {
        return "";
    }

    /**
     * 返回节点名称
     *
     * @return 节点名称
     */
    @Override
    public String getPresentableText() {
        Function<Object, String> handler = PRESENTATION_HANDLERS.get(element.getClass());
        if (handler != null) {
            return handler.apply(element);
        }

        // 默认处理
        ASTNode node = element.getNode();
        return node.getText();
    }

    /**
     * 返回图标
     *
     * @param isOpen isOpen
     * @return 图标
     */
    @Nullable
    @Override
    public Icon getIcon(boolean isOpen) {
        Icon baseIcon = element.getIcon(0);
        Icon modifierIcon = getModifierIcon();
        if (baseIcon != null && modifierIcon != null) {
            LayeredIcon layeredIcon = new LayeredIcon(2);
            // 基础符图标放在左侧 (x=0)
            layeredIcon.setIcon(baseIcon, 0, 0, 0);
            // 范围图标放在右侧 (x=基础符图标的宽度)
            layeredIcon.setIcon(modifierIcon, 1, baseIcon.getIconWidth(), 0);
            return layeredIcon;
        }
        return baseIcon != null ? baseIcon : modifierIcon;
    }

    private Icon getModifierIcon() {
        return MODIFIER_ICON_PROVIDERS.getOrDefault(element.getClass(),
                e -> AllIcons.Nodes.PackageLocal).apply(element);
    }

    private static Icon getModifierIcon(PsiElement element) {
        if (element == null) {
            return AllIcons.Nodes.PackageLocal;
        }

        PsiElement currentElem = element.getFirstChild();
        if (currentElem instanceof CjModifier) {
            return switch (currentElem.getText()) {
                case "public" -> AllIcons.Nodes.Public;
                case "private" -> AllIcons.Nodes.Private;
                case "protected" -> AllIcons.Nodes.Protected;
                case null, default -> AllIcons.Nodes.PackageLocal;
            };
        } else {
            return getModifierIcon(currentElem);
        }
    }

    private static String getCjVarName(CjVariableDeclaration elm) {
        String varName = "";
        String type = "";
        for (@NotNull PsiElement child : elm.getChildren()) {
            if (child instanceof CjModifier) {
                continue;
            }
            if (child instanceof CJPsiNode && Objects.equals("", varName)) {
                varName = child.getText();
            }
            if (child instanceof CjType) {
                type = child.getText();
            }
        }
        return Objects.equals("", type) ? varName : varName + ": " + type;
    }
}