/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.terminal;

import static com.huawei.deveco.cjquality.constants.CangjieConstants.CANGJIE_HOME;
import static com.huawei.deveco.cjquality.constants.CmdConstants.MAC_PATH;
import static com.huawei.deveco.cjquality.constants.CmdConstants.WIN_PATH;
import static com.huawei.deveco.cjquality.constants.CommonConstants.MAC_PATH_SEPARATOR;
import static com.huawei.deveco.cjquality.constants.CommonConstants.PATH_SEPARATOR;
import static com.huawei.deveco.cjquality.utils.PathUtils.getCangjieCompilerSdkPath;

import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.cangjie.projectmgmt.utils.SdkUtils;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.hvigor.api.HvigorExtendEnvProvider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.LocalTerminalCustomizer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canjie plugin local terminal customizer
 *
 * @since 2024-07-03
 */
public class CangjiePluginLocalTerminalCustomizer extends LocalTerminalCustomizer {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CangjiePluginLocalTerminalCustomizer.class);

    @Override
    public String[] customizeCommandAndEnvironment(@NotNull Project project, @Nullable String workingDirectory,
        @NotNull String[] command, @NotNull Map<String, String> envs) {
        String path;
        if (SystemInfo.isWindows) {
            path = WIN_PATH;
        } else {
            path = MAC_PATH;
        }
        String separator;
        if (SystemInfo.isWindows) {
            separator = PATH_SEPARATOR;
        } else {
            separator = MAC_PATH_SEPARATOR;
        }
        try {
            ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
            if (projectModel == null) {
                LOGGER.warn("Can't get projectModel.");
                return super.customizeCommandAndEnvironment(project, workingDirectory, command, envs);
            }
            Map<String, String> projectEnvs = CangjieEnvUtils.getProjectEnvs(project);
            if (projectEnvs != null) {
                envs.putAll(projectEnvs);
            }
            String originPath = envs.get(path);
            String cjEnvPath = SdkUtils.getSdkEnvPath(projectModel);
            if (StringUtils.isEmpty(originPath)) {
                envs.put(path, cjEnvPath);
            } else {
                envs.put(path, originPath + separator + cjEnvPath);
            }
            Optional<String> cangjieCompilerPath = getCangjieCompilerSdkPath(project);
            if (cangjieCompilerPath.isEmpty()) {
                LOGGER.warn("Can't get cangjieCompilerPath.");
                return super.customizeCommandAndEnvironment(project, workingDirectory, command, envs);
            }
            envs.put(CANGJIE_HOME, cangjieCompilerPath.get());
            addHvigorExtendEnv(envs);
        } catch (IOException e) {
            LOGGER.warn("Can't get environment variables.");
        }
        return super.customizeCommandAndEnvironment(project, workingDirectory, command, envs);
    }

    private void addHvigorExtendEnv(@NotNull Map<String, String> envs) {
        List<HvigorExtendEnvProvider> envProviderList =
            HvigorExtendEnvProvider.HVIGOR_ENV_PROVIDER_EXTENSION_LIST.getExtensionList();
        if (CollectionUtils.isEmpty(envProviderList)) {
            return;
        }
        for (HvigorExtendEnvProvider envProvider : envProviderList) {
            Map<String, String> extendEnvsMap = envProvider.getExtendEnvs();
            if (extendEnvsMap == null || extendEnvsMap.isEmpty()) {
                continue;
            }
            // 已有环境变量，不做覆盖，只有不存在的环境变量才添加
            extendEnvsMap.forEach(envs::putIfAbsent);
        }
    }
}