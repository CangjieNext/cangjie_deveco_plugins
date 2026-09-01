/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.structnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;
import com.huawei.ideacj.language.utils.CjFunctionInfoHandlers;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * CjRecordPrimaryInit
 *
 * @since 2022-02-18
 */
public class CjStructPrimaryInit extends CangjieBaseNode {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjStructPrimaryInit(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement element = PsiTreeUtil.getChildOfType(this, CjStructName.class);
        if (element == null) {
            return "";
        }
        PsiElement identifier = element.getFirstChild();
        if (identifier != null && identifier.getNode() != null) {
            return identifier.getNode().getText();
        }
        return "";
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_FUNCTION;
    }

    /**
     * get FunctionDefinition info
     *
     * @return info
     */
    public FunctionDefinitionInfo getFunctionDefinitionInfo() {
        initStructPrimaryInitFunctionInfo();
        return functionDefinitionInfo;
    }

    private void initStructPrimaryInitFunctionInfo() {
        functionDefinitionInfo = new FunctionDefinitionInfo();
        new CjFunctionInfoHandlers(functionDefinitionInfo).processAllElement(this.getChildren());
    }
}
