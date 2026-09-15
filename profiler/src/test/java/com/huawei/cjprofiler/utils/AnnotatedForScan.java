/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

/**
 * Top-level class annotated for {@link ClassPathUtil} scanner tests; Guava's
 * recursive scan only reports top-level classes, so the marker must live here
 * (not nested) to be discoverable.
 *
 * @since 2026-09-09
 */
@ClassPathUtilTest.TestAnnotation
public class AnnotatedForScan {
}
