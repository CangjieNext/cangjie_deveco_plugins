/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.actions;

import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;

import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * ActionsUtil
 *
 * @since 2024/11/23
 */
public class ActionsUtil {
    /**
     * check if should new cangjie file button
     *
     * @param event event
     * @return if should new cangjie file button
     */
    public static boolean shouldShowNewCjFileButton(AnActionEvent event) {
        if (event == null) {
            return false;
        }
        Project project = event.getProject();
        VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        OhosModuleModel moduleModel = getSelectFileOhosModuleModel(project, virtualFile);
        return !LspConfigUtils.isInEtsCjLoader(moduleModel, virtualFile);
    }
}
