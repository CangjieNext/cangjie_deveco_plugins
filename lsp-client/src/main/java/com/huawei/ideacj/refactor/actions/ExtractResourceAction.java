/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.actions;

import com.huawei.ideacj.refactor.RefactorBaseAction;
import com.huawei.ideacj.refactor.extract.CangjieExtractResourceHandler;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * ExtractResourceAction 硬编码提取为资源
 *
 * @since 2026-03-28
 */
public class ExtractResourceAction extends RefactorBaseAction {
    private CangjieExtractResourceHandler.ResourceInfo myResourceInfo;

    @Override
    @Nullable
    protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return new CangjieExtractResourceHandler(myResourceInfo);
    }

    @Override
    protected boolean enable(@NotNull AnActionEvent event) {
        // 1. 获取基本的上下文环境
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || file == null) {
            return false;
        }

        // 2. 获取当前光标处的元素
        SelectionModel selectionModel = editor.getSelectionModel();
        int offset = selectionModel.hasSelection()
            ? selectionModel.getSelectionStart() : editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return false;
        }

        Optional<CangjieExtractResourceHandler.ResourceInfo> infoOpt
                = CangjieExtractResourceHandler.parsingParameterType(element);

        // 如果 Optional 内部有值，则解包并存储，返回 true；否则返回 false
        if (infoOpt.isPresent()) {
            this.myResourceInfo = infoOpt.get();
            return true;
        }

        return isSameLevelPsi(event);
    }
}
