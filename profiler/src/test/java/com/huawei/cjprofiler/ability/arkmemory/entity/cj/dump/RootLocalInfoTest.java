/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and constructors of {@link RootLocalInfo}.
 *
 * @since 2026-08-14
 */
class RootLocalInfoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        RootLocalInfo info = new RootLocalInfo();

        assertEquals(0, info.getId());
        assertEquals(0L, info.getThreadIdx());
        assertEquals(0, info.getFrameId());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        RootLocalInfo info = new RootLocalInfo(1, 2L, 3);

        assertEquals(1, info.getId());
        assertEquals(2L, info.getThreadIdx());
        assertEquals(3, info.getFrameId());
    }

    @Test
    void builder_setsAllFields() {
        RootLocalInfo info = RootLocalInfo.builder()
            .id(10)
            .threadIdx(20L)
            .frameId(30)
            .build();

        assertEquals(10, info.getId());
        assertEquals(20L, info.getThreadIdx());
        assertEquals(30, info.getFrameId());
    }

    @Test
    void setters_updateFields() {
        RootLocalInfo info = new RootLocalInfo();

        info.setId(5);
        info.setThreadIdx(6L);
        info.setFrameId(7);

        assertEquals(5, info.getId());
        assertEquals(6L, info.getThreadIdx());
        assertEquals(7, info.getFrameId());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        RootLocalInfo a = RootLocalInfo.builder().id(1).threadIdx(2L).frameId(3).build();
        RootLocalInfo b = RootLocalInfo.builder().id(1).threadIdx(2L).frameId(3).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
