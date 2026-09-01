/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type Cangjie file import reference.
 *
 * @since 2025-08-21
 */
public class CangjiePackageImportReference extends PsiReferenceBase<PsiElement> {
    private final PsiElement targetElement;

    /**
     * Instantiates a new Cangjie file import reference.
     *
     * @param element the file
     * @param textRange the text range
     * @param targetElement the target element
     */
    public CangjiePackageImportReference(@NotNull PsiElement element, @NotNull TextRange textRange,
        @NotNull PsiElement targetElement) {
        super(element, textRange);
        this.targetElement = targetElement;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return targetElement;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element.equals(targetElement);
    }

    @Override
    public Object @NotNull [] getVariants() {
        return EMPTY_ARRAY;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return getElement();
    }
}