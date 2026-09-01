/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.apidocs;

/**
 * 窗口初始坐标信息
 *
 * @since 2024-11-25
 */
public class WindowCoordinate {
    /**
     * x轴
     */
    private int windowX;

    /**
     * y轴
     */
    private int windowY;

    /**
     * 宽度
     */
    private int width;

    /**
     * 高度
     */
    private int height;

    /**
     * 无参构造函数
     */
    public WindowCoordinate() {}

    /**
     * 有参构造函数
     *
     * @param windowX windowX
     * @param windowY windowY
     * @param width width
     * @param height height
     */
    public WindowCoordinate(int windowX, int windowY, int width, int height) {
        this.windowX = windowX;
        this.windowY = windowY;
        this.width = width;
        this.height = height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }
}
