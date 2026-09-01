/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.filetypes;

import com.huawei.ideacj.lsp.utils.CangjieMacrocallLanguage;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * CangjieMacroCallFileType
 *
 * @since 2024/04/30
 */
public class CangjieMacroCallFileType extends LanguageFileType {
    /**
     * declare Cangjie MacroCall instance
     */
    public static final CangjieMacroCallFileType INSTANCE = new CangjieMacroCallFileType();

    public CangjieMacroCallFileType() {
        super(CangjieMacrocallLanguage.INSTANCE);
    }

    @Override
    @NotNull
    public String getName() {
        return "CangjieMacrocall";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Cangjie MacroCall file support";
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
        return "macrocall";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
