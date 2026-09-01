/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.dialog;

import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.ConfirmationDialog;

/**
 * 移除未使用import提示窗操作类
 *
 * @since 2025-12-11
 */
public class CjRemoveImportConfirmDialog {
    private static final String OK_ACTION_NAME = "OK";

    private static final String CANCEL_ACTION_NAME = "Cancel";

    private static final String NOT_SHOW_AGAIN_MESSAGE = "Don't ask again";

    /**
     * 移除未使用import提示窗
     */
    static class CustomConfirmDialog extends ConfirmationDialog {
        public CustomConfirmDialog(Project project, String removeImport) {
            super(project, CangjieBundle.message("lsp.remove.unused.import.confirm.message", removeImport),
                    CangjieBundle.message("lsp.remove.unused.import.confirm.title", removeImport),
                    null, new CangjieConfirmationOption(), OK_ACTION_NAME, CANCEL_ACTION_NAME);
        }

        @Override
        protected boolean isToBeShown() {
            return super.isToBeShown();
        }
    }

    /**
     * 创建提示窗，返回用户选择
     *
     * @param project project
     * @param removeImport removeImport
     * @return 取消该次行为 or 继续执行
     */
    public static boolean createConfirmDialog(Project project, String removeImport) {
        CustomConfirmDialog dialog = new CustomConfirmDialog(project, removeImport);
        dialog.setDoNotShowAgainMessage(NOT_SHOW_AGAIN_MESSAGE);
        if (dialog.isToBeShown()) {
            dialog.show();
            return dialog.isOK();
        }
        return true;
    }
}
