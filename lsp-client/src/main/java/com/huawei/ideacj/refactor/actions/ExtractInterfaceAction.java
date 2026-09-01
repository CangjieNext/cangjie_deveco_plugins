/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.actions;

import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.refactor.RefactorBaseAction;
import com.huawei.ideacj.refactor.RefactorStrings;
import com.huawei.ideacj.refactor.extract.CangjieExtractInterfaceHandler;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;

import org.jetbrains.annotations.NotNull;

/**
 * ExtractInterfaceAction
 *
 * @since 2026-05-08
 */
public class ExtractInterfaceAction extends RefactorBaseAction {
    ExtractInterfaceAction() {
        title = RefactorStrings.getInstance().extractInterface;
    }

    @Override
    protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return new CangjieExtractInterfaceHandler();
    }

    @Override
    protected boolean enable(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || file == null) {
            return false;
        }

        int offset = editor.getSelectionModel().getSelectionStart();
        if (offset != editor.getSelectionModel().getSelectionEnd()) {
            return true;
        }

        PsiElement elementAtCaret = file.findElementAt(offset);
        return elementAtCaret != null
                && PsiTreeUtil.findFirstParent(elementAtCaret, e -> e instanceof CjFunctionDefinition) == null;
    }
}
