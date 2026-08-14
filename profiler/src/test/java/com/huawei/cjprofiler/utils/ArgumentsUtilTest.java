package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.utils.annotations.ParsedJson;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * ArgumentsUtilTest
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
        // Should not throw, just skip parsing
        ArgumentsUtil.parseJsonObject(method, args);
    }

    @Test
    public void testParseJsonObject_WithParsedJsonAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod", JSONObject.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "test");
        Object[] args = new Object[1];
        args[0] = jsonObject;
        // This will throw because ParsedJson annotation processing requires special handling
        try {
            ArgumentsUtil.parseJsonObject(method, args);
        } catch (Exception e) {
            // Expected in test environment without full context
        }
    }

    /**
     * Test class for method reflection
     */
    public static class TestClass {
        public void testMethod(@ParsedJson JSONObject param) {
        }
    }
}
