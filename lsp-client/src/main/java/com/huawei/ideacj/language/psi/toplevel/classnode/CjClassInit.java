/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;
import com.huawei.ideacj.language.utils.CjFunctionInfoHandlers;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

/**
 * ClassInit
 *
 * @since 2021-07-31
 */
public class CjClassInit extends CangjieBaseNode implements CangjieGetID {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjClassInit(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "init";
    }

    /**
     * Return ID PsiElement
     *
     * @param myElement PsiElement
     * @return List<PsiElement>
     */
    @Override
    public List<PsiElement> getIDPsi(PsiElement myElement) {
        List<PsiElement> aimElement = new ArrayList<>();
        PsiElement[] elements = myElement.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isAimString(childElement, "init")) {
                aimElement.add(childElement);
            }
        }
        return aimElement;
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
        initClassInitFunctionInfo();
        return functionDefinitionInfo;
    }

    private void initClassInitFunctionInfo() {
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
