/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract.dialog;

import static com.huawei.ideacj.lsp.utils.CommonUtils.isValidIdentifier;

import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;

import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog for Extract Function
 *
 * @since 2025-06-10
 */
public class CangjieExtractFunctionDialog extends DialogWrapper {
    private boolean isDoOK = false;

    private JPanel panel1;

    private JTextField textField1;

    /**
     * CppLspExtractFunctionDialog constructor
     *
     * @param project Project
     * @param title String
     */
    public CangjieExtractFunctionDialog(Project project, String title) {
        super(project);
        setOKActionEnabled(false);
        setValidationDelay(10);
        init();
        setTitle(title);
    }

    @Override
    protected boolean postponeValidation() {
        return false;
    }

    /**
     * return customized new function name
     *
     * @return String
     */
    public String getFunctionName() {
        return textField1.getText();
    }

    @Override
    public void doCancelAction() {
        isDoOK = false;
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        isDoOK = true;
        super.doOKAction();
    }

    /**
     * return whether click OK button or not
     *
     * @return doOK
     */
    public boolean isDoOK() {
        return isDoOK;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (textField1.getText().isEmpty()) {
            setOKActionEnabled(false);
            return null;
        }
        if (!isValidIdentifier(textField1.getText())) {
            setOKActionEnabled(false);
            return new ValidationInfo(CangjieBundle.message("lsp.refactor.extract.method.dialog.invalid",
                    textField1.getText()), textField1);
        }
        setOKActionEnabled(true);
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel1;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return textField1;
    }
}
