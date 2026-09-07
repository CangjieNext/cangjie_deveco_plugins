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

import com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests accessor of {@link CjThreadInfoData} record.
 *
 * @since 2026-08-14
 */
class CjThreadInfoDataTest {
    @Test
    void accessor_returnsCjThreadInfoList() {
        List<CjThreadInfoVo> infos = new ArrayList<>();

        CjThreadInfoData data = new CjThreadInfoData(infos);

        assertSame(infos, data.cjThreadInfoList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        List<CjThreadInfoVo> infos = new ArrayList<>();
        CjThreadInfoData a = new CjThreadInfoData(infos);
        CjThreadInfoData b = new CjThreadInfoData(infos);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsClassName() {
        CjThreadInfoData data = new CjThreadInfoData(new ArrayList<>());
        assertEquals(true, data.toString().contains("CjThreadInfoData"));
    }
}
