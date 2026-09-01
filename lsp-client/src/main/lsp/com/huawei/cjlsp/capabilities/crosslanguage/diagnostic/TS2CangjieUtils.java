/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage.diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * TS2CangjieUtils
 *
 * @since 2025/09/03
 */
public class TS2CangjieUtils {
    private final TSFuncSignature tsFuncSignature = new TSFuncSignature();

    // ArkTS->Cangjie
    private final HashMap<String, String> typeMap = new HashMap<>() {
        {
            put("number", "Float64");
            put("boolean", "Bool");
            put("string", "String");
            put("void", "Unit");
            put("ArrayBuffer", "Array<Byte>");
        }
    };

    // cangjie->cangjieBody
    private final HashMap<String, String> returnBodyMap = new HashMap<>() {
        {
            put("Float64", "1.0");
            put("Bool", "true");
            put("String", "\"template\"");
            put("Unit", "");
            put("Array<Byte>", "[0x48]");
        }
    };

    /**
     * TSFuncSignature
     */
    public static class TSFuncSignature {
        private String funcName;
        private String returnType;
        private List<String> parameterTypeList = new ArrayList<>();
        private List<String> parameterNameList = new ArrayList<>();

        public String getFuncName() {
            return funcName;
        }

        public String getReturnType() {
            return returnType;
        }

        public List<String> getParameterTypeList() {
            return parameterTypeList;
        }

        public List<String> getParameterNameList() {
            return parameterNameList;
        }
    }

    /**
     * update d.ts function signature
     *
     * @param parameterTypes parameter type list
     * @param returnType     return type
     * @param funcName       d.ts function name
     */
    public void updateTSFuncSignature(List<String> parameterTypes, String returnType, String funcName) {
        tsFuncSignature.parameterTypeList = parameterTypes;
        tsFuncSignature.returnType = returnType;
        tsFuncSignature.funcName = funcName;
        for (int i = 1; i <= parameterTypes.size(); i++) {
            tsFuncSignature.getParameterNameList().add("arg" + i);
        }
    }

    /**
     * change d.ts function to Cangejie Function
     *
     * @return Cangjie function body
     */
    public String changeToCangjieGlobalFunc() {
        String cangjieReturnType = toCangjieType(tsFuncSignature.returnType);
        StringBuilder cangjieParameter = getCangjieParameter();
        String returnBodyContent = toCangjieBodyType(cangjieReturnType);
        String template = """

                @Interop[ArkTS]
                public func %s(%s): %s {
                    %s
                    %s
                }""";

        String cangjieFunctionContent = String.format(template,
                tsFuncSignature.funcName,
                cangjieParameter,
                cangjieReturnType,
                "// todo，template code:",
                returnBodyContent
        );
        return cangjieFunctionContent;
    }

    private String toCangjieType(String arkTSType) {
        if (typeMap.get(arkTSType) != null) {
            return typeMap.get(arkTSType);
        }
        return "";
    }

    private String toCangjieBodyType(String arkTSType) {
        if (returnBodyMap.get(arkTSType) != null) {
            return returnBodyMap.get(arkTSType);
        }
        return "";
    }

    private StringBuilder getCangjieParameter() {
        StringBuilder cangjieParameter = new StringBuilder();
        List<String> parameterTypeList = tsFuncSignature.getParameterTypeList();
        List<String> parameterNameList = tsFuncSignature.getParameterNameList();
        if (parameterTypeList == null || parameterTypeList.size() == 0 || parameterNameList == null
                || parameterNameList.size() == 0) {
            cangjieParameter = new StringBuilder(" ");
        } else {
            for (int i = 0; i < parameterTypeList.size(); i++) {
                String type = toCangjieType(parameterTypeList.get(i));
                cangjieParameter.append("".equals(parameterNameList.get(i))
                                ? generateCangjieParameter(parameterNameList, tsFuncSignature.getParameterNameList())
                                : parameterNameList.get(i))
                        .append(": ")
                        .append(type)
                        .append(",");
            }
            cangjieParameter = new StringBuilder(cangjieParameter.substring(0, cangjieParameter.length() - 1));
        }
        return cangjieParameter;
    }

    private String generateCangjieParameter(List<String> tsParameterNameList, List<String> parameterNameList) {
        StringBuilder cangjieParameter = new StringBuilder("tsPrm0");
        boolean isUnique = false;
        int index = 1;
        while (!isUnique) {
            isUnique = true;
            cangjieParameter.replace(cangjieParameter.length() - 1, cangjieParameter.length(), String.valueOf(index++));
            for (String s : parameterNameList) {
                if (cangjieParameter.toString().equals(s)) {
                    isUnique = false;
                    break;
                }
            }
        }
        tsParameterNameList.add(cangjieParameter.toString());
        return cangjieParameter.toString();
    }

    /**
     * check d.ts function parameter is valid
     *
     * @param tsParameterTypeList d.ts function parameter type list
     * @return isValid
     */
    public boolean isParameterListValid(List<String> tsParameterTypeList) {
        return tsParameterTypeList.stream().allMatch(typeMap::containsKey);
    }

    /**
     * check d.ts function return type is valid
     *
     * @param returnType return type
     * @return isValid
     */
    public boolean isReturnTypeValid(String returnType) {
        return typeMap.get(returnType) != null;
    }
}