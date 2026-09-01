/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.highlighting;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 注册: 文件有错误时 目录树飘红
 *
 * @since 2024-01-27
 */
public class CangjieLanguageServiceHighlightingPassFactory
    implements TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
        FileType fileType = file.getFileType();
        if (!CangjieProblemFileHighlightFilter.FILE_ERROR_SUPPORTED_TYPE.contains(fileType)) {
            return null;
        }
        Document document = editor.getDocument();
        return new CangjieLanguageServiceHighlightingPass(file.getProject(), file, editor, document);
    }

    @Override
    public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar,
        @NotNull Project project) {
        registrar.registerTextEditorHighlightingPass(this, null, new int[] {Pass.UPDATE_ALL}, false, -1);
    }
}