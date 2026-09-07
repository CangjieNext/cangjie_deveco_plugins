/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.ability.arkmemory.entity.CjStack;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests constructor and getter of {@link CjStackList}.
 *
 * @since 2026-08-14
 */
class CjStackListTest {
    @Test
    void constructor_setsCjStackList() {
        List<CjStack> stacks = new ArrayList<>();

        CjStackList list = new CjStackList(stacks);

        assertSame(stacks, list.getCjStackList());
    }

    @Test
    void setter_updatesCjStackList() {
        CjStackList list = new CjStackList(new ArrayList<>());

        List<CjStack> stacks = new ArrayList<>();
        list.setCjStackList(stacks);

        assertSame(stacks, list.getCjStackList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjStackList a = new CjStackList(new ArrayList<>());
        CjStackList b = new CjStackList(new ArrayList<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
