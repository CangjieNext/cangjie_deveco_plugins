/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.dialog;

import static com.huawei.ideacj.lsp.utils.CommonUtils.isValidIdentifier;

import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;

import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * CangjieRenameDialog
 *
 * @since 2024/11/27
 */
public class CangjieRenameDialog extends DialogWrapper {
    private JTextField inputField;

    private JPanel myPanel;

    private JLabel icon;

    private final String initialValue;

    public CangjieRenameDialog(@Nullable Project project, String title, String initialValue) {
        super(project, false);
        icon.setIcon(Messages.getQuestionIcon());
        setTitle(title);
        inputField.setText(initialValue);
        setValidationDelay(10);
        setOKActionEnabled(false);
        setSize(400, 100);
        this.initialValue = initialValue;
        init();
    }

    /**
     * get input text
     *
     * @return input text
     */
    public String getInput() {
        return inputField.getText();
    }

    /**
     * get input
     *
     * @return JTextField
     */
    public JTextField getTextField() {
        return inputField;
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        boolean isValid = !this.initialValue.equals(inputField.getText()) && isValidIdentifier(inputField.getText());
        if (isValid) {
            setOKActionEnabled(true);
            return null;
        }
        setOKActionEnabled(false);
        String input = inputField.getText();
        if (input.equals(initialValue)) {
            return null;
        }
        return new ValidationInfo(CangjieBundle.message("lsp.rename.dialog.invalid.info", input), inputField);
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }
}
