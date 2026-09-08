/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter of {@link CjThreadSliceRequest}.
 *
 * @since 2026-08-14
 */
class CjThreadSliceRequestTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadSliceRequest request = new CjThreadSliceRequest();

        assertNull(request.getSessionId());
        assertNull(request.getStartTime());
        assertNull(request.getEndTime());
        assertNull(request.getStateName());
        assertNull(request.getProcessIdList());
    }

    @Test
    void setters_updateFields() {
        CjThreadSliceRequest request = new CjThreadSliceRequest();

        request.setSessionId("session-4");
        request.setStartTime(100L);
        request.setEndTime(200L);
        request.setStateName("RUNNING");

        List<Long> processIds = new ArrayList<>();
        request.setProcessIdList(processIds);

        assertEquals("session-4", request.getSessionId());
        assertEquals(100L, request.getStartTime());
        assertEquals(200L, request.getEndTime());
        assertEquals("RUNNING", request.getStateName());
        assertSame(processIds, request.getProcessIdList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadSliceRequest a = new CjThreadSliceRequest();
        a.setSessionId("s");
        a.setStartTime(1L);
        a.setEndTime(2L);
        a.setStateName("RUN");
        a.setProcessIdList(new ArrayList<>());

        CjThreadSliceRequest b = new CjThreadSliceRequest();
        b.setSessionId("s");
        b.setStartTime(1L);
        b.setEndTime(2L);
        b.setStateName("RUN");
        b.setProcessIdList(new ArrayList<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
