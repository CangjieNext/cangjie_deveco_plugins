/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import static com.huawei.deveco.cjquality.constants.CommonConstants.MAC_PATH_SEPARATOR;
import static com.huawei.deveco.cjquality.constants.CommonConstants.PATH_SEPARATOR;

import com.huawei.deveco.sdkmanager.core.util.StringUtil;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Cangjie compile arg class
 *
 * @since 2023-01-18
 */
public class CangjieCompileArg {
    /**
     * runtime path
     */
    public static final String RUNTIMEPATH = "build-tools/runtime/lib/windows_x86_64_cjnative";

    /**
     * mac x86 runtime path
     */
    public static final String MAC_X86_RUNTIME_PATH = "build-tools/runtime/lib/darwin_x86_64_cjnative";

    /**
     * mac aarch64 runtime path
     */
    public static final String MAC_AARCH64_RUNTIME_PATH = "build-tools/runtime/lib/darwin_aarch64_cjnative";

    /**
     * mac tools lib path
     */
    public static final String MAC_TOOLS_LIB_PATH = "build-tools/tools/lib";

    /**
     * Linux runtime path
     */
    public static final String LINUX_RUNTIME_PATH = "build-tools/runtime/lib/linux_ohos_aarch64_cjnative";

    /**
     * llvm path
     */
    public static final String THRIDPARTLLVMBINPATH = "build-tools/third_party/llvm/bin";

    /**
     * third path
     */
    public static final String THRIDPARTLLVMLIBPATH = "build-tools/third_party/llvm/lib";

    /**
     * cj build-tools path
     */
    public static final String CJCPATH = "build-tools/bin";

    /**
     * tool path
     */
    public static final String TOOLSPATH = "build-tools/tools/bin";

    /**
     * debugger path
     */
    public static final String DEBUGGERPATH = "build-tools/debugger/bin";

    /**
     * preview cj lib path
     */
    public static final String PREVIEWCJLIB = "common/bin/module/cj_sdk_libraries";

    /**
     * previewer lib path
     */
    public static final String PREVIEWERLIB = "common/bin";

    /**
     * mingw path
     */
    public static final String MINGW64 = "mingw64/bin";

    /**
     * cj sdk path
     * -- GETTER --
     * get cj sdk path
     */
    @Getter
    private static String cjSdkPath = "";

    /**
     * previewer sdk path
     */
    private static String previewerSdkPath = "";

    /**
     * init cangjie sdk path
     *
     * @param project project
     */
    public static void initCjSdkPath(Project project) {
        Optional<String> cangjieSdkOptional = PathUtils.getCangjieSdkPath(project);
        cjSdkPath = cangjieSdkOptional.orElse(StringUtil.EMPTY);
        previewerSdkPath = PathUtils.getPrePath(project);
    }

    /**
     * get cjc env
     *
     * @return string
     */
    public static String getCjcEnvs() {
        String cjcEnvs = "";
        String separator;
        if (SystemInfo.isWindows) {
            separator = PATH_SEPARATOR;
        } else {
            separator = MAC_PATH_SEPARATOR;
        }
        cjcEnvs = cjcEnvs.concat(getRuntimePath()) + separator;
        cjcEnvs = cjcEnvs.concat(getCjcpath()) + separator;
        cjcEnvs = cjcEnvs.concat(getToolspath()) + separator;
        cjcEnvs = cjcEnvs.concat(getCjOhosDllPath()) + separator;
        cjcEnvs = cjcEnvs.concat(getPreviewCjLibRoot()) + separator;
        cjcEnvs = cjcEnvs.concat(getPreviewerLibPath()) + separator;
        cjcEnvs = cjcEnvs.concat(getMingw64Path()) + separator;
        cjcEnvs = cjcEnvs.concat(getDebuggerpath()) + separator;
        cjcEnvs = cjcEnvs.concat(getThirdTimeBinPath()) + separator;
        cjcEnvs = cjcEnvs.concat(getThirdTimeLibPath()) + separator;
        return cjcEnvs;
    }

    public static String getRuntimePath() {
        if (SystemInfo.isWindows) {
            return String.valueOf(Path.of(cjSdkPath, RUNTIMEPATH));
        } else if (SystemInfo.isMac) {
            return Path.of(cjSdkPath, MAC_X86_RUNTIME_PATH) + ":" + Path.of(cjSdkPath, MAC_AARCH64_RUNTIME_PATH) + ":"
                + Path.of(cjSdkPath, MAC_TOOLS_LIB_PATH);
        } else {
            return String.valueOf(Path.of(cjSdkPath, LINUX_RUNTIME_PATH));
        }
    }

    private static String getCjcpath() {
        return String.valueOf(Path.of(cjSdkPath, CJCPATH));
    }

    private static String getToolspath() {
        return String.valueOf(Path.of(cjSdkPath, TOOLSPATH));
    }

    private static String getDebuggerpath() {
        return String.valueOf(Path.of(cjSdkPath, DEBUGGERPATH));
    }

    private static String getPreviewCjLibRoot() {
        return String.valueOf(Path.of(previewerSdkPath, PREVIEWCJLIB));
    }

    private static String getPreviewerLibPath() {
        return String.valueOf(Path.of(previewerSdkPath, PREVIEWERLIB));
    }

    private static String getThirdTimeBinPath() {
        return String.valueOf(Path.of(cjSdkPath, THRIDPARTLLVMBINPATH));
    }

    /**
     * get third time lib path
     *
     * @return string
     */
    public static String getThirdTimeLibPath() {
        return String.valueOf(Path.of(cjSdkPath, THRIDPARTLLVMLIBPATH));
    }

    private static String getMingw64Path() {
        return String.valueOf(Path.of(cjSdkPath, MINGW64));
    }

    private static String getCjOhosDllPath() {
        return String.valueOf(Path.of(cjSdkPath, "build", "ohos"));
    }
}