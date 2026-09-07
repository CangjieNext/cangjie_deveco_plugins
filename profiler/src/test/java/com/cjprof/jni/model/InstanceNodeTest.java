/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.cjprof.jni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter, builder and BaseNode contract of {@link InstanceNode}.
 *
 * @since 2026-08-14
 */
class InstanceNodeTest {
    @Test
    void noArgConstructor_initializesChildrenAndRetainers() {
        InstanceNode node = new InstanceNode();

        assertNotNull(node.getChildren());
        assertEquals(0, node.getChildren().size());
        assertNotNull(node.getRetainerNodes());
        assertEquals(0, node.getRetainerNodes().size());
        assertEquals(0L, node.getId());
        assertEquals(0, node.getNodeIndex());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<InstanceNode> children = new ArrayList<>();
        List<InstanceNode> retainers = new ArrayList<>();

        InstanceNode node = new InstanceNode(
            "Foo", 1, 20, 10, 0.5, 0.6, 100,
            children, retainers, 5L, 0, "Object", "global",
            2, 3, 0, 4, 8);

        assertEquals("Foo", node.getClassName());
        assertEquals(1, node.getDistance());
        assertEquals(20, node.getRetainedSize());
        assertEquals(10, node.getShallowSize());
        assertEquals(0.5, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.6, node.getRetainedSizePercent(), 0.0001);
        assertEquals(100, node.getTotalSize());
        assertEquals(children, node.getChildren());
        assertEquals(retainers, node.getRetainerNodes());
        assertEquals(5L, node.getId());
        assertEquals(0, node.getNodeIndex());
        assertEquals("Object", node.getType());
        assertEquals("global", node.getRootType());
        assertEquals(2, node.getChildrenCount());
        assertEquals(3, node.getRetainerCount());
        assertEquals(0, node.getStartPosition());
        assertEquals(4, node.getEndPosition());
        assertEquals(8, node.getArrayLength());
    }

    @Test
    void builder_setsAllFields() {
        InstanceNode node = InstanceNode.builder()
            .className("Bar")
            .distance(2)
            .retainedSize(30)
            .shallowSize(15)
            .shallowSizePercent(0.25)
            .retainedSizePercent(0.75)
            .totalSize(200)
            .id(7L)
            .nodeIndex(1)
            .type("String")
            .rootType("local")
            .childrenCount(4)
            .retainerCount(5)
            .startPosition(0)
            .endPosition(6)
            .arrayLength(12)
            .build();

        assertEquals("Bar", node.getClassName());
        assertEquals(2, node.getDistance());
        assertEquals(30, node.getRetainedSize());
        assertEquals(15, node.getShallowSize());
        assertEquals(0.25, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.75, node.getRetainedSizePercent(), 0.0001);
        assertEquals(200, node.getTotalSize());
        assertEquals(7L, node.getId());
        assertEquals(1, node.getNodeIndex());
        assertEquals("String", node.getType());
        assertEquals("local", node.getRootType());
        assertEquals(4, node.getChildrenCount());
        assertEquals(5, node.getRetainerCount());
        assertEquals(0, node.getStartPosition());
        assertEquals(6, node.getEndPosition());
        assertEquals(12, node.getArrayLength());
    }

    @Test
    void setters_updateFields() {
        InstanceNode node = new InstanceNode();

        node.setClassName("X");
        node.setDistance(3);
        node.setRetainedSize(40);
        node.setShallowSize(20);
        node.setShallowSizePercent(0.1);
        node.setRetainedSizePercent(0.2);
        node.setTotalSize(300);
        node.setId(8L);
        node.setNodeIndex(2);
        node.setType("Array");
        node.setRootType("unknown");
        node.setChildrenCount(6);
        node.setRetainerCount(7);
        node.setStartPosition(1);
        node.setEndPosition(9);
        node.setArrayLength(16);

        List<InstanceNode> children = new ArrayList<>();
        List<InstanceNode> retainers = new ArrayList<>();
        node.setChildren(children);
        node.setRetainerNodes(retainers);

        assertEquals("X", node.getClassName());
        assertEquals(3, node.getDistance());
        assertEquals(40, node.getRetainedSize());
        assertEquals(20, node.getShallowSize());
        assertEquals(0.1, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.2, node.getRetainedSizePercent(), 0.0001);
        assertEquals(300, node.getTotalSize());
        assertEquals(8L, node.getId());
        assertEquals(2, node.getNodeIndex());
        assertEquals("Array", node.getType());
        assertEquals("unknown", node.getRootType());
        assertEquals(6, node.getChildrenCount());
        assertEquals(7, node.getRetainerCount());
        assertEquals(1, node.getStartPosition());
        assertEquals(9, node.getEndPosition());
        assertEquals(16, node.getArrayLength());
        assertEquals(children, node.getChildren());
        assertEquals(retainers, node.getRetainerNodes());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        InstanceNode a = InstanceNode.builder()
            .className("C").distance(1).retainedSize(1).shallowSize(1)
            .shallowSizePercent(0).retainedSizePercent(0).totalSize(1)
            .id(1L).nodeIndex(0).type("t").rootType("r")
            .childrenCount(0).retainerCount(0).startPosition(0).endPosition(0).arrayLength(0)
            .build();
        InstanceNode b = InstanceNode.builder()
            .className("C").distance(1).retainedSize(1).shallowSize(1)
            .shallowSizePercent(0).retainedSizePercent(0).totalSize(1)
            .id(1L).nodeIndex(0).type("t").rootType("r")
            .childrenCount(0).retainerCount(0).startPosition(0).endPosition(0).arrayLength(0)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
