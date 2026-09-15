/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.controller;

/**
 * Test-only class placed in the {@code controller} package so that
 * {@code Constants.CONTROLLER_ROOT_PATH} resolves on the test classpath and
 * {@code ClassUtil#findProjectClasses} executes its full scanning path.
 *
 * @since 2026-09-09
 */
@ScannerMarker
public class ControllerScanSample {
    /**
     * 恒等方法，用于让扫描链路完整跑通.
     *
     * @param input 输入
     * @return 原样返回
     */
    public int echo(int input) {
        return input;
    }
}
