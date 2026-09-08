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
 * Tests getter/setter and constructors of {@link CjThreadInfoVo}.
 *
 * @since 2026-08-14
 */
class CjThreadInfoVoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjThreadInfoVo vo = new CjThreadInfoVo();

        assertNull(vo.getCjThreadId());
        assertNull(vo.getCjThreadName());
        assertNull(vo.getProcessId());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        CjThreadInfoVo vo = new CjThreadInfoVo(1, "main", 100);

        assertEquals(1, vo.getCjThreadId());
        assertEquals("main", vo.getCjThreadName());
        assertEquals(100, vo.getProcessId());
    }

    @Test
    void builder_setsAllFields() {
        CjThreadInfoVo vo = CjThreadInfoVo.builder()
            .cjThreadId(2)
            .cjThreadName("worker")
            .processId(200)
            .build();

        assertEquals(2, vo.getCjThreadId());
        assertEquals("worker", vo.getCjThreadName());
        assertEquals(200, vo.getProcessId());
    }

    @Test
    void setters_updateFields() {
        CjThreadInfoVo vo = new CjThreadInfoVo();

        vo.setCjThreadId(3);
        vo.setCjThreadName("bg");
        vo.setProcessId(300);

        assertEquals(3, vo.getCjThreadId());
        assertEquals("bg", vo.getCjThreadName());
        assertEquals(300, vo.getProcessId());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjThreadInfoVo a = CjThreadInfoVo.builder().cjThreadId(1).cjThreadName("t").processId(2).build();
        CjThreadInfoVo b = CjThreadInfoVo.builder().cjThreadId(1).cjThreadName("t").processId(2).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
