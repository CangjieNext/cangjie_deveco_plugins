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
 * Tests getter/setter of {@link CjThreadListRequest}.
 *
 * @since 2026-08-14
 */
class CjThreadListRequestTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadListRequest request = new CjThreadListRequest();

        assertNull(request.getSessionId());
        assertNull(request.getStartTime());
        assertNull(request.getEndTime());
    }

    @Test
    void setters_updateFields() {
        CjThreadListRequest request = new CjThreadListRequest();

        request.setSessionId("session-1");
        request.setStartTime(100L);
        request.setEndTime(200L);

        assertEquals("session-1", request.getSessionId());
        assertEquals(100L, request.getStartTime());
        assertEquals(200L, request.getEndTime());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadListRequest a = new CjThreadListRequest();
        a.setSessionId("s");
        a.setStartTime(1L);
        a.setEndTime(2L);

        CjThreadListRequest b = new CjThreadListRequest();
        b.setSessionId("s");
        b.setStartTime(1L);
        b.setEndTime(2L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
