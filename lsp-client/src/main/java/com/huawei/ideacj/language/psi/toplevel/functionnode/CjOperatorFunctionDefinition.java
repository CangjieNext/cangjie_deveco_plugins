/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.functionnode;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.utils.CjFunctionInfoHandlers;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

/**
 * CjOperatorFunctionDefinition
 *
 * @since 2020-08-09
 */
public class CjOperatorFunctionDefinition extends CangjieBaseNode implements CangjieGetID {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjOperatorFunctionDefinition(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public Icon getIcon(int flags) {
        return CangjieIcons.CANGJIE_FUNCTION;
    }

    @Override
    public String getName() {
        String lamdaName = this.getText();
        String regex = "func([^\\(]*)\\(";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lamdaName);
        if (matcher.find()) {
            return "operator" + matcher.group(1).trim();
        }
        return "";
    }

    /**
     * get FunctionDefinition info
     *
     * @return info
     */
    public FunctionDefinitionInfo getFunctionDefinitionInfo() {
        initOperatorFunctionInfo();
        return functionDefinitionInfo;
    }

    private void initOperatorFunctionInfo() {
        functionDefinitionInfo = new FunctionDefinitionInfo();
        new CjFunctionInfoHandlers(functionDefinitionInfo).processAllElement(this.getChildren());
    }
}
