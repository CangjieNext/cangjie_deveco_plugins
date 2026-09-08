/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.EdgeInfo;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.NodeInfo;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.MemTypeEnum;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.ObjectInfo;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.PrimitiveArrayDump;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.StructArrayDump;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.TreeInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@code CjBuildTreeService} memory tree building.
 *
 * @since 2026-08-14
 */
class CjBuildTreeServiceTest {
    private final CjBuildTreeService service = CjBuildTreeService.getInstance();

    @BeforeEach
    @AfterEach
    void clearServiceState() {
        service.clearAllCollection();
    }

    @Test
    void getInstance_returnsSingleton() {
        assertThat(CjBuildTreeService.getInstance()).isSameAs(CjBuildTreeService.getInstance());
    }

    @Test
    void buildMemTree_buildsReachableAndUnreachableNodesWithSizesNamesAnd() {
        ObjectInfo object = new ObjectInfo(2, 20, List.of(3, 999));
        PrimitiveArrayDump primitiveArray = new PrimitiveArrayDump(3, 3, MemTypeEnum.INT32.getType());
        StructArrayDump unreachableArray = new StructArrayDump(4, 40, List.of(), 2);
        Map<Integer, ObjectInfo> objects = new LinkedHashMap<>();
        objects.put(2, object);
        objects.put(3, primitiveArray);
        objects.put(4, unreachableArray);

        TreeInfo tree = service.buildMemTree(objects, Map.of(20, 24), Map.of(20, 200, 40, 400), Set.of(2));

        assertThat(tree.getNodeInfoList()).extracting(NodeInfo::getId).containsExactly(1, 2, 3, 999, 4);
        assertThat(nodeById(tree, 1)).satisfies(node -> {
            assertThat(node.getType()).isEqualTo(3);
            assertThat(node.getSelfSize()).isZero();
            assertThat(node.getEdgeCount()).isEqualTo(1);
        });
        assertThat(nodeById(tree, 2)).satisfies(node -> {
            assertThat(node.getName()).isEqualTo(200);
            assertThat(node.getSelfSize()).isEqualTo(24);
            assertThat(node.getEdgeCount()).isEqualTo(2);
        });
        assertThat(nodeById(tree, 3)).satisfies(node -> {
            assertThat(node.getName()).isZero();
            assertThat(node.getSelfSize()).isEqualTo(12);
        });
        assertThat(nodeById(tree, 999)).satisfies(node -> {
            assertThat(node.getType()).isEqualTo(-1);
            assertThat(node.getSelfSize()).isZero();
        });
        assertThat(nodeById(tree, 4)).satisfies(node -> {
            assertThat(node.getName()).isEqualTo(400);
            assertThat(node.getSelfSize()).isEqualTo(16);
        });
        assertThat(tree.getEdgeInfoList()).hasSize(3);
        assertThat(tree.getEdgeInfoList()).extracting(EdgeInfo::getToNode)
            .containsExactly(service.getNodeIndex(2) * 8, service.getNodeIndex(3) * 8, service.getNodeIndex(999) * 8);
        assertThat(object.getSize()).isEqualTo(24);
        assertThat(primitiveArray.getSize()).isEqualTo(12);
        assertThat(unreachableArray.getSize()).isEqualTo(16);
    }

    @Test
    void buildMemTree_handlesCyclesAndMissingClassNamesWithoutLooping() {
        ObjectInfo first = new ObjectInfo(5, 50, List.of(6));
        ObjectInfo second = new ObjectInfo(6, 60, List.of(5));
        Map<Integer, ObjectInfo> objects = new HashMap<>();
        objects.put(5, first);
        objects.put(6, second);

        TreeInfo tree = service.buildMemTree(objects, Map.of(50, 4, 60, 8), Map.of(), Set.of());

        assertThat(tree.getNodeInfoList()).extracting(NodeInfo::getId).containsExactlyInAnyOrder(1, 5, 6);
        assertThat(nodeById(tree, 5).getName()).isZero();
        assertThat(nodeById(tree, 6).getName()).isZero();
        assertThat(tree.getEdgeInfoList()).hasSize(2);
        assertThat(service.getNodeIndex(5)).isNotNull();
        assertThat(service.getNodeIndex(6)).isNotNull();
    }

    @Test
    void buildMemTree_skipsUnreachableNodeWhenMapKeyDiffersFromObjectId() {
        // key=5 但 getId()=7：unreachableNodes 按 getId() 收集，
        // findUnreachableRoots 与 iterativeUnreachableDfs 都会对 id=7 查 map 得到 null
        Map<Integer, ObjectInfo> objects = new LinkedHashMap<>();
        objects.put(5, new ObjectInfo(7, 70, List.of()));

        TreeInfo tree = service.buildMemTree(objects, Map.of(70, 8), Map.of(70, 700), Set.of());

        assertThat(tree.getNodeInfoList()).extracting(NodeInfo::getId).containsExactly(1);
        assertThat(tree.getEdgeInfoList()).isEmpty();
    }

    @Test
    void clearAllCollection_removesPreviouslyRecordedIndexesAndAllowsFres() {
        service.buildMemTree(Map.of(2, new ObjectInfo(2, 20, List.of())), Map.of(20, 1), Map.of(20, 2), Set.of(2));
        assertThat(service.getNodeIndex(2)).isNotNull();

        service.clearAllCollection();
        TreeInfo emptyTree = service.buildMemTree(Map.of(), Map.of(), Map.of(), Set.of());

        assertThat(service.getNodeIndex(2)).isNull();
        assertThat(emptyTree.getNodeInfoList()).extracting(NodeInfo::getId).containsExactly(1);
        assertThat(emptyTree.getEdgeInfoList()).isEmpty();
    }

    private static NodeInfo nodeById(TreeInfo tree, int id) {
        return tree.getNodeInfoList().stream()
            .filter(node -> node.getId() == id)
            .findFirst()
            .orElseThrow();
    }
}
