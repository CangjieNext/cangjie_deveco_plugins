/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * LSPThreadPoolManager
 *
 * @since 2024/07/24
 */
public class LSPThreadPoolManager {
    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        int sysCoreCount = Runtime.getRuntime().availableProcessors();
        int threadCnt = sysCoreCount > 1 ? (sysCoreCount / 2) : sysCoreCount;
        EXECUTOR_SERVICE = new ThreadPoolExecutor(threadCnt, threadCnt, 10L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * lsp pool
     *
     * @param runnable runnable
     */
    public static void pool(Runnable runnable) {
        EXECUTOR_SERVICE.submit(runnable);
    }

    /**
     * lsp pool
     *
     * @param callable callable
     * @param <T> T
     * @return Future
     */
    public static <T> Future<T> pool(Callable<T> callable) {
        return EXECUTOR_SERVICE.submit(callable);
    }
}