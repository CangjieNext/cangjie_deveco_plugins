/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.filetypes;

import com.huawei.ideacj.lsp.utils.Toml;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Toml file support
 *
 * @since 2024/03/27
 */
public class TomlFile extends LanguageFileType {
    /**
     * toml file instance
     */
    public static final TomlFile INSTANCE = new TomlFile();

    public TomlFile() {
        super(Toml.INSTANCE);
    }

    @Override
    @NotNull
    public String getName() {
        return "Toml";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Toml";
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
        return "Toml_EXTENSION";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
