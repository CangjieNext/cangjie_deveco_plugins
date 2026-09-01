/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * NotificationUtil
 *
 * @since 2024/03/15
 */
public class NotificationUtil {
    private static final Map<Project, Set<Notification>> notifications = new HashMap();

    /**
     * notify info
     *
     * @param message message
     * @param project project
     * @param level level
     * @return msg Notification Message
     */
    public static Notification notifyInfo(String message, Project project, NotificationType level) {
        NotificationGroupManager notificationGroupManager = NotificationGroupManager.getInstance();
        NotificationGroup balloon = notificationGroupManager.getNotificationGroup("Cangjie.lsp.notification");
        Notification msg = balloon.createNotification(message, level);
        msg.notify(project);
        return msg;
    }

    /**
     * notify info
     *
     * @param message message
     * @param project project
     * @param level message level
     * @param action button action
     * @return msg Notification Message
     */
    public static Notification notifyInfo(String message, Project project, NotificationType level, AnAction action) {
        NotificationGroupManager notificationGroupManager = NotificationGroupManager.getInstance();
        NotificationGroup balloon = notificationGroupManager.getNotificationGroup("Cangjie.lsp.notification");
        Notification msg = balloon.createNotification(message, level).addAction(action);
        msg.notify(project);
        return msg;
    }

    /**
     * get open file action
     *
     * @param path target path
     * @return open file action
     */
    public static AnAction getOpenFileAction(String path) {
        if (!new File(path).exists()) {
            return new AnAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                }
            };
        }
        return new AnAction("Open") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
                Project project = e.getProject();
                if (file != null && project != null) {
                    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                    FileEditorManager.getInstance(project).openEditor(descriptor, true);
                }
            }
        };
    }

    /**
     * add notification
     *
     * @param project project
     * @param notification notification
     */
    public static void addNotification(Project project, Notification notification) {
        if (!notifications.containsKey(project)) {
            notifications.put(project, new HashSet<>());
        }
        notifications.get(project).add(notification);
    }

    /**
     * clear target project notification set
     *
     * @param project project
     */
    public static void clearTargetProjectNotification(Project project) {
        if (!notifications.containsKey(project)) {
            return;
        }
        Set<Notification> notificationSet = notifications.get(project);
        for (Notification notification : notificationSet) {
            notification.expire();
        }
        notifications.remove(project);
    }
}
