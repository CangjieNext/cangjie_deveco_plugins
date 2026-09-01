/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.usage;

import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
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
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CangjieUsageUtil
 *
 * @since 2025-10-29
 */
public class CangjieUsageUtil {
    private static final Map<Class<? extends CangjieBaseNode>, Function<CangjieBaseNode, String>>
            PRESENTERS = new HashMap<>();

    static {
        PRESENTERS.put(CjClassDefinition.class, element ->
                (element instanceof CjClassDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTERS.put(CjFunctionDefinition.class, element ->
                (element instanceof CjFunctionDefinition elm) ? getFuncDefString(elm) : "");

        PRESENTERS.put(CjClassPrimaryInit.class, element ->
                (element instanceof CjClassPrimaryInit elm) ? getClassPrimaryInitString(elm) : "");

        PRESENTERS.put(CjClassInit.class, element ->
                (element instanceof CjClassInit elm) ? getClassStructInitString(elm.getFunctionDefinitionInfo()) : "");

        PRESENTERS.put(CjStructInit.class, element ->
                (element instanceof CjStructInit elm) ? getClassStructInitString(elm.getFunctionDefinitionInfo()) : "");

        PRESENTERS.put(CjStructPrimaryInit.class, element ->
                (element instanceof CjStructPrimaryInit elm) ? getStructPrimaryInitString(elm) : "");

        PRESENTERS.put(CjExtendDefinition.class, element ->
                (element instanceof CjExtendDefinition elm) ? elm.getName() : "");

        PRESENTERS.put(CjEnumDefinition.class, element ->
                (element instanceof CjEnumDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTERS.put(CjInterfaceDefinition.class, element ->
                (element instanceof CjInterfaceDefinition elm) ? elm.getName() + elm.getCjTypeParameters() : "");

        PRESENTERS.put(CjStructDefinition.class, element ->
                (element instanceof CjStructDefinition elm) ? elm.getName() + CjPsiUtils.getCjTypeParameters(elm) : "");

        PRESENTERS.put(CjPropertyDefinition.class, element ->
                (element instanceof CjPropertyDefinition elm) ? elm.getName() : "");

        PRESENTERS.put(CjMacroDefinition.class, element ->
                (element instanceof CjMacroDefinition elm) ? getMacroDefString(elm) : "");

        PRESENTERS.put(CjOperatorFunctionDefinition.class, element ->
                (element instanceof CjOperatorFunctionDefinition elm) ? getOperatorFuncDefString(elm) : "");

        PRESENTERS.put(CjMainDefinition.class, element ->
                (element instanceof CjMainDefinition elm) ? elm.getName() + "()" : "");

        PRESENTERS.put(CjTypeAlias.class, element ->
                (element instanceof CjTypeAlias elm) ? elm.getName() : "");
    }

    /**
     * 返回节点名称
     *
     * @param element element
     * @return 节点名称
     */
    public static String getPresentableText(CangjieBaseNode element) {
        // 查找对应的处理器
        Function<CangjieBaseNode, String> presenter = PRESENTERS.get(element.getClass());

        if (presenter != null) {
            return presenter.apply(element);
        }

        // 默认处理
        ASTNode node = element.getNode();
        return node.getText();
    }

    /**
     * 返回类初始化方法名称
     *
     * @param cjClassInit cjClass初始化方法
     * @return 初始化方法名
     */
    @NotNull
    public static String getClassStructInitString(FunctionDefinitionInfo cjClassInit) {
        String argsWithType = getArgsWithType(cjClassInit);
        if (StringUtils.isNotBlank(argsWithType)) {
            return "init" + "(" + argsWithType + ")";
        }
        return "init()";
    }

    /**
     * 返回结构体优先初始化名
     *
     * @param cjStructPrimaryInit cjStructPrimaryInit
     * @return 结构体优先初始化名
     */
    @NotNull
    public static String getStructPrimaryInitString(CjStructPrimaryInit cjStructPrimaryInit) {
        FunctionDefinitionInfo funcDefinitionInfo = cjStructPrimaryInit.getFunctionDefinitionInfo();
        String argsWithType = getArgsWithType(funcDefinitionInfo);
        String returnType = funcDefinitionInfo.getReturnType();
        String cjTypeParameters = CjPsiUtils.getCjTypeParameters(cjStructPrimaryInit);
        return buildFunctionSignature(cjStructPrimaryInit.getName(), cjTypeParameters, argsWithType, returnType);
    }

    /**
     * 返回类优先初始化名
     *
     * @param classPrimaryInit classPrimaryInit
     * @return 类优先初始化名
     */
    @NotNull
    public static String getClassPrimaryInitString(CjClassPrimaryInit classPrimaryInit) {
        FunctionDefinitionInfo funcDefinitionInfo = classPrimaryInit.getFunctionDefinitionInfo();
        String argsWithType = getArgsWithType(funcDefinitionInfo);
        String returnType = funcDefinitionInfo.getReturnType();
        String cjTypeParameters = CjPsiUtils.getCjTypeParameters(classPrimaryInit);
        return buildFunctionSignature(classPrimaryInit.getName(), cjTypeParameters, argsWithType, returnType);
    }

    /**
     * 返回方法名
     *
     * @param functionDefinition functionDefinition
     * @return 方法名
     */
    @NotNull
    public static String getFuncDefString(CjFunctionDefinition functionDefinition) {
        FunctionDefinitionInfo funcDefinitionInfo = functionDefinition.getFunctionDefinitionInfo();
        String argsWithType = getArgsWithType(funcDefinitionInfo);
        String returnType = funcDefinitionInfo.getReturnType();
        String cjTypeParameters = CjPsiUtils.getCjTypeParameters(functionDefinition);
        return buildFunctionSignature(funcDefinitionInfo.getName(), cjTypeParameters, argsWithType, returnType);
    }

    /**
     * 返回operator方法名
     *
     * @param functionDefinition functionDefinition
     * @return operator方法名
     */
    @NotNull
    public static String getOperatorFuncDefString(CjOperatorFunctionDefinition functionDefinition) {
        return ReadAction.compute(() -> {
            FunctionDefinitionInfo funcDefinitionInfo = functionDefinition.getFunctionDefinitionInfo();
            String argsWithType = getArgsWithType(funcDefinitionInfo);
            String returnType = funcDefinitionInfo.getReturnType();
            String cjTypeParameters = CjPsiUtils.getCjTypeParameters(functionDefinition);

            return buildFunctionSignature(functionDefinition.getName(), cjTypeParameters, argsWithType, returnType);
        });
    }

    @NotNull
    private static String getArgsWithType(FunctionDefinitionInfo funcDefinitionInfo) {
        LinkedHashMap<String, String> unNamedArgs = funcDefinitionInfo.getUnNamedArgs();
        LinkedHashMap<String, String> namedArgs = funcDefinitionInfo.getNamedArgs();
        return Stream.concat(
                Objects.requireNonNullElse(unNamedArgs, Collections.<String, String>emptyMap())
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue()),
                Objects.requireNonNullElse(namedArgs, Collections.<String, String>emptyMap())
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + "!: " + entry.getValue())
        ).collect(Collectors.joining(", "));
    }

    /**
     * 返回macro的签名
     *
     * @param cjMacroDefinition cjMacroDefinition
     * @return macro的签名
     */
    public static String getMacroDefString(CjMacroDefinition cjMacroDefinition) {
        FunctionDefinitionInfo functionDefinitionInfo = cjMacroDefinition.getFunctionDefinitionInfo();
        String returnType = CjPsiUtils.getMacroReturnType(cjMacroDefinition);
        String argsWithType = "";
        // macro 参数索引始终为3
        int indexOrArgs = 3;
        if (cjMacroDefinition.getChildren().length > indexOrArgs) {
            argsWithType = Optional.of(cjMacroDefinition.getChildren()[indexOrArgs])
                    .map(PsiElement::getNode)
                    .map(ASTNode::getText)
                    .orElse("");
        }
        String returnTypeStr = StringUtils.isEmpty(returnType) ? "" : ": " + returnType;
        return String.format("%s%s%s",
                functionDefinitionInfo.getName(),
                argsWithType,
                returnTypeStr);
    }

    /**
     * 组合方法签名
     *
     * @param funcName 方法名
     * @param cjTypeParameters 泛型类型
     * @param argsWithType 参数名以及参数类型
     * @param returnType 返回类型
     * @return 方法签名
     */
    public static String buildFunctionSignature(String funcName, String cjTypeParameters,
                                                String argsWithType, String returnType) {
        // 处理类型参数部分：如果为空则为空字符串，否则正常显示
        String typeParamsStr = StringUtils.isEmpty(cjTypeParameters) ? "" : cjTypeParameters;
        // 处理返回类型部分：如果为空则为空字符串，否则添加冒号和空格前缀
        String returnTypeStr = StringUtils.isEmpty(returnType) ? "" : ": " + returnType;

        return String.format("%s%s(%s)%s",
                funcName,
                typeParamsStr,
                argsWithType,
                returnTypeStr);
    }
}
