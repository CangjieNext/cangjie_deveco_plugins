/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.apidocs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理api文档和文档映射
 *
 * @since 2024-11-25
 */
public class ApiDocsManager {
    /**
     * 插件端id
     */
    public static final String CANGJIE_PLUGIN_ID = "com.huawei.cangjie-support-plugin";

    /**
     * 兼容包sdk key
     */
    public static final String CANGJIE_COMPATIBLE_SDK_KEY = "cangjie.compatible.sdk.location";

    private static final Map<String, Map<String, String>> apiToDocMap = new ConcurrentHashMap<>();

    private static final Logger LOG = Logger.getInstance(ApiDocsManager.class);

    private final Project project;

    public ApiDocsManager(Project project) {
        this.project = project;
    }

    /**
     * Is config compatible sdk boolean.
     *
     * @return the boolean
     */
    public static boolean hasCompatibleSdkConfig() {
        return StringUtils.isNotBlank(System.getProperty(CANGJIE_COMPATIBLE_SDK_KEY));
    }

    /**
     * 初始化API到文档的映射
     *
     * @since 2024-11-25
     */
    public static void initializeMapping() {
        try {
            // 读取StdApiFastSearch.json文件内容
            String stdApiJsonContent =
                    new String(Files.readAllBytes(Paths.get(getAbsoluteUrl("StdApiFastSearch.json"))));

            // 读取ohosApi.json文件内容
            String ohosApiJsonContent = new String(Files.readAllBytes(Paths.get(getAbsoluteUrl("OhosApiFastSearch"
                    + ".json"))));

            // 使用FastJSON解析JSON
            Map<String, List<Map<String, String>>> stdPackageMap = JSON.parseObject(stdApiJsonContent,
                    new TypeReference<>() {});

            Map<String, List<Map<String, String>>> ososPackageMap = JSON.parseObject(ohosApiJsonContent,
                    new TypeReference<>() {});
            // 合并处理两个JSON文件的包映射
            processPackageMap(stdPackageMap);
            processPackageMap(ososPackageMap);
        } catch (IOException e) {
            LOG.warn("Error reading API JSON files: " + e.getMessage());
        }
    }

    private static void processPackageMap(Map<String, List<Map<String, String>>> packageMap) {
        // 处理每个包
        for (String packageName : packageMap.keySet()) {
            // 为每个包创建或获取已存在的API映射HashMap
            Map<String, String> packageApiMap = apiToDocMap.computeIfAbsent(packageName,
                    k -> new ConcurrentHashMap<>());

            // 获取包的API列表
            List<Map<String, String>> apiList = packageMap.getOrDefault(packageName, new ArrayList<>());

            // 转换每个API条目到HashMap
            for (Map<String, String> apiEntry : apiList) {
                String apiName = apiEntry.get("api_name");
                String apiUrl = apiEntry.get("api_url");

                // 仅在名称和URL都存在时添加
                if (apiName != null && apiUrl != null) {
                    packageApiMap.put(apiName, apiUrl);
                }
            }
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        if (hasCompatibleSdkConfig()) {
            String sdkPath = System.getProperty(CANGJIE_COMPATIBLE_SDK_KEY);
            return Paths.get(sdkPath, "docs", relativeUrl).toString();
        } else {
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(CANGJIE_PLUGIN_ID));
            if (plugin == null) {
                return "";
            }
            Path pluginPath = plugin.getPluginPath();
            if (pluginPath == null) {
                return "";
            }
            return Paths.get(pluginPath.toString(), "lib", "docs", relativeUrl).toString();
        }
    }

    /**
     * 根据PsiElement和apiQualifiedName获取对应的文档URL，如果map里查找不到，通过search命令帮助用户搜索函数名。
     *
     * @param element PsiElement
     * @param apiQualifiedName 符号的全名称限定，形如Package/Class.Function(Args[])，用于索引查询
     * @return 符号所在文档的url路径
     * @since 2024-11-25
     */
    public String getDocumentationUrl(PsiElement element, String apiQualifiedName) {
        // format: Package/Class.Function(Args[])
        String apiName = apiQualifiedName.replaceAll("Class-", "")
                .replaceAll("Enum-", "")
                .replaceAll("Struct-", "")
                .replaceAll("Interface-", "")
                .replaceAll("Generics-", "");
        String[] parts = apiName.split("/");
        if (parts.length < 2) {
            return "";
        }
        String packageName = parts[0];
        // 区分是 CJ 还是 OHOS
        Map<String, String> apiMap = apiToDocMap.get(packageName);

        // 找具体组件/api
        if (Objects.nonNull(apiMap) && apiMap.containsKey(apiName)) {
            return getAbsoluteUrl(apiMap.get(apiName));
        }

        // 组件中的方法
        if (parts[1].contains(".")) {
            // 对应文档中缺少Class的情况，去除Class查找
            String apiKey = parts[0] + "/" + parts[1].split("\\.")[1];
            if (Objects.nonNull(apiMap) && apiMap.containsKey(apiKey)) {
                return getAbsoluteUrl(apiMap.get(apiKey));
            }

            // 去除类名，查找通用方法或属性
            String funcName = parts[1].split("\\.")[1];
            apiMap = apiToDocMap.get("universal");
            if (apiMap != null && apiMap.containsKey(funcName)) {
                return getAbsoluteUrl(apiMap.get(funcName));
            }
        }

        return "";
    }
}
