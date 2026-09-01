/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

import com.huawei.cangjie.sdkmanager.idea.api.CangjieComponent;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieIdeaSdkInfoHandler;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.deveco.projectmodel.ohos.model.constants.RuntimeOS;
import com.huawei.deveco.sdkmanager.core.api.PathAndApiVersion;
import com.huawei.deveco.sdkmanager.core.api.SdkInfoHandler;
import com.huawei.deveco.sdkmanager.core.domain.Component;
import com.huawei.deveco.sdkmanager.core.util.StringUtil;
import com.huawei.deveco.sdkmanager.hos.common.api.HosPrjSdkType;
import com.huawei.deveco.sdkmanager.hos.common.api.UniSdkInfoHandler;
import com.huawei.deveco.sdkmanager.hos.idea.api.IdeHosPrjSdkHandlerV2;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkHandler;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkType;
import com.huawei.deveco.sdkmanager.ohos.idea.api.IdeaOhPrjSdkInfoHandler;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Path util class
 *
 * @since 2023-01-18
 */
public class PathUtils {
    private static final Logger LOG = Logger.getInstance(PathUtils.class);

    private static final String PREVIEWER = "previewer";

    private static final String BUILD_TOOLS = "build-tools";

    private static final String TOOLS = "tools";

    private static final String BIN = "bin";

    private static final String API = "api";

    private static final String LIB = "lib";

    private static final String LINUX_OHOS_AARCH64_CJNATIVE = "linux_ohos_aarch64_cjnative";

    private static final String WIN_MINGW = "x86_64-w64-mingw32";

    private static final String OHOS = "ohos";

    private static final String MACRO = "macro";

    /**
     * get cangjie sdk path
     *
     * @param project project
     * @return path string
     */
    public static Optional<String> getCangjieSdkPath(Project project) {
        ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
        if (projectModel == null) {
            return Optional.empty();
        }
        String apiVersion = projectModel.getFullCompileSdkVersion().getValue();
        Map<String, CangjieComponent> localSdks = new CangjieIdeaSdkInfoHandler()
            .getLocalSdks(projectModel.getActiveRuntimeOS() == RuntimeOS.HARMONY_OS, apiVersion);
        CangjieComponent cangjieComponent = localSdks.get(CangjieComponentPath.CANGJIE.value());
        if (cangjieComponent != null) {
            return Optional.of(cangjieComponent.getLocation().toString());
        }
        return Optional.empty();
    }

    private static String getPreviewerSdkPath(Project project) {
        ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
        if (projectModel == null) {
            return StringUtil.EMPTY;
        }
        RuntimeOS runtimeOS = projectModel.getActiveRuntimeOS();
        int apiVersion = projectModel.getFullCompileSdkVersion().getMajor();
        Component cangjieComponent;
        if (runtimeOS == RuntimeOS.HARMONY_OS) {
            // get HOS sdk
            IdeHosPrjSdkHandlerV2 ideHosPrjSdkHandlerV2 = new IdeHosPrjSdkHandlerV2();
            UniSdkInfoHandler sdkHandler = ideHosPrjSdkHandlerV2.getSdkHandler(HosPrjSdkType.OPENHARMONY);
            var localComponents = sdkHandler.getLocalSdks(String.valueOf(apiVersion));
            cangjieComponent = localComponents.get(PREVIEWER);
        } else {
            // get OH sdk
            OhPrjSdkHandler ideaOhPrjSdkHandler = new IdeaOhPrjSdkInfoHandler();
            SdkInfoHandler ohSdkInfoHandler = ideaOhPrjSdkHandler.getSdkHandler(OhPrjSdkType.OPENHARMONY);
            Map<PathAndApiVersion, Component> localComponents = ohSdkInfoHandler.getLocalSdks();
            cangjieComponent = localComponents.get(
                    new PathAndApiVersion(PREVIEWER, projectModel.getFullCompileSdkVersion())
            );
        }
        if (cangjieComponent != null) {
            return cangjieComponent.getLocation().toString();
        }
        return StringUtil.EMPTY;
    }

