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

import com.huawei.cjprofiler.model.vo.cjthread.CjThreadSliceInfoVo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests accessor of {@link CjThreadSliceInfoListData} record.
 *
 * @since 2026-08-14
 */
class CjThreadSliceInfoListDataTest {
    @Test
    void accessor_returnsCjThreadStateList() {
        List<CjThreadSliceInfoVo> states = new ArrayList<>();

        CjThreadSliceInfoListData data = new CjThreadSliceInfoListData(states);

        assertSame(states, data.cjThreadStateList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        List<CjThreadSliceInfoVo> states = new ArrayList<>();
        CjThreadSliceInfoListData a = new CjThreadSliceInfoListData(states);
        CjThreadSliceInfoListData b = new CjThreadSliceInfoListData(states);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsClassName() {
        CjThreadSliceInfoListData data = new CjThreadSliceInfoListData(new ArrayList<>());
        assertEquals(true, data.toString().contains("CjThreadSliceInfoListData"));
    }
}
