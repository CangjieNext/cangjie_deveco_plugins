/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotRetainerNode;
import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;

import com.cjprof.jni.model.ConstructorNode;
import com.cjprof.jni.model.InstanceNode;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 零风险补测 {@link CjMemoryService} 中不依赖 native（{@code com.cjprof.jni.Cjprof}）的纯逻辑方法。
 *
 * <p>这些方法完全基于 MapStruct mapper（{@code CjMemoryBaseMapper.INSTANCE}）+ 手造
 * {@link InstanceNode}/{@link ConstructorNode} 即可驱动，不触碰 Cjprof 静态初始化器，也不接触
 * IntelliJ ApplicationManager，因此可以安全地在纯 UT 环境中执行。</p>
 *
 * @since 2026-09-03
 */
class CjMemoryServicePureLogicTest {

    /**
     * 手造一个具备完整映射字段的 {@link InstanceNode}，供 mapper 转换与树组装使用。
     */
    private static InstanceNode instanceNode(String className, int nodeIndex, long id) {
        InstanceNode node = new InstanceNode();
        node.setClassName(className);
        node.setDistance(0);
        node.setRetainedSize(100);
        node.setShallowSize(50);
        node.setShallowSizePercent(25.0);
        node.setRetainedSizePercent(75.0);
        node.setTotalSize(150);
        node.setId(id);
        node.setNodeIndex(nodeIndex);
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

    private static Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object... args)
        throws Exception {
        Method method = CjMemoryService.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    /**
     * buildSingleTree：单层路径（仅根节点）。
     * 期望：返回的 instanceNode distance == 1，retainerNodes 为空。
     */
    @Test
    void buildSingleTree_singleNodePath_setsDistanceAndNoRetainers() throws Exception {
        CjMemoryService service = new CjMemoryService();
        List<InstanceNode> path = new ArrayList<>();
        path.add(instanceNode("com.example.Root", 0, 1L));

        @SuppressWarnings("unchecked")
        JsHeapSnapshotInstanceNode result = (JsHeapSnapshotInstanceNode) invoke(service, "buildSingleTree",
            new Class<?>[] {List.class}, path);

        assertThat(result).isNotNull();
        assertThat(result.getDistance()).isEqualTo(1);
        assertThat(result.getRetainerNodes()).isEmpty();
        assertThat(result.getName()).isEqualTo("com.example.Root");
    }

    /**
     * buildSingleTree：多层路径（根 + 若干 retainer 层）。
     * 期望：第二层进入 retainerNodes，第三层及以后逐层下挂到 children，distance 逐层递减。
     */
    @Test
    void buildSingleTree_multiNodePath_nestsRetainersByDistance() throws Exception {
        CjMemoryService service = new CjMemoryService();
        List<InstanceNode> path = new ArrayList<>();
        path.add(instanceNode("Root", 0, 1L));
        path.add(instanceNode("Mid", 1, 2L));
        path.add(instanceNode("Leaf", 2, 3L));

        @SuppressWarnings("unchecked")
        JsHeapSnapshotInstanceNode result = (JsHeapSnapshotInstanceNode) invoke(service, "buildSingleTree",
            new Class<?>[] {List.class}, path);

        // 根节点
        assertThat(result.getDistance()).isEqualTo(3);
        assertThat(result.getName()).isEqualTo("Root");

        // 第二层：retainerNodes 恰好一个，distance = 2
        List<JsHeapSnapshotRetainerNode> retainers = result.getRetainerNodes();
        assertThat(retainers).hasSize(1);
        JsHeapSnapshotRetainerNode second = retainers.get(0);
        assertThat(second.getName()).isEqualTo("Mid");
        assertThat(second.getDistance()).isEqualTo(2);

        // 第三层：挂在第二层 children 下，distance = 1
        assertThat(second.getChildren()).hasSize(1);
        JsHeapSnapshotRetainerNode third = second.getChildren().get(0);
        assertThat(third.getName()).isEqualTo("Leaf");
        assertThat(third.getDistance()).isEqualTo(1);
    }

    /**
     * covertInstanceToTree：空路径列表 → 直接返回空 list（走日志告警分支）。
     */
    @Test
    void covertInstanceToTree_emptyPathList_returnsEmptyList() throws Exception {
        CjMemoryService service = new CjMemoryService();

        @SuppressWarnings("unchecked")
        List<JsHeapSnapshotInstanceNode> result = (List<JsHeapSnapshotInstanceNode>) invoke(service,
            "covertInstanceToTree", new Class<?>[] {List.class, int.class}, new ArrayList<>(), 0);

        assertThat(result).isEmpty();
    }

    /**
     * covertInstanceToTree：多条路径 → 每条转成一棵树，并写入 countDelta。
     */
    @Test
    void covertInstanceToTree_multiplePaths_buildsTreePerPathWithCountDelta() throws Exception {
        CjMemoryService service = new CjMemoryService();
        List<List<InstanceNode>> pathList = new ArrayList<>();
        List<InstanceNode> path1 = new ArrayList<>();
        path1.add(instanceNode("A", 0, 1L));
        List<InstanceNode> path2 = new ArrayList<>();
        path2.add(instanceNode("B", 0, 2L));
        path2.add(instanceNode("B-child", 1, 3L));
        pathList.add(path1);
        pathList.add(path2);

        @SuppressWarnings("unchecked")
        List<JsHeapSnapshotInstanceNode> result = (List<JsHeapSnapshotInstanceNode>) invoke(service,
            "covertInstanceToTree", new Class<?>[] {List.class, int.class}, pathList, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("A");
        assertThat(result.get(0).getCountDelta()).isEqualTo(5);
        assertThat(result.get(1).getName()).isEqualTo("B");
        assertThat(result.get(1).getCountDelta()).isEqualTo(5);
        // 第二条路径有 retainer 子层
        assertThat(result.get(1).getRetainerNodes()).hasSize(1);
    }

    /**
     * isFind：children 为空 → 返回 false。
     */
    @Test
    void isFind_emptyChildren_returnsFalse() throws Exception {
        CjMemoryService service = new CjMemoryService();
        ConstructorNode node = new ConstructorNode();
        node.setChildren(new ArrayList<>());
        ArkHeapNodeExpandRequest request = org.mockito.Mockito.mock(ArkHeapNodeExpandRequest.class);

        Boolean isFoundEmpty = (Boolean) invoke(service, "isFind",
            new Class<?>[] {ArkHeapNodeExpandRequest.class, ConstructorNode.class}, request, node);

        assertThat(isFoundEmpty).isFalse();
    }

    /**
     * isFind：children 中存在 nodeIndex 匹配节点 → 设置 start 并返回 true。
     */
    @Test
    void isFind_matchingNodeIndex_setsStartAndReturnsTrue() throws Exception {
        CjMemoryService service = new CjMemoryService();
        ConstructorNode node = new ConstructorNode();
        List<InstanceNode> children = new ArrayList<>();
        InstanceNode target = instanceNode("target", 7, 10L);
        children.add(instanceNode("other", 1, 11L));
        children.add(target);
        node.setChildren(children);

        ArkHeapNodeExpandRequest request = org.mockito.Mockito.mock(ArkHeapNodeExpandRequest.class);
        org.mockito.Mockito.when(request.getNodeId()).thenReturn(7);

        Boolean isFoundMatching = (Boolean) invoke(service, "isFind",
            new Class<?>[] {ArkHeapNodeExpandRequest.class, ConstructorNode.class}, request, node);

        assertThat(isFoundMatching).isTrue();
        org.mockito.Mockito.verify(request).setStart(1);
    }

    /**
     * isFind：children 中无匹配节点 → 返回 false，不设置 start。
     */
    @Test
    void isFind_noMatchingNodeIndex_returnsFalse() throws Exception {
        CjMemoryService service = new CjMemoryService();
        ConstructorNode node = new ConstructorNode();
        List<InstanceNode> children = new ArrayList<>();
        children.add(instanceNode("other", 1, 11L));
        node.setChildren(children);

        ArkHeapNodeExpandRequest request = org.mockito.Mockito.mock(ArkHeapNodeExpandRequest.class);
        org.mockito.Mockito.when(request.getNodeId()).thenReturn(999);

        Boolean isFoundNoMatch = (Boolean) invoke(service, "isFind",
            new Class<?>[] {ArkHeapNodeExpandRequest.class, ConstructorNode.class}, request, node);

        assertThat(isFoundNoMatch).isFalse();
        org.mockito.Mockito.verify(request, org.mockito.Mockito.never()).setStart(org.mockito.Mockito.anyInt());
    }

    /**
     * saveSnapshotFileData：文件名无后缀 → 在 native 调用前抛 ProfilerException。
     * <p>该分支位于 saveHeapSnapshotCacheData（native）之前，属于零风险可测分支。</p>
     */
    @Test
    void saveSnapshotFileData_fileNameWithoutSuffix_throwsBeforeNativeCall() throws Exception {
        CjMemoryService service = new CjMemoryService();
        // "noSuffix" 无 '.'，suffixDotIndex == -1 <= 0 -> 抛异常
        File file = new File("noSuffix");

        assertThatThrownBy(() -> invoke(service, "saveSnapshotFileData",
            new Class<?>[] {String.class, long.class, long.class, File.class, boolean.class},
            "session", 100L, 200L, file, false))
            .hasCauseInstanceOf(ProfilerException.class);

        // 确认异常类型
        try {
            invoke(service, "saveSnapshotFileData",
                new Class<?>[] {String.class, long.class, long.class, File.class, boolean.class},
                "session", 100L, 200L, file, false);
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(ProfilerException.class);
        }
    }

    /**
     * handleCjStackTree：空列表 → 直接返回，不产生任何子节点。
     * <p>该方法是递归入口，传入空列表时 while 条件不成立，属最浅的零风险分支。</p>
     */
    @Test
    void handleCjStackTree_emptyProps_doesNothing() throws Exception {
        CjMemoryService service = new CjMemoryService();
        com.huawei.cjprofiler.ability.arkmemory.entity.CjStack parent =
            new com.huawei.cjprofiler.ability.arkmemory.entity.CjStack();
        parent.setName("root");
        parent.setChildren(new ArrayList<>());

        invoke(service, "handleCjStackTree",
            new Class<?>[] {int.class, List.class, com.huawei.cjprofiler.ability.arkmemory.entity.CjStack.class},
            0, new ArrayList<>(), parent);

        assertThat(parent.getChildren()).isEmpty();
    }
}