/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.cjprofiler.ability.arkmemory.parser.CjMemoryDataService;
import com.huawei.cjprofiler.common.enums.RootTypeEnum;
import com.huawei.cjprofiler.model.po.cjprof.HeapThreadInfo;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.CjprofExtension;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用真实导出快照(real-cangjie-heap-snapshot.cjheapsnapshot)解析出的
 * RawHeapSnapshot 驱动 CjMemoryService 下游数据初始化方法。
 *
 * <p>真实文件通过 handlerTraceFile 正确解析后：
 * <ul>
 *   <li>root_data_by_types: 4 行(下标1~3为真实数据)，首行 null 跳过</li>
 *   <li>node_array_lengths: 2762 个 int，步长 2 → 1381 个 [objectId, arrayLength]</li>
 *   <li>stack_frames_map / thread_summary: 该快照为空 → parseHeapThreadDetail 返回空列表</li>
 *   <li>trace_tree: [[]] → initTraceTreeMap 走到空树分支，put 一个空 children</li>
 * </ul>
 */
class CjMemoryServiceRealDataTest {
    private static final String FIXTURE = "src/test/resources/fixtures/real-cangjie-heap-snapshot.cjheapsnapshot";
    private static final String START_TIME = "1787729306440";

    private static RawHeapSnapshot parseRealSnapshot() {
        return new CjMemoryService().handlerTraceFile(FIXTURE);
    }

    @Test
    void handlerTraceFile_parsesRealSnapshotCompletely() {
        RawHeapSnapshot raw = parseRealSnapshot();

        assertThat(raw).isNotNull();
        assertThat(raw.getStrings()).hasSize(327);
        assertThat(raw.getNodes()).hasSize(80288);   // 10036 nodes * 8 fields
        assertThat(raw.getEdges()).hasSize(132330);  // 44110 edges * 3 fields
        assertThat(raw.getSnapshot()).isNotNull();
        assertThat(raw.getSnapshot().getNodeCount()).isEqualTo(10036);
        assertThat(raw.getSnapshot().getEdgeCount()).isEqualTo(44110);
        assertThat(raw.getStartTime()).isNotNull();

        CjprofExtension ext = raw.getCjprofExtension();
        assertThat(ext).isNotNull();
        assertThat(ext.getVersion()).isNotNull();
        assertThat(ext.getNodeArrayLengths()).isNotEmpty();
    }

    // ---- CjMemoryDataService 用真实 cjprof_ext 数据 ----

    @Test
    void parseRootTypeMap_withRealSnapshot_bindsAllRootData() {
        RawHeapSnapshot raw = parseRealSnapshot();
        Map<Integer, String> rootTypeMap = new HashMap<>();

        CjMemoryDataService.parseRootTypeMap(rootTypeMap, raw);

        // 真实数据第 1 行以 652 起始
        assertThat(rootTypeMap).isNotEmpty();
        assertThat(rootTypeMap.containsKey(652)).isTrue();
        assertThat(rootTypeMap.get(652)).isEqualTo(RootTypeEnum.getValueByKey((byte) 1));
        assertThat(rootTypeMap.values()).allMatch(v ->
            RootTypeEnum.GLOBAL.getValue().equals(v)
                || RootTypeEnum.LOCAL.getValue().equals(v)
                || RootTypeEnum.UNKNOWN.getValue().equals(v));
    }

    @Test
    void parseArrayLengthMap_withRealSnapshot_bindsAllPairs() {
        RawHeapSnapshot raw = parseRealSnapshot();
        Map<Integer, Integer> arrayLengthMap = new HashMap<>();

        CjMemoryDataService.parseArrayLengthMap(arrayLengthMap, raw);

        // 2762 个 int / 2 = 1381 个 [objectId, arrayLength]
        assertThat(arrayLengthMap).hasSize(1381);
        assertThat(arrayLengthMap.values()).allMatch(length -> length >= 0);
    }

    @Test
    void parseHeapThreadDetail_withRealSnapshot_returnsEmptyWhenNoFrames() {
        RawHeapSnapshot raw = parseRealSnapshot();

        List<HeapThreadInfo> result = CjMemoryDataService.parseHeapThreadDetail(raw);

        assertThat(result).isEmpty();
    }

