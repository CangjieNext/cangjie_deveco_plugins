/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

/**
 * creat dialog  to creat new Cangjie file
 *
 * @since 2022-01-25
 */
public class CangjieFileDialog extends DialogWrapper implements DialogWithParameters {
    /**
     * DEFAULT_FILENAME
     */
    public static final String DEFAULT_FILENAME = "FILENAME";

    private static final Pattern CANGJIE_NAME_PATTERN = Pattern.compile(
            "^[-_{}\\[\\]#\\(\\)%;\\+\\-=^&~!,'`@\\w]"
                + "+(?:-|_|\\{|}|\\[|]|#|\\(|\\)|%|;|\\+|-|=|^|&|~|!|,|'|`|@|\\.|\\w)*$",
            Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * the properties of dialog boxes
     */
    protected final CangjieNewFileAction myAction;

    private final Pattern myRegex;

    private JTextField fileNameField;

    private JPanel myPanel;

    private JComboBox<?> fileTypeBox;

    private String myErrorMessage = null;

    private List<String> fileNames = new ArrayList<>();

    public CangjieFileDialog(Project project,
                             String title,
                             CangjieNewFileAction cangjieNewFileAction,
                             List<String> fileNames) {
        this(project, title, cangjieNewFileAction, CANGJIE_NAME_PATTERN, fileNames);
    }

    /**
     * the dialog to creat new Cangjie file
     *
     * @param project interface of Project
     * @param title  string of title
     * @param cangjieNewFileAction class of CangjieNewFileAction
     * @param regex class of Pattern
     * @param fileNames fileNames in dir
     */
    public CangjieFileDialog(Project project,
                             String title,
                             CangjieNewFileAction cangjieNewFileAction,
                             Pattern regex,
                             List<String> fileNames) {
        super(project, false);
        this.fileNames.addAll(fileNames);
        myRegex = regex;
        myAction = cangjieNewFileAction;
        init();
        setTitle(title);
        setValidationDelay(10);
        setOKActionEnabled(false);

        fileNameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                myErrorMessage = null;
            }
        });
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (myErrorMessage != null) {
            setOKActionEnabled(false);
            return new ValidationInfo(myErrorMessage, null);
        }

        String text = fileNameField.getText();
        if (text.isEmpty()) {
            setOKActionEnabled(false);
            return null;
        }
        if (Character.isDigit(text.charAt(0))) {
            setOKActionEnabled(false);
            return new ValidationInfo("Do not start with a digit", fileNameField);
        }
        if (!myRegex.matcher(text).matches()) {
            setOKActionEnabled(false);
            return new ValidationInfo(
                    "New Cangjie file name contains illegal character "
                        + "(i.e., '*', '|', '\\', '/', '\"', ':', '?', ' ', '>', '&lt', '$'), "
                        + "the recommended examples are: 'foo', 'foo_bar', 'foo1'", fileNameField);
        }
        if (this.fileNames.contains(text.toLowerCase(Locale.ROOT) + ".cj")) {
            setOKActionEnabled(false);
            return new ValidationInfo("Filename already exists", fileNameField);
        }
        setOKActionEnabled(true);
        return null;
    }

    @Override
    protected void doOKAction() {
        myErrorMessage = myAction.validateInput(getParameters());
        if (myErrorMessage == null) {
            super.doOKAction();
        } else {
            initValidation();
            setOKActionEnabled(false);
        }
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return fileNameField;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    /**
     * Obtain parameters in the dialog box.
     *
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(DEFAULT_FILENAME, fileNameField.getText());
        return params;
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }
}
