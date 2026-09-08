/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.model.dto.response.cjthread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests accessor of {@link CjThreadStateMetadata} record.
 *
 * @since 2026-08-14
 */
class CjThreadStateMetadataTest {
    @Test
    void accessor_returnsMetadataList() {
        List<String> metadata = new ArrayList<>();

        CjThreadStateMetadata data = new CjThreadStateMetadata(metadata);

        assertSame(metadata, data.metadataList());
    }

    @Test
    void equalsAndHashCode_areConsistent() {
        List<String> metadata = new ArrayList<>();
        CjThreadStateMetadata a = new CjThreadStateMetadata(metadata);
        CjThreadStateMetadata b = new CjThreadStateMetadata(metadata);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsClassName() {
        CjThreadStateMetadata data = new CjThreadStateMetadata(new ArrayList<>());
        assertEquals(true, data.toString().contains("CjThreadStateMetadata"));
    }
}