    /**
     * get previewer path
     *
     * @param project project
     * @return path string
     */
    public static String getPrePath(Project project) {
        String compilerPath = getPreviewerSdkPath(project);
        if (StringUtil.isEmpty(compilerPath)) {
            LOG.warn("The path: " + compilerPath + " is invalid");
            return "";
        } else {
            return compilerPath;
        }
    }

    /**
     * get exe path
     *
     * @param project project
     * @return exe path
     */
    public static Optional<String> getExePath(Project project) {
        Optional<String> cangjieCompilerSdkOptional = getCangjieCompilerSdkPath(project);
        if (cangjieCompilerSdkOptional.isEmpty()) {
            return Optional.empty();
        }
        String cangjieSdkPath = cangjieCompilerSdkOptional.get();
        String cjToolPath = cangjieSdkPath + File.separator + TOOLS + File.separator + BIN;
        return Optional.of(toSystemIndependentName(cjToolPath));
    }

    /**
     * get cangjie build-tools path
     *
     * @param project project
     * @return build-tools path
     */
    public static Optional<String> getCangjieCompilerSdkPath(Project project) {
        Optional<String> cangjieSdkOptional = getCangjieSdkPath(project);
        if (cangjieSdkOptional.isEmpty()) {
            return Optional.empty();
        }
        String sdkPath = cangjieSdkOptional.get() + File.separator + BUILD_TOOLS;
        return Optional.of(toSystemIndependentName(sdkPath));
    }

    /**
     * get build-tools path
     *
     * @param project project obj
     * @return string path
     */
    public static Optional<String> getCompilerPath(Project project) {
        Optional<String> sdkPathOptional = getCangjieSdkPath(project);
        return sdkPathOptional.map(s -> s + File.separator + BUILD_TOOLS);
    }

    /**
     * get AARCH64_LIBS path
     *
     * @param cangjieSdkPath cangjie sdk path
     * @return string
     */
    public static String getAarchLibPath(String cangjieSdkPath) {
        return cangjieSdkPath + File.separator + API + File.separator
                + LIB + File.separator + LINUX_OHOS_AARCH64_CJNATIVE + File.separator + OHOS;
    }

    /**
     * get AARCH64_MACRO_LIBS path
     *
     * @param cangjieSdkPath cangjie sdk path
     * @return string
     */
    public static String getAarchMacroLibsPath(String cangjieSdkPath) {
        return cangjieSdkPath + File.separator + API + File.separator + MACRO + File.separator + OHOS;
    }

    /**
     * get X86_64_LIBS path
     *
     * @param cangjieSdkPath cangjie sdk path
     * @return string
     */
    public static String getX86LibsPath(String cangjieSdkPath) {
        return cangjieSdkPath + File.separator + API + File.separator
                + LIB + File.separator + WIN_MINGW + File.separator + OHOS;
    }

    /**
     * get X86_64_MACRO_LIBS path
     *
     * @param cangjieSdkPath cangjie sdk path
     * @return string
     */
    public static String getX86MacroLibsPath(String cangjieSdkPath) {
        return cangjieSdkPath + File.separator + API + File.separator + MACRO + File.separator + OHOS;
    }

    /**
     * get mac AARCH64_MACRO_LIBS path
     *
     * @param cangjieSdkPath cangjie sdk path
     * @return string
     */
    public static String getMacAarchMacroLibPath(String cangjieSdkPath) {
        return cangjieSdkPath + File.separator + API + File.separator + MACRO + File.separator + OHOS;
    }

    /**
     * get project canonicalPath
     *
     * @param project project
     * @return string
     */
    public static Optional<String> getProjectCanonicalPath(Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return Optional.empty();
        }
        try {
            projectPath = new File(projectPath).getCanonicalPath();
        } catch (IOException e) {
            LOG.warn("Can not get project path.");
            return Optional.empty();
        }
        return Optional.of(projectPath);
    }
}
