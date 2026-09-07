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

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and builder behavior of {@link ConstructorDiffNode}.
 *
 * @since 2026-08-14
 */
class ConstructorDiffNodeTest {
    @Test
    void noArgConstructor_setsDefaults() {
        ConstructorDiffNode node = new ConstructorDiffNode();

        assertEquals(0, node.getAddedCount());
        assertEquals(0, node.getRemovedCount());
        assertEquals(0L, node.getCountDelta());
        assertEquals(0, node.getAddedSize());
        assertEquals(0, node.getRemovedSize());
        assertEquals(0L, node.getSizeDelta());
        assertEquals(0, node.getBaseTotalSize());
        assertEquals(0, node.getTargetTotalSize());
    }

    @Test
    void builder_setsAllFields() {
        List<Boolean> childAddedStates = new ArrayList<>();
        childAddedStates.add(true);
        childAddedStates.add(false);

        ConstructorDiffNode node = ConstructorDiffNode.builder()
            .className("Foo")
            .id(1L)
            .nodeIndex(0)
            .addedCount(5)
            .removedCount(2)
            .countDelta(3L)
            .addedSize(100)
            .removedSize(40)
            .sizeDelta(60L)
            .baseTotalSize(200)
            .targetTotalSize(260)
            .childAddedStates(childAddedStates)
            .build();

        assertEquals("Foo", node.getClassName());
        assertEquals(1L, node.getId());
        assertEquals(0, node.getNodeIndex());
        assertEquals(5, node.getAddedCount());
        assertEquals(2, node.getRemovedCount());
        assertEquals(3L, node.getCountDelta());
        assertEquals(100, node.getAddedSize());
        assertEquals(40, node.getRemovedSize());
        assertEquals(60L, node.getSizeDelta());
        assertEquals(200, node.getBaseTotalSize());
        assertEquals(260, node.getTargetTotalSize());
        assertEquals(childAddedStates, node.getChildAddedStates());
    }

    @Test
    void setters_updateFields() {
        ConstructorDiffNode node = new ConstructorDiffNode();

        node.setAddedCount(7);
        node.setRemovedCount(3);
        node.setCountDelta(4L);
        node.setAddedSize(150);
        node.setRemovedSize(60);
        node.setSizeDelta(90L);
        node.setBaseTotalSize(300);
        node.setTargetTotalSize(390);

        List<Boolean> states = new ArrayList<>();
        node.setChildAddedStates(states);

        assertEquals(7, node.getAddedCount());
        assertEquals(3, node.getRemovedCount());
        assertEquals(4L, node.getCountDelta());
        assertEquals(150, node.getAddedSize());
        assertEquals(60, node.getRemovedSize());
        assertEquals(90L, node.getSizeDelta());
        assertEquals(300, node.getBaseTotalSize());
        assertEquals(390, node.getTargetTotalSize());
        assertEquals(states, node.getChildAddedStates());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        ConstructorDiffNode a = ConstructorDiffNode.builder()
            .className("C").id(1L).nodeIndex(0)
            .addedCount(1).removedCount(1).countDelta(0L)
            .addedSize(1).removedSize(1).sizeDelta(0L)
            .baseTotalSize(1).targetTotalSize(1)
            .build();
        ConstructorDiffNode b = ConstructorDiffNode.builder()
            .className("C").id(1L).nodeIndex(0)
            .addedCount(1).removedCount(1).countDelta(0L)
            .addedSize(1).removedSize(1).sizeDelta(0L)
            .baseTotalSize(1).targetTotalSize(1)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
