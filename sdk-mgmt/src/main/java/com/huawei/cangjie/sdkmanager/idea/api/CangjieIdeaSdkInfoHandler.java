/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.sdkmanager.idea.api;

import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_COMPATIBLE_SDK_KEY;
import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_HARMONY_SDK_KEY;
import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_OPEN_HARMONY_SDK_KEY;
import static com.huawei.cangjie.sdkmanager.idea.constants.CangjieIdeConstants.DEVECO_CANGJIE_PATH;
import static com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil.getCangjiePluginShortVersion;
import static com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil.getSdkDefaultPath;
import static com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil.isValidSdk;

import com.huawei.cangjie.sdkconfig.core.ConfigFactory;
import com.huawei.cangjie.sdkconfig.support.SdkConfig;
import com.huawei.cangjie.sdkmanager.component.CangjieComponentProvider;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil;
import com.huawei.deveco.sdkmanager.core.api.PathAndApiVersion;
import com.huawei.deveco.sdkmanager.core.api.SdkInfoHandler;
import com.huawei.deveco.sdkmanager.core.constants.ComponentPath;
import com.huawei.deveco.sdkmanager.core.domain.ApiVersion;
import com.huawei.deveco.sdkmanager.core.domain.Component;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkHandler;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkType;
import com.huawei.deveco.sdkmanager.ohos.idea.api.IdeaOhPrjSdkInfoHandler;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The type Cangjie idea sdk info handler.
 *
 * @since 2024-7-6
 */
public class CangjieIdeaSdkInfoHandler {
    /**
     * Gets local sdks.
     *
     * @param isHarmony the is harmony
     * @param apiVersion the api version
     * @return the local sdks
     */
    public Map<String, CangjieComponent> getLocalSdks(boolean isHarmony, int apiVersion) {
        Map<String, CangjieComponent> componentMap = new HashMap<>();
        if (isHarmony) {
            getHarmonyComponentMap(isHarmony, apiVersion, componentMap, true);
        } else {
            getOpenHarmonyComponentMap(isHarmony, apiVersion, componentMap);
        }
        addExtendComponents(isHarmony, apiVersion, componentMap);
        return componentMap;
    }

    /**
     * Gets local sdks.
     *
     * @param isHarmony the is harmony
     * @param fullApiVersion full api version
     * @return the local sdks
     */
    public Map<String, CangjieComponent> getLocalSdks(boolean isHarmony, String fullApiVersion) {
        Map<String, CangjieComponent> componentMap = new HashMap<>();
        if (StringUtils.isEmpty(fullApiVersion)) {
            return componentMap;
        }
        int apiVersion = Integer.parseInt(fullApiVersion.split("\\.")[0]);
        if (isHarmony) {
            getHarmonyComponentMap(isHarmony, apiVersion, componentMap, true);
        } else {
            getOpenHarmonyComponentMap(isHarmony, fullApiVersion, componentMap);
        }
        addExtendComponents(isHarmony, apiVersion, componentMap);
        return componentMap;
    }

    /**
     * Gets harmony component map.
     *
     * @param isHarmony the is harmony
     * @param apiVersion the api version
     * @param componentMap the component map
     * @param isObtainSdkPath the is obtain sdk path
     */
    public void getHarmonyComponentMap(boolean isHarmony, int apiVersion,
        Map<String, CangjieComponent> componentMap, boolean isObtainSdkPath) {
        if (isObtainSdkPath) {
            String configSdkLocation = CangjieSdkUtil.getIdeSdkConfigPath(CANGJIE_COMPATIBLE_SDK_KEY);
            if (StringUtils.isBlank(configSdkLocation)) {
                configSdkLocation = CangjieSdkUtil.getIdeSdkConfigPath(CANGJIE_HARMONY_SDK_KEY);
            }
            if (StringUtils.isNotBlank(configSdkLocation)) {
                if (isValidSdk(configSdkLocation)) {
                    addComponent(Paths.get(configSdkLocation), componentMap, isHarmony, String.valueOf(apiVersion));
                }
                return;
            }
        }
        Path cangejieSdkPath = getCangejieSdkPath();
        if (Files.exists(cangejieSdkPath)) {
            addComponent(cangejieSdkPath, componentMap, isHarmony, String.valueOf(apiVersion));
        }
    }

