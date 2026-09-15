/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link ClassPathUtil} classpath scanning logic, run against the
 * real test classpath so directory traversal, jar scanning and filter paths execute.
 *
 * @since 2026-09-09
 */
public class ClassPathUtilTest {
    /**
     * Test annotation for scanning.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface TestAnnotation {
    }

    /**
     * Second test annotation for multi-annotation matching.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface OtherAnnotation {
    }

    /**
     * Annotated holder class picked up by the scanner.
     */
    @TestAnnotation
    static class MarkerHolder {
    }

    /**
     * Helper to call the util with vararg-style annotation list.
     */
    private static List<Class<?>> scan(String resource, String pkg, Class<? extends Annotation>[] annotations) {
        return ClassPathUtil.findClassWithAnyAnnotation(Arrays.asList(annotations), resource, pkg);
    }

    @Test
    public void testFindClassWithAnyAnnotation_FindsAnnotatedClassesOnTestClasspath() {
        List<Class<?>> found = scan("com/huawei/cjprofiler", "com.huawei.cjprofiler",
            new Class[] {TestAnnotation.class, OtherAnnotation.class});
        assertNotNull(found);
        assertThat(found).contains(AnnotatedForScan.class);
    }

    @Test
    public void testFindClassWithAnyAnnotation_EmptyAnnotations() {
        List<Class<?>> result = ClassPathUtil.findClassWithAnyAnnotation(
            Collections.<Class<? extends Annotation>>emptyList(), "com/huawei/cjprofiler/utils",
            "com.huawei.cjprofiler.utils");
        assertThat(result).isEmpty();
    }

    @Test
    public void testFindClassWithAnyAnnotation_NonExistentPath() {
        List<Class<?>> result = ClassPathUtil.findClassWithAnyAnnotation(
            Arrays.<Class<? extends Annotation>>asList(TestAnnotation.class), "non/existent/path",
            "non.existent.package");
        assertThat(result).isEmpty();
    }

    @Test
    public void testFindClassWithAnyAnnotation_RealPackageWithoutMatches() {
        List<Class<?>> result = scan("java/lang", "java.lang", new Class[] {TestAnnotation.class});
        assertThat(result).isEmpty();
    }

    /**
     * Class annotated to guarantee at least one match exists in this package.
     */
    @TestAnnotation
    static class SelfReferenceHolder {
    }
}
