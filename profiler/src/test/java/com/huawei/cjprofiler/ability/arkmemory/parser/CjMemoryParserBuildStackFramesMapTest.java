/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.cjprofiler.ability.arkmemory.entity.cj.StackFrame;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.StackTrace;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.StartThread;
import com.huawei.cjprofiler.ability.arkmemory.entity.cj.dump.RootLocalInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 覆盖 {@link CjMemoryParser#buildStackFramesMap()} 的主路径与异常分支。
 *
 * <p>数据语义来自真实快照 demo0615.cjheapdump（经 Cjprof 解析导出的
 * cjprof-real-thread-data.json）：栈帧为 CJ_MCC_DumpCJHeapData/CjHeapDumpPage::dumpCjHeapDump
 * 等；这里以等价语义构造 STACK_TRACE_MAP/STACK_FRAME_MAP/START_THREAD_MAP/ROOT_LOCAL。
 *
 * <p>通过反射填充 static 集合并反射调用私有方法，聚焦 buildStackFramesMap 自身逻辑，
 * 规避大 JSON fixture 的构造复杂度与脆弱性。threadSummary 为实例字段，须在同一实例上读取。
 */
class CjMemoryParserBuildStackFramesMapTest {
    private static final long THREAD_ID = 594L;
    private static final int THREAD_NAME_IDX = 92;
    private static final long TRACE_IDX = 1L;

    @AfterEach
    void tearDown() throws Exception {
        CjMemoryParserTestSupport.resetStaticState();
    }

    private static void putStartThread(long idx, long stackTraceIdx, int id, int name) throws Exception {
        CjMemoryParserTestSupport.<Map<Long, StartThread>>getStaticField("START_THREAD_MAP").put(idx,
            StartThread.builder().idx(idx).id(id).stackTraceIdx(stackTraceIdx).name(name).build());
    }

    private static void putStackTrace(long idx, List<String> frames) throws Exception {
        CjMemoryParserTestSupport.<Map<Long, StackTrace>>getStaticField("STACK_TRACE_MAP").put(idx,
            StackTrace.builder().idx(idx).thread(1).frameNum(frames.size()).frames(frames).build());
    }

    private static void putStackFrame(int id, int nameIdx, int fileIdx, long line) throws Exception {
        CjMemoryParserTestSupport.<Map<Integer, StackFrame>>getStaticField("STACK_FRAME_MAP").put(id,
            StackFrame.builder().id(id).name(nameIdx).fileName(fileIdx).lineNum(line).build());
    }

    private static void putRootLocal(int id, long threadIdx, int frameId) throws Exception {
        CjMemoryParserTestSupport.<Map<Integer, RootLocalInfo>>getStaticField("ROOT_LOCAL").put(id,
            RootLocalInfo.builder().id(id).threadIdx(threadIdx).frameId(frameId).build());
    }

    /**
     * 在同一实例上调用 buildStackFramesMap，返回 map 与该实例的 threadSummary。
     *
     * @param parser 目标 CjMemoryParser 实例
     * @return frameId 到 [threadId, threadNameIdx, nameIdx, fileIdx, lineNum] 的映射
     * @throws Exception 反射调用失败时抛出
     */
    @SuppressWarnings("unchecked")
    private static Map<Integer, int[]> callBuildStackFramesMap(CjMemoryParser parser) throws Exception {
        return (Map<Integer, int[]>) CjMemoryParserTestSupport.invokeNoArg("buildStackFramesMap", parser);
    }

    private static List<Integer> readThreadSummary(CjMemoryParser parser) throws Exception {
        return CjMemoryParserTestSupport.<List<Integer>>getInstanceField(parser, "threadSummary");
    }

    private static void mockNodeIndex(int rootLocalId, Integer index) {
        MockedStatic<CjBuildTreeService> mocked = Mockito.mockStatic(CjBuildTreeService.class);
        CjBuildTreeService svc = Mockito.mock(CjBuildTreeService.class);
        mocked.when(CjBuildTreeService::getInstance).thenReturn(svc);
        Mockito.when(svc.getNodeIndex(rootLocalId)).thenReturn(index);
    }

    // ---------- 第一个循环：遍历 START_THREAD_MAP ----------

    @Test
    void buildsFrameEntriesForValidStartThreadAndTrace() throws Exception {
        // 真实语义：CJ_MCC_DumpCJHeapData 帧 id=100, line=66
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("100"));
        putStackFrame(100, 11, 22, 66);

        CjMemoryParser parser = new CjMemoryParser();
        Map<Integer, int[]> map = callBuildStackFramesMap(parser);

        assertThat(map).containsKey(100);
        assertThat(map.get(100)).containsExactly((int) THREAD_ID, THREAD_NAME_IDX, 11, 22, 66);
        assertThat(readThreadSummary(parser)).isEmpty();
    }

    @Test
    void skipsStartThreadWhenTraceNotInMap() throws Exception {
        putStartThread(1L, 999L, (int) THREAD_ID, THREAD_NAME_IDX); // trace 999 不存在
        putStackFrame(100, 11, 22, 66);

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map).isEmpty();
    }

    @Test
    void skipsStartThreadWhenTraceFramesEmpty() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, List.of()); // frames 为空

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map).isEmpty();
    }

    @Test
    void skipsEmptyFrameId() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("", "100")); // 第一个 frameId 为空串
        putStackFrame(100, 11, 22, 66);

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map).containsKey(100);
    }

    @Test
    void skipsNonNumericFrameId() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("abc", "100")); // 非数字
        putStackFrame(100, 11, 22, 66);

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map).containsKey(100);
    }

    @Test
    void skipsFrameIdNotInStackFrameMap() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("200", "100")); // 200 无对应 StackFrame
        putStackFrame(100, 11, 22, 66);

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map).containsKey(100);
        assertThat(map).doesNotContainKey(200);
    }

    @Test
    void handlesMultipleFramesInOneTrace() throws Exception {
        // 真实语义：dumpCjHeapDump(line=66) -> runtime_memoryInfo.cj(line=0)
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("100", "101"));
        putStackFrame(100, 11, 22, 66);
        putStackFrame(101, 33, 44, 0);

        Map<Integer, int[]> map = callBuildStackFramesMap(new CjMemoryParser());

        assertThat(map.get(100)).containsExactly((int) THREAD_ID, THREAD_NAME_IDX, 11, 22, 66);
        assertThat(map.get(101)).containsExactly((int) THREAD_ID, THREAD_NAME_IDX, 33, 44, 0);
    }

    // ---------- 第二个循环：遍历 ROOT_LOCAL ----------

    @Test
    void appendsThreadSummaryForRootLocalWithExistingFrame() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putStackTrace(TRACE_IDX, Arrays.asList("300")); // 让第一个循环也产出 frame 300
        putStackFrame(300, 11, 22, 7);
        putRootLocal(777, 1L, 300);

        CjMemoryParser parser = new CjMemoryParser();
        try (MockedStatic<CjBuildTreeService> mocked = Mockito.mockStatic(CjBuildTreeService.class)) {
            CjBuildTreeService svc = Mockito.mock(CjBuildTreeService.class);
            mocked.when(CjBuildTreeService::getInstance).thenReturn(svc);
            Mockito.when(svc.getNodeIndex(777)).thenReturn(42);

            Map<Integer, int[]> map = callBuildStackFramesMap(parser);

            assertThat(map.get(300)).containsExactly((int) THREAD_ID, THREAD_NAME_IDX, 11, 22, 7);
            assertThat(readThreadSummary(parser)).containsExactly(777, 300, 42);
        }
    }

    @Test
    void skipsRootLocalWhenNodeIndexMissing() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putRootLocal(888, 1L, 301);

        CjMemoryParser parser = new CjMemoryParser();
        try (MockedStatic<CjBuildTreeService> mocked = Mockito.mockStatic(CjBuildTreeService.class)) {
            CjBuildTreeService svc = Mockito.mock(CjBuildTreeService.class);
            mocked.when(CjBuildTreeService::getInstance).thenReturn(svc);
            Mockito.when(svc.getNodeIndex(888)).thenReturn(null); // nodeIndex 缺失

            Map<Integer, int[]> map = callBuildStackFramesMap(parser);

            assertThat(map).isEmpty();
            assertThat(readThreadSummary(parser)).isEmpty();
        }
    }

    @Test
    void skipsRootLocalWhenThreadMissing() throws Exception {
        // 不注册 START_THREAD_MAP，但 ROOT_LOCAL 指向 threadIdx=1
        putRootLocal(999, 1L, 302);

        CjMemoryParser parser = new CjMemoryParser();
        try (MockedStatic<CjBuildTreeService> mocked = Mockito.mockStatic(CjBuildTreeService.class)) {
            CjBuildTreeService svc = Mockito.mock(CjBuildTreeService.class);
            mocked.when(CjBuildTreeService::getInstance).thenReturn(svc);
            Mockito.when(svc.getNodeIndex(999)).thenReturn(1);

            Map<Integer, int[]> map = callBuildStackFramesMap(parser);

            assertThat(map).isEmpty();
            assertThat(readThreadSummary(parser)).isEmpty();
        }
    }

    @Test
    void putsDummyEntryWhenRootLocalFrameMissing() throws Exception {
        putStartThread(1L, TRACE_IDX, (int) THREAD_ID, THREAD_NAME_IDX);
        putRootLocal(1234, 1L, 404); // frame 404 不存在

        CjMemoryParser parser = new CjMemoryParser();
        try (MockedStatic<CjBuildTreeService> mocked = Mockito.mockStatic(CjBuildTreeService.class)) {
            CjBuildTreeService svc = Mockito.mock(CjBuildTreeService.class);
            mocked.when(CjBuildTreeService::getInstance).thenReturn(svc);
            Mockito.when(svc.getNodeIndex(1234)).thenReturn(55);

            Map<Integer, int[]> map = callBuildStackFramesMap(parser);

            // 框架缺失 -> 塞入 [id, nameIdx, -1, -1, -1] 占位 + threadSummary
            assertThat(map.get(404)).containsExactly((int) THREAD_ID, THREAD_NAME_IDX, -1, -1, -1);
            assertThat(readThreadSummary(parser)).containsExactly(1234, 404, 55);
        }
    }
}