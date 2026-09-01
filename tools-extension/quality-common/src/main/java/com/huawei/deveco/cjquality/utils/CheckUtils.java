/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Code check util class
 *
 * @since 2023-02-18
 */
public class CheckUtils {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CheckUtils.class);

    /**
     * aka Paths.get("xxx", "xxx")
     *
     * @param first first
     * @param more more
     * @return Path
     */
    public static Optional<Path> getPath(String first, String... more) {
        try {
            Method getMethod = Paths.class.getMethod("get", String.class, String[].class);
            Object obj = getMethod.invoke(null, first, more);
            return Optional.of(cast(obj));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("get coverage path error.");
            return Optional.empty();
        }
    }

    /**
     * unsafe cast obj to T
     *
     * @param obj obj
     * @param <T> T
     * @return T
     */
    private static <T> T cast(Object obj) {
        return (T) obj;
    }
}
