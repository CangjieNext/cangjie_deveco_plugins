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
 * cang jie language
 *
 * @since 2020-12-18
 */
public class CangJieLanguage extends Language {
    /**
     * cang jie language instance
     */
    public static final CangJieLanguage INSTANCE = new CangJieLanguage();

    protected CangJieLanguage() {
        super("Cangjie");
    }
}
