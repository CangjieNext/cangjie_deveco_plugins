/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.generation.override;

import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Cangjie Membser Chooser
 *
 * @since 2025-07-01
 */
public class CangjieMemberChooser extends MemberChooser<CangjieChooserElementNode> {
    private JCheckBox myCustomCheckBox;
    @NotNull
    private final List<CangjieChooserElementNode> myOriginalCandidates;

    public CangjieMemberChooser(@NotNull List<CangjieChooserElementNode> candidates, @NotNull Project project,
                                boolean allowEmptySelection) {
        super(candidates.toArray(CangjieChooserElementNode.EMPTY_ARRAY), allowEmptySelection, true, project);
        this.myOriginalCandidates = candidates;
        if (!allowEmptySelection) {
            this.myTree.addTreeSelectionListener(e -> setOKActionEnabled(myTree.getSelectionCount() > 0));
        } else {
            this.setOKActionEnabled(true);
        }
    }

    @Override
    protected void fillToolbarActions(DefaultActionGroup group) {
        super.fillToolbarActions(group);
    }

    @Override
    protected boolean isContainerNode(MemberChooserObject key) {
        return key instanceof CangjieChooserElementNode;
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent originalPanel = super.createSouthPanel();
        myCustomCheckBox = new JCheckBox("Insert override modifier");
        myCustomCheckBox.setToolTipText("Insert override modifier");

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(originalPanel, BorderLayout.CENTER);
        southPanel.add(myCustomCheckBox, BorderLayout.WEST);

        return southPanel;
    }

    /**
     * Option is Selected
     *
     * @return boolean
     */
    public boolean isMyCustomOptionSelected() {
        return myCustomCheckBox != null && myCustomCheckBox.isSelected();
    }

    /**
     * Add override modifier
     */
    public void addOverrideModifier() {
        for (CangjieChooserElementNode candidate : myOriginalCandidates) {
            candidate.addOverride();
        }
    }
}