    /**
     * Gets cangejie sdk path.
     *
     * @return the cangejie sdk path
     */
    public Path getCangejieSdkPath() {
        String envPath = System.getenv(DEVECO_CANGJIE_PATH);
        if (StringUtils.isNotBlank(envPath) && isValidSdk(envPath)) {
            return Paths.get(envPath);
        }
        return getCangejieSdkVersionPath().resolve(CangjieComponentPath.CANGJIE.value());
    }

    /**
     * Gets cangejie sdk version path.
     *
     * @return the cangejie sdk version path
     */
    public Path getCangejieSdkVersionPath() {
        return getSdkDefaultPath().resolve(getCangjiePluginShortVersion());
    }

    private void getOpenHarmonyComponentMap(boolean isHarmony, int apiVersion,
        Map<String, CangjieComponent> componentMap) {
        this.getOpenHarmonyComponentMap(isHarmony, String.valueOf(apiVersion), componentMap);
    }

    private void getOpenHarmonyComponentMap(boolean isHarmony, String fullApiVersion,
        Map<String, CangjieComponent> componentMap) {
        int apiVersion = Integer.parseInt(fullApiVersion.split("\\.")[0]);
        String sdkLocation = CangjieSdkUtil.getIdeSdkConfigPath(CANGJIE_OPEN_HARMONY_SDK_KEY);
        if (StringUtils.isNotBlank(sdkLocation)) {
            if (StringUtils.isNotBlank(sdkLocation)) {
                if (isValidSdk(sdkLocation)) {
                    addComponent(Paths.get(sdkLocation), componentMap, isHarmony, String.valueOf(apiVersion));
                }
            }
        } else {
            // get OH sdk
            OhPrjSdkHandler ideaOhPrjSdkHandler = new IdeaOhPrjSdkInfoHandler();
            SdkInfoHandler ohSdkInfoHandler = ideaOhPrjSdkHandler.getSdkHandler(OhPrjSdkType.OPENHARMONY);
            Map<PathAndApiVersion, Component> localComponents = ohSdkInfoHandler.getLocalSdks();
            Component toolchainsComponent = localComponents.get(
                new PathAndApiVersion(ComponentPath.TOOLCHAINS.value(), new ApiVersion(fullApiVersion)));
            if (toolchainsComponent != null) {
                Path cangjieSdkPath = toolchainsComponent.getLocation()
                    .getParent()
                    .resolve(CangjieComponentPath.CANGJIE.value());
                if (Files.exists(cangjieSdkPath)) {
                    addComponent(cangjieSdkPath, componentMap, isHarmony, fullApiVersion);
                }
            }
        }
    }

    private void addComponent(Path compatibleSdkPath, Map<String, CangjieComponent> componentMap, boolean isHarmony,
        String apiVersion) {
        CangjieComponent component = new CangjieComponent();
        component.setLocation(compatibleSdkPath);
        Optional<SdkConfig> sdkConfig = ConfigFactory.create(SdkConfig.class, isHarmony, apiVersion);
        sdkConfig.ifPresent(component::setSdkConfig);
        componentMap.put(CangjieComponentPath.CANGJIE.value(), component);
    }

    private void addExtendComponents(boolean isHarmony, int apiVersion, Map<String, CangjieComponent> componentMap) {
        for (CangjieComponentProvider componentProvider
            : CangjieComponentProvider.COMPONENT_PROVIDER_EXTENSION_LIST.getExtensionList()) {
            Map<String, CangjieComponent> components =
                componentProvider.collectCangjieComponent(isHarmony, apiVersion);
            for (Map.Entry<String, CangjieComponent> componentItem : components.entrySet()) {
                String itemKey = componentItem.getKey();
                if (componentMap.containsKey(itemKey)) {
                    continue;
                }
                componentMap.put(itemKey, componentItem.getValue());
            }
        }
    }
}
