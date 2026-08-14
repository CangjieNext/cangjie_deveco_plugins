/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor.actions;

import com.huawei.idea.refactor.RefactorBaseAction;
import com.huawei.idea.refactor.introduce.CangjieIntroduceConstHandler;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.refactoring.RefactoringActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IntroduceConstAction
 *
 * @since 2025-07-20
 */
public class IntroduceConstAction extends RefactorBaseAction {
    @Override
    @Nullable
    protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return new CangjieIntroduceConstHandler();
    }

    @Override
    protected boolean enable(@NotNull AnActionEvent event) {
        return isSameLevelPsi(event);
    }
}
