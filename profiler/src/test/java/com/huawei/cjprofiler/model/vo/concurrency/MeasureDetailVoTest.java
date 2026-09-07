/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.vo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter of {@link MeasureDetailVo} (excludes SDK-typed {@code relatedTaskDetail}).
 *
 * @since 2026-08-14
 */
class MeasureDetailVoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        MeasureDetailVo vo = new MeasureDetailVo();

        assertNull(vo.getType());
        assertNull(vo.getName());
        assertEquals(0, vo.getCjThreadId());
        assertEquals(0L, vo.getAvgDuration());
        assertEquals(0L, vo.getMinDuration());
        assertEquals(0L, vo.getMaxDuration());
        assertEquals(0L, vo.getTotalDuration());
        assertNull(vo.getRelatedTaskDetail());
        assertNull(vo.getChildren());
    }

    @Test
    void builder_setsAllOwnFields() {
        List<MeasureDetailVo> children = new ArrayList<>();

        MeasureDetailVo vo = MeasureDetailVo.builder()
            .type("STATE")
            .name("RUNNING")
            .cjThreadId(1)
            .avgDuration(10L)
            .minDuration(5L)
            .maxDuration(20L)
            .totalDuration(100L)
            .children(children)
            .build();

        assertEquals("STATE", vo.getType());
        assertEquals("RUNNING", vo.getName());
        assertEquals(1, vo.getCjThreadId());
        assertEquals(10L, vo.getAvgDuration());
        assertEquals(5L, vo.getMinDuration());
        assertEquals(20L, vo.getMaxDuration());
        assertEquals(100L, vo.getTotalDuration());
        assertSame(children, vo.getChildren());
        assertNull(vo.getRelatedTaskDetail());
    }

    @Test
    void setters_updateOwnFields() {
        MeasureDetailVo vo = new MeasureDetailVo();

        vo.setType("CJTHREAD");
        vo.setName("task-1");
        vo.setCjThreadId(2);
        vo.setAvgDuration(15L);
        vo.setMinDuration(8L);
        vo.setMaxDuration(25L);
        vo.setTotalDuration(150L);

        List<MeasureDetailVo> children = new ArrayList<>();
        vo.setChildren(children);

        assertEquals("CJTHREAD", vo.getType());
        assertEquals("task-1", vo.getName());
        assertEquals(2, vo.getCjThreadId());
        assertEquals(15L, vo.getAvgDuration());
        assertEquals(8L, vo.getMinDuration());
        assertEquals(25L, vo.getMaxDuration());
        assertEquals(150L, vo.getTotalDuration());
        assertSame(children, vo.getChildren());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        MeasureDetailVo a = MeasureDetailVo.builder()
            .type("t").name("n").cjThreadId(1).avgDuration(1L).minDuration(1L)
            .maxDuration(1L).totalDuration(1L).build();
        MeasureDetailVo b = MeasureDetailVo.builder()
            .type("t").name("n").cjThreadId(1).avgDuration(1L).minDuration(1L)
            .maxDuration(1L).totalDuration(1L).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
