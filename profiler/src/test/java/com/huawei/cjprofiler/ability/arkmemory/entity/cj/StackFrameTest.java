/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and constructors of {@link StackFrame}.
 *
 * @since 2026-08-14
 */
class StackFrameTest {
    @Test
    void noArgConstructor_setsDefaults() {
        StackFrame frame = new StackFrame();

        assertEquals(0, frame.getId());
        assertEquals(0, frame.getName());
        assertEquals(0, frame.getFileName());
        assertEquals(0L, frame.getLineNum());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        StackFrame frame = new StackFrame(1, 2, 3, 100L);

        assertEquals(1, frame.getId());
        assertEquals(2, frame.getName());
        assertEquals(3, frame.getFileName());
        assertEquals(100L, frame.getLineNum());
    }

    @Test
    void builder_setsAllFields() {
        StackFrame frame = StackFrame.builder()
            .id(5)
            .name(6)
            .fileName(7)
            .lineNum(200L)
            .build();

        assertEquals(5, frame.getId());
        assertEquals(6, frame.getName());
        assertEquals(7, frame.getFileName());
        assertEquals(200L, frame.getLineNum());
    }

    @Test
    void setters_updateFields() {
        StackFrame frame = new StackFrame();

        frame.setId(9);
        frame.setName(10);
        frame.setFileName(11);
        frame.setLineNum(300L);

        assertEquals(9, frame.getId());
        assertEquals(10, frame.getName());
        assertEquals(11, frame.getFileName());
        assertEquals(300L, frame.getLineNum());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        StackFrame a = StackFrame.builder().id(1).name(2).fileName(3).lineNum(4L).build();
        StackFrame b = StackFrame.builder().id(1).name(2).fileName(3).lineNum(4L).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsId() {
        StackFrame frame = StackFrame.builder().id(42).build();
        assertEquals(true, frame.toString().contains("42"));
    }
}
