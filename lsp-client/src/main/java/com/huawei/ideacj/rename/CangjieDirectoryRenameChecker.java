/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import com.huawei.ace.refactor.rename.FileOrDirectoryRenameChecker;

import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The type Cangjie file or directory rename checker.
 *
 * @since 2025-08-21
 */
public class CangjieDirectoryRenameChecker implements FileOrDirectoryRenameChecker {
    @Override
    public boolean canProcessElement(@NotNull PsiElement psiElement) {
        Optional<CangjieRenameDirectoryInfo> pathOptional =
            CangjieDirectoryRenameProcessor.getCangjieModuleDirectory(psiElement);
        return pathOptional.isPresent();
    }
}
