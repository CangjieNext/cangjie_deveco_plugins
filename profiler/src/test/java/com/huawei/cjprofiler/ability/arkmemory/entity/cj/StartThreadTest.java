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
 * Tests getter/setter and constructors of {@link StartThread}.
 *
 * @since 2026-08-14
 */
class StartThreadTest {
    @Test
    void noArgConstructor_setsDefaults() {
        StartThread thread = new StartThread();

        assertEquals(0L, thread.getIdx());
        assertEquals(0, thread.getId());
        assertEquals(0L, thread.getStackTraceIdx());
        assertEquals(0, thread.getName());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        StartThread thread = new StartThread(1L, 2, 3L, 4);

        assertEquals(1L, thread.getIdx());
        assertEquals(2, thread.getId());
        assertEquals(3L, thread.getStackTraceIdx());
        assertEquals(4, thread.getName());
    }

    @Test
    void builder_setsAllFields() {
        StartThread thread = StartThread.builder()
            .idx(10L)
            .id(20)
            .stackTraceIdx(30L)
            .name(40)
            .build();

        assertEquals(10L, thread.getIdx());
        assertEquals(20, thread.getId());
        assertEquals(30L, thread.getStackTraceIdx());
        assertEquals(40, thread.getName());
    }

    @Test
    void setters_updateFields() {
        StartThread thread = new StartThread();

        thread.setIdx(5L);
        thread.setId(6);
        thread.setStackTraceIdx(7L);
        thread.setName(8);

        assertEquals(5L, thread.getIdx());
        assertEquals(6, thread.getId());
        assertEquals(7L, thread.getStackTraceIdx());
        assertEquals(8, thread.getName());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        StartThread a = StartThread.builder().idx(1L).id(2).stackTraceIdx(3L).name(4).build();
        StartThread b = StartThread.builder().idx(1L).id(2).stackTraceIdx(3L).name(4).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
