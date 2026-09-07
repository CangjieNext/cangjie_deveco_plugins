/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.EdgeInfo;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeInfo;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link BaseTreeInfo} contract through {@link EdgeInfo} and {@link NodeInfo}.
 *
 * @since 2026-08-14
 */
class BaseTreeInfoTest {
    @Test
    void edgeInfoImplements_toIntArray() {
        EdgeInfo edge = new EdgeInfo(7, 9);

        assertTrue(edge instanceof BaseTreeInfo);
        int[] array = edge.toIntArray();

        assertEquals(3, array.length);
        assertEquals(3, array[0]);
        assertEquals(7, array[1]);
        assertEquals(9, array[2]);
    }

    @Test
    void nodeInfoImplements_toIntArray() {
        NodeInfo node = NodeInfo.builder()
            .type(1).name(2).id(3).selfSize(4)
            .edgeCount(5).traceNodeId(6).detachedness(7).nativeSize(8)
            .build();

        assertTrue(node instanceof BaseTreeInfo);
        int[] array = node.toIntArray();

        assertEquals(8, array.length);
        assertEquals(1, array[0]);
        assertEquals(2, array[1]);
        assertEquals(3, array[2]);
        assertEquals(4, array[3]);
        assertEquals(5, array[4]);
        assertEquals(6, array[5]);
        assertEquals(7, array[6]);
        assertEquals(8, array[7]);
    }
}