    // ---- CjMemoryService 私有 init 方法（反射驱动真实 RawHeapSnapshot）----

    @Test
    void initRootTypeMap_realSnapshot_populatesRootMap() throws Exception {
        CjMemoryService service = new CjMemoryService();
        invokePrivate(service, "initRootTypeMap", List.of(parseRealSnapshot()));

        @SuppressWarnings("unchecked")
        Map<Integer, String> rootTypeMap = getField(service, "rootTypeMap");
        assertThat(rootTypeMap).isNotEmpty();
        assertThat(rootTypeMap.get(652)).isEqualTo(RootTypeEnum.getValueByKey((byte) 1));
    }

    @Test
    void initArrayLength_realSnapshot_populatesLengthMap() throws Exception {
        CjMemoryService service = new CjMemoryService();
        invokePrivate(service, "initArrayLength", List.of(parseRealSnapshot()));

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> arrayLengthMap = getField(service, "arrayLengthMap");
        assertThat(arrayLengthMap).hasSize(1381);
    }

    @Test
    void initHeapThreadDetail_realSnapshot_keysHeapThreadMapByStartTime() throws Exception {
        CjMemoryService service = new CjMemoryService();
        invokePrivate(service, "initHeapThreadDetail", List.of(parseRealSnapshot()));

        @SuppressWarnings("unchecked")
        Map<String, List<HeapThreadInfo>> heapThreadMap = getField(service, "heapThreadMap");
        assertThat(heapThreadMap).containsKey(START_TIME);
        assertThat(heapThreadMap.get(START_TIME)).isEmpty();
    }

    @Test
    void initTraceTreeMap_realSnapshot_emptyTree_storesEmptyChildren() throws Exception {
        CjMemoryService service = new CjMemoryService();
        invokePrivate(service, "initTraceTreeMap", List.of(parseRealSnapshot(), 1));

        // 空树: cjTraceTreeMap 记录 tid → 空 children
        assertThat(service.getStackList(1)).isEmpty();
    }

    @Test
    void getHeapThreadInfo_whenSnapshotKeyPresentButEmpty_returnsEmptyWit() {
        CjMemoryService service = new CjMemoryService();
        invokePrivateQuietly(service, "initHeapThreadDetail", List.of(parseRealSnapshot()));

        List<HeapThreadInfo> result = service.getHeapThreadInfo("session", START_TIME);

        assertThat(result).isEmpty();
    }

    // ---- parseArkHeapTimeline：真实快照 + isImport=true 跳过 savaTraceFile ----

    @Test
    void parseArkHeapTimeline_realSnapshot_importMode_processesWithoutSav() {
        CjMemoryService service = new CjMemoryService();
        RawHeapSnapshot raw = parseRealSnapshot();

        boolean isParsed = service.parseArkHeapTimeline("session", 42, raw, true);

        assertThat(isParsed).isTrue();
        // initTraceTreeMap 已执行：cjTraceTreeMap 记录了 tid → 空 children
        assertThat(service.getStackList(42)).isNotNull();
    }

    @Test
    void parseArkHeapSnapshot_whenSnapshotNull_returnsFalse() {
        CjMemoryService service = new CjMemoryService();

        boolean isSnapshotValid = service.parseArkHeapSnapshot("session", 42, null, false);

        assertThat(isSnapshotValid).isFalse();
    }

    // ---- 工具方法 ----

    private static void invokePrivate(Object target, String name, List<Object> args)
        throws ReflectiveOperationException {
        Class<?>[] types = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            types[i] = args.get(i).getClass();
            if (types[i] == Integer.class) {
                types[i] = int.class;
            } else if (types[i] == Boolean.class) {
                types[i] = boolean.class;
            } else {
                types[i] = args.get(i).getClass();
            }
        }
        Method method = CjMemoryService.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, args.toArray());
    }

    private static void invokePrivateQuietly(Object target, String name, List<Object> args) {
        try {
            invokePrivate(target, name, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        assertThat(field).isNotNull();
        field.setAccessible(true);
        return (T) field.get(target);
    }
}