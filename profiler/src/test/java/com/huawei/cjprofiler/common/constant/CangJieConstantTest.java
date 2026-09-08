/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests constant values of {@link CangJieConstant}.
 *
 * @since 2026-08-14
 */
class CangJieConstantTest {
    @Test
    void cangjieProfilerDispatchKeyConstant_isExpectedValue() {
        assertEquals("cangjieProfiler", CangJieConstant.CANGJIE_PROFILER_DISPATCH_KEY);
    }

    @Test
    void cjPluginIdConstant_isExpectedValue() {
        assertEquals("com.huawei.cangjie-support-plugin", CangJieConstant.CJ_PLUGIN_ID);
    }

    @Test
    void forceLoadClass_initializesCangJieConstant() {
        assertNotNull(new CangJieConstant());
    }
}
