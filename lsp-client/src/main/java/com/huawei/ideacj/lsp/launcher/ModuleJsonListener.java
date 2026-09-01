/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.huawei.ideacj.lsp.utils.PathConstants;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;

/**
 * listener of module.json
 *
 * @since 2022-11-5
 */
public class ModuleJsonListener implements BulkFileListener {
    private Project project;

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        events.forEach(event -> {
            if (!(event instanceof VFileContentChangeEvent)) {
                return;
            }
            VirtualFile file = event.getFile();
            this.project = ProjectUtil.guessProjectForFile(file);
            if (project == null) {
                return;
            }
            if (!ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
                return;
            }
            String filePath = event.getFile().getPath();
            String[] fileArray = filePath.split("/");
            String fileName = fileArray[fileArray.length - 1];
            if (!PathConstants.MODULE_JSON.equals(fileName)) {
                return;
            }
            NotificationGroupManager manager = NotificationGroupManager.getInstance();
            NotificationGroup balloon = manager.getNotificationGroup("Cangjie.module.json.notification");
            Notification msg = balloon.createNotification("lsp server restart", NotificationType.INFORMATION);
            msg.addAction(new AnAction("Restart LSPServer", "Restart LSPServer", null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent actionEvent) {
                    IntellijLanguageClient.getProjectToLanguageWrappers().get(FileUtils.projectToUri(project))
                            .forEach(LanguageServerWrapper::restart);
                    msg.hideBalloon();
                }
            });
            msg.addAction(new MyAnAction(msg));
            msg.notify(project);
        });
    }

    @Override
    public String toString() {
        return "ModuleJsonListener{" + "project=" + project + '}';
    }

    private static class MyAnAction extends AnAction {
        private final Notification msg;

        /**
         * Instantiates a new My an action.
         *
         * @param msg the msg
         */
        public MyAnAction(Notification msg) {
            super("Cancel", "Cancel", null);
            this.msg = msg;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent actionEvent) {
            msg.hideBalloon();
        }
    }
}
