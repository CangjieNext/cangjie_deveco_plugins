/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.EdgeInfo;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeInfo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests getter/setter and constructors of {@link TreeInfo}.
 *
 * @since 2026-08-14
 */
class TreeInfoTest {
    @Test
    void noArgConstructor_initializesEmptyLists() {
        TreeInfo info = new TreeInfo();

        assertNotNull(info.getEdgeInfoList());
        assertEquals(0, info.getEdgeInfoList().size());
        assertNotNull(info.getNodeInfoList());
        assertEquals(0, info.getNodeInfoList().size());
    }

    @Test
    void allArgConstructor_setsAllFields() {
        List<EdgeInfo> edges = new ArrayList<>();
        List<NodeInfo> nodes = new ArrayList<>();

        TreeInfo info = new TreeInfo(edges, nodes);

        assertSame(edges, info.getEdgeInfoList());
        assertSame(nodes, info.getNodeInfoList());
    }

    @Test
    void setters_updateFields() {
        TreeInfo info = new TreeInfo();

        List<EdgeInfo> edges = new ArrayList<>();
        List<NodeInfo> nodes = new ArrayList<>();
        info.setEdgeInfoList(edges);
        info.setNodeInfoList(nodes);

        assertSame(edges, info.getEdgeInfoList());
        assertSame(nodes, info.getNodeInfoList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        TreeInfo a = new TreeInfo(new ArrayList<>(), new ArrayList<>());
        TreeInfo b = new TreeInfo(new ArrayList<>(), new ArrayList<>());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
