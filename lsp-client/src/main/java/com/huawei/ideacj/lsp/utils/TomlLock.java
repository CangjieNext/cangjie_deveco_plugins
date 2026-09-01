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
 * Toml Lock file support
 *
 * @since 2024/03/27
 */
public class TomlLock extends Language {
    /**
     * toml lock file instance
     */
    public static final TomlLock INSTANCE = new TomlLock();

    protected TomlLock() {
        super("TomlLock");
    }
}
