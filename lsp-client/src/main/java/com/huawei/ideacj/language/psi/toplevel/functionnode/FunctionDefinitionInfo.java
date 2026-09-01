/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.functionnode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * NamedParameterList info
 *
 * @since 2021-08-09
 */
public class FunctionDefinitionInfo {
    private String name;

    private List<String> typeParams;

    private LinkedHashMap<String, String> unNamedArgs;

    private LinkedHashMap<String, String> namedArgs;

    private HashMap<String, String> defaultParametersWithType;

    private HashMap<String, String> defaultParametersWithValue;

    private String returnType = "";

    public List<String> getTypeParams() {
        return typeParams;
    }

    public LinkedHashMap<String, String> getUnNamedArgs() {
        return unNamedArgs;
    }

    public LinkedHashMap<String, String> getNamedArgs() {
        return namedArgs;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamedArgs(LinkedHashMap<String, String> namedArgs) {
        this.namedArgs = namedArgs;
    }

    public void setReturnType(String returnType) {
        if (!returnType.isEmpty()) {
            this.returnType = returnType;
        }
    }

    public void setTypeParams(List<String> typeParams) {
        this.typeParams = typeParams;
    }

    public void setUnNamedArgs(LinkedHashMap<String, String> unNamedArgs) {
        this.unNamedArgs = unNamedArgs;
    }

    public void setDefaultParametersWithType(HashMap<String, String> defaultParametersWithType) {
        this.defaultParametersWithType = defaultParametersWithType;
    }

    public void setDefaultParametersWithValue(HashMap<String, String> defaultParametersWithValue) {
        this.defaultParametersWithValue = defaultParametersWithValue;
    }

    public HashMap<String, String> getDefaultParametersWithType() {
        return defaultParametersWithType;
    }

    public HashMap<String, String> getDefaultParametersWithValue() {
        return defaultParametersWithValue;
    }
}
