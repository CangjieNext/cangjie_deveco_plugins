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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Integration test: feed the REAL cjheapsnapshot file (final/exported format, as produced by
 * DevEco Studio Snapshot) into {@link CjMemoryParser#parseJson}.
 *
 * <p>Note: the file's top-level keys (snapshot/nodes/edges/strings/...) are the EXPORT format,
 * so the record-level readers are NOT hit. The resulting RawHeapSnapshot is an EMPTY shell
 * (only the virtual root node). This test locks in the contract: unknown-but-valid JSON must
 * be tolerated without exception.
 */
class CjMemoryParserRealFileProbeTest {
    private static final Path FI = Paths.get(
        "src/test/resources/fixtures/real-cangjie-heap-snapshot.cjheapsnapshot");

    @Test
    void parseRealFile_doesNotThrowAndReturnsShell() throws Exception {
        RawHeapSnapshot result = CjMemoryParserTestSupport.parseThroughPipe(FI);

        // Shell-only output: virtual root node exists, real data (strings/nodes/edges) not ingested
        assertThat(result).isNotNull();
        assertThat(result.getStrings()).containsExactly("Primitive Array");
        assertThat(result.getNodes()).hasSize(8); // 1 virtual root node * 8 node fields
        assertThat(result.getEdges()).isEmpty();
        assertThat(result.getStartTime()).isZero();
    }
}