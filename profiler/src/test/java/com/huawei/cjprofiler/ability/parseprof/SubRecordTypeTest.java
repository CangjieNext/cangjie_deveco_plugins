/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.parseprof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

/**
 * Tests type/size lookup and {@code fromType} resolution of {@link SubRecordType}.
 *
 * @since 2026-08-14
 */
class SubRecordTypeTest {
    @Test
    void getTypeAndGetSize_returnConfiguredValues() {
        assertEquals(255, SubRecordType.ROOT_UNKNOWN.getType());
        assertEquals(9, SubRecordType.ROOT_UNKNOWN.getSize());

        assertEquals(1, SubRecordType.ROOT_GLOBAL.getType());
        assertEquals(9, SubRecordType.ROOT_GLOBAL.getSize());

        assertEquals(2, SubRecordType.ROOT_LOCAL.getType());
        assertEquals(17, SubRecordType.ROOT_LOCAL.getSize());

        assertEquals(32, SubRecordType.CLASS_DUMP.getType());
        assertEquals(13, SubRecordType.CLASS_DUMP.getSize());

        assertEquals(33, SubRecordType.INSTANCE_DUMP.getType());
        assertEquals(21, SubRecordType.INSTANCE_DUMP.getSize());

        assertEquals(34, SubRecordType.OBJECT_ARRAY_DUMP.getType());
        assertEquals(21, SubRecordType.OBJECT_ARRAY_DUMP.getSize());

        assertEquals(35, SubRecordType.PRIMITIVE_ARRAY_DUMP.getType());
        assertEquals(14, SubRecordType.PRIMITIVE_ARRAY_DUMP.getSize());

        assertEquals(36, SubRecordType.STRUCT_ARRAY_DUMP.getType());
        assertEquals(25, SubRecordType.STRUCT_ARRAY_DUMP.getSize());

        assertEquals(45, SubRecordType.UNMOVABLE_STRUCT_ARRAY_DUMP.getType());
        assertEquals(25, SubRecordType.UNMOVABLE_STRUCT_ARRAY_DUMP.getSize());
    }

    @ParameterizedTest
    @EnumSource(SubRecordType.class)
    void fromType_resolvesKnownType(SubRecordType subRecordType) {
        Optional<SubRecordType> resolved = SubRecordType.fromType(subRecordType.getType());

        assertTrue(resolved.isPresent());
        assertEquals(subRecordType, resolved.get());
        assertEquals(subRecordType.getSize(), resolved.get().getSize());
    }

    @Test
    void fromType_returnsEmptyForUnknownType() {
        assertTrue(SubRecordType.fromType(0).isEmpty());
        assertTrue(SubRecordType.fromType(100).isEmpty());
        assertTrue(SubRecordType.fromType(-1).isEmpty());
        assertTrue(SubRecordType.fromType(Integer.MAX_VALUE).isEmpty());
    }

    @Test
    void enumConstantsCount_isSeventeen() {
        assertEquals(17, SubRecordType.values().length);
    }
}
