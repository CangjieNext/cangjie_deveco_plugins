/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.cjprof.jni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and constructor behavior of {@link Frame}.
 *
 * @since 2026-08-14
 */
class FrameTest {
    @Test
    void noArgConstructor_initializesLocalsToEmptyList() {
        Frame frame = new Frame();

        assertNotNull(frame.getLocals());
        assertEquals(0, frame.getLocals().size());
        assertEquals(0, frame.getLine());
        assertEquals(0L, frame.getId());
        assertNull(frame.getFuncName());
        assertNull(frame.getFileName());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<InstanceNode> locals = new ArrayList<>();
        InstanceNode local = new InstanceNode();
        locals.add(local);

        Frame frame = new Frame("main", "Main.cj", 42, locals, 100L);

        assertEquals("main", frame.getFuncName());
        assertEquals("Main.cj", frame.getFileName());
        assertEquals(42, frame.getLine());
        assertSame(locals, frame.getLocals());
        assertEquals(100L, frame.getId());
    }

    @Test
    void builder_setsAllFields() {
        List<InstanceNode> locals = new ArrayList<>();

        Frame frame = Frame.builder()
            .funcName("run")
            .fileName("Run.cj")
            .line(7)
            .locals(locals)
            .id(9L)
            .build();

        assertEquals("run", frame.getFuncName());
        assertEquals("Run.cj", frame.getFileName());
        assertEquals(7, frame.getLine());
        assertSame(locals, frame.getLocals());
        assertEquals(9L, frame.getId());
    }

    @Test
    void setters_updateFields() {
        Frame frame = new Frame();

        frame.setFuncName("init");
        frame.setFileName("Init.cj");
        frame.setLine(3);
        frame.setId(5L);

        List<InstanceNode> locals = new ArrayList<>();
        frame.setLocals(locals);

        assertEquals("init", frame.getFuncName());
        assertEquals("Init.cj", frame.getFileName());
        assertEquals(3, frame.getLine());
        assertEquals(5L, frame.getId());
        assertSame(locals, frame.getLocals());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        Frame a = Frame.builder().funcName("f").fileName("F.cj").line(1).id(1L).build();
        Frame b = Frame.builder().funcName("f").fileName("F.cj").line(1).id(1L).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsFuncName() {
        Frame frame = Frame.builder().funcName("hello").build();
        assertEquals(true, frame.toString().contains("hello"));
    }
}
