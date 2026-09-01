/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for LogLevel
 *
 * @since 2024-02-20
 */
public class LogLevelTest {
    @Test
    public void getLevel() {
        Assert.assertEquals(0, LogLevel.FATAL.getLevel());
        Assert.assertEquals(1, LogLevel.ERROR.getLevel());
        Assert.assertEquals(2, LogLevel.WARN.getLevel());
        Assert.assertEquals(3, LogLevel.INFO.getLevel());
        Assert.assertEquals(4, LogLevel.DEBUG.getLevel());
    }
}