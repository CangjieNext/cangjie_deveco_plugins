/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.cjprof.jni.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotConstructorAggregate;
import com.huawei.deveco.insight.ohos.ability.arkmemory.instancefilters.JsHeapSnapshot;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests constructor behavior of {@link JsHeapSnapshotConstructorDiffNode}.
 *
 * @since 2026-09-08
 */
class JsHeapSnapshotConstructorDiffNodeTest {
    @Test
    void constructor_setsAllFieldsIncludingJsTargetHeapSnapshotId() {
        JsHeapSnapshotConstructorAggregate aggregate = mock(JsHeapSnapshotConstructorAggregate.class);
        Map<String, JsHeapSnapshotConstructorAggregate> aggregates = new HashMap<>();
        aggregates.put("Foo", aggregate);

        JsHeapSnapshot.Diff diff = mock(JsHeapSnapshot.Diff.class);

        JsHeapSnapshot snapshot = mock(JsHeapSnapshot.class);
        when(snapshot.aggregates("allObjects")).thenAnswer(invocation -> aggregates);
        JsHeapSnapshot baseSnapshot = mock(JsHeapSnapshot.class);
        when(baseSnapshot.aggregates("allObjects")).thenAnswer(invocation -> aggregates);

        JsHeapSnapshotConstructorDiffNode node =
            new JsHeapSnapshotConstructorDiffNode("Foo", diff, snapshot, baseSnapshot, "base-id", "target-id");

        assertEquals("Foo", node.getClassName());
        assertEquals("base-id", node.getJsBaseHeapSnapshotId());
        assertEquals("target-id", node.getJsTargetHeapSnapshotId());
    }
}
