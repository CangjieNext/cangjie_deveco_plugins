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
 * Tests getter/setter and constructors of {@link LocalObject}.
 *
 * @since 2026-08-14
 */
class LocalObjectTest {
    @Test
    void noArgConstructor_setsDefaults() {
        LocalObject obj = new LocalObject();

        assertNull(obj.getTypeName());
        assertEquals(0, obj.getObjectId());
        assertEquals(0, obj.getShallowSize());
        assertEquals(0, obj.getRetainedSize());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        LocalObject obj = new LocalObject("Foo", 1, 100, 200);

        assertEquals("Foo", obj.getTypeName());
        assertEquals(1, obj.getObjectId());
        assertEquals(100, obj.getShallowSize());
        assertEquals(200, obj.getRetainedSize());
    }

    @Test
    void builder_setsAllFields() {
        LocalObject obj = LocalObject.builder()
            .typeName("Bar")
            .objectId(2)
            .shallowSize(50)
            .retainedSize(150)
            .build();

        assertEquals("Bar", obj.getTypeName());
        assertEquals(2, obj.getObjectId());
        assertEquals(50, obj.getShallowSize());
        assertEquals(150, obj.getRetainedSize());
    }

    @Test
    void setters_updateFields() {
        LocalObject obj = new LocalObject();

        obj.setTypeName("Baz");
        obj.setObjectId(9);
        obj.setShallowSize(80);
        obj.setRetainedSize(180);

        assertEquals("Baz", obj.getTypeName());
        assertEquals(9, obj.getObjectId());
        assertEquals(80, obj.getShallowSize());
        assertEquals(180, obj.getRetainedSize());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        LocalObject a = LocalObject.builder().typeName("X").objectId(1).shallowSize(2).retainedSize(3).build();
        LocalObject b = LocalObject.builder().typeName("X").objectId(1).shallowSize(2).retainedSize(3).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
