/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.cjprofiler.controller.ControllerScanSample;
import com.huawei.cjprofiler.controller.ScannerMarker;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        // Null array must be ignored silently
        assertDoesNotThrow(() -> ClassUtil.closeAllIo((Closeable[]) null));
    }

    @Test
    public void testCloseAllIo_WithNullElement() {
        // Null element inside varargs must be skipped, not NPE
        Closeable nullCloseable = null;
        assertDoesNotThrow(() -> ClassUtil.closeAllIo(nullCloseable));
    }

    @Test
    public void testCloseAllIo_WithValidCloseable() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Closeable closeable = () -> closed.set(true);
        ClassUtil.closeAllIo(closeable);
        assertTrue(closed.get(), "close() must be invoked on the closeable");
    }

    @Test
    public void testCloseAllIo_WithIOException() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Closeable throwing = () -> {
            closed.set(true);
            throw new IOException("test exception");
        };
        AtomicBoolean closedAfter = new AtomicBoolean(false);
        Closeable after = () -> closedAfter.set(true);
        // IOException on one closeable must be swallowed and NOT prevent closing the rest
        ClassUtil.closeAllIo(throwing, after);
        assertTrue(closed.get(), "close() must be invoked even though it throws IOException");
        assertTrue(closedAfter.get(), "following closeable must still be closed after IOException");
    }

    @Test
    public void testCloseAllIo_MultipleCloseables() {
        AtomicBoolean closed1 = new AtomicBoolean(false);
        AtomicBoolean closed2 = new AtomicBoolean(false);
        ClassUtil.closeAllIo(() -> closed1.set(true), () -> closed2.set(true));
        assertTrue(closed1.get(), "first closeable must be closed");
        assertTrue(closed2.get(), "second closeable must be closed");
    }

    @Test
    public void testCloseAllIo_MixedNullAndValid() {
        AtomicBoolean closed = new AtomicBoolean(false);
        ClassUtil.closeAllIo(null, (Closeable) () -> closed.set(true), null);
        assertTrue(closed.get(), "valid closeable between nulls must be closed");
    }

    @Test
    public void testFindClassesWithAnnotation_FindsAnnotatedSample() {
        // The controller test package makes CONTROLLER_ROOT_PATH resolvable, so the
        // full classpath scan path must locate the annotated sample class.
        List<Class<?>> classes = ClassUtil.findClassesWithAnnotation(ScannerMarker.class);
        assertNotNull(classes);
        assertTrue(classes.contains(ControllerScanSample.class),
            "scan must find @ScannerMarker-class in com/huawei/cjprofiler/controller");
        assertTrue(classes.stream().allMatch(clz -> clz.isAnnotationPresent(ScannerMarker.class)),
            "every returned class must carry the annotation");
    }

    @Test
    public void testFindClassesWithAnnotation_NoMatchReturnsEmpty() {
        List<Class<?>> result = ClassUtil.findClassesWithAnnotation(Deprecated.class);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "no class in the package should carry @Deprecated");
    }

    /**
     * Test annotation
     */
    @interface TestAnnotation {
    }
}
