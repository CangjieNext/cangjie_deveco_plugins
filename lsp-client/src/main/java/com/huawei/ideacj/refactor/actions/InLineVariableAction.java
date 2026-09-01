/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.actions;

import com.huawei.ideacj.refactor.RefactorBaseAction;
import com.huawei.ideacj.refactor.RefactorStrings;
import com.huawei.ideacj.refactor.inline.CangjieInLineVariableHandler;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.refactoring.RefactoringActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InLineVariableAction
 *
 * @since 2026-05-14
 */
public class InLineVariableAction extends RefactorBaseAction {
    InLineVariableAction() {
        title = RefactorStrings.getInstance().inlineVariable;
    }

    @Override
    @Nullable
    protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return new CangjieInLineVariableHandler();
    }

    @Override
    protected boolean enable(@NotNull AnActionEvent event) {
        return true;
    }
}
