/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;

/**
 * Tests constructor and getters of {@link JcefRequest} and {@link JcefRequest.Data}.
 *
 * @since 2026-08-14
 */
class JcefRequestTest {
    @Test
    void dataConstructor_setsParamsAndMethod() {
        JSONObject params = new JSONObject();
        params.put("k", "v");

        JcefRequest.Data data = new JcefRequest.Data(params, "doSomething");

        assertSame(params, data.getParams());
        assertEquals("doSomething", data.getMethod());
    }

    @Test
    void jcefRequestConstructor_setsKeyAndData() {
        JSONObject params = new JSONObject();
        JcefRequest.Data data = new JcefRequest.Data(params, "run");

        JcefRequest request = new JcefRequest("myKey", data);

        assertEquals("myKey", request.getKey());
        assertSame(data, request.getData());
    }

    @Test
    void setters_updateFields() {
        JcefRequest.Data data = new JcefRequest.Data(new JSONObject(), "m");

        data.setMethod("newMethod");

        JSONObject params = new JSONObject();
        data.setParams(params);

        assertEquals("newMethod", data.getMethod());
        assertSame(params, data.getParams());

        JcefRequest request = new JcefRequest("k", data);
        request.setKey("newKey");

        assertEquals("newKey", request.getKey());

        JcefRequest.Data other = new JcefRequest.Data(new JSONObject(), "x");
        request.setData(other);
        assertSame(other, request.getData());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        JSONObject params = new JSONObject();
        JcefRequest.Data d1 = new JcefRequest.Data(params, "m");
        JcefRequest.Data d2 = new JcefRequest.Data(params, "m");

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());

        JcefRequest r1 = new JcefRequest("k", d1);
        JcefRequest r2 = new JcefRequest("k", d2);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
