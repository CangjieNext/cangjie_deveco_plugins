/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.filetypes;

import com.huawei.ideacj.lsp.utils.CangjieDeclarationLanguage;
import com.huawei.ideacj.language.CangjieIcons;

import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * definition Cangjie declaration language
 *
 * @author rice
 * @since 2020-06-08
 */
public final class CangjieDeclarationFile extends LanguageFileType {
    /**
     * declare Cangjie declaration language instance
     */
    public static final CangjieDeclarationFile INSTANCE = new CangjieDeclarationFile();

    /**
     * declare Cangjie declaration language instance
     */
    public CangjieDeclarationFile() {
        super(CangjieDeclarationLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "CangjieDeclaration";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cangjie declaration file support";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "cj.d";
    }

    @Override
    public Icon getIcon() {
        return CangjieIcons.CANGJIE_FILE;
    }
}
