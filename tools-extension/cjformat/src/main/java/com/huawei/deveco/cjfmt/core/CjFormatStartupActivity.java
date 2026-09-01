/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.core;

import com.huawei.deveco.cjfmt.settings.CangjieCodeStyleSettings;
import com.huawei.deveco.cjfmt.utils.FormatUtils;
import com.huawei.deveco.cjquality.utils.LogPrinter;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * CjFormatStartupActivity
 *
 * @since 2025-07-11
 */
public class CjFormatStartupActivity implements StartupActivity {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjFormatStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        if (PluginManagerCore.isDisabled(PluginId.getId("com.huawei.cangjie-support-plugin"))) {
            return;
        }
        DumbService.getInstance(project).runWhenSmart(() -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                CodeStyleSettings settings = CodeStyle.getProjectOrDefaultSettings(project);
                CangjieCodeStyleSettings customSettings = settings.getCustomSettings(CangjieCodeStyleSettings.class);
                try {
                    CangjieCodeStyleSettings.writeConfigToFile(customSettings,
                            FormatUtils.getCjfmtConfigPath(project));
                } catch (IOException e) {
                    LOGGER.error("Cangjie cjfmt write codeStyle config file failed when StartupActivity.", e);
                }
            });
        });
    }
}
