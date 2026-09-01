/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;

import com.intellij.ide.util.DeleteNameDescriptionLocation;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.NonCodeSearchDescriptionLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieElementDescriptionProvider
 *
 * @since 2025/8/18
 */
public class CangjieElementDescriptionProvider implements ElementDescriptionProvider {
    @Override
    @Nullable
    public String getElementDescription(@NotNull PsiElement psiElement, @NotNull ElementDescriptionLocation location) {
        if (!isInCangjieFile(psiElement)) {
            return null;
        }
        if (location == DeleteNameDescriptionLocation.INSTANCE
                || location instanceof NonCodeSearchDescriptionLocation) {
            if (psiElement.getText().equals("~")) {
                return "~init";
            }
            if (psiElement instanceof CangJiePsiFileRoot cjFileRoot) {
                return cjFileRoot.getVirtualFile().getName();
            }
            return psiElement.getText();
        }
        return null;
    }

    private static boolean isInCangjieFile(@NotNull PsiElement element) {
        return element.getContainingFile() instanceof CangJiePsiFileRoot;
    }
}
