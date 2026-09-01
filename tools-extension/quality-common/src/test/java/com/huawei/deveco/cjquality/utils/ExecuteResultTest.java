/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for CangjieCompileArg
 *
 * @since 2024-02-20
 */
public class ExecuteResultTest {
    private ExecuteResult result;

    private final String executeOut = "testOut";

    private final int exitCode = 1;

    @Before
    public void setUp() {
        this.result = new ExecuteResult(exitCode, executeOut);
    }

    @Test
    public void getExitCode() {
        Assert.assertEquals(this.result.exitCode(), exitCode);
    }

    @Test
    public void getExecuteOut() {
        Assert.assertEquals(this.result.executeOut(), executeOut);
    }
}