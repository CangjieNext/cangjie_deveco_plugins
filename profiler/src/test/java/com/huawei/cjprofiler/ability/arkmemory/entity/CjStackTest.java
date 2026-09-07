/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and constructor behavior of {@link CjStack}.
 *
 * @since 2026-08-14
 */
class CjStackTest {
    @Test
    void noArgConstructor_setsDefaults() {
        CjStack stack = new CjStack();

        assertNull(stack.getNodeId());
        assertNull(stack.getFunctionInfoIndex());
        assertNull(stack.getFunctionId());
        assertNull(stack.getName());
        assertNull(stack.getScriptName());
        assertNull(stack.getRootPath());
        assertNull(stack.getLine());
        assertNull(stack.getColumn());
        assertNull(stack.getSize());
        assertNotNull(stack.getChildren());
        assertEquals(0, stack.getChildren().size());
    }

    @Test
    void setters_updateAllFields() {
        CjStack stack = new CjStack();

        stack.setNodeId(1);
        stack.setFunctionInfoIndex(2);
        stack.setFunctionId(3);
        stack.setName("main");
        stack.setScriptName("main.cj");
        stack.setRootPath("/root");
        stack.setLine(10);
        stack.setColumn(20);
        stack.setSize(100);

        List<CjStack> children = new ArrayList<>();
        stack.setChildren(children);

        assertEquals(1, stack.getNodeId());
        assertEquals(2, stack.getFunctionInfoIndex());
        assertEquals(3, stack.getFunctionId());
        assertEquals("main", stack.getName());
        assertEquals("main.cj", stack.getScriptName());
        assertEquals("/root", stack.getRootPath());
        assertEquals(10, stack.getLine());
        assertEquals(20, stack.getColumn());
        assertEquals(100, stack.getSize());
        assertSame(children, stack.getChildren());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        CjStack a = new CjStack();
        a.setNodeId(1);
        a.setFunctionInfoIndex(2);
        a.setFunctionId(3);
        a.setName("main");
        a.setScriptName("main.cj");
        a.setRootPath("/root");
        a.setLine(10);
        a.setColumn(20);
        a.setSize(100);

        CjStack b = new CjStack();
        b.setNodeId(1);
        b.setFunctionInfoIndex(2);
        b.setFunctionId(3);
        b.setName("main");
        b.setScriptName("main.cj");
        b.setRootPath("/root");
        b.setLine(10);
        b.setColumn(20);
        b.setSize(100);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsName() {
        CjStack stack = new CjStack();
        stack.setName("hello");
        assertEquals(true, stack.toString().contains("hello"));
    }
}
