/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.CjprofExtension;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 用真实快照数据的增强版 fixture 补齐低覆盖解析方法。
 *
 * <p>根因（已在真实文件 raw_cangjie_heap_snapshot72d29bf....data 上验证）：
 * 真实文件 STACKFRAME 为空、OBJECTS 中只有 ROOT_GLOBAL(tag=1)/INSTANCE(33)/数组 dump，
 * 缺少 ROOT_UNKNOWN(255)/ROOT_LOCAL(2)，也没有 samples 节 —— 导致
 * {@code readRootUnknown}/{@code readRootLocal}/{@code readStackFrameList}/{@code samples}
 * 覆盖极低甚至为 0。
 *
 * <p>此 fixture 基于真实数据构造，仅在 OBJECTS 末尾追加 tag=255/2 记录，
 * 并补齐 STACKFRAME/STACKTRACE/STARTTHREAD/samples 节，其余保持原样，
 * 保证 checkHasTag/nodeType 语义与真实快照完全一致。
 */
class CjMemoryParserExtendedSectionsTest {
    private static final String FIXTURE =
        "src/test/resources/fixtures/full-coverage-heap.json";

    @AfterEach
    void tearDown() throws Exception {
        CjMemoryParserTestSupport.resetStaticState();
    }

    @Test
    void parsesUnknownAndLocalRoots_plusFramesAndSamples() throws Exception {
        RawHeapSnapshot snapshot = CjMemoryParserTestSupport.processRawChunkFile(FIXTURE);

        assertThat(snapshot).isNotNull();
        // 常规解析仍正常：字符串表已载入、真实对象图已建成
        assertThat(snapshot.getStrings()).contains("Primitive Array");
        assertThat(snapshot.getNodes().length).isGreaterThan(8);

        CjprofExtension ext = snapshot.getCjprofExtension();
        assertThat(ext).isNotNull();

        // readRootUnknown/tag=255 追加的记录 id=700 落入 UNKNOWN(=3) 槽位
        int[][] rootData = ext.getRootDataByTypes();
        assertThat(rootData.length).isEqualTo(4);
        assertThat(rootData[3]).containsExactly(700);

        // readRootLocal/tag=2 追加的记录 id=800 落入 LOCAL(=2) 槽位
        assertThat(rootData[2]).containsExactly(800);
        // ROOT_TYPE_NUM=4，索引 0 (NOT_ROOT) 保持 null
        assertThat(rootData[0]).isNull();

        // readStackFrameList: 两条 STACKFRAME 记录被解析进 map
        Map<Integer, int[]> stackFramesMap = ext.getStackFramesMap();
        assertThat(stackFramesMap).containsKey(100);
        assertThat(stackFramesMap).containsKey(101);
        // [threadId=594, threadNameIdx, methodIdx, fileIdx, line]
        int[] frame100 = stackFramesMap.get(100);
        assertThat(frame100[0]).isEqualTo(594);
        assertThat(frame100[4]).isEqualTo(66);

        // samples: 追加的样本 [nodeId=1000, ordinal=0]
        assertThat(snapshot.getSamples()).contains(1000, 0);
    }
}