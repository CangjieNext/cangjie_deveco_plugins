/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.response.cjthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.model.vo.CjLaneSliceVo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests accessor of {@link CjLaneSliceData} record.
 *
 * @since 2026-08-14
 */
class CjLaneSliceDataTest {
    @Test
    void accessor_returnsCjLaneSliceInfo() {
        List<CjLaneSliceVo> slices = new ArrayList<>();

        CjLaneSliceData data = new CjLaneSliceData(slices);

        assertSame(slices, data.cjLaneSliceInfo());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        List<CjLaneSliceVo> slices = new ArrayList<>();
        CjLaneSliceData a = new CjLaneSliceData(slices);
        CjLaneSliceData b = new CjLaneSliceData(slices);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsClassName() {
        CjLaneSliceData data = new CjLaneSliceData(new ArrayList<>());
        assertEquals(true, data.toString().contains("CjLaneSliceData"));
    }
}
