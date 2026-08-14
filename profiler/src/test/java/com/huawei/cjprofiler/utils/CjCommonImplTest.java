package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.huawei.cjprofiler.common.Constants;
import com.huawei.deveco.insight.ohos.ability.persistence.importconfig.TypeRule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * CjCommonImplTest
 */
public class CjCommonImplTest {

    private CjCommonImpl cjCommonImpl;

    @BeforeEach
    public void setUp() {
        cjCommonImpl = new CjCommonImpl();
    }

    /**
     * Test isNotifyCjFileNumExceedLimit when type does not equal CJPROF_FILE_TYPE
     * Should return false
     */
    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeNotMatches() throws Exception {
        TypeRule typeRule = new TypeRule();
        setTypeField(typeRule, "other_type");

        boolean result = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(result);
    }

    /**
     * Test isNotifyCjFileNumExceedLimit when type is null
     * Should return false (no exception)
     */
    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeIsNull() throws Exception {
        TypeRule typeRule = new TypeRule();
        setTypeField(typeRule, null);

        // Should not throw NullPointerException
        boolean result = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(result);
    }

    /**
     * Test isNotifyCjFileNumExceedLimit when type is empty string
     * Should return false
     */
    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeIsEmpty() throws Exception {
        TypeRule typeRule = new TypeRule();
        setTypeField(typeRule, "");

        boolean result = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(result);
    }

    /**
     * Test instance creation
     */
    @Test
    public void testInstanceCreation() {
        CjCommonImpl instance = new CjCommonImpl();
        assertNotNull(instance);
    }

    /**
     * Helper method to set the type field of TypeRule using reflection
     * since TypeRule is a compileOnly dependency
     */
    private void setTypeField(TypeRule typeRule, String typeValue) throws Exception {
        Field typeField = TypeRule.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(typeRule, typeValue);
    }
}
