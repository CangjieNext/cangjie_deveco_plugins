/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import org.jetbrains.annotations.NotNull;

/**
 * Line Range record
 *
 * @param startLine start line
 * @param endLine end line
 * @since 2025-07-11
 */
public record LineRange(int startLine, int endLine) {

    /**
     * create line range
     *
     * @param startLine start line
     * @param endLine end line
     * @return LineRange obj
     */
    @NotNull
    public static LineRange create(int startLine, int endLine) {
        return new LineRange(startLine, endLine);
    }
}
