/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.core;

import static com.huawei.deveco.cjfmt.utils.FormatUtils.getFormatToolPath;
import static com.huawei.deveco.cjquality.utils.ModuleUtils.hasCangjieModule;
import static com.huawei.deveco.cjquality.utils.PathUtils.getExePath;

import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.sync.SyncRequest;
import com.huawei.deveco.projectmodel.ohos.sync.syncinterface.SyncProject;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.deveco.cjquality.utils.ShellCommand;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * execute cjfmt -h when sync project
 *
 * @since 2024-02-27
 */
public class SyncFormat implements SyncProject {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(SyncFormat.class);

    private static final String REFORMAT_HELP = "-h";

    @Override
    public void syncProject(@NotNull ProjectModel projectModel, @NotNull SyncRequest syncRequest) {
        LOGGER.info("execute format -h when sync project.");
        Project project = projectModel.getProject();
        if (project == null || project.getBasePath() == null) {
            LOGGER.warn("Project is null");
            return;
        }
        boolean hasCangjieModule = hasCangjieModule(project);
        if (!hasCangjieModule) {
            LOGGER.warn("Project not contains cangjie module.");
            return;
        }
        executeFormatHelp(project);
        LOGGER.info("execute format -h end.");
    }

    private void executeFormatHelp(Project project) {
        Optional<String> executePathOptional = getExePath(project);
        if (executePathOptional.isEmpty()) {
            LOGGER.warn("Can't get Cangjie SDK path.");
            return;
        }
        String executePath = executePathOptional.get();
        List<String> processArgs = getFormatToolPath(executePath);
        processArgs.add(REFORMAT_HELP);
        ShellCommand.executeCommand(processArgs, executePath, project);
    }
}
