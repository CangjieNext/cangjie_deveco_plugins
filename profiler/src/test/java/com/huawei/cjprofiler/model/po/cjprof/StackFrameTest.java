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

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.LocalObject;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests builder and getters of {@link StackFrame}.
 *
 * @since 2026-08-14
 */
class StackFrameTest {
    @Test
    void builder_setsAllFields() {
        List<LocalObject> locals = new ArrayList<>();

        StackFrame frame = StackFrame.builder()
            .id(1)
            .methodName("main")
            .fileName("Main.cj")
            .lineNumber(42)
            .localObjectList(locals)
            .build();

        assertEquals(1, frame.getId());
        assertEquals("main", frame.getMethodName());
        assertEquals("Main.cj", frame.getFileName());
        assertEquals(42, frame.getLineNumber());
        assertSame(locals, frame.getLocalObjectList());
    }

    @Test
    void setters_updateFields() {
        StackFrame frame = StackFrame.builder().build();

        frame.setId(2);
        frame.setMethodName("run");
        frame.setFileName("Run.cj");
        frame.setLineNumber(7);

        List<LocalObject> locals = new ArrayList<>();
        frame.setLocalObjectList(locals);

        assertEquals(2, frame.getId());
        assertEquals("run", frame.getMethodName());
        assertEquals("Run.cj", frame.getFileName());
        assertEquals(7, frame.getLineNumber());
        assertSame(locals, frame.getLocalObjectList());
    }

    @Test
    void builder_defaultsReferenceFieldsToNull() {
        StackFrame frame = StackFrame.builder().build();

        assertEquals(0, frame.getId());
        assertEquals(0, frame.getLineNumber());
        assertNull(frame.getMethodName());
        assertNull(frame.getFileName());
        assertNull(frame.getLocalObjectList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        StackFrame a = StackFrame.builder().id(1).methodName("m").fileName("f").lineNumber(2).build();
        StackFrame b = StackFrame.builder().id(1).methodName("m").fileName("f").lineNumber(2).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsMethodName() {
        StackFrame frame = StackFrame.builder().methodName("hello").build();
        assertEquals(true, frame.toString().contains("hello"));
    }
}
