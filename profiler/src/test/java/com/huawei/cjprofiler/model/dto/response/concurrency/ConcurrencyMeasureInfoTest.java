/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.response.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.model.vo.concurrency.MeasureDetailVo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and constructors of {@link ConcurrencyMeasureInfo}.
 *
 * @since 2026-08-14
 */
class ConcurrencyMeasureInfoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        ConcurrencyMeasureInfo info = new ConcurrencyMeasureInfo();

        assertNull(info.getMeasureInfoDetail());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<MeasureDetailVo> details = new ArrayList<>();

        ConcurrencyMeasureInfo info = new ConcurrencyMeasureInfo(details);

        assertSame(details, info.getMeasureInfoDetail());
    }

    @Test
    void setter_updatesField() {
        ConcurrencyMeasureInfo info = new ConcurrencyMeasureInfo();

        List<MeasureDetailVo> details = new ArrayList<>();
        info.setMeasureInfoDetail(details);

        assertSame(details, info.getMeasureInfoDetail());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        ConcurrencyMeasureInfo a = new ConcurrencyMeasureInfo(new ArrayList<>());
        ConcurrencyMeasureInfo b = new ConcurrencyMeasureInfo(new ArrayList<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
