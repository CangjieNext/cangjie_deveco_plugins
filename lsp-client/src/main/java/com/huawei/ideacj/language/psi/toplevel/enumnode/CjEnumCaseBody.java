/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.enumnode;

import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * CjEnumCaseBody
 *
 * @since 2022-02-18
 */
public class CjEnumCaseBody extends CangjieBaseNode implements CangjieGetID {
    public CjEnumCaseBody(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_VARIABLE;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        ASTNode node = this.getNode();
        ASTNode check = node;
        ASTNode found = null;
        while ((check = check.getTreePrev()) != null) {
            if (check.getElementType() == CangJieTypes.BITOR) {
                found = check;
                break;
            }
        }

        ASTNode parentNode = this.getParent().getNode();
        parentNode.removeChild(node);
        if (found != null) {
            parentNode.removeChild(found);
        }
    }
}
