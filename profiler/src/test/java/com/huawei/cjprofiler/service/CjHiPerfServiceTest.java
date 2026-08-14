/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.cjprofiler.service;

import com.huawei.deveco.insight.ohos.common.constant.CangJieConstant;
import com.huawei.deveco.insight.ohos.model.vo.HiPerfNode;
import com.huawei.deveco.insight.ohos.model.vo.hiperf.HiPerfNodeStateVo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CjHiPerfServiceTest
 *
 * @since 2026-06-04
 */
public class CjHiPerfServiceTest {

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }

    private HiPerfNode createHiPerfNodeWithCategory(String category) {
        HiPerfNode node = new HiPerfNode();
        setFieldValue(node, "category", category);
        return node;
    }

    // ==================== selectCangJieStateVo tests ====================

    @Test
    public void testSelectCangJieStateVoWithMixedCategories() {
        List<HiPerfNodeStateVo> stateVoList = new ArrayList<>();

        HiPerfNodeStateVo cangJie1 = HiPerfNodeStateVo.builder()
            .startTime(0L)
            .endTime(100L)
            .duration(100L)
            .name("cangjie1")
            .category(CangJieConstant.CANG_JIE)
            .build();
        stateVoList.add(cangJie1);

        HiPerfNodeStateVo system1 = HiPerfNodeStateVo.builder()
            .startTime(100L)
            .endTime(200L)
            .duration(100L)
            .name("system1")
            .category("system")
            .build();
        stateVoList.add(system1);

        HiPerfNodeStateVo cangJie2 = HiPerfNodeStateVo.builder()
            .startTime(200L)
            .endTime(300L)
            .duration(100L)
            .name("cangjie2")
            .category(CangJieConstant.CANG_JIE)
            .build();
        stateVoList.add(cangJie2);

        List<HiPerfNodeStateVo> result = CjHiPerfService.selectCangJieStateVo(stateVoList);

        assertNotNull(result);
        // Implementation only adds cangjie nodes and idle inserted before cangjie
        // idle inserted before first cangjie (0-0), then cangjie1, idle (100-200) before cangjie2, then cangjie2
        assertEquals(4, result.size());

        // First should be idle from startTime to first cangjie start
        assertEquals("idle", result.get(0).getName());

        // Second should be cangjie1
        assertEquals("cangjie1", result.get(1).getName());

        // Third should be idle between system1 and cangjie2
        assertEquals("idle", result.get(2).getName());
    }

    @Test
    public void testSelectCangJieStateVoAllCangJie() {
        List<HiPerfNodeStateVo> stateVoList = new ArrayList<>();

        HiPerfNodeStateVo cangJie1 = HiPerfNodeStateVo.builder()
            .startTime(0L)
            .endTime(100L)
            .duration(100L)
            .name("cangjie1")
            .category(CangJieConstant.CANG_JIE)
            .build();
        stateVoList.add(cangJie1);

        HiPerfNodeStateVo cangJie2 = HiPerfNodeStateVo.builder()
            .startTime(100L)
            .endTime(200L)
            .duration(100L)
            .name("cangjie2")
            .category(CangJieConstant.CANG_JIE)
            .build();
        stateVoList.add(cangJie2);

        List<HiPerfNodeStateVo> result = CjHiPerfService.selectCangJieStateVo(stateVoList);

        assertNotNull(result);
        // Implementation adds idle(0-0) before first cangjie, then cangjie1, idle(100-100), then cangjie2
        assertEquals(4, result.size());
        assertEquals("idle", result.get(0).getName());
        assertEquals("cangjie1", result.get(1).getName());
        assertEquals("idle", result.get(2).getName());
        assertEquals("cangjie2", result.get(3).getName());
    }

    @Test
    public void testSelectCangJieStateVoAllSystem() {
        List<HiPerfNodeStateVo> stateVoList = new ArrayList<>();

        HiPerfNodeStateVo system1 = HiPerfNodeStateVo.builder()
            .startTime(0L)
            .endTime(100L)
            .duration(100L)
            .name("system1")
            .category("system")
            .build();
        stateVoList.add(system1);

        HiPerfNodeStateVo system2 = HiPerfNodeStateVo.builder()
            .startTime(100L)
            .endTime(200L)
            .duration(100L)
            .name("system2")
            .category("system")
            .build();
        stateVoList.add(system2);

        List<HiPerfNodeStateVo> result = CjHiPerfService.selectCangJieStateVo(stateVoList);

        assertNotNull(result);
        // Implementation only adds idle at end if last is not cangjie - does NOT add system nodes
        assertEquals(1, result.size());
        assertEquals("idle", result.get(0).getName());
    }

    // ==================== searchCangJieData tests ====================

    @Test
    public void testSearchCangJieDataWithCangJieNode() {
        HiPerfNode root = createHiPerfNodeWithCategory("system");
        HiPerfNode cangJieChild = createHiPerfNodeWithCategory(CangJieConstant.CANG_JIE);
        cangJieChild.setChildren(new ArrayList<>());

        root.setChildren(new ArrayList<>());
        root.getChildren().add(cangJieChild);

        Optional<HiPerfNode> result = CjHiPerfService.searchCangJieData(root);

        assertTrue(result.isPresent());
        // dfs flattens - cangJieChild is added to the children list of result
        assertEquals(1, result.get().getChildren().size());
        assertEquals(CangJieConstant.CANG_JIE, result.get().getChildren().get(0).getCategory());
    }

    @Test
    public void testSearchCangJieDataWithNoCangJieNode() {
        HiPerfNode root = createHiPerfNodeWithCategory("system");
        HiPerfNode systemChild = createHiPerfNodeWithCategory("system");
        systemChild.setChildren(new ArrayList<>());

        root.setChildren(new ArrayList<>());
        root.getChildren().add(systemChild);

        Optional<HiPerfNode> result = CjHiPerfService.searchCangJieData(root);

        assertFalse(result.isPresent());
    }

    @Test
    public void testSearchCangJieDataWithNestedCangJie() {
        HiPerfNode root = createHiPerfNodeWithCategory("system");
        HiPerfNode level1 = createHiPerfNodeWithCategory("system");
        HiPerfNode cangJieDeep = createHiPerfNodeWithCategory(CangJieConstant.CANG_JIE);
        cangJieDeep.setChildren(new ArrayList<>());

        level1.setChildren(new ArrayList<>());
        level1.getChildren().add(cangJieDeep);
        root.setChildren(new ArrayList<>());
        root.getChildren().add(level1);

        Optional<HiPerfNode> result = CjHiPerfService.searchCangJieData(root);

        assertTrue(result.isPresent());
        // dfs flattens, cangJieDeep is added directly to root's children
        assertEquals(1, result.get().getChildren().size());
        assertEquals(CangJieConstant.CANG_JIE, result.get().getChildren().get(0).getCategory());
    }

    @Test
    public void testSearchCangJieDataWithEmptyChildren() {
        HiPerfNode root = createHiPerfNodeWithCategory(CangJieConstant.CANG_JIE);
        root.setChildren(new ArrayList<>());

        Optional<HiPerfNode> result = CjHiPerfService.searchCangJieData(root);

        assertTrue(result.isPresent());
        // root itself is a cangjie node, so it's added to the list
        assertEquals(1, result.get().getChildren().size());
    }

    // ==================== searchIcicle tests ====================

    @Test
    public void testSearchIcicleWithCangJieData() {
        List<HiPerfNode> threadNodes = new ArrayList<>();

        HiPerfNode root = createHiPerfNodeWithCategory("system");

        HiPerfNode cangJieChild = createHiPerfNodeWithCategory(CangJieConstant.CANG_JIE);
        cangJieChild.setChildren(new ArrayList<>());
        setFieldValue(cangJieChild, "startBootTime", 0L);
        setFieldValue(cangJieChild, "endBootTime", 100L);
        cangJieChild.setDepth(0);

        root.setChildren(new ArrayList<>());
        root.getChildren().add(cangJieChild);
        threadNodes.add(root);

        List<HiPerfNode> result = CjHiPerfService.searchIcicle(threadNodes);

        assertNotNull(result);
        assertEquals(1, result.size());
        // searchIcicle returns the root node (with children set to cangJie nodes)
        assertEquals("system", result.get(0).getCategory());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(CangJieConstant.CANG_JIE, result.get(0).getChildren().get(0).getCategory());
    }

    @Test
    public void testSearchIcicleWithNoCangJieData() {
        List<HiPerfNode> threadNodes = new ArrayList<>();

        HiPerfNode root = createHiPerfNodeWithCategory("system");

        HiPerfNode systemChild = createHiPerfNodeWithCategory("system");
        systemChild.setChildren(new ArrayList<>());

        root.setChildren(new ArrayList<>());
        root.getChildren().add(systemChild);
        threadNodes.add(root);

        List<HiPerfNode> result = CjHiPerfService.searchIcicle(threadNodes);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchIcicleWithEmptyChildren() {
        List<HiPerfNode> threadNodes = new ArrayList<>();

        HiPerfNode root = createHiPerfNodeWithCategory("system");
        root.setChildren(new ArrayList<>());
        threadNodes.add(root);

        List<HiPerfNode> result = CjHiPerfService.searchIcicle(threadNodes);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
