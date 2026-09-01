/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;

/**
 * LogNotification
 *
 * @since 2022-3-21
 */
public class NotificationUtil {
    /**
     * show notification, title is about this plugin
     *
     * @param message message
     * @param title title
     * @param type NotifyType
     * @param project project
     */
    public static void showWithProject(String message, String title, Type type, Project project) {
        if (!StringUtils.isEmpty(message)) {
            show(message, title, type, project);
        }
    }

    private static void show(String message, String title, Type type, Project project) {
        NotificationType nType = (type == Type.ERROR) ? NotificationType.ERROR
                : (type == Type.WARN) ? NotificationType.WARNING
                : (type == Type.INFO) ? NotificationType.INFORMATION : NotificationType.IDE_UPDATE;
        Notification notification = new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID,
                title, message, nType);
        Notifications.Bus.notify(notification, project);
    }

    /**
     * NotifyType
     *
     * @since 2022-3-21
     */
    public enum Type {
        INFO,
        WARN,
        ERROR
    }
}