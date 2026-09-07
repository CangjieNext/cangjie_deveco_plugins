/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests getter/setter and constructors of {@link StackTrace}.
 *
 * @since 2026-08-14
 */
class StackTraceTest {
    @Test
    void noArgConstructor_setsDefaults() {
        StackTrace trace = new StackTrace();

        assertEquals(0L, trace.getIdx());
        assertEquals(0L, trace.getThread());
        assertEquals(0L, trace.getFrameNum());
        assertNull(trace.getFrames());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<String> frames = Arrays.asList("f1", "f2");

        StackTrace trace = new StackTrace(1L, 2L, 3L, frames);

        assertEquals(1L, trace.getIdx());
        assertEquals(2L, trace.getThread());
        assertEquals(3L, trace.getFrameNum());
        assertSame(frames, trace.getFrames());
    }

    @Test
    void builder_setsAllFields() {
        List<String> frames = new ArrayList<>();

        StackTrace trace = StackTrace.builder()
            .idx(10L)
            .thread(20L)
            .frameNum(30L)
            .frames(frames)
            .build();

        assertEquals(10L, trace.getIdx());
        assertEquals(20L, trace.getThread());
        assertEquals(30L, trace.getFrameNum());
        assertSame(frames, trace.getFrames());
    }

    @Test
    void setters_updateFields() {
        StackTrace trace = new StackTrace();

        trace.setIdx(5L);
        trace.setThread(6L);
        trace.setFrameNum(7L);

        List<String> frames = new ArrayList<>();
        trace.setFrames(frames);

        assertEquals(5L, trace.getIdx());
        assertEquals(6L, trace.getThread());
        assertEquals(7L, trace.getFrameNum());
        assertSame(frames, trace.getFrames());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        StackTrace a = StackTrace.builder().idx(1L).thread(2L).frameNum(3L).frames(new ArrayList<>()).build();
        StackTrace b = StackTrace.builder().idx(1L).thread(2L).frameNum(3L).frames(new ArrayList<>()).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
