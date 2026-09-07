/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.po.cjprof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and constructors of {@link HeapThreadInfo}.
 *
 * @since 2026-08-14
 */
class HeapThreadInfoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        HeapThreadInfo info = new HeapThreadInfo();

        assertEquals(0, info.getThreadId());
        assertNull(info.getThreadName());
        assertNull(info.getStackFrameInfoList());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<StackFrame> frames = new ArrayList<>();

        HeapThreadInfo info = new HeapThreadInfo(1, "main", frames);

        assertEquals(1, info.getThreadId());
        assertEquals("main", info.getThreadName());
        assertSame(frames, info.getStackFrameInfoList());
    }

    @Test
    void builder_setsAllFields() {
        List<StackFrame> frames = new ArrayList<>();

        HeapThreadInfo info = HeapThreadInfo.builder()
            .threadId(2)
            .threadName("worker")
            .stackFrameInfoList(frames)
            .build();

        assertEquals(2, info.getThreadId());
        assertEquals("worker", info.getThreadName());
        assertSame(frames, info.getStackFrameInfoList());
    }

    @Test
    void setters_updateFields() {
        HeapThreadInfo info = new HeapThreadInfo();

        info.setThreadId(3);
        info.setThreadName("bg");

        List<StackFrame> frames = new ArrayList<>();
        info.setStackFrameInfoList(frames);

        assertEquals(3, info.getThreadId());
        assertEquals("bg", info.getThreadName());
        assertSame(frames, info.getStackFrameInfoList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        HeapThreadInfo a = HeapThreadInfo.builder()
            .threadId(1).threadName("t").stackFrameInfoList(new ArrayList<>()).build();
        HeapThreadInfo b = HeapThreadInfo.builder()
            .threadId(1).threadName("t").stackFrameInfoList(new ArrayList<>()).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsThreadName() {
        HeapThreadInfo info = HeapThreadInfo.builder().threadName("hello").build();
        assertEquals(true, info.toString().contains("hello"));
    }
}
