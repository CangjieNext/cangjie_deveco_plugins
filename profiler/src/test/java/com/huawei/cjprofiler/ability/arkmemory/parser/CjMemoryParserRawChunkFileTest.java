/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.arkmemory.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * 用 DevEco 导出的真实 raw_cangjie_heap_snapshot*.data 文件验证两阶段解析。
 *
 * <p>外层文件内容是按帧追加的 JSON 对象流：
 * {@code {"method":"HeapProfiler.addHeapSnapshotChunk","params":{"chunk":"<json片段>"}}}
 * 只有把所有 chunk 字符串按顺序拼接，才构成一份完整可解析的 heap snapshot JSON
 * （顶层键：HEADER / STRING / CLASSLOAD / STRUCTCLASSLOAD / STACKFRAME / STACKTRACE /
 * STARTTHREAD / CLASS / STRUCTCLASS / OBJECTS）。
 *
 * <p>说明：手头两个 raw 文件（72d29bf3... 与 3564b3d5...）对 CjMemoryParser 的
 * JaCoCo 行覆盖完全一致（均 85/150 = 58%，diff 逐字节相同），因此测试只保留一个文件即可。
 *
 * <p>fixture 文件已纳入版本管理，存放于 {@code src/test/resources/fixtures/raw-chunk-file.data}，
 * 通过 classpath 定位，避免硬编码本地绝对路径。
 */
class CjMemoryParserRawChunkFileTest {
    private static final String RAW_FILE;

    static {
        try {
            RAW_FILE = Paths.get(CjMemoryParserRawChunkFileTest.class
                .getResource("/fixtures/raw-chunk-file.data").toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(
                "Cannot resolve fixture: /fixtures/raw-chunk-file.data — " + e.getMessage());
        }
    }

    @Test
    void processFile_parsesRawChunkFile() throws Exception {
        RawHeapSnapshot snapshot = CjMemoryParserTestSupport.processRawChunkFile(RAW_FILE);
        assertParsedSnapshot(snapshot);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 反射调用 readRawCjHeapSnapshot 绕过了 parseJson 的 finally 清理，
        // 必须在此恢复 CjMemoryParser 的 static 解析集合，避免污染其他测试类。
        CjMemoryParserTestSupport.resetStaticState();
    }

    private static void assertParsedSnapshot(RawHeapSnapshot snapshot) {
        assertThat(snapshot).isNotNull();
        // 字符串表已载入（始终含 Primitive Array 占位项，且远不止这一条）
        assertThat(snapshot.getStrings()).contains("Primitive Array");
        assertThat(snapshot.getStrings().size()).isGreaterThan(1);
        // 真实对象图已建成：节点数远超 1 个虚拟根节点（8 = 1 节点 * 8 字段）
        assertThat(snapshot.getNodes().length).isGreaterThan(8);
        // 起始时间来源于 HEADER 帧
        assertThat(snapshot.getStartTime()).isPositive();
    }
}