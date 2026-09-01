/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.apidocs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

import java.awt.Rectangle;

/**
 * ApiDocsToolWindowFactory
 *
 * @since 2024-11-25
 */
public class ApiDocsToolWindowFactory implements ToolWindowFactory {
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ToolWindowFactory.super.init(toolWindow);
        toolWindow.setShowStripeButton(false);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        WindowCoordinate windowCoordinate = WindowCoordinateUtil.computeInitWindowCoordinate();
        // 设置主面板的大小
        toolWindow.setDefaultState(ToolWindowAnchor.RIGHT, ToolWindowType.FLOATING,
                new Rectangle(windowCoordinate.getWindowX(), windowCoordinate.getWindowY(), windowCoordinate.getWidth(),
                        windowCoordinate.getHeight()));

        ApiDocsToolWindow apiDocsWindow = new ApiDocsToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(apiDocsWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}