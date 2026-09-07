/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * RootTypeEnumTest - POC 验证测试链路是否畅通
 *
 * @since 2026-08-14
 */
class RootTypeEnumTest {
    @Test
    void getValueByKey_returnsCorrectValueForKnownKeys() {
        assertEquals("global", RootTypeEnum.getValueByKey((byte) 1));
        assertEquals("local", RootTypeEnum.getValueByKey((byte) 2));
        assertEquals("unknown", RootTypeEnum.getValueByKey((byte) 3));
        assertEquals("-", RootTypeEnum.getValueByKey((byte) 0));
    }

    @Test
    void getValueByKey_returnsNotRootForUnknownKey() {
        // 未知 key 应返回 NOT_ROOT 的 value "-"
        assertEquals("-", RootTypeEnum.getValueByKey((byte) 99));
        assertEquals("-", RootTypeEnum.getValueByKey((byte) -1));
    }

    @Test
    void getKeyByValue_returnsCorrectKeyForKnownValues() {
        assertEquals(Byte.valueOf((byte) 1), RootTypeEnum.getKeyByValue("global"));
        assertEquals(Byte.valueOf((byte) 2), RootTypeEnum.getKeyByValue("local"));
        assertEquals(Byte.valueOf((byte) 3), RootTypeEnum.getKeyByValue("unknown"));
        assertEquals(Byte.valueOf((byte) 0), RootTypeEnum.getKeyByValue("-"));
    }

    @Test
    void getKeyByValue_returnsNullForUnknownValue() {
        assertNull(RootTypeEnum.getKeyByValue("nonexistent"));
        assertNull(RootTypeEnum.getKeyByValue(null));
        assertNull(RootTypeEnum.getKeyByValue(""));
    }

    @ParameterizedTest
    @EnumSource(RootTypeEnum.class)
    void enumValues_haveValidKeyAndValue(RootTypeEnum type) {
        // 验证每个枚举值的 key 和 value 能双向解析
        assertEquals(type, RootTypeEnum.valueOf(type.name()));
        assertEquals(type.getKey(), RootTypeEnum.getKeyByValue(type.getValue()));
    }

    @Test
    void rootTypeNumConstant_isFour() {
        assertEquals(4, RootTypeEnum.ROOT_TYPE_NUM);
        assertEquals(4, RootTypeEnum.values().length);
    }
}
