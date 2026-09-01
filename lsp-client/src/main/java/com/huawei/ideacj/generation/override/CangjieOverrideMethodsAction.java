/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.generation.override;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.OverrideMethodsHandler;
import com.intellij.codeInsight.generation.actions.PresentableActionHandlerBasedAction;
import com.intellij.lang.CodeInsightActions;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * Cangjie Override Methods Action
 *
 * @since 2025-07-01
 */
public class CangjieOverrideMethodsAction extends PresentableActionHandlerBasedAction implements DumbAware {
    @Override
    @NotNull
    protected CodeInsightActionHandler getHandler() {
        return new OverrideMethodsHandler();
    }

    @Override
    @NotNull
    protected LanguageExtension<LanguageCodeInsightActionHandler> getLanguageExtension() {
        return CodeInsightActions.OVERRIDE_METHOD;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            super.update(event);
            return;
        }
        String extension = file.getExtension();
        if (extension == null) {
            super.update(event);
            return;
        }
        boolean isCangjieFile = "cj".equals(extension);
        if (!isCangjieFile) {
            super.update(event);
        } else {
            event.getPresentation().setText("Override Functions...");
        }
    }
}

