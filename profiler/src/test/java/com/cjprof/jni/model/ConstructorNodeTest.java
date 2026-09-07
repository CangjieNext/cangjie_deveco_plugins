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
 * Tests getter/setter, builder and BaseNode contract of {@link ConstructorNode}.
 *
 * @since 2026-08-14
 */
class ConstructorNodeTest {
    @Test
    void noArgConstructor_initializesChildrenToEmptyList() {
        ConstructorNode node = new ConstructorNode();

        assertNotNull(node.getChildren());
        assertEquals(0, node.getChildren().size());
        assertEquals(0, node.getTotalSize());
        assertEquals(0L, node.getId());
        assertEquals(0, node.getNodeIndex());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<InstanceNode> children = new ArrayList<>();

        ConstructorNode node = new ConstructorNode(
            "Foo", 100, 1L, 0, 3, 2,
            10, 20, 0.1, 0.2, 0.3,
            children, 0, 5);

        assertEquals("Foo", node.getClassName());
        assertEquals(100, node.getTotalSize());
        assertEquals(1L, node.getId());
        assertEquals(0, node.getNodeIndex());
        assertEquals(3, node.getChildrenCount());
        assertEquals(2, node.getDistance());
        assertEquals(10, node.getShallowSize());
        assertEquals(20, node.getRetainedSize());
        assertEquals(0.1, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.2, node.getRetainedSizePercent(), 0.0001);
        assertEquals(0.3, node.getTotalInstanceCountPercent(), 0.0001);
        assertEquals(children, node.getChildren());
        assertEquals(0, node.getStartPosition());
        assertEquals(5, node.getEndPosition());
    }

    @Test
    void builder_setsAllFields() {
        ConstructorNode node = ConstructorNode.builder()
            .className("Bar")
            .totalSize(50)
            .id(7L)
            .nodeIndex(1)
            .childrenCount(2)
            .distance(1)
            .shallowSize(5)
            .retainedSize(15)
            .shallowSizePercent(0.5)
            .retainedSizePercent(0.6)
            .totalInstanceCountPercent(0.7)
            .startPosition(0)
            .endPosition(3)
            .build();

        assertEquals("Bar", node.getClassName());
        assertEquals(50, node.getTotalSize());
        assertEquals(7L, node.getId());
        assertEquals(1, node.getNodeIndex());
        assertEquals(2, node.getChildrenCount());
        assertEquals(1, node.getDistance());
        assertEquals(5, node.getShallowSize());
        assertEquals(15, node.getRetainedSize());
        assertEquals(0.5, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.6, node.getRetainedSizePercent(), 0.0001);
        assertEquals(0.7, node.getTotalInstanceCountPercent(), 0.0001);
        assertEquals(0, node.getStartPosition());
        assertEquals(3, node.getEndPosition());
    }

    @Test
    void setters_updateFields() {
        ConstructorNode node = new ConstructorNode();

        node.setClassName("Cls");
        node.setTotalSize(80);
        node.setId(4L);
        node.setNodeIndex(9);
        node.setChildrenCount(1);
        node.setDistance(5);
        node.setShallowSize(8);
        node.setRetainedSize(16);
        node.setShallowSizePercent(0.25);
        node.setRetainedSizePercent(0.75);
        node.setTotalInstanceCountPercent(0.5);
        node.setStartPosition(2);
        node.setEndPosition(8);

        List<InstanceNode> children = new ArrayList<>();
        node.setChildren(children);

        assertEquals("Cls", node.getClassName());
        assertEquals(80, node.getTotalSize());
        assertEquals(4L, node.getId());
        assertEquals(9, node.getNodeIndex());
        assertEquals(1, node.getChildrenCount());
        assertEquals(5, node.getDistance());
        assertEquals(8, node.getShallowSize());
        assertEquals(16, node.getRetainedSize());
        assertEquals(0.25, node.getShallowSizePercent(), 0.0001);
        assertEquals(0.75, node.getRetainedSizePercent(), 0.0001);
        assertEquals(0.5, node.getTotalInstanceCountPercent(), 0.0001);
        assertEquals(2, node.getStartPosition());
        assertEquals(8, node.getEndPosition());
        assertEquals(children, node.getChildren());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        ConstructorNode a = ConstructorNode.builder()
            .className("C").totalSize(1).id(1L).nodeIndex(0)
            .childrenCount(0).distance(0).shallowSize(0).retainedSize(0)
            .shallowSizePercent(0).retainedSizePercent(0).totalInstanceCountPercent(0)
            .startPosition(0).endPosition(0).build();
        ConstructorNode b = ConstructorNode.builder()
            .className("C").totalSize(1).id(1L).nodeIndex(0)
            .childrenCount(0).distance(0).shallowSize(0).retainedSize(0)
            .shallowSizePercent(0).retainedSizePercent(0).totalInstanceCountPercent(0)
            .startPosition(0).endPosition(0).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
