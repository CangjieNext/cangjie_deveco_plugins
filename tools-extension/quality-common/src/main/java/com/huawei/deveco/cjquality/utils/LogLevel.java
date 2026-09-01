/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import lombok.Getter;

/**
 * log level for this project
 * different from HiLogLevel
 *
 * @since 2022-1-1
 */
@Getter
public enum LogLevel {
    DEBUG(4),
    INFO(3),
    WARN(2),
    ERROR(1),
    FATAL(0);

    /**
     * -- GETTER --
     * get log level int value
     */
    private final int level;

    /**
     * constructor
     *
     * @param level level int value
     */
    LogLevel(int level) {
        this.level = level;
    }
}