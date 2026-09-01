/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.apidocs;

import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * WindowCoordinateUtil
 *
 * @since 2024-11-25
 */
public class WindowCoordinateUtil {
    private static final float X_FLEX = 0.5f;

    private static final float Y_FLEX = 0.125f;

    private static final float HEIGHT_FLEX = 0.75f;

    /**
     * 计算初始窗口大小
     *
     * @return 窗口坐标
     */
    public static WindowCoordinate computeInitWindowCoordinate() {
        // 得到屏幕的尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int windowX = (int) (screenSize.getWidth() * X_FLEX - screenSize.getHeight() * X_FLEX);
        int windowY = (int) (screenSize.getHeight() * Y_FLEX);
        int width = (int) (screenSize.getHeight());
        int height = (int) (screenSize.getHeight() * HEIGHT_FLEX);
        // 设置主面板的大小
        return new WindowCoordinate(windowX, windowY, width, height);
    }
}
