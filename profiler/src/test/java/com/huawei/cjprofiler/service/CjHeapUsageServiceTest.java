/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * CjHeapUsageServiceTest - 基础测试
 *
 * 注意：
 * 当前测试环境缺少完整的数据库配置和测试数据源，
 * 因此只进行基础的功能验证。完整的业务逻辑测试需要：
 * 1. 建立测试数据库环境
 * 2. 配置MyBatis测试配置
 * 3. 准备测试数据
 *
 * @since 2026-06-04
 */
public class CjHeapUsageServiceTest {

    @Test
    public void testGetInstanceReturnsSingleton() {
        // 测试单例模式 - 这是唯一能够可靠测试的部分
        CjHeapUsageService instance1 = CjHeapUsageService.getInstance();
        CjHeapUsageService instance2 = CjHeapUsageService.getInstance();
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertEquals(instance1, instance2);
    }

    @Test
    public void testServiceNotNull() {
        // 验证服务可以被实例化
        CjHeapUsageService service = CjHeapUsageService.getInstance();
        assertNotNull(service);
    }
}