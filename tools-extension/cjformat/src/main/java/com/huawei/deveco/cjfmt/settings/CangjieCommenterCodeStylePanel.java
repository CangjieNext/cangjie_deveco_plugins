/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.ideacj.filetypes.CangjieCodeFile;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.codeStyle.CommenterForm;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.CommenterOption;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Code style panel that renders the "Code Generation" tab for the Cangjie
 * language settings page. It exposes the commenter options (line comment at
 * first column / add space) and keeps the panel visible even when the platform
 * CommenterForm hides it because no LanguageCodeStyleSettingsProvider shows
 * commenter options for the language.
 *
 * @since 2026-09-08
 */
public class CangjieCommenterCodeStylePanel extends CodeStyleAbstractPanel {
    private final CommenterForm myCommenterForm;
    private final JPanel myPanel;

    public CangjieCommenterCodeStylePanel(@Nullable Language language, @Nullable CodeStyleSettings currentSettings,
        @NotNull CodeStyleSettings settings) {
        super(language, currentSettings, settings);
        Language lang = language != null ? language : CangJieLanguage.INSTANCE;
        myCommenterForm = new CommenterForm(lang, "Comments");
        myCommenterForm.showStandardOptions(
            CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name(),
            CommenterOption.LINE_COMMENT_ADD_SPACE.name()
        );
        // CommenterForm's constructor hides the whole panel when no
        // LanguageCodeStyleSettingsProvider shows any commenter option for the
        // language — re-show it after we have enabled our two options.
        myCommenterForm.getCommenterPanel().setVisible(true);
        // The CommenterForm panel is built with zero insets, which makes the
        // "Comments" group flush to the tab edge and looks jarring when
        // switching tabs. Wrap it with the standard settings-page margin.
        myPanel = new JPanel(new BorderLayout());
        myPanel.setBorder(JBUI.Borders.empty(10));
        myPanel.add(myCommenterForm.getCommenterPanel(), BorderLayout.CENTER);
    }

    @Override
    protected int getRightMargin() {
        return 120;
    }

    @Override
    protected EditorHighlighter createHighlighter(final @NotNull EditorColorsScheme scheme) {
        return null;
    }

    @Override
    @NotNull
    protected FileType getFileType() {
        return CangjieCodeFile.INSTANCE;
    }

    @NlsContexts.TabTitle
    @Override
    @NotNull
    protected String getTabTitle() {
        return "Code Generation";
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    public void apply(@NotNull CodeStyleSettings rootSettings) {
        myCommenterForm.apply(rootSettings);
        // Persist through CangjieCodeStyleSettings: the per-language common
        // settings node is not written for Cangjie, so the custom-settings
        // fields are the durable storage across IDE restarts.
        CommonCodeStyleSettings common = rootSettings.getCommonSettings(CangJieLanguage.INSTANCE);
        CangjieCodeStyleSettings custom = rootSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        custom.setLineCommentAtFirstColumn(common.LINE_COMMENT_AT_FIRST_COLUMN);
        custom.setLineCommentAddSpace(common.LINE_COMMENT_ADD_SPACE);
    }

    @Override
    protected void resetImpl(@NotNull CodeStyleSettings rootSettings) {
        // Restore the last persisted choice into the common settings first so
        // the form shows what the user selected before the restart.
        CommonCodeStyleSettings common = rootSettings.getCommonSettings(CangJieLanguage.INSTANCE);
        CangjieCodeStyleSettings custom = rootSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        common.LINE_COMMENT_AT_FIRST_COLUMN = custom.isLineCommentAtFirstColumn();
        common.LINE_COMMENT_ADD_SPACE = custom.isLineCommentAddSpace();
        myCommenterForm.reset(rootSettings);
    }

    @Override
    public boolean isModified(CodeStyleSettings rootSettings) {
        return myCommenterForm.isModified(rootSettings);
    }

    @Override
    @Nullable
    protected String getPreviewText() {
        return null;
    }
}
