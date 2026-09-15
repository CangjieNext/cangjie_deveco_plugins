/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import com.huawei.deveco.insight.ohos.resourcehandler.controller.RequestMapping;

/**
 * Test-only mapping sample whose class-level path is empty, exercising the
 * "class has no path" warning branch of
 * {@code CangjieMappingRegistyManager#loadRequestMappingForClass}. It never
 * registers any mapping.
 *
 * @since 2026-09-15
 */
@RequestMapping(path = "")
public class CjMappingWarnSampleNoClassPath {
}
