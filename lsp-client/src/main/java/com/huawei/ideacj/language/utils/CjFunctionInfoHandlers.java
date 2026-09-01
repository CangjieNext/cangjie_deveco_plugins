/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.utils;

import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.toplevel.CjType;
import com.huawei.ideacj.language.psi.toplevel.CjTypeParameters;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInitParamLists;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInitParamLists;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionParameters;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;

/**
 * CjFunctionInfoHandlers
 *
 * @since 2024/05/20
 */
public class CjFunctionInfoHandlers extends PsiElementInfoHandler {
    private transient FunctionDefinitionInfo functionDefinitionInfo = null;

    public CjFunctionInfoHandlers(FunctionDefinitionInfo functionDefinitionInfo) {
        initHandlers();
        this.functionDefinitionInfo = functionDefinitionInfo;
    }

    @Override
    public final void initHandlers() {
        handlers.put(CjIdentifier.class, element -> {
            if (CjPsiUtils.INSTANCE.isIdentifier(element)) {
                functionDefinitionInfo.setName(element.getText().trim());
            }
        });
        handlers.put(CjType.class, element -> functionDefinitionInfo.setReturnType(element.getText().trim()));
        handlers.put(CjTypeParameters.class, element -> {
            if (element instanceof CjTypeParameters) {
                functionDefinitionInfo.setTypeParams(((CjTypeParameters) element).getTypeParams());
            }
        });
        handlers.put(CjFunctionParameters.class, element -> {
            if (element instanceof CjFunctionParameters) {
                FunctionDefinitionInfo info =
                        ((CjFunctionParameters) element).getCjFunctionParametersInfo();
                functionDefinitionInfo.setTypeParams(info.getTypeParams());
                functionDefinitionInfo.setNamedArgs(info.getNamedArgs());
                functionDefinitionInfo.setUnNamedArgs(info.getUnNamedArgs());
                functionDefinitionInfo.setDefaultParametersWithType(info.getDefaultParametersWithType());
                functionDefinitionInfo.setDefaultParametersWithValue(info.getDefaultParametersWithValue());
            }
        });
        handlers.put(CjClassPrimaryInitParamLists.class, element -> {
            if (element instanceof CjClassPrimaryInitParamLists) {
                FunctionDefinitionInfo info =
                        ((CjClassPrimaryInitParamLists) element).getFunctionDefinitionInfo();
                functionDefinitionInfo.setTypeParams(info.getTypeParams());
                functionDefinitionInfo.setNamedArgs(info.getNamedArgs());
                functionDefinitionInfo.setUnNamedArgs(info.getUnNamedArgs());
            }
        });
        handlers.put(CjStructPrimaryInitParamLists.class, element -> {
            if (element instanceof CjStructPrimaryInitParamLists) {
                FunctionDefinitionInfo info =
                        ((CjStructPrimaryInitParamLists) element).getFunctionDefinitionInfo();
                functionDefinitionInfo.setTypeParams(info.getTypeParams());
                functionDefinitionInfo.setNamedArgs(info.getNamedArgs());
                functionDefinitionInfo.setUnNamedArgs(info.getUnNamedArgs());
            }
        });
    }
}
