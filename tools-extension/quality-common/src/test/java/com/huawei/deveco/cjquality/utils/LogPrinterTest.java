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
 * Test for LogPrinter
 *
 * @since 2024-02-20
 */
public class LogPrinterTest {
    private static final String MESSAGE = "LogPrinterTestMessage";

    private LogPrinter stringLogger;

    private LogPrinter classLogger;

    @Before
    public void setUp() {
        stringLogger = LogPrinter.createLogger("StringLogger");
        classLogger = LogPrinter.createLogger(LogPrinterTest.class);
    }

    @Test
    public void debug() {
        LogProperties.notLessThanDebug();
        stringLogger.debug(MESSAGE);
        stringLogger.debug(MESSAGE, MESSAGE);
        stringLogger.debug(MESSAGE, new Throwable());
        classLogger.debug(MESSAGE);
        classLogger.debug(MESSAGE, MESSAGE);
        classLogger.debug(MESSAGE, new Throwable());
    }

    @Test
    public void info() {
        LogProperties.notLessThanInfo();
        stringLogger.info(MESSAGE);
        stringLogger.info(MESSAGE, MESSAGE);
        stringLogger.info(MESSAGE, new Throwable());
        classLogger.info(MESSAGE);
        classLogger.info(MESSAGE, MESSAGE);
        classLogger.info(MESSAGE, new Throwable());
    }

    @Test
    public void warn() {
        LogProperties.notLessThanWarn();
        stringLogger.warn(MESSAGE);
        stringLogger.warn(MESSAGE, MESSAGE);
        stringLogger.warn(MESSAGE, new Throwable());
        classLogger.warn(MESSAGE);
        classLogger.warn(MESSAGE, MESSAGE);
        classLogger.warn(MESSAGE, new Throwable());
    }

    @Test(expected = Throwable.class)
    public void error1() {
        LogProperties.notLessThanError();
        stringLogger.error(MESSAGE);
        stringLogger.error(MESSAGE, MESSAGE, MESSAGE);
        stringLogger.error(MESSAGE, new Throwable());
        classLogger.error(MESSAGE);
        classLogger.error(MESSAGE, MESSAGE);
        classLogger.error(MESSAGE, new Throwable());
    }

    @Test(expected = Throwable.class)
    public void error2() {
        LogProperties.notLessThanError();
        stringLogger.error(MESSAGE, MESSAGE);
    }

    @Test(expected = Throwable.class)
    public void error3() {
        LogProperties.notLessThanError();
        stringLogger.error(MESSAGE, new Throwable());
    }

    @Test(expected = Throwable.class)
    public void error4() {
        LogProperties.notLessThanError();
        classLogger.error(MESSAGE);
    }

    @Test(expected = Throwable.class)
    public void error5() {
        LogProperties.notLessThanError();
        classLogger.error(MESSAGE, MESSAGE);
    }

    @Test(expected = Throwable.class)
    public void error6() {
        LogProperties.notLessThanError();
        classLogger.error(MESSAGE, new Throwable());
    }

    @Test
    public void toStringTest() {
        Assert.assertNotNull(stringLogger.toString());
        Assert.assertNotNull(classLogger.toString());
    }
}