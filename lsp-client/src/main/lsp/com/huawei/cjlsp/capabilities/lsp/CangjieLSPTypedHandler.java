/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.listeners.LSPTypedHandler;

/**
 * Cangjie LSPTypedHandler
 *
 * @since 2021-11-10
 */
public class CangjieLSPTypedHandler extends LSPTypedHandler {
    @Override
    public Result charTyped(char character, Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return Result.CONTINUE;
    }
}