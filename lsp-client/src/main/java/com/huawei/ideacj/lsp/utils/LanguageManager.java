/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * language manager
 *
 * @since 2020-12-11
 */
public class LanguageManager {
    /**
     * cangjie language
     */
    public static final Language CANGJIE_LANGUAGE = new CangJieLanguage();

    /**
     * cangjie extension
     */
    public static final @NonNls
    String CANGJIE_EXTENSION = "cj";

    /**
     * cangjie language file type
     */
    public static final LanguageFileType CANGJIE_FILETYPE = new LanguageFileType(CANGJIE_LANGUAGE) {
        @Override
        @NotNull
        public String getName() {
            return "Cangjie";
        }

        @Override
        @NotNull
        public String getDescription() {
            return "Cang jie file support";
        }

        @Override
        @NotNull
        public String getDefaultExtension() {
            return CANGJIE_EXTENSION;
        }

        @Override
        @Nullable
        public Icon getIcon() {
            return IconLoader.findIcon("/icons/cangjie_file_obj.gif", LanguageManager.class);
        }
    };
}
