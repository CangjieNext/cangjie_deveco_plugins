/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.cjprofiler.model.po.cjprof.HeapThreadInfo;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.CjprofExtension;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests boundary behavior of {@link CjMemoryDataService} with real method calls.
 *
 * @since 2026-08-14
 */
class CjMemoryDataServiceStaticMockTest {
    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjMemoryDataService();
    }

    @Test
    void parseRootTypeMapCorrectlyAssignsTypes() {
        Map<Integer, String> rootTypeMap = new HashMap<>();
        RawHeapSnapshot snapshot = snapshotWithExtension();
        snapshot.getCjprofExtension().setRootDataByTypes(new int[][] {{}, {10, 11}, {20}, {30}});

        CjMemoryDataService.parseRootTypeMap(rootTypeMap, snapshot);

        assertEquals("global", rootTypeMap.get(10));
        assertEquals("global", rootTypeMap.get(11));
        assertEquals("local", rootTypeMap.get(20));
        assertEquals("unknown", rootTypeMap.get(30));
    }

    @Test
    void parseRootTypeMapHandlesNullMapDataAndEmptyRows() {
        RawHeapSnapshot snapshot = snapshotWithExtension();
        snapshot.getCjprofExtension().setRootDataByTypes(new int[][] {null, {}, {7}});
        Map<Integer, String> map = new HashMap<>();

        assertDoesNotThrow(() -> CjMemoryDataService.parseRootTypeMap(map, snapshot));
        assertEquals(Map.of(7, "local"), map);
        assertDoesNotThrow(() -> CjMemoryDataService.parseRootTypeMap(null, snapshot));

        snapshot.getCjprofExtension().setRootDataByTypes(null);
        CjMemoryDataService.parseRootTypeMap(map, snapshot);
        assertEquals(1, map.size());
    }

    @Test
    void parseArrayLengthMapHandlesAbsentSnapshotOrData() {
        Map<Integer, Integer> result = new HashMap<>();
        RawHeapSnapshot withoutExtension = new RawHeapSnapshot();
        RawHeapSnapshot snapshot = snapshotWithExtension();

        assertDoesNotThrow(() -> CjMemoryDataService.parseArrayLengthMap(result, null));
        assertDoesNotThrow(() -> CjMemoryDataService.parseArrayLengthMap(result, withoutExtension));
        assertDoesNotThrow(() -> CjMemoryDataService.parseArrayLengthMap(result, snapshot));
        snapshot.getCjprofExtension().setNodeArrayLengths(new int[0]);
        assertDoesNotThrow(() -> CjMemoryDataService.parseArrayLengthMap(result, snapshot));
        assertTrue(result.isEmpty());
    }

    @Test
    void parseArrayLengthMapParsesPairsAndRejectsTrailingId() {
        Map<Integer, Integer> result = new HashMap<>();
        RawHeapSnapshot snapshot = snapshotWithExtension();
        snapshot.getCjprofExtension().setNodeArrayLengths(new int[] {5, 20, 8, 30});

        CjMemoryDataService.parseArrayLengthMap(result, snapshot);
        assertEquals(Map.of(5, 20, 8, 30), result);

        snapshot.getCjprofExtension().setNodeArrayLengths(new int[] {5});
        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> CjMemoryDataService.parseArrayLengthMap(result, snapshot));
    }

    @Test
    void parseHeapThreadDetailSkipsMalformedFramesAndSummaryEntries() {
        RawHeapSnapshot snapshot = snapshotWithExtension();
        snapshot.setStrings(List.of("thread", "method", "file", "object"));
        snapshot.setNodes(new int[] {0, 3, 10, 64, 0, 0, 0, 0});
        Map<Integer, int[]> frames = new HashMap<>();
        frames.put(9, null);
        frames.put(8, new int[] {1, 0});
        frames.put(3, new int[] {1, 0, 1, 2, 50});
        snapshot.getCjprofExtension().setStackFramesMap(frames);
        snapshot.getCjprofExtension().setThreadSummary(new int[] {10, 3, -1, 10, 3, 8, 10});

        List<HeapThreadInfo> result = CjMemoryDataService.parseHeapThreadDetail(snapshot);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getStackFrameInfoList().size());
        assertTrue(result.get(0).getStackFrameInfoList().get(0).getLocalObjectList().isEmpty());
    }

    private static RawHeapSnapshot snapshotWithExtension() {
        RawHeapSnapshot snapshot = new RawHeapSnapshot();
        snapshot.setCjprofExtension(new CjprofExtension());
        return snapshot;
    }
}