/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

/**
 * ExecuteResult
 *
 * @param exitCode exitCode
 * @param executeOut executeOut
 * @since 2025-07-11
 */
public record ExecuteResult(int exitCode, String executeOut) {
    public ExecuteResult(int exitCode, String executeOut) {
        this.exitCode = exitCode;
        this.executeOut = executeOut;
    }

    public int exitCode() {
        return this.exitCode;
    }

    public String executeOut() {
        return this.executeOut;
    }
}