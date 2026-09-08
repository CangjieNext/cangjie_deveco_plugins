/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * ClassUtilTest
 */
public class ClassUtilTest {

    @Test
    public void testFindClassesWithAnnotation() {
        List<Class<?>> classes = ClassUtil.findClassesWithAnnotation(TestAnnotation.class);
        assertNotNull(classes);
    }

    @Test
    public void testCloseAllIo_NullArray() {
        ClassUtil.closeAllIo((Closeable[]) null);
    }

    @Test
    public void testCloseAllIo_WithNullElement() {
        ClassUtil.closeAllIo((Closeable) null);
    }

    @Test
    public void testCloseAllIo_WithValidCloseable() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ClassUtil.closeAllIo(in);
    }

    @Test
    public void testCloseAllIo_WithIOException() {
        Closeable closeable = () -> {
            throw new IOException("test exception");
        };
        ClassUtil.closeAllIo(closeable);
    }

    @Test
    public void testCloseAllIo_MultipleCloseables() {
        ByteArrayInputStream in1 = new ByteArrayInputStream(new byte[0]);
        ByteArrayInputStream in2 = new ByteArrayInputStream(new byte[0]);
        ClassUtil.closeAllIo(in1, in2);
    }

    @Test
    public void testCloseAllIo_MixedNullAndValid() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ClassUtil.closeAllIo(null, in, null);
    }

    /**
     * Test annotation
     */
    @interface TestAnnotation {
    }
}
