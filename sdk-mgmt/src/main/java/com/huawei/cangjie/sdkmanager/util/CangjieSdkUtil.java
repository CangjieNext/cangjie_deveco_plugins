/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.sdkmanager.util;

import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_COMPATIBLE_SDK_KEY;
import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.UNKNOWN_VERSION;
import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_PLUGIN_ID;

import com.huawei.cangjie.sdkmanager.idea.api.CangjieComponent;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieIdeaSdkInfoHandler;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The type Cangjie sdk util.
 *
 * @since 2024-07-06
 */
public class CangjieSdkUtil {
    /**
     * Gets cangjie sdk path.
     *
     * @param isHarmony is harmony
     * @param sdkVersion the sdk version
     * @return the cangjie sdk path
     */
    public static String getCangjieSdkPath(boolean isHarmony, int sdkVersion) {
        return getSdkPathCommon(CangjieComponentPath.CANGJIE, isHarmony, sdkVersion);
    }

    /**
     * Gets cangjie sdk path.
     *
     * @param isHarmony is harmony
     * @param sdkVersion the sdk version
     * @return the cangjie sdk path
     */
    public static String getCangjieSdkPath(boolean isHarmony, String sdkVersion) {
        return getSdkPathCommon(CangjieComponentPath.CANGJIE, isHarmony, sdkVersion);
    }

    /**
     * Gets sdk path common.
     *
     * @param component the component
     * @param isHarmony is harmony
     * @param sdkVersion the sdk version
     * @return the sdk path common
     */
    public static String getSdkPathCommon(CangjieComponentPath component, boolean isHarmony, int sdkVersion) {
        Map<String, CangjieComponent> localSdks = new CangjieIdeaSdkInfoHandler().getLocalSdks(isHarmony, sdkVersion);
        return getComponentPath(component, localSdks);
    }

    /**
     * Gets sdk path common.
     *
     * @param component the component
     * @param isHarmony is harmony
     * @param sdkVersion the sdk version
     * @return the sdk path common
     */
    public static String getSdkPathCommon(CangjieComponentPath component, boolean isHarmony, String sdkVersion) {
        Map<String, CangjieComponent> localSdks = new CangjieIdeaSdkInfoHandler().getLocalSdks(isHarmony, sdkVersion);
        return getComponentPath(component, localSdks);
    }

    /**
     * Obtain installed sdk path string.
     *
     * @param component the component
     * @param isHarmony the is harmony
     * @param sdkVersion the sdk version
     * @return the string
     */
    public static String obtainInstalledSdkPath(CangjieComponentPath component, boolean isHarmony, int sdkVersion) {
        Map<String, CangjieComponent> installedSdkMap = new HashMap<>();
        new CangjieIdeaSdkInfoHandler().getHarmonyComponentMap(isHarmony, sdkVersion, installedSdkMap, false);
        return getComponentPath(component, installedSdkMap);
    }

    /**
     * Gets sdk expect path.
     *
     * @return the sdk expect path
     */
    public static Path getSdkExpectPath() {
        return getSdkDefaultPath().resolve(getCangjiePluginShortVersion()).normalize();
    }

    /**
     * Gets the SDK directory for a plugin version.
     *
     * @param pluginVersion plugin version
     * @return SDK directory for the plugin version
     */
    public static Path getSdkExpectPath(String pluginVersion) {
        return getSdkDefaultPath().resolve(getCangjiePluginShortVersion(pluginVersion)).normalize();
    }

    /**
     * Gets the first three parts of a plugin version.
     *
     * @param pluginVersion plugin version
     * @return shortened plugin version
     */
    public static String getCangjiePluginShortVersion(String pluginVersion) {
        if (StringUtils.isBlank(pluginVersion)) {
            return UNKNOWN_VERSION;
        }
        String[] versionParts = pluginVersion.trim().split("\\.");
        if (versionParts.length <= 3) {
            return pluginVersion.trim();
        }
        return String.join(".", versionParts[0], versionParts[1], versionParts[2]);
    }

