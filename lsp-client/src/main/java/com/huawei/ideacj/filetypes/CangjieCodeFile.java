/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.filetypes;

import static com.huawei.ideacj.lsp.utils.LanguageManager.CANGJIE_EXTENSION;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * definition Cangjie language
 *
 * @author rice
 * @since 2020-06-08
 */
public class CangjieCodeFile extends LanguageFileType {
    /**
     * declare Cangjie language instance
     */
    public static final CangjieCodeFile INSTANCE = new CangjieCodeFile();

    public CangjieCodeFile() {
        super(CangJieLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Cangjie";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cangjie file support";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return CANGJIE_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CangjieIcons.CANGJIE_FILE;
    }
}