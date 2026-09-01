/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.apidocs;

import static com.huawei.cangjie.projectmgmt.utils.Constants.CANGJIE_PLUGIN_ID;
import static com.huawei.cjlsp.capabilities.apidocs.ApiDocsManager.CANGJIE_COMPATIBLE_SDK_KEY;
import static com.huawei.cjlsp.capabilities.apidocs.ApiDocsManager.hasCompatibleSdkConfig;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.PsiElement;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JLabel;

/**
 * ApiDocsToolWindow
 *
 * @since 2024-11-25
 */
public class ApiDocsToolWindow extends SimpleToolWindowPanel {
    private static final String DEFAULT_URL = "./docs/index.html";
    private static final String COMPATIBLE_DEFAULT_URL = "./docs/API_Reference/index.html";

    private final Project project;
    private final ApiDocsManager docManager;
    private JBCefBrowser browser;

    public ApiDocsToolWindow(Project project) {
        super(true, true);
        this.project = project;
        this.docManager = new ApiDocsManager(project);
        ApiDocsManager.initializeMapping();

        if (!JBCefApp.isSupported()) {
            JLabel errorLabel = new JLabel("当前平台不支持 JCEF");
            setContent(errorLabel);
            return;
        }

        browser = new JBCefBrowser("file:///" + getDefaultUrl());
        setContent(browser.getComponent());
    }

    /**
     * 根据符号导航去文档页面
     *
     * @param element PsiElement
     * @param apiQualifiedName 符号的全名称限定，用于索引查询
     * @since 2024-11-25
     */
    public void navigateToElement(PsiElement element, String apiQualifiedName) {
        String docUrl = docManager.getDocumentationUrl(element, apiQualifiedName);
        if (!docUrl.isEmpty()) {
            IdeEventQueue.getInstance().getPopupManager().closeAllPopups();
            browser.loadURL("file://" + docUrl);
        }
    }

    private String getDefaultUrl() {
        if (hasCompatibleSdkConfig()) {
            String sdkPath = System.getProperty(CANGJIE_COMPATIBLE_SDK_KEY);
            return Paths.get(sdkPath, COMPATIBLE_DEFAULT_URL).toString();
        } else {
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(CANGJIE_PLUGIN_ID));
            if (plugin == null) {
                return "";
            }
            Path pluginPath = plugin.getPluginPath();
            if (pluginPath == null) {
                return "";
            }
            return Paths.get(pluginPath.toString(), "lib", DEFAULT_URL).toString();
        }
    }
}
