/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * CangjieFindUsagesHandlerFactory
 *
 * @since 2022/10/19
 */
public class CangjieFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    private static final Set<String> FILE_EXTENSIONS = Set.of(LanguageManager.CANGJIE_EXTENSION);

    @Override
    public boolean canFindUsages(@NotNull final PsiElement element) {
        String extension = "";
        if (element.getContainingFile() != null && element.getContainingFile().getVirtualFile() != null) {
            extension = element.getContainingFile().getVirtualFile().getExtension();
        }
        return extension != null && FILE_EXTENSIONS.contains(extension);
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, final boolean forHighlightUsages) {
        return new CangjieFindUsagesHandler(element);
    }
}
