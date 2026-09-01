/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.deveco.cjfmt.core.ReformatCodeService;
import com.huawei.deveco.cjfmt.utils.FormatConstants;
import com.huawei.deveco.cjfmt.utils.FormatUtils;
import com.huawei.deveco.cjquality.utils.CangjieCompileArg;
import com.huawei.deveco.cjquality.utils.ExecuteResult;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.ideacj.filetypes.CangjieCodeFile;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.util.LocalTimeCounter;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;

/**
 * The type Cangjie code style panel.
 *
 * @since 2024 -11-7
 */
public class CangjieCodeStylePanel extends CodeStyleAbstractPanel {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CangjieCodeStylePanel.class);

    private JPanel myPanel;

    private JPanel myPreviewPanel;

    private JTextField indentWidth;

    private JTextField lineLimitLength;

    private JComboBox<LineBreakTypeStyle> lineBreakType;

    private JCheckBox allowMultiLineMethodChain;

    private JTextField multipleLineMethodChainLevel;

    private JCheckBox multipleLineMethodChainOverLineLength;

    /**
     * Instantiates a new Cangjie code style panel.
     *
     * @param defaultLanguage the default language
     * @param currentSettings the current settings
     * @param settings the settings
     */
    public CangjieCodeStylePanel(@Nullable Language defaultLanguage, @Nullable CodeStyleSettings currentSettings,
        @NotNull CodeStyleSettings settings) {
        super(defaultLanguage, currentSettings, settings);
        installPreviewPanel(myPreviewPanel);
        fillEnumCombobox(lineBreakType);
        initInputFilterAndListener();
    }

    @Override
    protected int getRightMargin() {
        return CangjieCodeStyleSettings.LINE_LIMIT_LENGTH_DEFAULT;
    }

    @Override
    protected EditorHighlighter createHighlighter(final @NotNull EditorColorsScheme scheme) {
        return CangjieSyntaxHighlighterFactory.createCangjieHighlighter(scheme);
    }

    @Override
    public void apply(@NotNull CodeStyleSettings rootSettings) {
        if (!validateInput()) {
            return;
        }
        CangjieCodeStyleSettings settings = rootSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        settings.indentWidth = getIndentWidth();
        settings.lineLimitLength = getLineLimitLength();
        settings.lineBreakType = getLineBreakType();
        settings.allowMultiLineMethodChain = allowMultiLineMethodChain.isSelected();
        settings.methodChainLevel = getMultipleLineMethodChainLevel();
        settings.methodChainOverLength = multipleLineMethodChainOverLineLength.isSelected();
    }

    @Override
    protected void resetImpl(final @NotNull CodeStyleSettings rootSettings) {
        CangjieCodeStyleSettings settings = rootSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        indentWidth.setText(String.valueOf(settings.indentWidth));
        lineLimitLength.setText(String.valueOf(settings.lineLimitLength));
        lineBreakType.setSelectedItem(settings.lineBreakType);
        allowMultiLineMethodChain.setSelected(settings.allowMultiLineMethodChain);
        multipleLineMethodChainLevel.setText(String.valueOf(settings.methodChainLevel));
        multipleLineMethodChainOverLineLength.setSelected(settings.methodChainOverLength);
    }

    @Override
    public boolean isModified(CodeStyleSettings rootSettings) {
        if (!validateInput()) {
            return false;
        }
        CangjieCodeStyleSettings settings = rootSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        if (getIndentWidth() != settings.indentWidth) {
            return true;
        }
        if (getLineLimitLength() != settings.lineLimitLength) {
            return true;
        }
        if (lineBreakType.getSelectedItem() != settings.lineBreakType) {
            return true;
        }
        if (allowMultiLineMethodChain.isSelected() != settings.allowMultiLineMethodChain) {
            return true;
        }
        if (getMultipleLineMethodChainLevel() != settings.methodChainLevel) {
            return true;
        }
        return multipleLineMethodChainOverLineLength.isSelected() != settings.methodChainOverLength;
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    protected String getPreviewText() {
        return readFromFile(this.getClass(), FormatConstants.PREVIEW_FILE);
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
        return "Tabs and Indents";
    }

    @Override
    @NotNull
    protected PsiFile doReformat(final Project project, @NotNull PsiFile psiFile) {
        // Step 1: Get the content from the PsiFile
        VirtualFile virtualFile = psiFile.getViewProvider().getVirtualFile();
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return psiFile;
        }
        try {
            // Step 2: Write cjfmt config to toml file
            CangjieCodeStyleSettings settings = getSettings().getCustomSettings(CangjieCodeStyleSettings.class);
            CangjieCodeStyleSettings.writeConfigToFile(settings, FormatUtils.getCjfmtConfigPath(project));
            if (StringUtils.isBlank(CangjieCompileArg.getCjSdkPath())) {
                CangjieCompileArg.initCjSdkPath(project);
            }
            // Step 3: Perform logic processing on the file content
            Optional<ExecuteResult> executeResult =
                ReformatCodeService.getInstance(project).reformatCode(virtualFile, project);
            if (executeResult.isEmpty()) {
                LOGGER.warn("reformat file failed.");
                return psiFile;
            }
            if (executeResult.get().exitCode() != 0) {
                LOGGER.warn("reformat file failed, result is {}", executeResult.get().executeOut());
                return psiFile;
            }
            virtualFile.refresh(false, false);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return psiFile;
    }

    @Override
    protected PsiFile createFileFromText(@NotNull Project project, @NotNull String text) {
        Language language = getDefaultLanguage();
        if (language != null) {
            LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(language);
            if (provider != null) {
                final PsiFile file = provider.createFileFromText(project, text);
                if (file != null) {
                    return file;
                }
            }
        }
        return PsiFileFactory.getInstance(project)
            .createFileFromText(FormatConstants.PREVIEW_FILE, getFileType(), text, LocalTimeCounter.currentTime(),
                false);
    }

    private void initInputFilterAndListener() {
        if (indentWidth.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) indentWidth.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        }
        indentWidth.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                indentWidth.putClientProperty("JComponent.outline", validateIndentWidth() ? null : "error");
            }
        });
        if (lineLimitLength.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) lineLimitLength.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        }
        lineLimitLength.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                lineLimitLength.putClientProperty("JComponent.outline", validateLineLimitLength() ? null : "error");
            }
        });
        if (multipleLineMethodChainLevel.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) multipleLineMethodChainLevel.getDocument()).setDocumentFilter(
                new NumericDocumentFilter());
        }
        multipleLineMethodChainLevel.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                multipleLineMethodChainLevel.putClientProperty("JComponent.outline",
                    validateChainLevel() ? null : "error");
            }
        });
    }

    private boolean validateInput() {
        if (!validateIndentWidth()) {
            return false;
        }
        if (!validateLineLimitLength()) {
            return false;
        }
        return validateChainLevel();
    }

    private boolean validateChainLevel() {
        return CangjieCodeStyleSettings.validateChainLevel(getMultipleLineMethodChainLevel());
    }

    private boolean validateLineLimitLength() {
        return CangjieCodeStyleSettings.validateLineLimitLength(getLineLimitLength());
    }

    private boolean validateIndentWidth() {
        return CangjieCodeStyleSettings.validateIndentWidth(getIndentWidth());
    }

    private static <T extends Enum<T>> void fillEnumCombobox(JComboBox<LineBreakTypeStyle> combo) {
        combo.setModel(new EnumComboBoxModel<>(LineBreakTypeStyle.class));
    }

    private LineBreakTypeStyle getLineBreakType() {
        Object lineBreakTypeItem = lineBreakType.getSelectedItem();
        if (lineBreakTypeItem instanceof LineBreakTypeStyle) {
            return (LineBreakTypeStyle) lineBreakTypeItem;
        }
        return LineBreakTypeStyle.LF;
    }

    private int getIndentWidth() {
        return getInputNumValue(this.indentWidth, CangjieCodeStyleSettings.INDENT_WIDTH_DEFAULT);
    }

    private int getLineLimitLength() {
        return getInputNumValue(this.lineLimitLength, CangjieCodeStyleSettings.LINE_LIMIT_LENGTH_DEFAULT);
    }

    private int getMultipleLineMethodChainLevel() {
        return getInputNumValue(this.multipleLineMethodChainLevel,
            CangjieCodeStyleSettings.MULTIPLE_LINE_METHOD_CHAIN_LEVEL_DEFAULT);
    }

    private int getInputNumValue(JTextField inputField, int defaultValue) {
        String text = inputField.getText();
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
