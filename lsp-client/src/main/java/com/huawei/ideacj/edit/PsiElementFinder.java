/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.edit;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.Optional;

/**
 * pair completion in preprocess command
 *
 * @since 2024-07-09
 */
public class PsiElementFinder {
    /**
     * Get the current PsiElement based on the cursor position
     *
     * @param project Current Projects
     * @param editor Current Editor
     * @return The PsiElement at the current cursor position, or null if it cannot be obtained
     */
    public static Optional<PsiElement> findPsiElementAtCaret(Project project, Editor editor) {
        if (project == null || editor == null) {
            return Optional.empty();
        }

        return ReadAction.compute(() -> {
            CaretModel caretModel = editor.getCaretModel();
            int offset = caretModel.getOffset();

            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile == null) {
                return Optional.empty();
            }

            PsiElement element = psiFile.findElementAt(offset - 1);
            return Optional.ofNullable(element);
        });
    }
}
