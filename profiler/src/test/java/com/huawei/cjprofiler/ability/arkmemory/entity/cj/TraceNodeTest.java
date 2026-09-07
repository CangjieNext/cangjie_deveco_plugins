/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and constructors of {@link TraceNode}.
 *
 * @since 2026-08-14
 */
class TraceNodeTest {
    @Test
    void noArgConstructor_setsDefaults() {
        TraceNode node = new TraceNode();

        assertNull(node.getId());
        assertNull(node.getFunctionInfoIndex());
        assertNull(node.getCount());
        assertNull(node.getSize());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        TraceNode node = new TraceNode("1", "2", "3", "4");

        assertEquals("1", node.getId());
        assertEquals("2", node.getFunctionInfoIndex());
        assertEquals("3", node.getCount());
        assertEquals("4", node.getSize());
    }

    @Test
    void builder_setsAllFields() {
        TraceNode node = TraceNode.builder()
            .id("a")
            .functionInfoIndex("b")
            .count("c")
            .size("d")
            .build();

        assertEquals("a", node.getId());
        assertEquals("b", node.getFunctionInfoIndex());
        assertEquals("c", node.getCount());
        assertEquals("d", node.getSize());
    }

    @Test
    void setters_updateFields() {
        TraceNode node = new TraceNode();

        node.setId("10");
        node.setFunctionInfoIndex("20");
        node.setCount("30");
        node.setSize("40");

        assertEquals("10", node.getId());
        assertEquals("20", node.getFunctionInfoIndex());
        assertEquals("30", node.getCount());
        assertEquals("40", node.getSize());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        TraceNode a = TraceNode.builder().id("1").functionInfoIndex("2").count("3").size("4").build();
        TraceNode b = TraceNode.builder().id("1").functionInfoIndex("2").count("3").size("4").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
