/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;

/**
 * TraceUtilsTest
 *
 * @since 2026/09/15
 */
class TraceUtilsTest {
    @Test
    public void testTraceWithNormalData() {
        JSONObject data = new JSONObject();
        data.put(TraceConstant.REASON, "gc");

        assertDoesNotThrow(() -> TraceUtils.trace("cangjieGc", data));
    }

    @Test
    public void testTraceWithNullData() {
        assertDoesNotThrow(() -> TraceUtils.trace("cangjieGc", null));
    }

    @Test
    public void testTraceConstantReason() {
        assertEquals("reason", TraceConstant.REASON);
    }
}
