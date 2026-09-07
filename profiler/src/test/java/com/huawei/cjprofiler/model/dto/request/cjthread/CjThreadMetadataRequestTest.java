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
 * Tests getter/setter of {@link CjThreadMetadataRequest}.
 *
 * @since 2026-08-14
 */
class CjThreadMetadataRequestTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadMetadataRequest request = new CjThreadMetadataRequest();

        assertNull(request.getSessionId());
        assertNull(request.getStartTime());
        assertNull(request.getEndTime());
    }

    @Test
    void setters_updateFields() {
        CjThreadMetadataRequest request = new CjThreadMetadataRequest();

        request.setSessionId("session-2");
        request.setStartTime(300L);
        request.setEndTime(400L);

        assertEquals("session-2", request.getSessionId());
        assertEquals(300L, request.getStartTime());
        assertEquals(400L, request.getEndTime());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadMetadataRequest a = new CjThreadMetadataRequest();
        a.setSessionId("s");
        a.setStartTime(1L);
        a.setEndTime(2L);

        CjThreadMetadataRequest b = new CjThreadMetadataRequest();
        b.setSessionId("s");
        b.setStartTime(1L);
        b.setEndTime(2L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
