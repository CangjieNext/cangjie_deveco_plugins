/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import com.huawei.deveco.insight.ohos.resourcehandler.controller.RequestMapping;

/**
 * Test-only mapping sample used to exercise the "empty path" warning branches of
 * {@code CangjieMappingRegistyManager#loadRequestMappingForClass}. It never
 * registers a usable mapping: the method mapping has no path, so the registry
 * only logs a warning and skips it.
 */
@RequestMapping(path = "test.warnSample")
public class CjMappingWarnSample {

    /**
     * 无 path 的测试方法，用于触发方法级空 path 告警分支.
     */
    @RequestMapping(path = "")
    public void warnWithoutPath() {
        // nothing to do
    }
}
