/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common;

import com.huawei.deveco.insight.ohos.utils.LogPrinter;

import com.alibaba.fastjson2.JSONObject;

/**
 * Trace utils for cangjie profiler
 *
 * @since 2026/09/15
 */
public class TraceUtils {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(TraceUtils.class);

    private TraceUtils() {
    }

    /**
     * trace
     *
     * @param name trace name
     * @param data trace data
     */
    public static void trace(String name, JSONObject data) {
        LOGGER.info("trace name is {}, data is {}.", name, data);
    }
}
