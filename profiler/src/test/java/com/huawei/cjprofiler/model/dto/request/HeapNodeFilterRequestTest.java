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
 * Tests getter/setter and constructors of {@link HeapNodeFilterRequest}.
 *
 * @since 2026-08-14
 */
class HeapNodeFilterRequestTest {
    @Test
    void noArgConstructor_setsDefaults() {
        HeapNodeFilterRequest request = new HeapNodeFilterRequest();

        assertNull(request.getSessionId());
        assertNull(request.getRawId());
        assertNull(request.getCurRawId());
        assertNull(request.getRootTypeList());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<String> rootTypes = new ArrayList<>();

        HeapNodeFilterRequest request = new HeapNodeFilterRequest("s", "r1", "r2", rootTypes);

        assertEquals("s", request.getSessionId());
        assertEquals("r1", request.getRawId());
        assertEquals("r2", request.getCurRawId());
        assertSame(rootTypes, request.getRootTypeList());
    }

    @Test
    void setters_updateFields() {
        HeapNodeFilterRequest request = new HeapNodeFilterRequest();

        request.setSessionId("session");
        request.setRawId("raw");
        request.setCurRawId("cur");

        List<String> rootTypes = new ArrayList<>();
        request.setRootTypeList(rootTypes);

        assertEquals("session", request.getSessionId());
        assertEquals("raw", request.getRawId());
        assertEquals("cur", request.getCurRawId());
        assertSame(rootTypes, request.getRootTypeList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        HeapNodeFilterRequest a = new HeapNodeFilterRequest("s", "r", "c", new ArrayList<>());
        HeapNodeFilterRequest b = new HeapNodeFilterRequest("s", "r", "c", new ArrayList<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
