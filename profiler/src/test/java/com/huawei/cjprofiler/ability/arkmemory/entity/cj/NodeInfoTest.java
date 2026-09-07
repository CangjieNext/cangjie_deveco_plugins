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
 * Tests getter/setter, constructors and {@code toIntArray} of {@link NodeInfo}.
 *
 * @since 2026-08-14
 */
class NodeInfoTest {
    @Test
    void noArgConstructor_setsDefaults() {
        NodeInfo node = new NodeInfo();

        assertEquals(0, node.getType());
        assertEquals(0, node.getName());
        assertEquals(0, node.getId());
        assertEquals(0, node.getSelfSize());
        assertEquals(0, node.getEdgeCount());
        assertEquals(0, node.getTraceNodeId());
        assertEquals(0, node.getDetachedness());
        assertEquals(0, node.getNativeSize());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        NodeInfo node = new NodeInfo(1, 2, 3, 4, 5, 6, 7, 8);

        assertEquals(1, node.getType());
        assertEquals(2, node.getName());
        assertEquals(3, node.getId());
        assertEquals(4, node.getSelfSize());
        assertEquals(5, node.getEdgeCount());
        assertEquals(6, node.getTraceNodeId());
        assertEquals(7, node.getDetachedness());
        assertEquals(8, node.getNativeSize());
    }

    @Test
    void builder_setsAllFields() {
        NodeInfo node = NodeInfo.builder()
            .type(10).name(20).id(30).selfSize(40)
            .edgeCount(50).traceNodeId(60).detachedness(70).nativeSize(80)
            .build();

        assertEquals(10, node.getType());
        assertEquals(20, node.getName());
        assertEquals(30, node.getId());
        assertEquals(40, node.getSelfSize());
        assertEquals(50, node.getEdgeCount());
        assertEquals(60, node.getTraceNodeId());
        assertEquals(70, node.getDetachedness());
        assertEquals(80, node.getNativeSize());
    }

    @Test
    void setters_updateFields() {
        NodeInfo node = new NodeInfo();

        node.setType(1);
        node.setName(2);
        node.setId(3);
        node.setSelfSize(4);
        node.setEdgeCount(5);
        node.setTraceNodeId(6);
        node.setDetachedness(7);
        node.setNativeSize(8);

        assertEquals(1, node.getType());
        assertEquals(2, node.getName());
        assertEquals(3, node.getId());
        assertEquals(4, node.getSelfSize());
        assertEquals(5, node.getEdgeCount());
        assertEquals(6, node.getTraceNodeId());
        assertEquals(7, node.getDetachedness());
        assertEquals(8, node.getNativeSize());
    }

    @Test
    void toIntArray_returnsAllFieldsInOrder() {
        NodeInfo node = NodeInfo.builder()
            .type(1).name(2).id(3).selfSize(4)
            .edgeCount(5).traceNodeId(6).detachedness(7).nativeSize(8)
            .build();

        assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8}, node.toIntArray());
    }

    @Test
    void toIntArray_viaBaseTreeInfoContract() {
        NodeInfo node = NodeInfo.builder().type(9).build();

        assertTrue(node instanceof BaseTreeInfo);
        int[] array = node.toIntArray();

        assertEquals(9, array[0]);
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        NodeInfo a = NodeInfo.builder()
            .type(1).name(2).id(3).selfSize(4)
            .edgeCount(5).traceNodeId(6).detachedness(7).nativeSize(8)
            .build();
        NodeInfo b = NodeInfo.builder()
            .type(1).name(2).id(3).selfSize(4)
            .edgeCount(5).traceNodeId(6).detachedness(7).nativeSize(8)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
