/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ClassPathUtilTest
 */
public class ClassPathUtilTest {

    @Test
    public void testFindClassWithAnyAnnotation_EmptyAnnotations() {
        List<Class<?>> result = ClassPathUtil.findClassWithAnyAnnotation(
            Collections.<Class<? extends Annotation>>emptyList(), "some/resource", "some.package");
        assertNotNull(result);
    }

    @Test
    public void testFindClassWithAnyAnnotation_NullAnnotations() {
        List<Class<?>> result = ClassPathUtil.findClassWithAnyAnnotation(
            null, "some/resource", "some.package");
        assertNotNull(result);
    }

    @Test
    public void testFindClassWithAnyAnnotation_NonExistentPath() {
        List<Class<?>> result = ClassPathUtil.findClassWithAnyAnnotation(
            Arrays.<Class<? extends Annotation>>asList(TestAnnotation.class), "non/existent/path", "non.existent.package");
        assertNotNull(result);
    }

    /**
     * Test annotation
     */
    @interface TestAnnotation {
    }
}
