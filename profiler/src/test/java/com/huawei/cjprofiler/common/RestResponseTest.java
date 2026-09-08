/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests factory and status behavior of {@link RestResponse}.
 *
 * @since 2026-08-14
 */
class RestResponseTest {
    @Test
    void successWithDataBuildsSuccessfulResponse() {
        Object payload = new Object();

        RestResponse<Object> response = RestResponse.success(payload, "created");

        assertEquals(RestResponse.RestCode.SUCCESS.getCode(), response.getCode());
        assertSame(payload, response.getData());
        assertEquals("created", response.getMessage());
        assertFalse(response.isFailed());
    }

    @Test
    void successWithoutDataUsesEmptyStringPayload() {
        RestResponse<String> response = RestResponse.success("done");

        assertEquals(200, response.getCode());
        assertEquals("", response.getData());
        assertEquals("done", response.getMessage());
        assertFalse(response.isFailed());
    }

    @Test
    void failureFactoriesBuildFailedResponses() {
        RestResponse<Integer> withData = RestResponse.failure(42, "invalid");
        RestResponse<String> withoutData = RestResponse.failure("missing");

        assertEquals(400, withData.getCode());
        assertEquals(42, withData.getData());
        assertEquals("invalid", withData.getMessage());
        assertTrue(withData.isFailed());
        assertEquals("", withoutData.getData());
        assertEquals("missing", withoutData.getMessage());
        assertTrue(withoutData.isFailed());
    }

    @Test
    void isFailedTreatsEveryNonSuccessCodeAsFailure() {
        RestResponse<Void> response = new RestResponse<>();
        response.setCode(201);

        assertTrue(response.isFailed());

        response.setCode(RestResponse.RestCode.SUCCESS.getCode());
        assertFalse(response.isFailed());
    }

    @Test
    void setDataAndSetMessage_updateFieldsCorrectly() {
        RestResponse<String> response = new RestResponse<>();
        response.setData("test-data");
        response.setMessage("test-message");

        assertEquals("test-data", response.getData());
        assertEquals("test-message", response.getMessage());
    }
}