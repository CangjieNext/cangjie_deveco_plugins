/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests constant values of {@link Constants}.
 *
 * @since 2026-08-14
 */
class ConstantsTest {
    @Test
    void packageRootNameConstant_isExpectedValue() {
        assertEquals("com.huawei.cjprofiler", Constants.PACKAGE_ROOT_NAME);
    }

    @Test
    void debugModeConstant_isTrue() {
        assertTrue(Constants.DEBUG_MODE);
    }

    @Test
    void controllerRootPathConstant_isExpectedValue() {
        assertEquals("com/huawei/cjprofiler/controller", Constants.CONTROLLER_ROOT_PATH);
    }

    @Test
    void jarUrlSeparatorConstant_isExpectedValue() {
        assertEquals("!/", Constants.JAR_URL_SEPARATOR);
    }

    @Test
    void cjprofFileTypeConstant_isExpectedValue() {
        assertEquals("cjheapdump", Constants.CJPROF_FILE_TYPE);
    }

    @Test
    void urlSchemeNameConstant_isExpectedValue() {
        assertEquals("http", Constants.URL.SCHEME_NAME);
    }

    @Test
    void urlPrefixConstant_isHttpScheme() {
        assertEquals("http://", Constants.URL.URL_PREFIX);
    }

    @Test
    void urlDomainNameConstant_isExpectedValue() {
        assertEquals("localhost", Constants.URL.DOMAIN_NAME);
    }

    @Test
    void cangjieProfilerPrefixConstant_isExpectedValue() {
        assertEquals("cjprofiler", Constants.URL.CANGJIE_PROFILER_PREFIX);
    }

    @Test
    void forceLoadClass_initializesConstantsAndUrlClass() {
        // Instantiate to cover class declaration lines (init methods)
        assertNotNull(new Constants());
        assertNotNull(new Constants.URL());
    }
}