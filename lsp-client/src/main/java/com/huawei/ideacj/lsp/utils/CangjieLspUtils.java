/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import static com.huawei.ideacj.lsp.utils.CommonUtils.renameLspBuildPath;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.removeEditorListeners;
import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;
import static org.wso2.lsp4intellij.utils.FileUtils.getAllOpenedEditors;

import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.launcher.CangjieCompileBuildListener;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * The type Cangjie lsp utils.
 *
 * @since 2024 -12-22
 */
public class CangjieLspUtils {
    /**
     * Restart lsp.
     *
     * @param projects the projects
     */
    public static void restartLsp(Set<Project> projects) {
        if (CollectionUtils.isEmpty(projects)) {
            return;
        }
        for (Project project : projects) {
            LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION, FileUtils.projectToUri(project));
            if (lspWrapper == null) {
                continue;
            }
            if (!(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
                continue;
            }
            boolean isStopLsp = false;
            if (ServerStatus.INITIALIZED.equals(lspWrapper.getStatus())) {
                removeEditorListeners(lspWrapper);
                lspWrapper.stop(true);
                List<Editor> allOpenedEditors = getAllOpenedEditors(project);
                allOpenedEditors.forEach(IntellijLanguageClient::editorClosed);
                isStopLsp = true;
            }
            if (isStopLsp) {
                Path lspBuildTempPath = renameLspBuildPath(project);
                CangjieCompileBuildListener.preStartLsp(project);
                ExtendRequestManager finalRequestManager = requestManager;
                LSPThreadPoolManager.pool(() -> {
                    IntellijLanguageClient.initProjectConnections(project);
                    finalRequestManager.setRestarting(false);
                });
                CommonUtils.tryDeleteDirectory(lspBuildTempPath);
            }
        }
    }
}
