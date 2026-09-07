/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.resourcehandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link CangjieMappingRegistyManager} singleton and empty-state behavior.
 */
class CangjieMappingRegistyManagerTest {
    @Test
    void getInstance_returnsSingleton() {
        assertThat(CangjieMappingRegistyManager.getInstance())
            .isSameAs(CangjieMappingRegistyManager.getInstance());
    }

    @Test
    void getHandlerMethod_whenNotLoaded_returnsEmpty() {
        CangjieMappingRegistyManager manager = CangjieMappingRegistyManager.getInstance();
        Optional<?> result = manager.getHandlerMethod(List.of("unknown", "path"));
        assertThat(result).isEmpty();
    }

    @Test
    void getHandlerMethod_withEmptyPathList_returnsEmpty() {
        CangjieMappingRegistyManager manager = CangjieMappingRegistyManager.getInstance();
        Optional<?> result = manager.getHandlerMethod(List.of());
        assertThat(result).isEmpty();
    }
}
