/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests message propagation of {@link CjProfilerException}.
 *
 * @since 2026-08-14
 */
class CjProfilerExceptionTest {
    @Test
    void constructor_setsMessageOnRuntimeException() {
        CjProfilerException ex = new CjProfilerException("something went wrong");

        assertEquals("something went wrong", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void constructor_acceptsNullMessage() {
        CjProfilerException ex = new CjProfilerException(null);

        assertNull(ex.getMessage());
    }

    @Test
    void constructor_canBeThrownAndCaught() {
        CjProfilerException thrown = assertThrows(CjProfilerException.class,
            () -> {
                throw new CjProfilerException("boom");
            });

        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void constructor_acceptsEmptyMessage() {
        CjProfilerException ex = new CjProfilerException("");

        assertEquals("", ex.getMessage());
    }
}
