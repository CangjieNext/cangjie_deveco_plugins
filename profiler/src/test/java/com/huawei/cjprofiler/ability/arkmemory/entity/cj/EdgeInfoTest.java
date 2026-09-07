/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.BaseTreeInfo;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter, constructors and {@code toIntArray} of {@link EdgeInfo}.
 *
 * @since 2026-08-14
 */
class EdgeInfoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        EdgeInfo edge = new EdgeInfo();

        assertEquals(0, edge.getType());
        assertEquals(0, edge.getIndex());
        assertEquals(0, edge.getToNode());
    }

    @Test
    void twoArgConstructor_setsTypeToThree() {
        EdgeInfo edge = new EdgeInfo(7, 9);

        assertEquals(3, edge.getType());
        assertEquals(7, edge.getIndex());
        assertEquals(9, edge.getToNode());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        EdgeInfo edge = new EdgeInfo(1, 2, 3);

        assertEquals(1, edge.getType());
        assertEquals(2, edge.getIndex());
        assertEquals(3, edge.getToNode());
    }

    @Test
    void builder_setsAllFields() {
        EdgeInfo edge = EdgeInfo.builder()
            .type(5)
            .index(6)
            .toNode(7)
            .build();

        assertEquals(5, edge.getType());
        assertEquals(6, edge.getIndex());
        assertEquals(7, edge.getToNode());
    }

    @Test
    void setters_updateFields() {
        EdgeInfo edge = new EdgeInfo();

        edge.setType(1);
        edge.setIndex(2);
        edge.setToNode(3);

        assertEquals(1, edge.getType());
        assertEquals(2, edge.getIndex());
        assertEquals(3, edge.getToNode());
    }

    @Test
    void toIntArray_returnsTypeIndexToNode() {
        EdgeInfo edge = new EdgeInfo(11, 22);

        int[] array = edge.toIntArray();

        assertArrayEquals(new int[] {3, 11, 22}, array);
    }

    @Test
    void toIntArray_viaBaseTreeInfoContract() {
        EdgeInfo edge = EdgeInfo.builder().type(1).index(2).toNode(3).build();

        assertTrue(edge instanceof BaseTreeInfo);
        int[] array = edge.toIntArray();

        assertArrayEquals(new int[] {1, 2, 3}, array);
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        EdgeInfo a = EdgeInfo.builder().type(1).index(2).toNode(3).build();
        EdgeInfo b = EdgeInfo.builder().type(1).index(2).toNode(3).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
