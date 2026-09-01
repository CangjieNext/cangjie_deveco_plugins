/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieFindUsagesProvider
 *
 * @since 2022/10/25
 */
public class CangjieFindUsagesProvider implements FindUsagesProvider {
    /**
     * words scanner
     *
     * @return {@link WordsScanner}
     */
    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return null;
    }

    /**
     * find usages enable
     *
     * @param psiElement psiElement
     * @return boolean
     */
    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof PsiNamedElement;
    }

    /**
     * get help id
     *
     * @param psiElement psiElement
     * @return {@link String}
     */
    @Nullable
    @NonNls
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return "";
    }

    /**
     * get type
     *
     * @param element element
     * @return {@link String}
     */
    @Nls

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        return "";
    }

    /**
     * get descriptive name
     *
     * @param element element
     * @return {@link String}
     */
    @Nls
    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        String text = element.getText();
        return text == null ? "" : text;
    }

    /**
     * get node text
     *
     * @param element     element
     * @param useFullName useFullName
     * @return {@link String}
     */
    @Nls
    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        String text = element.getText();
        return text == null ? "" : text;
    }
}
