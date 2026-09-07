/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.cjprof.jni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and builder behavior of {@link InstanceDiffNode}.
 *
 * @since 2026-08-14
 */
class InstanceDiffNodeTest {
    @Test
    void noArgConstructor_setsDefaults() {
        InstanceDiffNode node = new InstanceDiffNode();

        assertEquals(0, node.getAddedCount());
        assertEquals(0, node.getRemovedCount());
        assertEquals(0L, node.getCountDelta());
        assertEquals(0, node.getAddedSize());
        assertEquals(0, node.getRemovedSize());
        assertEquals(0L, node.getSizeDelta());
        assertEquals(false, node.isAdded());
    }

    @Test
    void builder_setsAllFields() {
        InstanceDiffNode node = InstanceDiffNode.builder()
            .className("Foo")
            .id(2L)
            .nodeIndex(1)
            .addedCount(3)
            .removedCount(1)
            .countDelta(2L)
            .addedSize(50)
            .removedSize(10)
            .sizeDelta(40L)
            .added(true)
            .build();

        assertEquals("Foo", node.getClassName());
        assertEquals(2L, node.getId());
        assertEquals(1, node.getNodeIndex());
        assertEquals(3, node.getAddedCount());
        assertEquals(1, node.getRemovedCount());
        assertEquals(2L, node.getCountDelta());
        assertEquals(50, node.getAddedSize());
        assertEquals(10, node.getRemovedSize());
        assertEquals(40L, node.getSizeDelta());
        assertEquals(true, node.isAdded());
    }

    @Test
    void setters_updateFields() {
        InstanceDiffNode node = new InstanceDiffNode();

        node.setAddedCount(8);
        node.setRemovedCount(4);
        node.setCountDelta(4L);
        node.setAddedSize(80);
        node.setRemovedSize(20);
        node.setSizeDelta(60L);
        node.setAdded(true);

        assertEquals(8, node.getAddedCount());
        assertEquals(4, node.getRemovedCount());
        assertEquals(4L, node.getCountDelta());
        assertEquals(80, node.getAddedSize());
        assertEquals(20, node.getRemovedSize());
        assertEquals(60L, node.getSizeDelta());
        assertEquals(true, node.isAdded());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        InstanceDiffNode a = InstanceDiffNode.builder()
            .className("C").id(1L).nodeIndex(0)
            .addedCount(1).removedCount(1).countDelta(0L)
            .addedSize(1).removedSize(1).sizeDelta(0L).added(true)
            .build();
        InstanceDiffNode b = InstanceDiffNode.builder()
            .className("C").id(1L).nodeIndex(0)
            .addedCount(1).removedCount(1).countDelta(0L)
            .addedSize(1).removedSize(1).sizeDelta(0L).added(true)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
