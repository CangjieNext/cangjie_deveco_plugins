/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests constant values of {@link EventKey}.
 *
 * @since 2026-08-14
 */
class EventKeyTest {
    @Test
    void cjHeapUsageConstant_isExpectedValue() {
        assertEquals("cj.heap.usage", EventKey.CJ_HEAP_USAGE);
    }

    @Test
    void forceLoadClass_initializesEventKey() {
        assertNotNull(new EventKey());
    }
}
