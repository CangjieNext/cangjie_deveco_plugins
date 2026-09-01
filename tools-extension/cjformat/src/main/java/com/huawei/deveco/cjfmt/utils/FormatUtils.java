/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.utils;

import static com.huawei.deveco.cjfmt.utils.FormatConstant.AVAILABLE_LANGUAGE;
import static com.huawei.deveco.cjfmt.utils.FormatConstant.CJFMT_MAC;
import static com.huawei.deveco.cjfmt.utils.FormatConstant.CJFMT_WINDOWS;
import static com.huawei.deveco.cjfmt.utils.FormatConstant.REFORMAT_CODE;
import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

import com.huawei.deveco.cjquality.utils.LanguageProperties;
import com.huawei.deveco.cjquality.utils.NotificationUtil;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Format util class
 *
 * @since 2023-01-17
 */
public class FormatUtils {
    private static final String SUCCESS_MESSAGE = LanguageProperties.message("message.format_success");

    private static final String FAIL_MESSAGE = LanguageProperties.message("message.format_fail");

    /**
     * 检查是否为仓颉文件
     *
     * @param file file
     * @return boolean
     */
    public static boolean checkFile(VirtualFile file) {
        if (!file.exists()) {
            return false;
        }
        return AVAILABLE_LANGUAGE.matcher(file.getName()).matches();
    }

    /**
     * notify result
     *
     * @param isSucceed true means success
     * @param project project obj
     */
    public static void notifyResult(boolean isSucceed, Project project) {
        String message = isSucceed ? SUCCESS_MESSAGE : FAIL_MESSAGE;
        NotificationUtil.showWithProject(message, REFORMAT_CODE, NotificationUtil.Type.INFO, project);
    }

    /**
     * get cjfmt tool
     *
     * @param executePath exec path
     * @return string list
     */
    public static List<String> getFormatToolPath(String executePath) {
        List<String> processArgs = new ArrayList<>();
        if (SystemInfo.isWindows) {
            processArgs.add(executePath + File.separator + CJFMT_WINDOWS);
        } else {
            processArgs.add(executePath + File.separator + CJFMT_MAC);
        }
        return processArgs;
    }

    /**
     * Gets project tools path.
     *
     * @param project the project
     * @return the default tool path
     */
    public static Path getProjectToolsPath(Project project) {
        return Paths.get(project.getBasePath(), ".idea", ".deveco", "cangjie", "tools");
    }


    /**
     * Gets project tools config path.
     *
     * @param project the project
     * @return the project tools config path
     */
    public static Path getCjfmtConfigPath(Project project) {
        return getProjectToolsPath(project).resolve("cjfmt").resolve("cangjie-format.toml");
    }

    /**
     * Add cjfmt config param.
     *
     * @param processArgs the process args
     * @param project the project
     */
    public static void addCjfmtConfigParam(List<String> processArgs, Project project) {
        Path cjfmtConfigPath = FormatUtils.getCjfmtConfigPath(project);
        if (cjfmtConfigPath.toFile().exists()) {
            processArgs.add("-c");
            processArgs.add(toSystemIndependentName(cjfmtConfigPath.toString()));
        }
    }
}
