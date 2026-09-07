/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.cjprof.jni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests getter/setter and builder behavior of {@link HeapSnapshot}.
 *
 * @since 2026-08-14
 */
class HeapSnapshotTest {
    @Test
    void allArgConstructor_setsAllFields() {
        HeapSnapshot snapshot = new HeapSnapshot(1L, 1024L, "/tmp/snapshot.heapsnapshot");

        assertEquals(1L, snapshot.getId());
        assertEquals(1024L, snapshot.getFileSize());
        assertEquals("/tmp/snapshot.heapsnapshot", snapshot.getFilePath());
    }

    @Test
    void builder_setsAllFields() {
        HeapSnapshot snapshot = HeapSnapshot.builder()
            .id(2L)
            .fileSize(2048L)
            .filePath("/data/heap.hprof")
            .build();

        assertEquals(2L, snapshot.getId());
        assertEquals(2048L, snapshot.getFileSize());
        assertEquals("/data/heap.hprof", snapshot.getFilePath());
    }

    @Test
    void setters_updateFields() {
        HeapSnapshot snapshot = new HeapSnapshot(0L, 0L, "");

        snapshot.setId(10L);
        snapshot.setFileSize(4096L);
        snapshot.setFilePath("/new/path");

        assertEquals(10L, snapshot.getId());
        assertEquals(4096L, snapshot.getFileSize());
        assertEquals("/new/path", snapshot.getFilePath());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        HeapSnapshot a = HeapSnapshot.builder().id(1L).fileSize(100L).filePath("a").build();
        HeapSnapshot b = HeapSnapshot.builder().id(1L).fileSize(100L).filePath("a").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsFilePath() {
        HeapSnapshot snapshot = HeapSnapshot.builder().filePath("/x/y").build();
        assertEquals(true, snapshot.toString().contains("/x/y"));
    }
}
