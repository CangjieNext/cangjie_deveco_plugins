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

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeTypeEnum;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * Tests constructor and behavior of {@link PrimitiveArrayDump}.
 *
 * @since 2026-08-14
 */
class PrimitiveArrayDumpTest {
    @Test
    void constructor_setsFieldsAndObjectType() {
        PrimitiveArrayDump dump = new PrimitiveArrayDump(1, 10, 4);

        assertEquals(1, dump.getId());
        assertEquals(10, dump.getNum());
        assertEquals(4, dump.getType());
        assertEquals(NodeTypeEnum.ARRAY, dump.getObjectType());
        assertEquals(0, dump.getCls());
        assertNotNull(dump.getRefIdList());
        assertEquals(0, dump.getRefIdList().size());
    }

    @Test
    void setters_updateNumAndType() {
        PrimitiveArrayDump dump = new PrimitiveArrayDump(0, 0, 0);

        dump.setNum(20);
        dump.setType(8);

        assertEquals(20, dump.getNum());
        assertEquals(8, dump.getType());
    }

    @Test
    void setters_updateInheritedFields() {
        PrimitiveArrayDump dump = new PrimitiveArrayDump(0, 0, 0);

        dump.setId(99);
        dump.setSize(512);
        dump.setName(3);
        dump.setObjectType(NodeTypeEnum.OBJECT);
        dump.setRefIdList(new ArrayList<>());

        assertEquals(99, dump.getId());
        assertEquals(512, dump.getSize());
        assertEquals(3, dump.getName());
        assertEquals(NodeTypeEnum.OBJECT, dump.getObjectType());
        assertNotNull(dump.getRefIdList());
    }
}
