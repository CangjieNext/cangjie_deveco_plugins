/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.bracematcher.selection;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.lang.Language;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

/**
 * 双击高亮CjBraceSelectionHandler功能如果有多个注册点满足条件就会使用第一个
 * 这里实现filter功能阻止双击高亮WordSelectioner生效(比如选中括号只高亮括号)
 * WordSelectioner：这是 IntelliJ 的通用处理器，它非常“贪婪”。当你双击 { 时，它会返回该花括号本身的 TextRange（长度为
 * 1）
 *
 * @since 2026-01-19
 */
public class CjBraceSelectionHandlerFilter implements Condition<PsiElement> {
    @Override
    public boolean value(@NotNull PsiElement element) {
        Language language = element.getLanguage();
        if (!(language.isKindOf(CangJieLanguage.INSTANCE))) {
            return true;
        }
        String text = element.getText();
        return !"{".equals(text) && !"}".equals(text);
    }
}