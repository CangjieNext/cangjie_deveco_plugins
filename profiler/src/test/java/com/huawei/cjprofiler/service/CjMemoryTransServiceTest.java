/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.cjprofiler.service;

import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotConstructorNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotRetainerNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CjMemoryTransServiceTest
 *
 * @since 2026-06-04
 */
public class CjMemoryTransServiceTest {

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }

    // ==================== buildPathNodeFirstLevel tests ====================

    @Test
    public void testBuildPathNodeFirstLevel() {
        JsHeapSnapshotConstructorNode constructorNode = new JsHeapSnapshotConstructorNode();
        setFieldValue(constructorNode, "id", 100);

        JsHeapSnapshotInstanceNode child1 = new JsHeapSnapshotInstanceNode();
        setFieldValue(child1, "id", 101);
        setFieldValue(child1, "nodeIndex", 2);

        JsHeapSnapshotInstanceNode child2 = new JsHeapSnapshotInstanceNode();
        setFieldValue(child2, "id", 102);
        setFieldValue(child2, "nodeIndex", 3);

        List<JsHeapSnapshotInstanceNode> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        setFieldValue(constructorNode, "children", children);

        CjMemoryTransService.buildPathNodeFirstLevel(constructorNode);

        // Check child1 pathNode
        assertNotNull(child1.getPathNode());
        assertEquals(2, child1.getPathNode().size());
        assertEquals(Integer.valueOf(100), child1.getPathNode().get(0));
        assertEquals(Integer.valueOf(101), child1.getPathNode().get(1));

        // Check child1 pathIndex
        assertNotNull(child1.getPathIndex());
        assertEquals(1, child1.getPathIndex().size());
        assertEquals(Integer.valueOf(2), child1.getPathIndex().get(0));

        // Check child2 pathNode
        assertNotNull(child2.getPathNode());
        assertEquals(2, child2.getPathNode().size());
        assertEquals(Integer.valueOf(100), child2.getPathNode().get(0));
        assertEquals(Integer.valueOf(102), child2.getPathNode().get(1));
    }

    @Test
    public void testBuildPathNodeFirstLevelWithEmptyChildren() {
        JsHeapSnapshotConstructorNode constructorNode = new JsHeapSnapshotConstructorNode();
        setFieldValue(constructorNode, "id", 100);

        List<JsHeapSnapshotInstanceNode> emptyChildren = new ArrayList<>();
        setFieldValue(constructorNode, "children", emptyChildren);

        // Should not throw exception
        CjMemoryTransService.buildPathNodeFirstLevel(constructorNode);

        assertNotNull(constructorNode.getChildren());
        assertEquals(0, constructorNode.getChildren().size());
    }

    // ==================== buildPathNode tests ====================

    @Test
    public void testBuildPathNodeWithInstanceChildren() {
        JsHeapSnapshotInstanceNode instanceNode = new JsHeapSnapshotInstanceNode();
        setFieldValue(instanceNode, "id", 100);
        setFieldValue(instanceNode, "nodeIndex", 1);
        instanceNode.setPathNode(new ArrayList<>(Arrays.asList(50, 100)));
        instanceNode.setPathIndex(new ArrayList<>(Arrays.asList(1)));

        JsHeapSnapshotInstanceNode child1 = new JsHeapSnapshotInstanceNode();
        setFieldValue(child1, "id", 101);
        setFieldValue(child1, "nodeIndex", 2);

        JsHeapSnapshotInstanceNode child2 = new JsHeapSnapshotInstanceNode();
        setFieldValue(child2, "id", 102);
        setFieldValue(child2, "nodeIndex", 3);

        List<JsHeapSnapshotInstanceNode> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        setFieldValue(instanceNode, "children", children);
        instanceNode.setRetainerNodes(new ArrayList<>());

        CjMemoryTransService.buildPathNode(instanceNode);

        // Check child1 pathNode - adds child's nodeIndex
        assertNotNull(child1.getPathNode());
        assertEquals(3, child1.getPathNode().size());
        assertEquals(Integer.valueOf(50), child1.getPathNode().get(0));
        assertEquals(Integer.valueOf(100), child1.getPathNode().get(1));
        assertEquals(Integer.valueOf(2), child1.getPathNode().get(2));

        assertNotNull(child1.getPathIndex());
        assertEquals(2, child1.getPathIndex().size());
    }

    @Test
    public void testBuildPathNodeWithRetainerChildren() {
        JsHeapSnapshotInstanceNode instanceNode = new JsHeapSnapshotInstanceNode();
        setFieldValue(instanceNode, "id", 100);
        setFieldValue(instanceNode, "nodeIndex", 1);
        instanceNode.setPathNode(new ArrayList<>(Arrays.asList(50, 100)));
        instanceNode.setPathIndex(new ArrayList<>(Arrays.asList(1)));

        JsHeapSnapshotRetainerNode retainer1 = new JsHeapSnapshotRetainerNode();
        setFieldValue(retainer1, "id", 200);
        setFieldValue(retainer1, "nodeIndex", 4);

        JsHeapSnapshotRetainerNode retainer2 = new JsHeapSnapshotRetainerNode();
        setFieldValue(retainer2, "id", 201);
        setFieldValue(retainer2, "nodeIndex", 5);

        instanceNode.setChildren(new ArrayList<>());
        List<JsHeapSnapshotRetainerNode> retainers = new ArrayList<>();
        retainers.add(retainer1);
        retainers.add(retainer2);
        setFieldValue(instanceNode, "retainerNodes", retainers);

        CjMemoryTransService.buildPathNode(instanceNode);

        // Check retainer1 pathNode - adds retainer's nodeIndex
        assertNotNull(retainer1.getPathNode());
        assertEquals(3, retainer1.getPathNode().size());
        assertEquals(Integer.valueOf(50), retainer1.getPathNode().get(0));
        assertEquals(Integer.valueOf(100), retainer1.getPathNode().get(1));
        assertEquals(Integer.valueOf(4), retainer1.getPathNode().get(2));
    }

    @Test
    public void testBuildPathNodeWithEmptyChildrenAndRetainers() {
        JsHeapSnapshotInstanceNode instanceNode = new JsHeapSnapshotInstanceNode();
        setFieldValue(instanceNode, "id", 100);
        setFieldValue(instanceNode, "nodeIndex", 1);
        instanceNode.setPathNode(new ArrayList<>(Arrays.asList(50, 100)));
        instanceNode.setPathIndex(new ArrayList<>(Arrays.asList(1)));
        instanceNode.setChildren(new ArrayList<>());
        instanceNode.setRetainerNodes(new ArrayList<>());

        // Should not throw exception
        CjMemoryTransService.buildPathNode(instanceNode);
    }

    // ==================== buildPathNodeForRetainer tests ====================

    @Test
    public void testBuildPathNodeForRetainer() {
        JsHeapSnapshotRetainerNode retainerNode = new JsHeapSnapshotRetainerNode();
        setFieldValue(retainerNode, "id", 100);
        setFieldValue(retainerNode, "nodeIndex", 1);
        retainerNode.setPathNode(new ArrayList<>(Arrays.asList(50, 100)));
        retainerNode.setPathIndex(new ArrayList<>(Arrays.asList(1)));

        JsHeapSnapshotRetainerNode child1 = new JsHeapSnapshotRetainerNode();
        setFieldValue(child1, "id", 101);
        setFieldValue(child1, "nodeIndex", 2);

        JsHeapSnapshotRetainerNode child2 = new JsHeapSnapshotRetainerNode();
        setFieldValue(child2, "id", 102);
        setFieldValue(child2, "nodeIndex", 3);

        List<JsHeapSnapshotRetainerNode> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        setFieldValue(retainerNode, "children", children);

        CjMemoryTransService.buildPathNodeForRetainer(retainerNode);

        // Check child1 pathNode - adds child's nodeIndex (not parent's)
        assertNotNull(child1.getPathNode());
        assertEquals(3, child1.getPathNode().size());
        assertEquals(Integer.valueOf(50), child1.getPathNode().get(0));
        assertEquals(Integer.valueOf(100), child1.getPathNode().get(1));
        assertEquals(Integer.valueOf(2), child1.getPathNode().get(2));
    }

    @Test
    public void testBuildPathNodeForRetainerWithEmptyChildren() {
        JsHeapSnapshotRetainerNode retainerNode = new JsHeapSnapshotRetainerNode();
        setFieldValue(retainerNode, "id", 100);
        setFieldValue(retainerNode, "nodeIndex", 1);
        retainerNode.setPathNode(new ArrayList<>(Arrays.asList(50, 100)));
        retainerNode.setPathIndex(new ArrayList<>(Arrays.asList(1)));
        setFieldValue(retainerNode, "children", new ArrayList<>());

        // Should not throw exception
        CjMemoryTransService.buildPathNodeForRetainer(retainerNode);
    }

    @Test
    public void testBuildPathNodeForRetainerDeepNesting() {
        JsHeapSnapshotRetainerNode rootRetainer = new JsHeapSnapshotRetainerNode();
        setFieldValue(rootRetainer, "id", 100);
        setFieldValue(rootRetainer, "nodeIndex", 1);
        rootRetainer.setPathNode(new ArrayList<>(Arrays.asList(50)));
        rootRetainer.setPathIndex(new ArrayList<>(Arrays.asList(1)));

        JsHeapSnapshotRetainerNode level1 = new JsHeapSnapshotRetainerNode();
        setFieldValue(level1, "id", 101);
        setFieldValue(level1, "nodeIndex", 2);

        JsHeapSnapshotRetainerNode level2 = new JsHeapSnapshotRetainerNode();
        setFieldValue(level2, "id", 102);
        setFieldValue(level2, "nodeIndex", 3);

        List<JsHeapSnapshotRetainerNode> level1Children = new ArrayList<>();
        level1Children.add(level2);
        setFieldValue(level1, "children", level1Children);

        List<JsHeapSnapshotRetainerNode> rootChildren = new ArrayList<>();
        rootChildren.add(level1);
        setFieldValue(rootRetainer, "children", rootChildren);

        CjMemoryTransService.buildPathNodeForRetainer(rootRetainer);

        // Check level1 pathNode - buildPathNodeForRetainer only processes immediate children
        assertNotNull(level1.getPathNode());
        assertEquals(2, level1.getPathNode().size());
        assertEquals(Integer.valueOf(50), level1.getPathNode().get(0));
        assertEquals(Integer.valueOf(2), level1.getPathNode().get(1));

        // level2 is NOT processed because buildPathNodeForRetainer only processes one level
        // (it does not recursively call itself for grandchildren)
        // level2.getPathNode() remains null because it was never set
        assertEquals(null, level2.getPathNode());
    }
}
