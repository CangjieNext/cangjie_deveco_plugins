/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.global;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests constructor and getter of {@link CJPluginInfo}.
 *
 * @since 2026-08-14
 */
class CJPluginInfoTest {
    @Test
    void constructor_setsHasCJPluginApplied() {
        CJPluginInfo info = new CJPluginInfo(true);

        assertEquals(true, info.isHasCJPluginApplied());
    }

    @Test
    void constructor_setsHasCJPluginAppliedToFalse() {
        CJPluginInfo info = new CJPluginInfo(false);

        assertEquals(false, info.isHasCJPluginApplied());
    }

    @Test
    void setter_updatesHasCJPluginApplied() {
        CJPluginInfo info = new CJPluginInfo(false);

        info.setHasCJPluginApplied(true);

        assertEquals(true, info.isHasCJPluginApplied());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CJPluginInfo a = new CJPluginInfo(true);
        CJPluginInfo b = new CJPluginInfo(true);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsHasCJPluginApplied() {
        CJPluginInfo info = new CJPluginInfo(true);

        assertEquals(true, info.toString().contains("true"));
    }
}
