/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.LocalObject;
import com.huawei.cjprofiler.common.enums.RootTypeEnum;
import com.huawei.cjprofiler.model.po.cjprof.HeapThreadInfo;
import com.huawei.cjprofiler.model.po.cjprof.StackFrame;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotConstructorNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotDetailNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotDiffNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotRetainerNode;

import com.cjprof.jni.model.ConstructorDiffNode;
import com.cjprof.jni.model.ConstructorNode;
import com.cjprof.jni.model.Frame;
import com.cjprof.jni.model.InstanceDiffNode;
import com.cjprof.jni.model.InstanceNode;
import com.cjprof.jni.model.ThreadInfo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class CjMemoryBaseMapperTest {
    private final CjMemoryBaseMapper mapper = CjMemoryBaseMapper.INSTANCE;

    private final Map<Integer, Long> indexToIdMap = new HashMap<>();

    @Test
    public void testToJsHeapSnapshotConstructorNodeWithoutChildren() {
        ConstructorNode constructorNode = new ConstructorNode();
        constructorNode.setClassName("TestClass");
        constructorNode.setTotalSize(100);
        constructorNode.setId(111111111111111111L);
        constructorNode.setNodeIndex(1);
        constructorNode.setChildrenCount(5);
        constructorNode.setDistance(2);
        constructorNode.setShallowSize(40);
        constructorNode.setRetainedSize(60);
        constructorNode.setShallowSizePercent(40.3);
        constructorNode.setRetainedSizePercent(60.3);
        constructorNode.setTotalInstanceCountPercent(10.4);
        constructorNode.setStartPosition(0);
        constructorNode.setEndPosition(10);

        JsHeapSnapshotConstructorNode result = mapper.toJsHeapSnapshotConstructorNode(constructorNode, indexToIdMap);

        assertNotNull(result);
        assertEquals(constructorNode.getNodeIndex(), result.getId());
        assertEquals(constructorNode.getClassName(), result.getName());
        assertEquals(constructorNode.getClassName(), result.getName());
        assertEquals(constructorNode.getClassName(), result.getClassName());
        assertEquals(constructorNode.getChildrenCount(), result.getChildrenCount());
        assertEquals(constructorNode.getDistance(), result.getDistance());
        assertEquals(constructorNode.getShallowSize(), result.getShallowSize());
        assertEquals(constructorNode.getRetainedSize(), result.getRetainedSize());
        assertEquals((int) (constructorNode.getShallowSizePercent()), result.getShallowSizePercent());
        assertEquals((int) (constructorNode.getRetainedSizePercent()), result.getRetainedSizePercent());
        assertEquals((int) (constructorNode.getTotalInstanceCountPercent()), result.getTotalInstanceCountPercent());
        assertEquals(constructorNode.getStartPosition(), result.getStartPosition());
        assertEquals(constructorNode.getEndPosition(), result.getEndPosition());
        assertEquals(constructorNode.getChildren().size(), result.getChildren().size());
    }
    @Test
    public void testToJsHeapSnapshotConstructorNodeWithChildren() {
        ConstructorNode constructorNode = new ConstructorNode();
        constructorNode.setClassName("TestClass");
        constructorNode.setTotalSize(100);
        constructorNode.setId(1L);
        constructorNode.setChildrenCount(2);
        constructorNode.setDistance(2);
        constructorNode.setShallowSize(40);
        constructorNode.setRetainedSize(60);
        constructorNode.setShallowSizePercent(40.2);
        constructorNode.setRetainedSizePercent(60.1);
        constructorNode.setTotalInstanceCountPercent(10.0);
        constructorNode.setStartPosition(0);
        constructorNode.setEndPosition(10);

        List<InstanceNode> children = new ArrayList<>();
        InstanceNode child1 = new InstanceNode();
        child1.setClassName("ChildClass1");
        child1.setDistance(1);
        child1.setRetainedSize(20);
        child1.setShallowSize(10);
        child1.setShallowSizePercent(50.1);
        child1.setRetainedSizePercent(30.2);
        child1.setTotalSize(30);
        child1.setChildren(new ArrayList<>());
        child1.setRetainerNodes(new ArrayList<>());
        child1.setId(2L);
        child1.setNodeIndex(1);
        child1.setType("[array]");
        child1.setChildrenCount(0);
        child1.setRetainerCount(0);
        child1.setStartPosition(1);
        child1.setEndPosition(2);
        child1.setRootType("[-]");
        children.add(child1);

        InstanceNode child2 = new InstanceNode();
        child2.setClassName("ChildClass2");
        child2.setDistance(2);
        child2.setRetainedSize(30);
        child2.setShallowSize(15);
        child2.setShallowSizePercent(60.3);
        child2.setRetainedSizePercent(40.4);
        child2.setTotalSize(45);
        child2.setChildren(new ArrayList<>());
        child2.setRetainerNodes(new ArrayList<>());
        child2.setId(3L);
        child2.setNodeIndex(2);
        child2.setType("[string]");
        child2.setChildrenCount(0);
        child2.setRetainerCount(0);
        child2.setStartPosition(3);
        child2.setEndPosition(4);
        child2.setRootType("[-]");
        children.add(child2);

        constructorNode.setChildren(children);

        JsHeapSnapshotConstructorNode result = mapper.toJsHeapSnapshotConstructorNode(constructorNode, indexToIdMap);

        assertNotNull(result);
        assertEquals(constructorNode.getClassName(), result.getName());
        assertEquals(constructorNode.getClassName(), result.getClassName());
        assertEquals(constructorNode.getChildrenCount(), result.getChildrenCount());
        assertEquals(constructorNode.getDistance(), result.getDistance());
        assertEquals(constructorNode.getShallowSize(), result.getShallowSize());
        assertEquals(constructorNode.getRetainedSize(), result.getRetainedSize());
        assertEquals((int) (constructorNode.getShallowSizePercent()), result.getShallowSizePercent());
        assertEquals((int) (constructorNode.getRetainedSizePercent()), result.getRetainedSizePercent());
        assertEquals((int) (constructorNode.getTotalInstanceCountPercent()), result.getTotalInstanceCountPercent());
        assertEquals(constructorNode.getStartPosition(), result.getStartPosition());
        assertEquals(constructorNode.getEndPosition(), result.getEndPosition());

        assertNotNull(result.getChildren());
        assertEquals(constructorNode.getChildren().size(), result.getChildren().size());

        JsHeapSnapshotInstanceNode mappedChild1 = result.getChildren().get(0);
        assertEquals(child1.getClassName(), mappedChild1.getClassName());
        assertEquals(child1.getDistance(), mappedChild1.getDistance());
        assertEquals(child1.getRetainedSize(), mappedChild1.getRetainedSize());
        assertEquals(child1.getShallowSize(), mappedChild1.getShallowSize());
        assertEquals((int) (child1.getShallowSizePercent()), mappedChild1.getShallowSizePercent());
        assertEquals((int) (child1.getRetainedSizePercent()), mappedChild1.getRetainedSizePercent());
        assertEquals(child1.getChildren().size(), mappedChild1.getChildren().size());
        assertEquals(child1.getRetainerNodes().size(), mappedChild1.getRetainerNodes().size());
        assertEquals(child1.getNodeIndex(), mappedChild1.getId());
        assertEquals(child1.getNodeIndex(), mappedChild1.getNodeIndex());
        assertEquals(child1.getType().replace("[", "").replace("]", ""), mappedChild1.getType());
        assertEquals(child1.getChildrenCount(), mappedChild1.getChildrenCount());
        assertEquals(child1.getRetainerCount(), mappedChild1.getRetainerCount());
        assertEquals(child1.getStartPosition(), mappedChild1.getStartPosition());
        assertEquals(child1.getEndPosition(), mappedChild1.getEndPosition());

        JsHeapSnapshotInstanceNode mappedChild2 = result.getChildren().get(1);
        assertEquals(child2.getClassName(), mappedChild2.getClassName());
        assertEquals(child2.getDistance(), mappedChild2.getDistance());
        assertEquals(child2.getRetainedSize(), mappedChild2.getRetainedSize());
        assertEquals(child2.getShallowSize(), mappedChild2.getShallowSize());
        assertEquals((int) (child2.getShallowSizePercent()), mappedChild2.getShallowSizePercent());
        assertEquals((int) (child2.getRetainedSizePercent()), mappedChild2.getRetainedSizePercent());
        assertEquals(child2.getChildren().size(), mappedChild2.getChildren().size());
        assertEquals(child2.getRetainerNodes().size(), mappedChild2.getRetainerNodes().size());
        assertEquals(child2.getNodeIndex(), mappedChild2.getId());
        assertEquals(child2.getNodeIndex(), mappedChild2.getNodeIndex());
        assertEquals(child2.getType().replace("[", "").replace("]", ""), mappedChild2.getType());
        assertEquals(child2.getChildrenCount(), mappedChild2.getChildrenCount());
        assertEquals(child2.getRetainerCount(), mappedChild2.getRetainerCount());
        assertEquals(child2.getStartPosition(), mappedChild2.getStartPosition());
        assertEquals(child2.getEndPosition(), mappedChild2.getEndPosition());
    }

    @Test
    public void testToJsHeapSnapshotInstanceNodeWithChildrenAndRetainerNodes() {
        InstanceNode instanceNode = createInstanceNodeWithChildrenAndRetainerNodes();

        JsHeapSnapshotInstanceNode result = mapper.toJsHeapSnapshotInstanceNode(instanceNode, indexToIdMap);

        assertBasicPropertiesMappedCorrectly(instanceNode, result);

        assertChildrenMappedCorrectly(instanceNode.getChildren(), result.getChildren());

        assertRetainerNodesMappedCorrectly(instanceNode.getRetainerNodes(), result.getRetainerNodes());
    }

    private InstanceNode createInstanceNodeWithChildrenAndRetainerNodes() {
        InstanceNode instanceNode = new InstanceNode();
        instanceNode.setClassName("TestClass");
        instanceNode.setDistance(2);
        instanceNode.setRetainedSize(60);
        instanceNode.setShallowSize(40);
        instanceNode.setShallowSizePercent(40.1); // 40%
        instanceNode.setRetainedSizePercent(60.2); // 60%
        instanceNode.setTotalSize(100);
        instanceNode.setId(1L);
        instanceNode.setNodeIndex(0);
        instanceNode.setType("[object]"); // 设置为InstanceNode类型
        instanceNode.setChildrenCount(2);
        instanceNode.setRetainerCount(3);
        instanceNode.setStartPosition(0);
        instanceNode.setEndPosition(10);
        instanceNode.setRootType("[-]");

        List<InstanceNode> children = new ArrayList<>();
        InstanceNode child1 = createChildNode("Child1", 1, 20, 10, 50.1, 30.3, 30);
        InstanceNode child2 = createChildNode("Child2", 2, 30, 15, 60.2, 40.4, 45);
        children.add(child1);
        children.add(child2);
        instanceNode.setChildren(children);

        List<InstanceNode> retainerNodes = new ArrayList<>();
        InstanceNode retainer1 = createRetainerNode("Retainer1", 1, 10, 5, 20.1, 10.3, 15);
        InstanceNode retainer2 = createRetainerNode("Retainer2", 2, 25, 12, 48.2, 32.4, 50);
        InstanceNode retainer3 = createRetainerNode("Retainer3", 2, 25, 12, 48.2, 32.4, 50);
        retainerNodes.add(retainer1);
        retainerNodes.add(retainer2);
        retainerNodes.add(retainer3);
        instanceNode.setRetainerNodes(retainerNodes);

        return instanceNode;
    }

    private InstanceNode createChildNode(String className, int distance, int retainedSize, int shallowSize,
        double shallowPercent, double retainedPercent, int totalSize) {
        InstanceNode child = new InstanceNode();
        child.setClassName(className);
        child.setDistance(distance);
        child.setRetainedSize(retainedSize);
        child.setShallowSize(shallowSize);
        child.setShallowSizePercent(shallowPercent);
        child.setRetainedSizePercent(retainedPercent);
        child.setTotalSize(totalSize);
        child.setChildren(new ArrayList<>());
        child.setRetainerNodes(new ArrayList<>());
        child.setType("[object]");
        child.setRootType("[-]");
        return child;
    }

    private InstanceNode createRetainerNode(String className, int distance, int retainedSize, int shallowSize,
        double shallowPercent, double retainedPercent, int totalSize) {
        InstanceNode retainer = new InstanceNode();
        retainer.setClassName(className);
        retainer.setDistance(distance);
        retainer.setRetainedSize(retainedSize);
        retainer.setShallowSize(shallowSize);
        retainer.setShallowSizePercent(shallowPercent);
        retainer.setRetainedSizePercent(retainedPercent);
        retainer.setTotalSize(totalSize);
        retainer.setChildren(new ArrayList<>());
        retainer.setRetainerNodes(new ArrayList<>());
        retainer.setType("[object]");
        retainer.setRootType("[-]");
        return retainer;
    }

    private void assertBasicPropertiesMappedCorrectly(InstanceNode source, JsHeapSnapshotInstanceNode target) {
        assertEquals(source.getClassName(), target.getClassName());
        assertEquals(source.getDistance(), target.getDistance());
        assertEquals(source.getRetainedSize(), target.getRetainedSize());
        assertEquals(source.getShallowSize(), target.getShallowSize());
        assertEquals((int) (source.getShallowSizePercent()), target.getShallowSizePercent());
        assertEquals((int) (source.getRetainedSizePercent()), target.getRetainedSizePercent());
        assertEquals((int) source.getShallowSizePercent(), target.getTotalShallowSizePercent());
        assertEquals((int) source.getRetainedSizePercent(), target.getTotalRetainedSizePercent());
        assertEquals(source.getNodeIndex(), target.getId());
        assertEquals(source.getNodeIndex(), target.getNodeIndex());
        assertEquals(source.getType().replace("[", "").replace("]", ""), target.getType());
        assertEquals(RootTypeEnum.NOT_ROOT.getValue(), target.getRootType());
        assertEquals(source.getChildrenCount(), target.getChildrenCount());
        assertEquals(source.getRetainerCount(), target.getRetainerCount());
        assertEquals(source.getStartPosition(), target.getStartPosition());
        assertEquals(source.getEndPosition(), target.getEndPosition());
    }

    private void assertChildrenMappedCorrectly(List<InstanceNode> sourceChildren,
        List<JsHeapSnapshotDetailNode> targetChildren) {
        assertNotNull(targetChildren);
        assertEquals(sourceChildren.size(), targetChildren.size());

        for (int i = 0; i < sourceChildren.size(); i++) {
            InstanceNode sourceChild = sourceChildren.get(i);
            JsHeapSnapshotDetailNode targetChild = (JsHeapSnapshotDetailNode) targetChildren.get(i);

            assertEquals(sourceChild.getClassName(), targetChild.getClassName());
            assertEquals(sourceChild.getDistance(), targetChild.getDistance());
            assertEquals(sourceChild.getRetainedSize(), targetChild.getRetainedSize());
            assertEquals(sourceChild.getShallowSize(), targetChild.getShallowSize());
            assertEquals((int) (sourceChild.getShallowSizePercent()), targetChild.getShallowSizePercent());
            assertEquals((int) (sourceChild.getRetainedSizePercent()), targetChild.getRetainedSizePercent());
            assertEquals((int) sourceChild.getShallowSizePercent(), targetChild.getTotalShallowSizePercent());
            assertEquals((int) sourceChild.getRetainedSizePercent(), targetChild.getTotalRetainedSizePercent());
            assertEquals(sourceChild.getId(), targetChild.getId());
            assertEquals(sourceChild.getNodeIndex(), targetChild.getNodeIndex());
            assertEquals(sourceChild.getStartPosition(), targetChild.getStartPosition());
            assertEquals(sourceChild.getEndPosition(), targetChild.getEndPosition());
        }
    }

    private void assertRetainerNodesMappedCorrectly(List<InstanceNode> sourceRetainers,
        List<JsHeapSnapshotRetainerNode> targetRetainers) {
        assertNotNull(targetRetainers);
        assertEquals(sourceRetainers.size(), targetRetainers.size());

        for (int i = 0; i < sourceRetainers.size(); i++) {
            InstanceNode sourceRetainer = sourceRetainers.get(i);
            JsHeapSnapshotRetainerNode targetRetainer = targetRetainers.get(i);

            assertEquals(sourceRetainer.getClassName(), targetRetainer.getClassName());
            assertEquals(sourceRetainer.getDistance(), targetRetainer.getDistance());
            assertEquals(sourceRetainer.getRetainedSize(), targetRetainer.getRetainedSize());
            assertEquals(sourceRetainer.getShallowSize(), targetRetainer.getShallowSize());
            assertEquals((int) (sourceRetainer.getShallowSizePercent()), targetRetainer.getShallowSizePercent());
            assertEquals((int) (sourceRetainer.getRetainedSizePercent()), targetRetainer.getRetainedSizePercent());
            assertEquals((int) sourceRetainer.getShallowSizePercent(), targetRetainer.getTotalShallowSizePercent());
            assertEquals((int) sourceRetainer.getRetainedSizePercent(), targetRetainer.getTotalRetainedSizePercent());
            assertEquals(sourceRetainer.getId(), targetRetainer.getId());
            assertEquals(sourceRetainer.getNodeIndex(), targetRetainer.getNodeIndex());
            assertEquals(sourceRetainer.getStartPosition(), targetRetainer.getStartPosition());
            assertEquals(sourceRetainer.getEndPosition(), targetRetainer.getEndPosition());
        }
    }

    @Test
    public void testToJsHeapSnapshotDiffNode() {
        ConstructorDiffNode diffNode = new ConstructorDiffNode();
        diffNode.setClassName("TestClass");
        diffNode.setTotalSize(1000);
        diffNode.setId(123L);
        diffNode.setChildrenCount(5);
        diffNode.setDistance(2);
        diffNode.setShallowSize(400);
        diffNode.setRetainedSize(600);
        diffNode.setShallowSizePercent(40.14); // 40.14% (百分比值)
        diffNode.setRetainedSizePercent(60.55); // 60.55% (百分比值)
        diffNode.setTotalInstanceCountPercent(50.75); // 50.75% (百分比值)
        diffNode.setStartPosition(0);
        diffNode.setEndPosition(10);

        diffNode.setAddedCount(2);
        diffNode.setRemovedCount(1);
        diffNode.setCountDelta(1);
        diffNode.setAddedSize(200);
        diffNode.setRemovedSize(100);
        diffNode.setSizeDelta(100);
        diffNode.setBaseTotalSize(800);
        diffNode.setTargetTotalSize(1200);

        List<InstanceNode> children = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            InstanceNode child = new InstanceNode();
            child.setClassName("ChildClass" + i);
            child.setDistance(1);
            child.setRetainedSize(20);
            child.setShallowSize(10);
            child.setShallowSizePercent(50.0);
            child.setRetainedSizePercent(30.0);
            child.setTotalSize(30);
            child.setChildren(new ArrayList<>());
            child.setRetainerNodes(new ArrayList<>());
            child.setId((long) (i + 1));
            child.setNodeIndex(i);
            child.setType("[object]");
            child.setChildrenCount(0);
            child.setRetainerCount(0);
            child.setStartPosition(0);
            child.setEndPosition(1);
            child.setRootType("[-]");
            children.add(child);
        }
        diffNode.setChildren(children);

        List<Boolean> childAddedStates = new ArrayList<>();
        childAddedStates.add(true);   // child[0] is added
        childAddedStates.add(true);   // child[1] is added
        childAddedStates.add(false);  // child[2] is removed
        childAddedStates.add(false);  // child[3] is removed
        childAddedStates.add(false);  // child[4] is removed
        diffNode.setChildAddedStates(childAddedStates);

        JsHeapSnapshotDiffNode result = mapper.toJsHeapSnapshotDiffNode(diffNode, indexToIdMap);

        assertEquals(diffNode.getClassName(), result.getName());
        assertEquals(diffNode.getClassName(), result.getClassName());
        assertEquals(diffNode.getNodeIndex(), result.getId());
        assertEquals(diffNode.getDistance(), result.getDistance());
        assertEquals(diffNode.getShallowSize(), result.getShallowSize());
        assertEquals(diffNode.getRetainedSize(), result.getRetainedSize());
        assertEquals((int) diffNode.getShallowSizePercent(), result.getShallowSizePercent()); // 取整后的百分比值
        assertEquals((int) diffNode.getRetainedSizePercent(), result.getRetainedSizePercent()); // 取整后的百分比值
        assertEquals((int) diffNode.getTotalInstanceCountPercent(), result.getTotalInstanceCountPercent()); // 取整后的百分比值
        assertEquals(diffNode.getStartPosition(), result.getStartPosition());
        assertEquals(diffNode.getEndPosition(), result.getEndPosition());

        assertEquals(diffNode.getAddedCount(), result.getAddedCount());
        assertEquals(diffNode.getRemovedCount(), result.getRemovedCount());
        assertEquals(diffNode.getCountDelta(), result.getCountDelta());
        assertEquals(diffNode.getAddedSize(), result.getAddedSize());
        assertEquals(diffNode.getRemovedSize(), result.getRemovedSize());
        assertEquals(diffNode.getSizeDelta(), result.getSizeDelta());

        // 验证updateAddedRemoved逻辑：children[0] is added
        JsHeapSnapshotInstanceNode child0 = result.getChildren().get(0);
        assertTrue(child0.isAddedRemoved());
        assertEquals(1, child0.getAddedCount());
        assertEquals(0, child0.getRemovedCount());
        assertEquals(1, child0.getCountDelta());

        // 验证updateAddedRemoved逻辑：children[1] is added
        JsHeapSnapshotInstanceNode child1 = result.getChildren().get(1);
        assertTrue(child1.isAddedRemoved());
        assertEquals(1, child1.getAddedCount());
        assertEquals(0, child1.getRemovedCount());
        assertEquals(1, child1.getCountDelta());

        // 验证updateAddedRemoved逻辑：children[2] is removed
        JsHeapSnapshotInstanceNode child2 = result.getChildren().get(2);
        assertTrue(!child2.isAddedRemoved());
        assertEquals(0, child2.getAddedCount());
        assertEquals(1, child2.getRemovedCount());
        assertEquals(-1, child2.getCountDelta());

        // 验证updateAddedRemoved逻辑：children[3] is removed
        JsHeapSnapshotInstanceNode child3 = result.getChildren().get(3);
        assertTrue(!child3.isAddedRemoved());
        assertEquals(0, child3.getAddedCount());
        assertEquals(1, child3.getRemovedCount());
        assertEquals(-1, child3.getCountDelta());

        // 验证updateAddedRemoved逻辑：children[4] is removed
        JsHeapSnapshotInstanceNode child4 = result.getChildren().get(4);
        assertTrue(!child4.isAddedRemoved());
        assertEquals(0, child4.getAddedCount());
        assertEquals(1, child4.getRemovedCount());
        assertEquals(-1, child4.getCountDelta());
    }

    @Test
    public void testToJsHeapSnapshotInstanceNode() {
        InstanceDiffNode instanceDiffNode = new InstanceDiffNode();
        instanceDiffNode.setClassName("TestDiffClass");
        instanceDiffNode.setDistance(3);
        instanceDiffNode.setRetainedSize(800);
        instanceDiffNode.setShallowSize(500);
        instanceDiffNode.setShallowSizePercent(50.25); // 50.25%
        instanceDiffNode.setRetainedSizePercent(70.75); // 70.75%
        instanceDiffNode.setTotalSize(1200);
        instanceDiffNode.setId(456L);
        instanceDiffNode.setNodeIndex(1);
        instanceDiffNode.setType("[object]");
        instanceDiffNode.setRootType("[-]");
        instanceDiffNode.setChildrenCount(3);
        instanceDiffNode.setRetainerCount(2);
        instanceDiffNode.setStartPosition(5);
        instanceDiffNode.setEndPosition(15);
        instanceDiffNode.setAddedCount(2);
        instanceDiffNode.setRemovedCount(1);
        instanceDiffNode.setCountDelta(1);
        instanceDiffNode.setAddedSize(100);
        instanceDiffNode.setRemovedSize(50);
        instanceDiffNode.setSizeDelta(50);
        instanceDiffNode.setAdded(true);

        List<InstanceNode> children = new ArrayList<>();
        InstanceNode child1 = createInstanceNode("Child1", 1, 200, 100, 40.5, 20.3);
        InstanceNode child2 = createInstanceNode("Child2", 2, 300, 150, 60.75, 40.8);
        InstanceNode child3 = createInstanceNode("Child3", 3, 400, 200, 80.1, 60.4);
        children.add(child1);
        children.add(child2);
        children.add(child3);
        instanceDiffNode.setChildren(children);

        List<InstanceNode> retainerNodes = new ArrayList<>();
        InstanceNode retainer1 = createInstanceNode("Retainer1", 1, 100, 50, 20.5, 10.3);
        InstanceNode retainer2 = createInstanceNode("Retainer2", 2, 250, 120, 48.75, 32.6);
        retainerNodes.add(retainer1);
        retainerNodes.add(retainer2);
        instanceDiffNode.setRetainerNodes(retainerNodes);

        JsHeapSnapshotInstanceNode result = mapper.toJsHeapSnapshotInstanceNode(instanceDiffNode, indexToIdMap);

        assertEquals(instanceDiffNode.getClassName(), result.getClassName());
        assertEquals(instanceDiffNode.getDistance(), result.getDistance());
        assertEquals(instanceDiffNode.getRetainedSize(), result.getRetainedSize());
        assertEquals(instanceDiffNode.getShallowSize(), result.getShallowSize());
        assertEquals((int) instanceDiffNode.getShallowSizePercent(), result.getShallowSizePercent());
        assertEquals((int) instanceDiffNode.getRetainedSizePercent(), result.getRetainedSizePercent());
        assertEquals(instanceDiffNode.getNodeIndex(), result.getId());
        assertEquals(instanceDiffNode.getNodeIndex(), result.getNodeIndex());
        assertEquals(result.getType(), instanceDiffNode.getType().replace("[", "").replace("]", ""));
        assertEquals(instanceDiffNode.getRetainerCount(), result.getRetainerCount());
        assertEquals(instanceDiffNode.getStartPosition(), result.getStartPosition());
        assertEquals(instanceDiffNode.getEndPosition(), result.getEndPosition());
        assertEquals(instanceDiffNode.getAddedCount(), result.getAddedCount());
        assertEquals(instanceDiffNode.getRemovedCount(), result.getRemovedCount());
        assertEquals(instanceDiffNode.getCountDelta(), result.getCountDelta());
        assertEquals(instanceDiffNode.getAddedSize(), result.getAddedSize());
        assertEquals(instanceDiffNode.getRemovedSize(), result.getRemovedSize());
        assertEquals(instanceDiffNode.getSizeDelta(), result.getSizeDelta());
        assertTrue(result.isAddedRemoved()); // InstanceDiffNode.isAdded映射到JsHeapSnapshotInstanceNode.isAddedRemoved

        assertNotNull(result.getChildren());
        assertEquals(instanceDiffNode.getChildren().size(), result.getChildren().size());

        JsHeapSnapshotDetailNode mappedChild1 = (JsHeapSnapshotDetailNode) result.getChildren().get(0);
        assertEquals(child1.getClassName(), mappedChild1.getClassName());
        assertEquals(child1.getDistance(), mappedChild1.getDistance());
        assertEquals(child1.getRetainedSize(), mappedChild1.getRetainedSize());
        assertEquals(child1.getShallowSize(), mappedChild1.getShallowSize());
        assertEquals((int) child1.getShallowSizePercent(), mappedChild1.getShallowSizePercent());
        assertEquals((int) child1.getRetainedSizePercent(), mappedChild1.getRetainedSizePercent());
        assertEquals(child1.getNodeIndex(), mappedChild1.getId());
        assertEquals(child1.getNodeIndex(), mappedChild1.getNodeIndex());
        assertEquals(mappedChild1.getType(), child1.getType().replace("[", "").replace("]", ""));
        assertEquals(child1.getStartPosition(), mappedChild1.getStartPosition());
        assertEquals(child1.getEndPosition(), mappedChild1.getEndPosition());

        JsHeapSnapshotDetailNode mappedChild2 = (JsHeapSnapshotDetailNode) result.getChildren().get(1);
        assertEquals(child2.getClassName(), mappedChild2.getClassName());
        assertEquals(child2.getDistance(), mappedChild2.getDistance());
        assertEquals(child2.getRetainedSize(), mappedChild2.getRetainedSize());
        assertEquals(child2.getShallowSize(), mappedChild2.getShallowSize());
        assertEquals((int) child2.getShallowSizePercent(), mappedChild2.getShallowSizePercent());
        assertEquals((int) child2.getRetainedSizePercent(), mappedChild2.getRetainedSizePercent());
        assertEquals(child2.getNodeIndex(), mappedChild2.getId());
        assertEquals(child2.getNodeIndex(), mappedChild2.getNodeIndex());
        assertEquals(mappedChild2.getType(), child2.getType().replace("[", "").replace("]", ""));
        assertEquals(child2.getStartPosition(), mappedChild2.getStartPosition());
        assertEquals(child2.getEndPosition(), mappedChild2.getEndPosition());

        JsHeapSnapshotDetailNode mappedChild3 = result.getChildren().get(2);
        assertEquals(child3.getClassName(), mappedChild3.getClassName());
        assertEquals(child3.getDistance(), mappedChild3.getDistance());
        assertEquals(child3.getRetainedSize(), mappedChild3.getRetainedSize());
        assertEquals(child3.getShallowSize(), mappedChild3.getShallowSize());
        assertEquals((int) child3.getShallowSizePercent(), mappedChild3.getShallowSizePercent());
        assertEquals((int) child3.getRetainedSizePercent(), mappedChild3.getRetainedSizePercent());
        assertEquals(child3.getNodeIndex(), mappedChild3.getId());
        assertEquals(child3.getNodeIndex(), mappedChild3.getNodeIndex());
        assertEquals(mappedChild3.getType(), child3.getType().replace("[", "").replace("]", ""));
        assertEquals(child3.getChildrenCount(), mappedChild3.getChildrenCount());
        assertEquals(child3.getRetainerCount(), mappedChild3.getRetainerCount());
        assertEquals(child3.getStartPosition(), mappedChild3.getStartPosition());
        assertEquals(child3.getEndPosition(), mappedChild3.getEndPosition());

        assertNotNull(result.getRetainerNodes());
        assertEquals(instanceDiffNode.getRetainerNodes().size(), result.getRetainerNodes().size());

        JsHeapSnapshotRetainerNode mappedRetainer1 = result.getRetainerNodes().get(0);
        assertEquals(retainer1.getClassName(), mappedRetainer1.getClassName());
        assertEquals(retainer1.getDistance(), mappedRetainer1.getDistance());
        assertEquals(retainer1.getRetainedSize(), mappedRetainer1.getRetainedSize());
        assertEquals(retainer1.getShallowSize(), mappedRetainer1.getShallowSize());
        assertEquals((int) retainer1.getShallowSizePercent(), mappedRetainer1.getShallowSizePercent());
        assertEquals((int) retainer1.getRetainedSizePercent(), mappedRetainer1.getRetainedSizePercent());
        assertEquals(retainer1.getNodeIndex(), mappedRetainer1.getId());
        assertEquals(retainer1.getNodeIndex(), mappedRetainer1.getNodeIndex());
        assertEquals(retainer1.getRetainerCount(), mappedRetainer1.getChildrenCount());
        assertEquals(retainer1.getStartPosition(), mappedRetainer1.getStartPosition());
        assertEquals(retainer1.getEndPosition(), mappedRetainer1.getEndPosition());

        JsHeapSnapshotRetainerNode mappedRetainer2 = result.getRetainerNodes().get(1);
        assertEquals(retainer2.getClassName(), mappedRetainer2.getClassName());
        assertEquals(retainer2.getDistance(), mappedRetainer2.getDistance());
        assertEquals(retainer2.getRetainedSize(), mappedRetainer2.getRetainedSize());
        assertEquals(retainer2.getShallowSize(), mappedRetainer2.getShallowSize());
        assertEquals((int) retainer2.getShallowSizePercent(), mappedRetainer2.getShallowSizePercent());
        assertEquals((int) retainer2.getRetainedSizePercent(), mappedRetainer2.getRetainedSizePercent());
        assertEquals(retainer2.getNodeIndex(), mappedRetainer2.getId());
        assertEquals(retainer2.getNodeIndex(), mappedRetainer2.getNodeIndex());
        assertEquals(retainer2.getRetainerCount(), mappedRetainer2.getChildrenCount());
        assertEquals(retainer2.getStartPosition(), mappedRetainer2.getStartPosition());
        assertEquals(retainer2.getEndPosition(), mappedRetainer2.getEndPosition());
    }

    private InstanceNode createInstanceNode(String className, int distance, int retainedSize, int shallowSize,
        double shallowPercent, double retainedPercent) {
        InstanceNode node = new InstanceNode();
        node.setClassName(className);
        node.setDistance(distance);
        node.setRetainedSize(retainedSize);
        node.setShallowSize(shallowSize);
        node.setShallowSizePercent(shallowPercent);
        node.setRetainedSizePercent(retainedPercent);
        node.setTotalSize(shallowSize * 2 + shallowSize / 2);
        node.setId((long) (Math.random() * 1000));
        node.setNodeIndex(distance);
        node.setType("[object]");
        node.setRootType("[-]");
        node.setChildrenCount(0);
        node.setRetainerCount(0);
        node.setStartPosition(0);
        node.setEndPosition(10);
        node.setChildren(new ArrayList<>());
        node.setRetainerNodes(new ArrayList<>());
        return node;
    }

    @Test
    public void testToJsHeapSnapshotRetainerNode() {
        InstanceNode source = new InstanceNode();
        source.setClassName("com.example.TestClass");
        source.setDistance(10);
        source.setRetainedSize(1000);
        source.setShallowSize(500);
        source.setShallowSizePercent(50.1);
        source.setRetainedSizePercent(60.2);
        source.setTotalSize(1500);
        source.setChildren(new ArrayList<>());
        source.setRetainerNodes(new ArrayList<>());
        source.setId(1);
        source.setNodeIndex(100);
        source.setType("object");
        source.setRootType("not_root");
        source.setChildrenCount(2);
        source.setRetainerCount(1);
        source.setStartPosition(0);
        source.setEndPosition(10);
        source.setArrayLength(0);

        Map<Integer, Long> indexToIdMap = new HashMap<>();
        indexToIdMap.put(1, 100L);

        JsHeapSnapshotRetainerNode result = mapper.toJsHeapSnapshotRetainerNode(source, indexToIdMap);

        assertEquals(100, result.getId());
        assertEquals("com.example.TestClass", result.getName());
        assertEquals(10, result.getDistance());
        assertEquals(500, result.getShallowSize());
        assertEquals(1000, result.getRetainedSize());
        assertEquals(50, result.getShallowSizePercent());
        assertEquals(60, result.getRetainedSizePercent());
        assertEquals(50, result.getTotalShallowSizePercent());
        assertEquals(60, result.getTotalRetainedSizePercent());
        assertEquals(1, result.getChildrenCount());
        assertEquals(0, result.getStartPosition());
        assertEquals(10, result.getEndPosition());

        assertEquals(0, result.getNativeSize());
        assertEquals(0, result.getRetainedNativeSize());
        assertEquals(0, result.getNativeSizePercent());
        assertEquals(0, result.getRetainedNativeSizePercent());
    }

    @Test
    public void testToHeapThreadInfo() {
        Frame frame = Frame.builder().funcName("testMethod").fileName("test.java").line(100).id(1)
                .locals(new ArrayList<>()).build();

        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setId(1);
        threadInfo.setName("TestThread");
        threadInfo.setFrames(List.of(frame));

        HeapThreadInfo result = mapper.toHeapThreadInfo(threadInfo, indexToIdMap);

        assertNotNull(result);
        assertEquals(1, result.getThreadId());
        assertEquals("TestThread", result.getThreadName());
        assertNotNull(result.getStackFrameInfoList());
        assertEquals(1, result.getStackFrameInfoList().size());

        StackFrame resultFrame = result.getStackFrameInfoList().get(0);
        assertEquals("testMethod", resultFrame.getMethodName());
        assertEquals("test.java", resultFrame.getFileName());
        assertEquals(100, resultFrame.getLineNumber());
        assertEquals(1, resultFrame.getId());
    }

    @Test
    public void testToHeapThreadInfoWithMultipleFrames() {
        Frame frame1 =
            Frame.builder().funcName("method1").fileName("file1.java").line(10).id(1).locals(new ArrayList<>()).build();

        Frame frame2 =
            Frame.builder().funcName("method2").fileName("file2.java").line(20).id(2).locals(new ArrayList<>()).build();

        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setId(2);
        threadInfo.setName("WorkerThread");
        threadInfo.setFrames(List.of(frame1, frame2));

        HeapThreadInfo result = mapper.toHeapThreadInfo(threadInfo, indexToIdMap);

        assertNotNull(result);
        assertEquals(2, result.getThreadId());
        assertEquals("WorkerThread", result.getThreadName());
        assertEquals(2, result.getStackFrameInfoList().size());

        StackFrame resultFrame1 = result.getStackFrameInfoList().get(0);
        assertEquals("method1", resultFrame1.getMethodName());
        assertEquals("file1.java", resultFrame1.getFileName());
        assertEquals(10, resultFrame1.getLineNumber());

        StackFrame resultFrame2 = result.getStackFrameInfoList().get(1);
        assertEquals("method2", resultFrame2.getMethodName());
        assertEquals("file2.java", resultFrame2.getFileName());
        assertEquals(20, resultFrame2.getLineNumber());
    }

    @Test
    public void testToHeapThreadInfos() {
        ThreadInfo thread1 = new ThreadInfo();
        thread1.setId(1);
        thread1.setName("Thread1");
        thread1.setFrames(new ArrayList<>());

        ThreadInfo thread2 = new ThreadInfo();
        thread2.setId(2);
        thread2.setName("Thread2");
        thread2.setFrames(new ArrayList<>());

        List<ThreadInfo> sources = List.of(thread1, thread2);

        List<HeapThreadInfo> results = mapper.toHeapThreadInfos(sources, indexToIdMap);

        assertNotNull(results);
        assertEquals(2, results.size());

        assertEquals(1, results.get(0).getThreadId());
        assertEquals("Thread1", results.get(0).getThreadName());

        assertEquals(2, results.get(1).getThreadId());
        assertEquals("Thread2", results.get(1).getThreadName());
    }

    @Test
    public void testToHeapThreadInfosWithEmptyList() {
        List<ThreadInfo> sources = new ArrayList<>();
        List<HeapThreadInfo> results = mapper.toHeapThreadInfos(sources, indexToIdMap);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testToHeapThreadInfoWithEmptyFrames() {
        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setId(3);
        threadInfo.setName("EmptyThread");
        threadInfo.setFrames(new ArrayList<>());

        HeapThreadInfo result = mapper.toHeapThreadInfo(threadInfo, indexToIdMap);

        assertNotNull(result);
        assertEquals(3, result.getThreadId());
        assertEquals("EmptyThread", result.getThreadName());
        assertNotNull(result.getStackFrameInfoList());
        assertTrue(result.getStackFrameInfoList().isEmpty());
    }

    @Test
    public void testToHeapThreadInfoWithLocalObjects() {
        InstanceNode local1 = new InstanceNode();
        local1.setClassName("String");
        local1.setId(100L);
        local1.setNodeIndex(1);
        local1.setShallowSize(1024);
        local1.setRetainedSize(2048);

        InstanceNode local2 = new InstanceNode();
        local2.setClassName("int[]");
        local2.setId(200L);
        local2.setNodeIndex(2);
        local2.setShallowSize(512);
        local2.setRetainedSize(1024);

        Frame frame = Frame.builder().funcName("testMethod").fileName("test.java").line(100).id(1)
                .locals(List.of(local1, local2)).build();

        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setId(1);
        threadInfo.setName("TestThread");
        threadInfo.setFrames(List.of(frame));

        HeapThreadInfo result = mapper.toHeapThreadInfo(threadInfo, indexToIdMap);

        StackFrame resultFrame = result.getStackFrameInfoList().get(0);
        assertEquals("testMethod", resultFrame.getMethodName());

        assertEquals(2, resultFrame.getLocalObjectList().size());

        // 验证第一个 localObject (已映射字段)
        LocalObject lo1 = resultFrame.getLocalObjectList().get(0);
        assertEquals("String", lo1.getTypeName());
        assertEquals(1, lo1.getObjectId());
        assertEquals(1024, lo1.getShallowSize());
        assertEquals(2048, lo1.getRetainedSize());

        // 验证第二个 localObject (已映射字段)
        LocalObject lo2 = resultFrame.getLocalObjectList().get(1);
        assertEquals("int[]", lo2.getTypeName());
        assertEquals(2, lo2.getObjectId());
        assertEquals(512, lo2.getShallowSize());
        assertEquals(1024, lo2.getRetainedSize());
    }
}