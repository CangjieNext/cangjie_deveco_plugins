/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.generation.override;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.lsp.extend.params.OverridableMethodInfo;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.MemberChooserObjectBase;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import javax.swing.Icon;

/**
 * Cangjie Chooser Element Node
 *
 * @since 2025-07-01
 */
public class CangjieChooserElementNode extends MemberChooserObjectBase implements ClassMember {
    /**
     * Empty element list
     */
    public static final CangjieChooserElementNode[] EMPTY_ARRAY = new CangjieChooserElementNode[0];

    private final String fullPkgName;
    private final String parentClass;
    private final String parentKind;
    private final boolean deprecated;
    private String insertText;

    public CangjieChooserElementNode(@NotNull String fullPkgName, @NotNull String parentClass,
                                     @NotNull String parentKind, @NotNull OverridableMethodInfo method, Icon icon) {
        super(method.getSignatureWithRet(), icon);
        this.fullPkgName = fullPkgName;
        this.parentClass = parentClass;
        this.parentKind = parentKind;
        this.deprecated = method.getDeprecated();
        this.insertText = method.getInsertText();
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        Icon icon;
        if (parentKind.equals("class")) {
            icon = CangjieIcons.CANGJIE_CLASS;
        } else if (parentKind.equals("interface")) {
            icon = CangjieIcons.CANGJIE_INTERFACE;
        } else {
            icon = CangjieIcons.CANGJIE_ENUM;
        }
        String showText = fullPkgName + "." + parentClass;
        return new CangjieChooserElementNode(fullPkgName, parentClass, parentKind,
                new OverridableMethodInfo(false, false, showText, showText), icon);
    }

    /**
     * Get insert text
     *
     * @return String
     */
    public String getInsertText() {
        return insertText;
    }

    /**
     * Add override modifier
     */
    public void addOverride() {
        if (!insertText.isEmpty()) {
            insertText = "override " + insertText;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CangjieChooserElementNode that = (CangjieChooserElementNode) obj;
        return Objects.equals(insertText, that.insertText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullPkgName + parentClass + insertText);
    }
}
