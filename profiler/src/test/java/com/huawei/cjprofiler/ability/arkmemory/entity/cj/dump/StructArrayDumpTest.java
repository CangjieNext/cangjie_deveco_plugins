/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeTypeEnum;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests constructor and behavior of {@link StructArrayDump}.
 *
 * @since 2026-08-14
 */
class StructArrayDumpTest {
    @Test
    void constructor_setsAllFieldsAndObjectType() {
        List<Integer> refs = Arrays.asList(1, 2, 3);

        StructArrayDump dump = new StructArrayDump(5, 6, refs, 7);

        assertEquals(5, dump.getId());
        assertEquals(6, dump.getCls());
        assertSame(refs, dump.getRefIdList());
        assertEquals(7, dump.getNum());
        assertEquals(NodeTypeEnum.ARRAY, dump.getObjectType());
    }

    @Test
    void constructor_assignsEmptyListWhenRefIdListIsNull() {
        StructArrayDump dump = new StructArrayDump(1, 2, null, 3);

        assertEquals(0, dump.getRefIdList().size());
    }

    @Test
    void numField_defaultsToZeroWhenConstructedWithoutExplicitSet() {
        StructArrayDump dump = new StructArrayDump(1, 2, null, 0);

        assertEquals(0, dump.getNum());
    }
}
