/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.utils.FileUtils;

/**
 * NavigateToRelatedFix
 *
 * @since 2025/10/09
 */
public class NavigateToRelatedFix implements IntentionAction {
    private final Project project;
    private final String uri;
    private final Range range;
    private final String message;

    public NavigateToRelatedFix(Project project, String uri, Range range, String message) {
        this.project = project;
        this.uri = uri;
        this.range = range;
        this.message = message;
    }

    @Override
    @NotNull
    public String getText() {
        return "Navigation to: " + message;
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return "Navigation";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        VirtualFile virtualFile = FileUtils.uriToVfs(FileUtils.sanitizeURI(uri));
        if (virtualFile == null) {
            return;
        }
        new OpenFileDescriptor(
                project, virtualFile, range.getStart().getLine(), range.getStart().getCharacter()
        ).navigate(true);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}