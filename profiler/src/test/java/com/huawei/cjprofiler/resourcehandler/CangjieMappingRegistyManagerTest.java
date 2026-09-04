package com.huawei.cjprofiler.resourcehandler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
