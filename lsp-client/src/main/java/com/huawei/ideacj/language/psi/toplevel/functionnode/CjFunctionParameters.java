/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.functionnode;

import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;
import com.huawei.ideacj.language.psi.toplevel.CjNamedParameterList;
import com.huawei.ideacj.language.psi.toplevel.CjUnnamedParameterList;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CjFunctionParameters
 *
 * @since 2020-08-09
 */
public class CjFunctionParameters extends CangjieNamedElement {
    private FunctionDefinitionInfo cjFunctionParametersInfo = null;

    public CjFunctionParameters(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return getCjFunctionParameters
     *
     * @return element
     */
    public FunctionDefinitionInfo getCjFunctionParametersInfo() {
        initFunctionParametersInfo();
        return cjFunctionParametersInfo;
    }

    private void initFunctionParametersInfo() {
        cjFunctionParametersInfo = new FunctionDefinitionInfo();
        new CjFunctionParametersInfoHandlers().processAllElement(this.getChildren());
    }

    private class CjFunctionParametersInfoHandlers extends PsiElementInfoHandler {
        public CjFunctionParametersInfoHandlers() {
            initHandlers();
        }

        @Override
        public final void initHandlers() {
            cjFunctionParametersInfo = new FunctionDefinitionInfo();
            AtomicBoolean hasUnnamedParam = new AtomicBoolean(false);
            handlers.put(
                    CjUnnamedParameterList.class,
                    element -> {
                        if (element instanceof CjUnnamedParameterList) {
                            cjFunctionParametersInfo.setUnNamedArgs(
                                    ((CjUnnamedParameterList) element).getUnnamedParamMap());
                            cjFunctionParametersInfo.setTypeParams(((CjUnnamedParameterList) element).getTypeParams());
                            hasUnnamedParam.set(true);
                        }
                    });
            handlers.put(
                    CjNamedParameterList.class,
                    element -> {
                        if (!(element instanceof CjNamedParameterList)) {
                            return;
                        }
                        CjNamedParameterList.CjNamedParameterListInfo info =
                            ((CjNamedParameterList) element).getCjNamedParameterListInfo();
                        List<String> nameParameters = info.getTypeParams();
                        if (hasUnnamedParam.get()) {
                            nameParameters = cjFunctionParametersInfo.getTypeParams();
                            if (!info.getTypeParams().isEmpty()) {
                                nameParameters.addAll(info.getTypeParams());
                            }
                            cjFunctionParametersInfo.setTypeParams(nameParameters);
                        }
                        cjFunctionParametersInfo.setTypeParams(nameParameters);
                        cjFunctionParametersInfo.setNamedArgs(info.getNamedParameters());
                        cjFunctionParametersInfo.setDefaultParametersWithType(info.getDefaultParametersWithType());
                        cjFunctionParametersInfo.setDefaultParametersWithValue(
                            info.getDefaultParametersWithValue());
                    });
        }
    }
}
