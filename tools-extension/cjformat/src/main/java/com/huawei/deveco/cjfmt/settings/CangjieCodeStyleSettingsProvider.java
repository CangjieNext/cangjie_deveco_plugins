/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * class CangjieCodeStyleSettingsProvider
 *
 * @since 2024 -11-7
 */
public class CangjieCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    /**
     * static variable DISPLAY_NAME
     */
    public static final String DISPLAY_NAME = "Cangjie";

    private static class CangjieCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        /**
         * Instantiates a new Cangjie code style main panel.
         *
         * @param currentSettings the current settings
         * @param settings the settings
         */
        CangjieCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(CangJieLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        protected void initTabs(CodeStyleSettings settings) {
            addTab(new CangjieCodeStylePanel(CangJieLanguage.INSTANCE, getCurrentSettings(), settings));
        }

        @Override
        protected EditorHighlighter createHighlighter(final @NotNull EditorColorsScheme scheme) {
            return CangjieSyntaxHighlighterFactory.createCangjieHighlighter(scheme);
        }

        @Override
        protected void applyLanguageSettings(Language lang, CodeStyleSettings rootSettings,
            CodeStyleSettings targetSettings) {
            super.applyLanguageSettings(lang, rootSettings, targetSettings);
            CangjieCodeStyleSettings.copySettings(lang, rootSettings, targetSettings);
        }
    }

    private static class CangjieCodeStyleConfigurable extends CodeStyleAbstractConfigurable {
        /**
         * Instantiates a new Cangjie code style configurable.
         *
         * @param settings the settings
         * @param modelSettings the model settings
         */
        CangjieCodeStyleConfigurable(CodeStyleSettings settings, CodeStyleSettings modelSettings) {
            super(settings, modelSettings, CangjieCodeStyleSettingsProvider.DISPLAY_NAME);
        }

        @Override
        @NotNull
        protected CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
            return new CangjieCodeStyleMainPanel(getCurrentSettings(), settings);
        }
    }

    @Override
    public String getConfigurableDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new CangjieCodeStyleSettings(settings);
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }

    /**
     * create configurable
     *
     * @param settings CodeStyleSettings
     * @param modelSettings CodeStyleSettings
     * @return CodeStyleConfigurable
     */
    @NotNull
    public CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings,
        @NotNull CodeStyleSettings modelSettings) {
        return new CangjieCodeStyleConfigurable(settings, modelSettings);
    }
}
