/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;
import com.huawei.ideacj.language.utils.CjFunctionInfoHandlers;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import javax.swing.Icon;

/**
 * ClassPrimaryInit
 *
 * @since 2021-07-31
 */
public class CjClassPrimaryInit extends CangjieBaseNode {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjClassPrimaryInit(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement element = PsiTreeUtil.getChildOfType(this, CjClassName.class);
        if (element == null) {
            return "";
        }
        PsiElement identifier = element.getFirstChild();
        if (identifier != null && identifier.getNode() != null) {
            return identifier.getNode().getText();
        }
        return "";
    }

    /**
     * Return ID PsiElement
     *
     * @return PsiElement
     */
    public Optional<PsiElement> getIDPsi() {
        PsiElement element = PsiTreeUtil.getChildOfType(this, CjClassName.class);
        if (element == null) {
            return Optional.empty();
        }
        PsiElement identifier = element.getFirstChild();
        if (identifier != null) {
            return Optional.of(identifier);
        }
        return Optional.empty();
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
        initClassPrimaryInitFunctionInfo();
        return functionDefinitionInfo;
    }

    private void initClassPrimaryInitFunctionInfo() {
        functionDefinitionInfo = new FunctionDefinitionInfo();
        new CjFunctionInfoHandlers(functionDefinitionInfo).processAllElement(this.getChildren());
    }
}
