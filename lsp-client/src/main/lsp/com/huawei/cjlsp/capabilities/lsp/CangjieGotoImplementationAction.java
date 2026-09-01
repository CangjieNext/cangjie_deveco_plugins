/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.navigation.GotoImplementationAction;

/**
 * CangjieGotoImplementationAction
 *
 * @since 2024-08-07
 */
public class CangjieGotoImplementationAction extends GotoImplementationAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
            return;
        }
        String extension = file.getExtension();
        if (extension == null) {
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
            return;
        }
        boolean isCangjieFile = "cj".equals(extension);
        event.getPresentation().setVisible(isCangjieFile);
        event.getPresentation().setEnabled(isCangjieFile);
        if (isCangjieFile) {
            super.update(event);
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
