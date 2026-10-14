/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.codeStyle.DocCommentSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Provides the per-language code style baseline for Cangjie. The
 * {@link CommonCodeStyleSettings} returned here is the diff baseline used when
 * persisting language-level settings: fields equal to this baseline are not
 * written to the scheme. It must stay consistent with the runtime defaults
 * customized in {@link CangjieLanguageCodeStyleSettingsProvider}, otherwise
 * user-selected values equal to the platform defaults are silently dropped on
 * restart.
 *
 * @since 2026-10-14
 */
public class CangjieLanguageCodeStyleProvider implements LanguageCodeStyleProvider {
    @Override
    @NotNull
    public Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }

    @Override
    @NotNull
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings defaults = new CommonCodeStyleSettings(CangJieLanguage.INSTANCE);
        // Both commenter options start unchecked (consistent with
        // CangjieLanguageCodeStyleSettingsProvider#customizeDefaults), so a
        // user-checked value is persisted as a diff against this baseline.
        defaults.LINE_COMMENT_AT_FIRST_COLUMN = false;
        defaults.LINE_COMMENT_ADD_SPACE = false;
        return defaults;
    }

    @Override
    @NotNull
    public DocCommentSettings getDocCommentSettings(@NotNull CodeStyleSettings settings) {
        return DocCommentSettings.DEFAULTS;
    }

    @Override
    @Nullable
    public Set<String> getSupportedFields() {
        return null;
    }

    @Override
    @NotNull
    public CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new CangjieCodeStyleSettings(settings);
    }
}
