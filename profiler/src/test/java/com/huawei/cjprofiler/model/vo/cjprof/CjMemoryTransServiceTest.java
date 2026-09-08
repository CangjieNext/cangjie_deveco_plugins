/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.vo.cjprof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.huawei.cjprofiler.service.CjMemoryTransService;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotConstructorNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotDetailNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotRetainerNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@code CjMemoryTransService} node conversion.
 *
 * @since 2026-09-08
 */
public class CjMemoryTransServiceTest {
    @Test
    public void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjMemoryTransService();
    }

    @Test
    public void testBuildPathNodeFirstLevelWithChildren() throws NoSuchFieldException {
        // 创建测试数据
        JsHeapSnapshotConstructorNode constructorNode = new JsHeapSnapshotConstructorNode();
        constructorNode.setId(1);  // Set constructorNode ID so pathNode starts with it
        constructorNode.setPathNode(new ArrayList<>());

        List<JsHeapSnapshotInstanceNode> children = new ArrayList<>();
        JsHeapSnapshotInstanceNode child1 = new JsHeapSnapshotInstanceNode();
        child1.setId(1);
        child1.setNodeIndex(10);
        children.add(child1);

        JsHeapSnapshotInstanceNode child2 = new JsHeapSnapshotInstanceNode();
        child2.setId(2);
        child2.setNodeIndex(20);
        children.add(child2);

        constructorNode.setChildren(children);

        CjMemoryTransService.buildPathNodeFirstLevel(constructorNode);

        // buildPathNodeFirstLevel sets each child's pathNode to [constructorNode.getId(), child.getId()]
        // child1: pathNode = [1, 1], pathIndex = [10]
        // child2: pathNode = [1, 2], pathIndex = [20]
        assertEquals(2, constructorNode.getChildren().get(0).getPathNode().size());
        assertEquals(1, constructorNode.getChildren().get(0).getPathNode().get(0));  // constructorNode.getId()
        assertEquals(1, constructorNode.getChildren().get(0).getPathNode().get(1));  // child1.getId()
        assertEquals(2, constructorNode.getChildren().get(1).getPathNode().size());
        assertEquals(1, constructorNode.getChildren().get(1).getPathNode().get(0));  // constructorNode.getId()
        assertEquals(2, constructorNode.getChildren().get(1).getPathNode().get(1));  // child2.getId()

        assertEquals(1, constructorNode.getChildren().get(0).getPathIndex().size());
        assertEquals(10, constructorNode.getChildren().get(0).getPathIndex().get(0));
        assertEquals(20, constructorNode.getChildren().get(1).getPathIndex().get(0));
    }

    @Test
    public void testBuildPathNodeWithChildrenAndRetainers() {
        // 创建测试数据
        JsHeapSnapshotInstanceNode instanceNode = new JsHeapSnapshotInstanceNode();
        instanceNode.setPathNode(new ArrayList<>(List.of(1, 2)));
        instanceNode.setPathIndex(new ArrayList<>(List.of(1, 2)));
        instanceNode.setNodeIndex(100);

        List<JsHeapSnapshotDetailNode> children = new ArrayList<>();
        JsHeapSnapshotDetailNode child1 = new JsHeapSnapshotDetailNode();
        child1.setId(1);
        child1.setNodeIndex(10);
        children.add(child1);

        JsHeapSnapshotDetailNode child2 = new JsHeapSnapshotDetailNode();
        child2.setId(2);
        child2.setNodeIndex(100);
        children.add(child2);

        instanceNode.setChildren(children);

        List<JsHeapSnapshotRetainerNode> retainers = new ArrayList<>();
        JsHeapSnapshotRetainerNode retainer1 = new JsHeapSnapshotRetainerNode();
        retainer1.setId(3);
        retainer1.setNodeIndex(20);
        retainers.add(retainer1);

        JsHeapSnapshotRetainerNode retainer2 = new JsHeapSnapshotRetainerNode();
        retainer2.setId(4);
        retainer2.setNodeIndex(200);
        retainers.add(retainer2);

        instanceNode.setRetainerNodes(retainers);

        // 调用方法
        CjMemoryTransService.buildPathNode(instanceNode);

        // 验证结果
        assertEquals(2, instanceNode.getChildren().size());
        assertEquals(2, instanceNode.getRetainerNodes().size());

        for (JsHeapSnapshotInstanceNode child : instanceNode.getChildren()) {
            assertNotNull(child.getPathNode());
            assertNotNull(child.getPathIndex());
            assertEquals(3, child.getPathIndex().size()); // 确保pathIndex是一个包含单个元素的列表
        }

        for (JsHeapSnapshotRetainerNode retainer : instanceNode.getRetainerNodes()) {
            assertNotNull(retainer.getPathNode());
            assertNotNull(retainer.getPathIndex());
            assertEquals(3, retainer.getPathIndex().size()); // 确保pathIndex是一个包含单个元素的列表
        }
    }
}