    /**
     * Gets sdk default path.
     *
     * @return the sdk default path
     */
    public static Path getSdkDefaultPath() {
        String envPath = System.getenv("DEVECO_CANGJIE_PATH");
        if (StringUtils.isEmpty(envPath)) {
            envPath = SystemInfo.isWindows ? System.getenv("USERPROFILE") : System.getenv("HOME");
        }
        return Paths.get(envPath).resolve(".cangjie-sdk");
    }

    /**
     * validate sdk
     *
     * @param sdkLocation String
     * @return boolean
     */
    public static boolean isValidSdk(String sdkLocation) {
        if (StringUtils.isEmpty(sdkLocation)) {
            return false;
        }
        Path sdkLocationPath = Paths.get(sdkLocation);
        Path uniJsonPath = sdkLocationPath.resolve("uni-package.json");
        Path ohUniJsonPath = sdkLocationPath.resolve("oh-uni-package.json");
        return Files.exists(uniJsonPath) || Files.exists(ohUniJsonPath);
    }

    /**
     * Gets ide sdk config path.
     *
     * @param configKey the config key
     * @return the ide sdk config path
     */
    public static String getIdeSdkConfigPath(String configKey) {
        String sdkLocation = System.getProperty(configKey);
        return StringUtils.isNotBlank(sdkLocation) ? sdkLocation.trim() : StringUtils.EMPTY;
    }

    /**
     * Is config compatible sdk boolean.
     *
     * @return the boolean
     */
    public static boolean isConfigCompatibleSdk() {
        return StringUtils.isNotBlank(System.getProperty(CANGJIE_COMPATIBLE_SDK_KEY));
    }

    /**
     * Gets dev eco short version.
     *
     * @return the dev eco short version
     */
    public static String getCangjiePluginShortVersion() {
        return getCangjiePluginShortVersion(getCangjiePluginVersion());
    }

    /**
     * Gets the installed Cangjie plugin version.
     *
     * @return plugin version, or {@code unknown} when it cannot be obtained
     */
    public static String getCangjiePluginVersion() {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(CANGJIE_PLUGIN_ID));
        if (plugin == null || StringUtils.isBlank(plugin.getVersion())) {
            return UNKNOWN_VERSION;
        }
        return plugin.getVersion().trim();
    }

    /**
     * Stop lsp server.
     *
     * @param projectModel the project model
     */
    public static void stopLspServer(ProjectModel projectModel) {
        stopLspServer(projectModel.getProject());
    }

    /**
     * Stops Cangjie LSP servers in all open projects.
     */
    public static void stopLspServers() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            stopLspServer(project);
        }
    }

    private static void stopLspServer(Project project) {
        Set<LanguageServerWrapper> lspWrappers = IntellijLanguageClient.getProjectToLanguageWrappers()
                .get(org.wso2.lsp4intellij.utils.FileUtils.projectToUri(project));
        if (!CollectionUtils.isEmpty(lspWrappers)) {
            for (LanguageServerWrapper lspWrapper : lspWrappers) {
                String serverDefinitionName = lspWrapper.serverDefinition.getClass().getName();
                if (StringUtils.isEmpty(serverDefinitionName) || !(serverDefinitionName.endsWith(
                        "CangjieCommandsServerDefinition"))) {
                    continue;
                }
                if (!ServerStatus.INITIALIZED.equals(lspWrapper.getStatus())) {
                    continue;
                }
                lspWrapper.stop(true);
                break;
            }
        }
    }

    @NotNull
    private static String getComponentPath(CangjieComponentPath component,
        Map<String, CangjieComponent> installedSdkMap) {
        CangjieComponent cangjieComponent = installedSdkMap.get(component.value());
        if (cangjieComponent == null) {
            return StringUtils.EMPTY;
        }
        Path componentLocation = cangjieComponent.getLocation();
        if (componentLocation == null) {
            return StringUtils.EMPTY;
        }
        return componentLocation.toString();
    }
}
