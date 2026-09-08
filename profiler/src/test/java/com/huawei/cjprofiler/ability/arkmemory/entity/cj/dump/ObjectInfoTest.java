/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeTypeEnum;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests constructor and inherited getters of {@link ObjectInfo}.
 *
 * @since 2026-08-14
 */
class ObjectInfoTest {
    @Test
    void constructor_setsIdClsAndRefIdList() {
        List<Integer> refs = Arrays.asList(1, 2, 3);

        ObjectInfo info = new ObjectInfo(10, 20, refs);

        assertEquals(10, info.getId());
        assertEquals(20, info.getCls());
        assertSame(refs, info.getRefIdList());
        assertEquals(NodeTypeEnum.OBJECT, info.getObjectType());
    }

    @Test
    void constructor_assignsEmptyListWhenRefIdListIsNull() {
        ObjectInfo info = new ObjectInfo(1, 2, null);

        assertNotNull(info.getRefIdList());
        assertEquals(0, info.getRefIdList().size());
    }

    @Test
    void constructor_assignsEmptyListWhenRefIdListIsEmpty() {
        ObjectInfo info = new ObjectInfo(1, 2, new ArrayList<>());

        assertNotNull(info.getRefIdList());
        assertEquals(0, info.getRefIdList().size());
    }

    @Test
    void setters_updateInheritedFields() {
        ObjectInfo info = new ObjectInfo(0, 0, null);

        info.setId(100);
        info.setSize(256);
        info.setCls(5);
        info.setName(9);
        info.setObjectType(NodeTypeEnum.STRING);

        List<Integer> refs = new ArrayList<>();
        info.setRefIdList(refs);

        assertEquals(100, info.getId());
        assertEquals(256, info.getSize());
        assertEquals(5, info.getCls());
        assertEquals(9, info.getName());
        assertEquals(NodeTypeEnum.STRING, info.getObjectType());
        assertSame(refs, info.getRefIdList());
    }
}
