/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests the externally persisted values of {@link NodeTypeEnum}.
 *
 * @since 2026-08-14
 */
class NodeTypeEnumTest {
    @Test
    void nodeTypesExposeStablePersistedValues() {
        assertEquals(1, NodeTypeEnum.ARRAY.getValue());
        assertEquals(2, NodeTypeEnum.STRING.getValue());
        assertEquals(3, NodeTypeEnum.OBJECT.getValue());
    }
}
