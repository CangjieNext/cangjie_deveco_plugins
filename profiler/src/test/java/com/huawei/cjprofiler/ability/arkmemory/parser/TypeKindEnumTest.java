/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests value-to-type conversion behavior of {@link TypeKindEnum}.
 *
 * @since 2026-08-14
 */
class TypeKindEnumTest {
    @Test
    void getByValueReturnsMatchingReferenceAndValueTypes() {
        assertSame(TypeKindEnum.TYPE_KIND_CLASS, TypeKindEnum.getByValue(-128));
        assertSame(TypeKindEnum.TYPE_KIND_INT64, TypeKindEnum.getByValue(12));
        assertSame(TypeKindEnum.TYPE_KIND_MAX, TypeKindEnum.getByValue(24));
    }

    @Test
    void getByValueDefaultsToNothingForUnknownOrNullValue() {
        assertSame(TypeKindEnum.TYPE_KIND_NOTHING, TypeKindEnum.getByValue(999));
        assertSame(TypeKindEnum.TYPE_KIND_NOTHING, TypeKindEnum.getByValue(null));
    }

    @Test
    void getTypeNameByValueUsesSameDefaultingRule() {
        assertEquals("weakref_class", TypeKindEnum.getTypeNameByValue(-123));
        assertEquals("nothing", TypeKindEnum.getTypeNameByValue(Integer.MIN_VALUE));
        assertEquals("nothing", TypeKindEnum.getTypeNameByValue(null));
    }
}
