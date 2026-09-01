/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.settings;

import com.huawei.cangjie.mgmt.settings.CangjiePluginSettingProvider;
import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.ui.TitledSeparator;

import org.jetbrains.annotations.Nullable;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * cangjie 语言配置
 *
 * @since 2025-12-15
 */
public class CangjieLspConfigurable implements CangjiePluginSettingProvider {
    private JPanel panel;

    private JCheckBox enableConfirmDiag;

    @Override
    public void createComponent() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        TitledSeparator autoImportTextSeparator = new TitledSeparator(
                CangjieBundle.message("cangjie.language.settings.auto.import"));
        autoImportTextSeparator.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(autoImportTextSeparator);
        panel.add(Box.createVerticalStrut(5));
        enableConfirmDiag = new JCheckBox(CangjieBundle.message("cangjie.language.settings.remove.import.ask"));
        enableConfirmDiag.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        enableConfirmDiag.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(enableConfirmDiag);
        enableConfirmDiag.setSelected(CangjieLspSettings.getInstance().isRemoveImportAsk());
    }

    @Nullable
    @Override
    public JComponent getConfigSettings() {
        return panel;
    }

    @Override
    public boolean isModified() {
        CangjieLspSettings settings = CangjieLspSettings.getInstance();
        return enableConfirmDiag.isSelected() != settings.isRemoveImportAsk();
    }

    @Override
    public void apply() {
        CangjieLspSettings settings = CangjieLspSettings.getInstance();
        if (settings.getState() != null) {
            settings.getState().setRemoveImportAsk(enableConfirmDiag.isSelected());
        }
    }

    @Override
    public void reset() {
        CangjieLspSettings settings = CangjieLspSettings.getInstance();
        enableConfirmDiag.setSelected(settings.isRemoveImportAsk());
    }
}