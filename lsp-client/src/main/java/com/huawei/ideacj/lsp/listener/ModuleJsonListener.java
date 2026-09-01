/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import com.huawei.ideacj.lsp.utils.PathConstants;
import com.huawei.ideacj.notification.CangjieEditorNotificationProvider;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.ui.EditorNotifications;

import org.jetbrains.annotations.NotNull;

/**
 * ModuleJson Listener
 *
 * @since 2023-12-28
 */
public class ModuleJsonListener implements PsiTreeChangeListener {
    @Override
    public void beforeChildAddition(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforeChildRemoval(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        PsiFile psiFile = event.getFile();
        if (psiFile == null || !PathConstants.CJPM_FILE.equals(psiFile.getName())) {
            return;
        }
        Project project = psiFile.getProject();
        CangjieEditorNotificationProvider.setIsFileChange(true);
        CangjieEditorNotificationProvider.setChangedFile(psiFile.getVirtualFile());
        EditorNotifications.getInstance(project).updateAllNotifications();
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {

    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {

    }
}
