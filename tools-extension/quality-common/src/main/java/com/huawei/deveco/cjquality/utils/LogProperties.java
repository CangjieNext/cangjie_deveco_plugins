/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import org.jetbrains.annotations.PropertyKey;

/**
 * log properties
 *
 * @since 2022-1-1
 */
public final class LogProperties {
    private static final String DEBUG_ENABLE_KEY = "debug_enable";
    private static final String INFO_ENABLE_KEY = "info_enable";
    private static final String WARN_ENABLE_KEY = "warn_enable";
    private static final String ERROR_ENABLE_KEY = "error_enable";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String INFO = "info";
    private static final String WARN = "warn";
    private static final String ERROR = "error";
    private static final String FATAL = "fatal";
    private static final String BUNDLE = "messages.CodeQuality";

    /**
     * judge that whether log level is not less than debug
     *
     * @return if log level is not less than debug return true, otherwise return false
     */
    public static boolean notLessThanDebug() {
        return notLessThan(LogLevel.DEBUG);
    }

    /**
     * judge that whether log level is not less than info
     *
     * @return if log level is not less than info return true, otherwise return false
     */
    public static boolean notLessThanInfo() {
        return notLessThan(LogLevel.INFO);
    }

    /**
     * judge that whether log level is not less than warn
     *
     * @return if log level is not less than warn return true, otherwise return false
     */
    public static boolean notLessThanWarn() {
        return notLessThan(LogLevel.WARN);
    }

    /**
     * judge that whether log level is not less than error
     *
     * @return if log level is not less than error return true, otherwise return false
     */
    public static boolean notLessThanError() {
        return notLessThan(LogLevel.ERROR);
    }

    /**
     * judge that whether debug switch from log properties file is true
     *
     * @return if debug switch is true return true, otherwise return false
     */
    public static boolean isDebugEnable() {
        return LogProperties.isLogLevelEnabled(DEBUG_ENABLE_KEY);
    }

    /**
     * judge that whether info switch from log properties file is true
     *
     * @return if info switch is true return true, otherwise return false
     */
    public static boolean isInfoEnable() {
        return LogProperties.isLogLevelEnabled(INFO_ENABLE_KEY);
    }

    /**
     * judge that whether warn switch from log properties file is true
     *
     * @return if warn switch is true return true, otherwise return false
     */
    public static boolean isWarnEnable() {
        return LogProperties.isLogLevelEnabled(WARN_ENABLE_KEY);
    }

    /**
     * judge that whether error switch from log properties file is true
     *
     * @return if error switch is true return true, otherwise return false
     */
    public static boolean isErrorEnable() {
        return LogProperties.isLogLevelEnabled(ERROR_ENABLE_KEY);
    }

    /**
     * message
     *
     * @param key bundle key
     * @param params params
     * @return value
     */
    private static String message(@PropertyKey(resourceBundle = BUNDLE) String key, String... params) {
        return CodeQualityBundle.message(key, params);
    }

    /**
     * get log level
     *
     * @return log level
     */
    public static LogLevel getLogLevel() {
        String logLevelStr = message("log_level", WARN);
        return switch (logLevelStr) {
            case INFO -> LogLevel.INFO;
            case WARN -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
            case FATAL -> LogLevel.FATAL;
            default -> LogLevel.DEBUG;
        };
    }

    /**
     * log level is not less than the specified level
     *
     * @param level log level
     * @return true/false
     */
    private static boolean notLessThan(LogLevel level) {
        return getLogLevel().getLevel() >= level.getLevel();
    }

    /**
     * is logger level enabled
     *
     * @param key log level
     * @return boolean
     */
    private static boolean isLogLevelEnabled(String key) {
        String enableStr = message(key, FALSE);
        return TRUE.equals(enableStr);
    }
}