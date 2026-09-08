/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.uitl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.huawei.cjprofiler.utils.PageToolsForCjprof;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotConstructorNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotDetailNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotDiffNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotRetainerNode;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@code PageToolsForCjprof} paging helpers.
 *
 * @since 2026-09-08
 */
public class PageToolsForCjprofTest {
    private ArkHeapNodeExpandRequest request;

    private ArkHeapNodeExpandRequest largeStartRequest;

    private JsHeapSnapshotConstructorNode constructorNode;

    private JsHeapSnapshotInstanceNode instanceNode;

    private JsHeapSnapshotInstanceNode instanceNodeWithFewRetainers;

    private JsHeapSnapshotRetainerNode retainerNode;

    private JsHeapSnapshotDiffNode diffNode;

    @BeforeEach
    public void setUp() {
        buildDefaultRequest();
        buildConstructorNode();
        buildInstanceNode();
        buildInstanceNodeWithFewRetainers();
        buildRetainerNode();
        buildDiffNode();
    }

    @Test
    public void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new PageToolsForCjprof();
    }

    private void buildDefaultRequest() {
        // 创建 request 对象并设置默认值
        request = new ArkHeapNodeExpandRequest();
        request.setStart(0);
        request.setLength(5);

        // 创建 largeStartRequest — 模拟 start > childrenCount 和 start > retainerCount
        largeStartRequest = new ArkHeapNodeExpandRequest();
        largeStartRequest.setStart(20);
        largeStartRequest.setLength(5);
    }

    private void buildConstructorNode() {
        // 创建 constructorNode 对象
        constructorNode = new JsHeapSnapshotConstructorNode();
        constructorNode.setChildrenCount(10);
        List<JsHeapSnapshotInstanceNode> children = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            children.add(new JsHeapSnapshotInstanceNode());
        }
        constructorNode.setChildren(children);
    }

    private void buildInstanceNode() {
        // 创建 instanceNode 对象
        instanceNode = new JsHeapSnapshotInstanceNode();
        instanceNode.setChildrenCount(10);
        instanceNode.setRetainerCount(5);
        instanceNode.setFromCjprof(true);
        List<JsHeapSnapshotRetainerNode> retainerNodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            retainerNodes.add(new JsHeapSnapshotRetainerNode());
        }
        instanceNode.setRetainerNodes(retainerNodes);

        List<JsHeapSnapshotDetailNode> detailChildren = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            detailChildren.add(new JsHeapSnapshotDetailNode());
        }
        instanceNode.setChildren(detailChildren);
    }

    private void buildInstanceNodeWithFewRetainers() {
        // 创建 instanceNodeWithFewRetainers — retainerCount < start
        instanceNodeWithFewRetainers = new JsHeapSnapshotInstanceNode();
        instanceNodeWithFewRetainers.setChildrenCount(10);
        instanceNodeWithFewRetainers.setRetainerCount(2);
        instanceNodeWithFewRetainers.setFromCjprof(true);
        List<JsHeapSnapshotRetainerNode> retainerNodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            retainerNodes.add(new JsHeapSnapshotRetainerNode());
        }
        instanceNodeWithFewRetainers.setRetainerNodes(retainerNodes);
        List<JsHeapSnapshotDetailNode> detailChildren = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            detailChildren.add(new JsHeapSnapshotDetailNode());
        }
        instanceNodeWithFewRetainers.setChildren(detailChildren);
    }

    private void buildRetainerNode() {
        // 创建 retainerNode 对象
        retainerNode = new JsHeapSnapshotRetainerNode();
        retainerNode.setChildrenCount(10);
        List<JsHeapSnapshotRetainerNode> retainerChildren = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            retainerChildren.add(new JsHeapSnapshotRetainerNode());
        }
        retainerNode.setChildren(retainerChildren);
    }

    private void buildDiffNode() {
        // 创建 diffNode 对象
        diffNode = JsHeapSnapshotDiffNode.builder().build();
        diffNode.setChildrenCount(10);
        List<JsHeapSnapshotInstanceNode> children = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            children.add(new JsHeapSnapshotInstanceNode());
        }
        diffNode.setChildren(children);
    }

    @Test
    public void testPageConstructorNode_Success() {
        // 调用被测试方法
        JsHeapSnapshotConstructorNode result = PageToolsForCjprof.pageConstructorNode(request, constructorNode);

        // 验证结果
        assertNotNull(result);
        assertEquals(request.getStart(), result.getStartPosition());
        assertEquals(request.getStart() + request.getLength() - 1, result.getEndPosition());
        assertEquals(
            constructorNode.getChildren().subList(request.getStart(), request.getStart() + request.getLength()),
            result.getChildren());
    }

    @Test
    public void testPageInstanceNode_Success() {
        // 调用被测试方法
        JsHeapSnapshotInstanceNode result = PageToolsForCjprof.pageInstanceNode(request, instanceNode);

        // 验证结果
        assertNotNull(result);
        assertEquals(request.getStart(), result.getStartPosition());
        assertEquals(request.getStart() + request.getLength() - 1, result.getEndPosition());
        assertEquals(instanceNode.getChildren().subList(request.getStart(), request.getStart() + request.getLength()),
            result.getChildren());
        assertEquals(
            instanceNode.getRetainerNodes().subList(request.getStart(), request.getStart() + request.getLength()),
            result.getRetainerNodes());
    }

    @Test
    public void testPageInstanceNode_StartExceedsChildren() {
        // start > childrenCount → 走三元 true 分支，使用 instanceNode.getStartPosition()
        JsHeapSnapshotInstanceNode result = PageToolsForCjprof.pageInstanceNode(largeStartRequest, instanceNode);

        assertNotNull(result);
        assertEquals(instanceNode.getStartPosition(), result.getStartPosition());
        assertEquals(instanceNode.getStartPositionRef(), result.getStartPositionRef());
    }

    @Test
    public void testPageInstanceNode_StartExceedsRetainers() {
        // start > retainerCount → 走三元 true 分支，使用 instanceNode.getStartPositionRef()
        // 对于 instanceNodeWithFewRetainers, retainerCount=2 < start=20
        JsHeapSnapshotInstanceNode result =
            PageToolsForCjprof.pageInstanceNode(largeStartRequest, instanceNodeWithFewRetainers);

        assertNotNull(result);
        assertEquals(instanceNodeWithFewRetainers.getStartPosition(), result.getStartPosition());
        assertEquals(instanceNodeWithFewRetainers.getStartPositionRef(), result.getStartPositionRef());
    }

    @Test
    public void testPageRetainerNode_Success() {
        // 调用被测试方法
        JsHeapSnapshotRetainerNode result = PageToolsForCjprof.pageRetainerNode(request, retainerNode);

        // 验证结果
        assertNotNull(result);
        assertEquals(request.getStart(), result.getStartPosition());
        assertEquals(request.getStart() + request.getLength() - 1, result.getEndPosition());
        assertEquals(retainerNode.getChildren().subList(request.getStart(), request.getStart() + request.getLength()),
            result.getChildren());
    }

    @Test
    public void testPageDiffNode_Success() {
        // 调用被测试方法
        JsHeapSnapshotDiffNode result = PageToolsForCjprof.pageDiffNode(request, diffNode);

        // 验证结果
        assertNotNull(result);
        assertEquals(request.getStart(), result.getStartPosition());
        assertEquals(request.getStart() + request.getLength() - 1, result.getEndPosition());
        assertEquals(diffNode.getChildren().subList(request.getStart(), request.getStart() + request.getLength()),
            result.getChildren());
    }
}
