/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ideacj.resource.CangjieResourceUtil.MACRO_TOKENS_RESOURCE_REFERENCE_PATTERN;

import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieResourceReferenceContributor
 *
 * @since 2024/10/10
 */
public class CangjieResourceReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(MACRO_TOKENS_RESOURCE_REFERENCE_PATTERN,
                new CangjieResourceReferenceProvider());
    }
}
