/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.po.cjthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and constructors of {@link CjThreadSliceInfoPo}.
 *
 * @since 2026-08-14
 */
class CjThreadSliceInfoPoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadSliceInfoPo po = new CjThreadSliceInfoPo();

        assertNull(po.getState());
        assertEquals(0, po.getCjThreadId());
        assertEquals(0L, po.getStartTime());
        assertEquals(0L, po.getEndTime());
        assertEquals(0L, po.getDuration());
        assertEquals(0, po.getThreadId());
        assertEquals(0, po.getProcessId());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        CjThreadSliceInfoPo po = new CjThreadSliceInfoPo("RUNNING", 1, 100L, 200L, 100L, 5, 9);

        assertEquals("RUNNING", po.getState());
        assertEquals(1, po.getCjThreadId());
        assertEquals(100L, po.getStartTime());
        assertEquals(200L, po.getEndTime());
        assertEquals(100L, po.getDuration());
        assertEquals(5, po.getThreadId());
        assertEquals(9, po.getProcessId());
    }

    @Test
    void builder_setsAllFields() {
        CjThreadSliceInfoPo po = CjThreadSliceInfoPo.builder()
            .state("SLEEPING")
            .cjThreadId(2)
            .startTime(300L)
            .endTime(400L)
            .duration(100L)
            .threadId(6)
            .processId(10)
            .build();

        assertEquals("SLEEPING", po.getState());
        assertEquals(2, po.getCjThreadId());
        assertEquals(300L, po.getStartTime());
        assertEquals(400L, po.getEndTime());
        assertEquals(100L, po.getDuration());
        assertEquals(6, po.getThreadId());
        assertEquals(10, po.getProcessId());
    }

    @Test
    void setters_updateFields() {
        CjThreadSliceInfoPo po = new CjThreadSliceInfoPo();

        po.setState("WAITING");
        po.setCjThreadId(3);
        po.setStartTime(500L);
        po.setEndTime(600L);
        po.setDuration(100L);
        po.setThreadId(7);
        po.setProcessId(11);

        assertEquals("WAITING", po.getState());
        assertEquals(3, po.getCjThreadId());
        assertEquals(500L, po.getStartTime());
        assertEquals(600L, po.getEndTime());
        assertEquals(100L, po.getDuration());
        assertEquals(7, po.getThreadId());
        assertEquals(11, po.getProcessId());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadSliceInfoPo a = CjThreadSliceInfoPo.builder()
            .state("S").cjThreadId(1).startTime(1L).endTime(2L)
            .duration(1L).threadId(3).processId(4).build();
        CjThreadSliceInfoPo b = CjThreadSliceInfoPo.builder()
            .state("S").cjThreadId(1).startTime(1L).endTime(2L)
            .duration(1L).threadId(3).processId(4).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
