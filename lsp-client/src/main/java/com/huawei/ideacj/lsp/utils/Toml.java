/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import com.intellij.lang.Language;

/**
 * Toml file support
 *
 * @since 2024/03/27
 */
public class Toml extends Language {
    /**
     * toml file instance
     */
    public static final Toml INSTANCE = new Toml();

    protected Toml() {
        super("Toml");
    }
}
