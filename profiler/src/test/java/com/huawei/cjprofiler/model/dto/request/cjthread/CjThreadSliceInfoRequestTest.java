/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.request.cjthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter of {@link CjThreadSliceInfoRequest}.
 *
 * @since 2026-08-14
 */
class CjThreadSliceInfoRequestTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadSliceInfoRequest request = new CjThreadSliceInfoRequest();

        assertNull(request.getSessionId());
        assertNull(request.getCjThreadId());
        assertNull(request.getStartTime());
        assertNull(request.getEndTime());
    }

    @Test
    void setters_updateFields() {
        CjThreadSliceInfoRequest request = new CjThreadSliceInfoRequest();

        request.setSessionId("session-3");
        request.setCjThreadId(5);
        request.setStartTime(500L);
        request.setEndTime(600L);

        assertEquals("session-3", request.getSessionId());
        assertEquals(5, request.getCjThreadId());
        assertEquals(500L, request.getStartTime());
        assertEquals(600L, request.getEndTime());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadSliceInfoRequest a = new CjThreadSliceInfoRequest();
        a.setSessionId("s");
        a.setCjThreadId(1);
        a.setStartTime(1L);
        a.setEndTime(2L);

        CjThreadSliceInfoRequest b = new CjThreadSliceInfoRequest();
        b.setSessionId("s");
        b.setCjThreadId(1);
        b.setStartTime(1L);
        b.setEndTime(2L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
