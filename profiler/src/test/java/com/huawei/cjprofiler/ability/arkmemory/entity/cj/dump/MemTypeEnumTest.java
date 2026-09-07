/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests memory-type size lookup behavior of {@link MemTypeEnum}.
 *
 * @since 2026-08-14
 */
class MemTypeEnumTest {
    @ParameterizedTest
    @CsvSource({"2, 0", "4, 1", "9, 2", "10, 4", "11, 8"})
    void getSizeReturnsConfiguredSize(int type, int expectedSize) {
        assertEquals(expectedSize, MemTypeEnum.getSize(type));
    }

    @ParameterizedTest
    @CsvSource({"-1", "0", "3", "999"})
    void getSizeReturnsZeroForUnknownType(int type) {
        assertEquals(0, MemTypeEnum.getSize(type));
    }
}
