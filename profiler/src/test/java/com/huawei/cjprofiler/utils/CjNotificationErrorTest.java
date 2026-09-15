/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.deveco.insight.ohos.common.NotificationError;

import org.junit.jupiter.api.Test;

/**
 * Tests the pre-defined {@link CjNotificationError} constant.
 *
 * <p>The {@link NotificationError} base class keeps {@code errorCode} and
 * {@code errorDescriptionKey} as private fields without dedicated getters, so this test only
 * verifies the constant is non-null, has the correct concrete type, and exposes inherited
 * {@code getContent()} behavior (both with and without an i18n bundle name).</p>
 *
 * @since 2026-08-14
 */
class CjNotificationErrorTest {
    @Test
    void cjImportExceedsQuantityLimitError_constantIsWellFormed() {
        NotificationError error = CjNotificationError.CJ_IMPORT_EXCEEDS_QUANTITY_LIMIT_ERROR;

        assertTrue(error instanceof CjNotificationError,
                "constant must be of the concrete CjNotificationError type");
        assertNotNull(error.getContent(),
                "getContent() without bundle must return a non-null content");
        assertNotNull(error.getContent("any.bundle"),
                "getContent(bundle) must return a non-null content");
    }

    @Test
    void getContent_bundleVariantFallsBackWhenBundleUnknown() {
        NotificationError error = CjNotificationError.CJ_IMPORT_EXCEEDS_QUANTITY_LIMIT_ERROR;

        assertEquals(error.getContent(), error.getContent("unknown.definitely.missing.bundle"),
                "unknown bundle should fall back to the default content");
    }
}
