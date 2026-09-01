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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * CjRecordInit
 *
 * @since 2022-02-18
 */
public class CjStructInit extends CangjieBaseNode {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjStructInit(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "init";
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
        initStructInitFunctionInfo();
        return functionDefinitionInfo;
    }

    private void initStructInitFunctionInfo() {
        functionDefinitionInfo = new FunctionDefinitionInfo();
        new CjFunctionInfoHandlers(functionDefinitionInfo).processAllElement(this.getChildren());
    }

    @Override
    @Nullable
    public PsiElement getNameIdentifier() {
        for (@NotNull PsiElement child : this.getChildren()) {
            if (child.getText().equals("init")) {
                return child;
            }
        }
        return this;
    }
}
