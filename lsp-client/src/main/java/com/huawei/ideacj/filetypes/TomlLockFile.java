/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.filetypes;

import com.huawei.ideacj.lsp.utils.TomlLock;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Toml Lock file support
 *
 * @since 2024/03/27
 */
public class TomlLockFile extends LanguageFileType {
    /**
     * toml file instance
     */
    public static final TomlLockFile INSTANCE = new TomlLockFile();

    public TomlLockFile() {
        super(TomlLock.INSTANCE);
    }

    @Override
    @NotNull
    public String getName() {
        return "TomlLock";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Toml lock file";
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
        return "TomlLock_EXTENSION";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
