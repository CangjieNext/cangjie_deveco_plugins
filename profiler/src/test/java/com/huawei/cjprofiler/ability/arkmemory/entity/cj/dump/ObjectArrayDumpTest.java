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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests constructor and behavior of {@link ObjectArrayDump}.
 *
 * @since 2026-08-14
 */
class ObjectArrayDumpTest {
    @Test
    void constructor_setsInheritedFieldsAndObjectType() {
        List<Integer> refs = Arrays.asList(1, 2);

        ObjectArrayDump dump = new ObjectArrayDump(7, 8, refs);

        assertEquals(7, dump.getId());
        assertEquals(8, dump.getCls());
        assertSame(refs, dump.getRefIdList());
        assertEquals(NodeTypeEnum.ARRAY, dump.getObjectType());
    }

    @Test
    void constructor_assignsEmptyListWhenRefIdListIsNull() {
        ObjectArrayDump dump = new ObjectArrayDump(1, 2, null);

        assertEquals(0, dump.getRefIdList().size());
    }

    @Test
    void numField_defaultsToZeroWhenNotSet() {
        // num is a private field without a setter (only @Getter on the subclass).
        // The ObjectArrayDump constructor does not assign num, so it defaults to 0.
        ObjectArrayDump dump = new ObjectArrayDump(1, 2, new ArrayList<>());

        assertEquals(0, dump.getNum());
    }
}
