/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * log printer
 *
 * @since 2022-1-1
 */
public final class LogPrinter {
    private static final String BIG_PARENTHESES = "{}";

    private final Logger intellijLogger;

    private LogPrinter(String name) {
        intellijLogger = Logger.getInstance(name);
    }

    /**
     * create logger printer
     *
     * @param name logger name
     * @return log printer
     */
    public static LogPrinter createLogger(String name) {
        return new LogPrinter(name);
    }

    /**
     * create logger printer
     *
     * @param clazz logger class
     * @return log printer
     */
    public static LogPrinter createLogger(Class<?> clazz) {
        return new LogPrinter(clazz.getSimpleName());
    }

    /**
     * debug
     *
     * @param message message
     */
    public void debug(String message) {
        if (LogProperties.isDebugEnable() && LogProperties.notLessThanDebug()) {
            intellijLogger.debug(message);
        }
    }

    /**
     * debug
     *
     * @param message message
     * @param throwable stack frame
     */
    public void debug(String message, Throwable throwable) {
        if (LogProperties.isDebugEnable() && LogProperties.notLessThanDebug()) {
            intellijLogger.debug(message, throwable);
        }
    }

    /**
     * debug
     *
     * @param message message formatter
     * @param args arguments
     */
    public void debug(String message, String... args) {
        if (LogProperties.isDebugEnable() && LogProperties.notLessThanDebug()) {
            intellijLogger.debug(formatMessage(message, args));
        }
    }

    /**
     * info
     *
     * @param message message
     */
    public void info(String message) {
        if (LogProperties.isInfoEnable() && LogProperties.notLessThanInfo()) {
            intellijLogger.info(message);
        }
    }

    /**
     * info
     *
     * @param message message
     * @param throwable stack frame
     */
    public void info(String message, Throwable throwable) {
        if (LogProperties.isInfoEnable() && LogProperties.notLessThanInfo()) {
            intellijLogger.info(message, throwable);
        }
    }

    /**
     * info
     *
     * @param message message formatter
     * @param args arguments
     */
    public void info(String message, String... args) {
        if (LogProperties.isInfoEnable() && LogProperties.notLessThanInfo()) {
            intellijLogger.info(formatMessage(message, args));
        }
    }

    /**
     * warn
     *
     * @param message message
     */
    public void warn(String message) {
        if (LogProperties.isWarnEnable() && LogProperties.notLessThanWarn()) {
            intellijLogger.warn(message);
        }
    }

    /**
     * warn
     *
     * @param message message
     * @param throwable stack frame
     */
    public void warn(String message, Throwable throwable) {
        if (LogProperties.isWarnEnable() && LogProperties.notLessThanWarn()) {
            intellijLogger.warn(message, throwable);
        }
    }

    /**
     * warn
     *
     * @param message message formatter
     * @param args arguments
     */
    public void warn(String message, String... args) {
        if (LogProperties.isWarnEnable() && LogProperties.notLessThanWarn()) {
            intellijLogger.warn(formatMessage(message, args));
        }
    }

    /**
     * error
     *
     * @param message message
     */
    public void error(String message) {
        if (LogProperties.isErrorEnable() && LogProperties.notLessThanError()) {
            intellijLogger.error(message);
        }
    }

    /**
     * error
     *
     * @param message message
     * @param throwable stack frame
     */
    public void error(String message, Throwable throwable) {
        if (LogProperties.isErrorEnable() && LogProperties.notLessThanError()) {
            intellijLogger.error(message, throwable);
        }
    }

    @Override
    public String toString() {
        return intellijLogger.toString();
    }

    /**
     * error
     *
     * @param message message formatter
     * @param args arguments
     */
    public void error(String message, String... args) {
        if (LogProperties.isErrorEnable() && LogProperties.notLessThanError()) {
            intellijLogger.error(formatMessage(message, args));
        }
    }

    private static String formatMessage(String message, String... args) {
        if (message == null) {
            return "";
        }
        List<Integer> keyIndexList = new ArrayList<>(8);
        int keyIndex = -1;
        while ((keyIndex = message.indexOf(BIG_PARENTHESES, keyIndex + 1)) != -1) {
            keyIndexList.add(keyIndex);
        }
        StringBuilder result = new StringBuilder();
        if (keyIndexList.isEmpty()) {
            result.append(message);
            return result.toString();
        }

        result.append(message, 0, keyIndexList.get(0));
        for (int i = 0; i < keyIndexList.size(); i++) {
            result.append(args.length > i ? args[i] : "{null}");
            if (i == keyIndexList.size() - 1) {
                result.append(message, keyIndexList.get(i) + BIG_PARENTHESES.length(), message.length());
            } else {
                result.append(message, keyIndexList.get(i) + BIG_PARENTHESES.length(), keyIndexList.get(i + 1));
            }
        }

        return result.toString();
    }
}