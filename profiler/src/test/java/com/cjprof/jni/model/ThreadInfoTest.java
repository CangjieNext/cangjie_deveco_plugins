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
 * Tests getter/setter and constructor behavior of {@link ThreadInfo}.
 *
 * @since 2026-08-14
 */
class ThreadInfoTest {
    @Test
    void noArgConstructor_initializesFramesToEmptyList() {
        ThreadInfo info = new ThreadInfo();

        assertNotNull(info.getFrames());
        assertEquals(0, info.getFrames().size());
        assertEquals(0L, info.getId());
        assertNull(info.getName());
    }

    @Test
    void setters_updateFields() {
        ThreadInfo info = new ThreadInfo();

        info.setName("main-thread");
        info.setId(99L);

        List<Frame> frames = new ArrayList<>();
        info.setFrames(frames);

        assertEquals("main-thread", info.getName());
        assertEquals(99L, info.getId());
        assertSame(frames, info.getFrames());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        ThreadInfo a = new ThreadInfo();
        a.setName("t");
        a.setId(1L);

        ThreadInfo b = new ThreadInfo();
        b.setName("t");
        b.setId(1L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsName() {
        ThreadInfo info = new ThreadInfo();
        info.setName("worker");
        assertEquals(true, info.toString().contains("worker"));
    }
}
