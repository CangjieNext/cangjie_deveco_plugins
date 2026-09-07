/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.vo.cjthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter of {@link CjThreadSliceInfoVo} (excludes SDK-typed {@code relateThreadDetail}).
 *
 * @since 2026-08-14
 */
class CjThreadSliceInfoVoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadSliceInfoVo vo = new CjThreadSliceInfoVo();

        assertNull(vo.getName());
        assertEquals(0, vo.getThreadId());
        assertNull(vo.getState());
        assertNull(vo.getStartTime());
        assertNull(vo.getEndTime());
        assertNull(vo.getDuration());
        assertNull(vo.getRelateThreadDetail());
    }

    @Test
    void builder_setsPrimitiveAndStringFields() {
        CjThreadSliceInfoVo vo = CjThreadSliceInfoVo.builder()
            .name("thread-1")
            .threadId(5)
            .state("RUNNING")
            .startTime(100L)
            .endTime(200L)
            .duration(100L)
            .build();

        assertEquals("thread-1", vo.getName());
        assertEquals(5, vo.getThreadId());
        assertEquals("RUNNING", vo.getState());
        assertEquals(100L, vo.getStartTime());
        assertEquals(200L, vo.getEndTime());
        assertEquals(100L, vo.getDuration());
        assertNull(vo.getRelateThreadDetail());
    }

    @Test
    void setters_updatePrimitiveAndStringFields() {
        CjThreadSliceInfoVo vo = new CjThreadSliceInfoVo();

        vo.setName("thread-2");
        vo.setThreadId(6);
        vo.setState("SLEEPING");
        vo.setStartTime(300L);
        vo.setEndTime(400L);
        vo.setDuration(100L);

        assertEquals("thread-2", vo.getName());
        assertEquals(6, vo.getThreadId());
        assertEquals("SLEEPING", vo.getState());
        assertEquals(300L, vo.getStartTime());
        assertEquals(400L, vo.getEndTime());
        assertEquals(100L, vo.getDuration());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadSliceInfoVo a = CjThreadSliceInfoVo.builder()
            .name("n").threadId(1).state("S").startTime(1L).endTime(2L).duration(1L).build();
        CjThreadSliceInfoVo b = CjThreadSliceInfoVo.builder()
            .name("n").threadId(1).state("S").startTime(1L).endTime(2L).duration(1L).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
