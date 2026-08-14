/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.cjprofiler.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CjThreadStateTest
 *
 * @since 2026-06-04
 */
public class CjThreadStateTest {

    // ==================== getKeyByValue tests ====================

    @Test
    public void testGetKeyByValueWithReady() {
        String result = CjThreadState.getKeyByValue("New READY");
        assertEquals("Ready", result);
    }

    @Test
    public void testGetKeyByValueWithPending() {
        String result = CjThreadState.getKeyByValue("Park PENDING");
        assertEquals("Pending", result);
    }

    @Test
    public void testGetKeyByValueWithRunning() {
        String result = CjThreadState.getKeyByValue("Execute RUNNING");
        assertEquals("Running", result);
    }

    @Test
    public void testGetKeyByValueWithIdle() {
        String result = CjThreadState.getKeyByValue("Exit IDLE");
        assertEquals("Idle", result);
    }

    @Test
    public void testGetKeyByValueWithUnknownValue() {
        String result = CjThreadState.getKeyByValue("unknown");
        assertEquals("", result);
    }

    @Test
    public void testGetKeyByValueWithNull() {
        String result = CjThreadState.getKeyByValue(null);
        assertEquals("", result);
    }

    // ==================== getValueByKey tests ====================

    @Test
    public void testGetValueByKeyWithReady() {
        String result = CjThreadState.getValueByKey("Ready");
        assertEquals("New READY", result);
    }

    @Test
    public void testGetValueByKeyWithPending() {
        String result = CjThreadState.getValueByKey("Pending");
        assertEquals("Park PENDING", result);
    }

    @Test
    public void testGetValueByKeyWithRunning() {
        String result = CjThreadState.getValueByKey("Running");
        assertEquals("Execute RUNNING", result);
    }

    @Test
    public void testGetValueByKeyWithIdle() {
        String result = CjThreadState.getValueByKey("Idle");
        assertEquals("Exit IDLE", result);
    }

    @Test
    public void testGetValueByKeyWithUnknownKey() {
        String result = CjThreadState.getValueByKey("unknown");
        assertEquals("", result); // returns TOTAL.value which is empty
    }

    @Test
    public void testGetValueByKeyWithNull() {
        String result = CjThreadState.getValueByKey(null);
        assertEquals("", result); // returns TOTAL.value which is empty
    }

    // ==================== getStateList tests ====================

    @Test
    public void testGetStateList() {
        List<CjThreadState> result = CjThreadState.getStateList();

        assertEquals(4, result.size());
        assertTrue(result.contains(CjThreadState.READY));
        assertTrue(result.contains(CjThreadState.PENDING));
        assertTrue(result.contains(CjThreadState.RUNNING));
        assertTrue(result.contains(CjThreadState.IDLE));
    }

    @Test
    public void testGetStateListDoesNotContainTotal() {
        List<CjThreadState> result = CjThreadState.getStateList();
        assertEquals(4, result.size());
    }

    // ==================== enum values tests ====================

    @Test
    public void testEnumValues() {
        CjThreadState[] values = CjThreadState.values();
        assertEquals(5, values.length);
        assertEquals(CjThreadState.READY, values[0]);
        assertEquals(CjThreadState.PENDING, values[1]);
        assertEquals(CjThreadState.RUNNING, values[2]);
        assertEquals(CjThreadState.IDLE, values[3]);
        assertEquals(CjThreadState.TOTAL, values[4]);
    }

    @Test
    public void testEnumGetters() {
        assertEquals("Ready", CjThreadState.READY.getKey());
        assertEquals("New READY", CjThreadState.READY.getValue());

        assertEquals("Pending", CjThreadState.PENDING.getKey());
        assertEquals("Park PENDING", CjThreadState.PENDING.getValue());

        assertEquals("Running", CjThreadState.RUNNING.getKey());
        assertEquals("Execute RUNNING", CjThreadState.RUNNING.getValue());

        assertEquals("Idle", CjThreadState.IDLE.getKey());
        assertEquals("Exit IDLE", CjThreadState.IDLE.getValue());

        assertEquals("Total", CjThreadState.TOTAL.getKey());
        assertEquals("", CjThreadState.TOTAL.getValue());
    }
}
