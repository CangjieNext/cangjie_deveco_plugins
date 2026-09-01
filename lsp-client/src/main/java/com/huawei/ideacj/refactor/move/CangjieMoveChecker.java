/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.move;

import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;

import com.huawei.ace.refactor.FileOrDirectoryMoveChecker;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieMoveChecker
 *
 * @since 2025/10/11
 */
public class CangjieMoveChecker implements FileOrDirectoryMoveChecker {
    private static PsiElement[] elements;

    private static PsiElement targetContainer;

    @Override
    public boolean canProcessElement(@NotNull PsiElement[] elements) {
        CangjieMoveChecker.elements = null;
        CangjieMoveChecker.targetContainer = null;
        boolean isInCangjieModule = false;
        // check root element is in cangjie source module
        for (PsiElement element : elements) {
            if (isInsideCangjieSourceModule(element)) {
                isInCangjieModule = true;
                break;
            }
        }
        return isInCangjieModule;
    }

    @Override
    public void recordSrcAndTarget(@NotNull PsiElement[] elements, @Nullable PsiElement targetContainer) {
        CangjieMoveChecker.elements = elements;
        CangjieMoveChecker.targetContainer = targetContainer;
    }

    public static PsiElement[] getElements() {
        return elements;
    }

    public static PsiElement getTargetContainer() {
        return targetContainer;
    }

    private boolean isInsideCangjieSourceModule(PsiElement element) {
        if (element == null) {
            return false;
        }
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        Project project = element.getProject();
        OhosModuleModel moduleModel = getSelectFileOhosModuleModel(project, virtualFile);
        return LspConfigUtils.isCangjieModule(moduleModel);
    }
}
