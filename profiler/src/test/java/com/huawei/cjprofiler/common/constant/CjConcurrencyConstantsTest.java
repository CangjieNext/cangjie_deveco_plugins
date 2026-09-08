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
 * Tests constant values of {@link CjConcurrencyConstants}.
 *
 * @since 2026-08-14
 */
class CjConcurrencyConstantsTest {
    @Test
    void concurrencyCjThreadConstant_isCjThread() {
        assertEquals("CJThread", CjConcurrencyConstants.CONCURRENCY_CJ_THREAD);
    }

    @Test
    void concurrencyNameTypeStateConstant_isState() {
        assertEquals("STATE", CjConcurrencyConstants.CONCURRENCY_NAME_TYPE_STATE);
    }

    @Test
    void concurrencyNameTypeCjThreadConstant_isCjthread() {
        assertEquals("CJTHREAD", CjConcurrencyConstants.CONCURRENCY_NAME_TYPE_CJ_THREAD);
    }

    @Test
    void forceLoadClass_initializesCjConcurrencyConstants() {
        assertNotNull(new CjConcurrencyConstants());
    }
}