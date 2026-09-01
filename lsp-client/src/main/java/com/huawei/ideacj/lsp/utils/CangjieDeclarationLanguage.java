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
 * cang jie declaration language
 *
 * @since 2025-02-17
 */
public class CangjieDeclarationLanguage extends Language {
    /**
     * cang jie declaration language instance
     */
    public static final CangjieDeclarationLanguage INSTANCE = new CangjieDeclarationLanguage();

    protected CangjieDeclarationLanguage() {
        super("CangjieDeclaration");
    }
}
