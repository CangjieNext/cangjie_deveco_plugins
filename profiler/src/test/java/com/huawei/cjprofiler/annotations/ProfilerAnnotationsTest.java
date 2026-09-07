/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.annotations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Unit tests for profiler annotations metadata.
 *
 * @since 2026-08-14
 */
class ProfilerAnnotationsTest {
    @Controller
    @RequestMapping("/threads")
    private static class SampleController {
        @RequestMapping("/metadata")
        void metadata() {
        }
    }

    @Test
    void controller_isRuntimeTypeAnnotationAndVisibleByReflection() {
        Retention retention = Controller.class.getAnnotation(Retention.class);
        Target target = Controller.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(Arrays.asList(target.value())).containsExactly(ElementType.TYPE);
        assertThat(SampleController.class.isAnnotationPresent(Controller.class)).isTrue();
    }

    @Test
    void requestMapping_supportsTypeAndMethodAndPreservesValues() throws NoSuchMethodException {
        Target target = RequestMapping.class.getAnnotation(Target.class);
        Method method = SampleController.class.getDeclaredMethod("metadata");

        assertThat(Arrays.asList(target.value())).containsExactly(ElementType.METHOD, ElementType.TYPE);
        assertThat(SampleController.class.getAnnotation(RequestMapping.class).value()).isEqualTo("/threads");
        assertThat(method.getAnnotation(RequestMapping.class).value()).isEqualTo("/metadata");
    }

    @Test
    void requestMapping_valueDefaultsToEmptyString() throws NoSuchMethodException {
        assertThat(RequestMapping.class.getDeclaredMethod("value").getDefaultValue()).isEqualTo("");
        assertThat(RequestMapping.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
}
