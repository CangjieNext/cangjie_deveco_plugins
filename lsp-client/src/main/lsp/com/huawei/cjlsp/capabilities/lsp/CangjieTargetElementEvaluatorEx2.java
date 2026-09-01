/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.othersnode.CjStringLiteral;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumCaseBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.expressionnode.CjExpression;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjLambdaParam;

import com.intellij.codeInsight.TargetElementEvaluatorEx;
import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

/**
 * CangjieTargetElementEvaluatorEx2
 *
 * @since 2024/11/15
 */
public class CangjieTargetElementEvaluatorEx2 extends TargetElementEvaluatorEx2 implements TargetElementEvaluatorEx {
    private final HashSet<String> validType = new HashSet<>(Arrays.asList("Identifier", "'init'"));

    @Override
    @Nullable
    public PsiElement getNamedElement(@NotNull PsiElement element) {
        if (validType.stream()
                .noneMatch(type -> type.equalsIgnoreCase(element.getNode().getElementType().toString()))) {
            return null;
        }
        PsiElement parent = element;
        if ((parent = PsiTreeUtil.getParentOfType(parent, CangjieNamedElement.class, false)) != null
                && !(parent instanceof PsiFile)) {
            Optional<PsiElement> targetElement = getTargetElement(parent);
            return targetElement.orElse(null);
        }
        return null;
    }

    @Override
    public boolean isIdentifierPart(@NotNull PsiFile element, @NotNull CharSequence text, int offset) {
        return true;
    }

    /**
     * used to show in 'Go to Declaration or Usages' Dialog
     *
     * @param element of current cursor position
     * @return namedElement
     */
    public Optional<PsiElement> getTargetElement(@NotNull PsiElement element) {
        if (element instanceof CjEnumCaseBody) {
            return Optional.ofNullable(PsiTreeUtil.getParentOfType(element, CjEnumDefinition.class, false));
        }
        if (element instanceof CjExpression) {
            return Optional.empty();
        }
        if (element instanceof CjLambdaParam) {
            return Optional.empty();
        }
        return Optional.of(element);
    }

    @Override
    public boolean isAcceptableNamedParent(@NotNull PsiElement parent) {
        // 1. 修改为匹配 @rawfile() 和 @r() 语法场景
        if (isAtRawFileOrAtR(parent)) {
            // 处理配置无效资源无法跳转没有提示信息问题
            return false;
        }
        // StringLiteral 需要标记为非 NamedElement 让底座能搜索到
        if (parent instanceof CjExpression) {
            CjStringLiteral child = PsiTreeUtil.findChildOfAnyType(parent, CjStringLiteral.class);
            return child == null;
        }

        return super.isAcceptableNamedParent(parent);
    }

    private boolean isAtRawFileOrAtR(PsiElement parent) {
        String text = parent.getText();
        // 根据实际 AST 结构，这里可能需要判断具体的 Element 类型或方法名
        return text != null && (text.startsWith("@rawfile") || text.startsWith("@r"));
    }
}
