/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.actions;

import static com.huawei.deveco.cjfmt.utils.FormatUtils.checkFile;

import com.huawei.deveco.cjfmt.core.ReformatCodeService;
import com.huawei.deveco.cjquality.utils.CangjieCompileArg;
import com.huawei.deveco.cjquality.utils.trace.TraceUtils;

import com.intellij.codeInsight.actions.FileInEditorProcessor;
import com.intellij.codeInsight.actions.LayoutCodeDialog;
import com.intellij.codeInsight.actions.ShowReformatFileDialog;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Reformat file action
 *
 * @since 2024-01-17
 */
public class CjReformatFileAction extends ShowReformatFileDialog {
    @NonNls
    private static final String HELP_ID = "editing.codeReformatting";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        long startTime = System.currentTimeMillis();
        DataContext dataContext = event.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (project == null || editor == null) {
            return;
        }
        if ("".equals(CangjieCompileArg.getCjSdkPath())) {
            CangjieCompileArg.initCjSdkPath(project);
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null || file.getVirtualFile() == null) {
            return;
        }

        boolean hasSelection = editor.getSelectionModel().hasSelection();

        if (checkFile(file.getVirtualFile())) {
            FileDocumentManager.getInstance().saveAllDocuments(); // Save all unsaved files
            final ReformatCodeService reformatCodeService = ReformatCodeService.getInstance(project);
            reformatCodeService.reformatFileCode(file, hasSelection, project, editor);
            ActionManager.getInstance().getAction("SynchronizeCurrentFile").actionPerformed(event); // 重新加载格式化后的文件
        } else {
            LayoutCodeDialog dialog = new LayoutCodeDialog(project, file, hasSelection, HELP_ID);
            dialog.show();

            if (dialog.isOK()) {
                new FileInEditorProcessor(file, editor, dialog.getRunOptions()).processCode();
            }
        }
        TraceUtils.trace(TraceUtils.Action.FORMAT, TraceUtils.Cause.DEFAULT,
                -1, System.currentTimeMillis() - startTime);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
