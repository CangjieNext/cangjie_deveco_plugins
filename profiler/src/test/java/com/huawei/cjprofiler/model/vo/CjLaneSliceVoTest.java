/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and constructors of {@link CjLaneSliceVo}.
 *
 * @since 2026-08-14
 */
class CjLaneSliceVoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjLaneSliceVo vo = new CjLaneSliceVo();

        assertNull(vo.getTimeStamp());
        assertNull(vo.getValue());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        CjLaneSliceVo vo = new CjLaneSliceVo(100L, 5);

        assertEquals(100L, vo.getTimeStamp());
        assertEquals(5, vo.getValue());
    }

    @Test
    void builder_setsAllFields() {
        CjLaneSliceVo vo = CjLaneSliceVo.builder()
            .timeStamp(200L)
            .value(10)
            .build();

        assertEquals(200L, vo.getTimeStamp());
        assertEquals(10, vo.getValue());
    }

    @Test
    void setters_updateFields() {
        CjLaneSliceVo vo = new CjLaneSliceVo();

        vo.setTimeStamp(300L);
        vo.setValue(15);

        assertEquals(300L, vo.getTimeStamp());
        assertEquals(15, vo.getValue());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjLaneSliceVo a = CjLaneSliceVo.builder().timeStamp(1L).value(2).build();
        CjLaneSliceVo b = CjLaneSliceVo.builder().timeStamp(1L).value(2).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
