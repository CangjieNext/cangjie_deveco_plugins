/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.utils.annotations.ParsedJson;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Unit tests for {@code ArgumentsUtil} JSON argument parsing.
 *
 * @since 2026-08-14
 */
public class ArgumentsUtilTest {
    @Test
    public void testParseJsonObject_NullMethod() {
        Object[] args = new Object[1];
        try {
            ArgumentsUtil.parseJsonObject(null, args);
            fail("Should throw ProfilerException");
        } catch (ProfilerException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testParseJsonObject_NullArgs() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod", JSONObject.class);
        try {
            ArgumentsUtil.parseJsonObject(method, null);
            fail("Should throw ProfilerException");
        } catch (ProfilerException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testParseJsonObject_InconsistentParameters() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod", JSONObject.class);
        Object[] args = new Object[2];
        try {
            ArgumentsUtil.parseJsonObject(method, args);
            fail("Should throw ProfilerException");
        } catch (ProfilerException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testParseJsonObject_NonJsonObjectArg() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod", JSONObject.class);
        Object[] args = new Object[1];
        args[0] = "not a json object";
        // Non-JSONObject args are skipped untouched
        assertDoesNotThrow(() -> ArgumentsUtil.parseJsonObject(method, args));
        assertEquals("not a json object", args[0], "arg must not be modified when not a JSONObject");
    }

    @Test
    public void testParseJsonObject_WithParsedJsonAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod", JSONObject.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "test");
        Object[] args = new Object[1];
        args[0] = jsonObject;
        // @ParsedJson param is parsed and validated in place; must not throw
        assertDoesNotThrow(() -> ArgumentsUtil.parseJsonObject(method, args));
        assertTrue(args[0] instanceof JSONObject, "parsed arg must remain a JSONObject");
    }

    /**
     * Test class for method reflection
     */
    public static class TestClass {
        public void testMethod(@ParsedJson JSONObject param) {
        }
    }
}
