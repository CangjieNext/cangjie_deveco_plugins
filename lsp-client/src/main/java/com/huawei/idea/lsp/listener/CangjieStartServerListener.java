/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.lsp.listener;

import com.huawei.idea.lsp.launcher.CangjieLspConfiguration;
import com.huawei.idea.lsp.utils.CommonUtils;
import com.huawei.idea.notification.NotificationUtil;
import com.huawei.idea.trace.TraceUtils;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;

import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.StartServerListener;
import org.wso2.lsp4intellij.utils.ApplicationUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CangjieStartServerListener
 *
 * @since 2024/07/09
 */
public class CangjieStartServerListener implements StartServerListener {
    private static final Logger LOG = Logger.getInstance(CangjieStartServerListener.class);
    private static final Map<Project, Integer> PROJECT_CRASH_COUNT = new ConcurrentHashMap<>();
    private static final int RESTART_TIMES = 3;

    private final Project project;

    /**
     * CangjieStartServerListener
     *
     * @param project the project
     */
    public CangjieStartServerListener(Project project) {
        this.project = project;
    }

    /**
     * initProjectCrashCount
     *
     * @param project init project
     */
    public static void initProjectCrashCount(Project project) {
        CangjieStartServerListener.PROJECT_CRASH_COUNT.putIfAbsent(project, 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void finished(boolean isInitializedSuccess, @Nullable String message) {
        ApplicationUtils.pool(() -> {
            if (Strings.isEmpty(message) || (!message.contains("initialized fail") && !message.contains("timeout"))) {
                return;
            }
            PROJECT_CRASH_COUNT.computeIfPresent(this.project, (proj, crashCount) -> {
                // delete cache directory before server restart to clear corrupted state
                deleteLspCaches();
                if (crashCount >= RESTART_TIMES) {
                    TraceUtils.trace(TraceUtils.Action.LSP_START, TraceUtils.Cause.CRASH);
                    return crashCount;
                }
                int newCount = crashCount + 1;
                NotificationUtil.notifyInfo("Cangjie language server try to restart " + newCount + " times",
                        project, NotificationType.WARNING);
                try {
                    CangjieLspConfiguration.addServerDefinitionForClient(project);
                } catch (IOException e) {
                    LOG.error(e);
                }
                return newCount;
            });
        });
    }

    private void deleteLspCaches() {
        Path astCacheDir = Paths.get(Objects.requireNonNull(project.getBasePath()),
            ".idea", ".deveco", "cangjie", ".cache", "astdata");
        CommonUtils.tryDeleteDirectory(astCacheDir);

        Path indexCacheDir = Paths.get(Objects.requireNonNull(project.getBasePath()),
            ".idea", ".deveco", "cangjie", ".cache", "index");
        CommonUtils.tryDeleteDirectory(indexCacheDir);
    }
}