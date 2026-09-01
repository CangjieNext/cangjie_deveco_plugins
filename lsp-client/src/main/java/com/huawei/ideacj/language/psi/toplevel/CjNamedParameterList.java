/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * NamedParameterList
 *
 * @since 2021-07-31
 */
public class CjNamedParameterList extends CJPsiNode {
    private CjNamedParameterListInfo cjNamedParameterListInfo = null;

    public CjNamedParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public CjNamedParameterListInfo getCjNamedParameterListInfo() {
        initCjNamedParameterListInfo();
        return cjNamedParameterListInfo;
    }

    private void initCjNamedParameterListInfo() {
        cjNamedParameterListInfo = new CjNamedParameterListInfo();
        new CjNamedParameterListHandler().processAllElement(this.getChildren());
    }

    private class CjNamedParameterListHandler extends PsiElementInfoHandler {
        public CjNamedParameterListHandler() {
            initHandlers();
        }

        @Override
        public final void initHandlers() {
            cjNamedParameterListInfo = new CjNamedParameterListInfo();
            handlers.put(
                    CjNamedParameter.class,
                    element -> {
                        if (element instanceof CjNamedParameter) {
                            CjNamedParameter parameter = (CjNamedParameter) element;
                            cjNamedParameterListInfo.addNamedParameters(
                                    parameter.getParameterName(), parameter.getParameterType());
                            cjNamedParameterListInfo.addTypeParams(
                                    parameter.getParameterName(), parameter.getParameterType());
                        }
                    });
            handlers.put(
                    CjDefaultParameter.class,
                    element -> {
                        if (element instanceof CjDefaultParameter) {
                            CjDefaultParameter parameter = (CjDefaultParameter) element;
                            String name = parameter.getParameterName();
                            cjNamedParameterListInfo.addDefaultParameterWithType(name, parameter.getParameterType());
                            cjNamedParameterListInfo.addDefaultParameterWithValue(name, parameter.getDefaultValue());
                            cjNamedParameterListInfo.addTypeParams(
                                    name, parameter.getParameterType(), parameter.getDefaultValue());
                        }
                    });
        }
    }

    /**
     * NamedParameterList info
     *
     * @since 2021-08-09
     */
    public static class CjNamedParameterListInfo {
        private final LinkedHashMap<String, String> namedParameters = new LinkedHashMap<>();

        private final HashMap<String, String> defaultParametersWithType = new HashMap<>();

        private final HashMap<String, String> defaultParametersWithValue = new HashMap<>();

        private final ArrayList<String> typeParams = new ArrayList<String>();

        /**
         * add addTypeParams
         *
         * @param  paramName string
         * @param  paramType string
         */
        public void addTypeParams(String paramName, String paramType) {
            typeParams.add(paramName + ": " + paramType);
        }

        /**
         * add addTypeParams
         *
         * @param  paramName String
         * @param  paramType String
         * @param  paramValue String
         */
        public void addTypeParams(String paramName, String paramType, String paramValue) {
            typeParams.add(paramName + ": " + paramType + "=" + paramValue);
        }

        /**
         * add param name and value to defaultParams value map
         *
         * @param paramName name
         * @param paramValue default value
         */
        public void addDefaultParameterWithValue(String paramName, String paramValue) {
            defaultParametersWithValue.put(paramName, paramValue);
        }

        /**
         * add param name and type to defaultParams type map
         *
         * @param paramName name
         * @param paramType type
         */
        public void addDefaultParameterWithType(String paramName, String paramType) {
            defaultParametersWithType.put(paramName, paramType);
        }

        /**
         * add param name and type to namedParams map
         *
         * @param paramName name
         * @param paramType type
         */
        public void addNamedParameters(String paramName, String paramType) {
            namedParameters.put(paramName, paramType);
        }

        public HashMap<String, String> getDefaultParametersWithValue() {
            return defaultParametersWithValue;
        }

        public HashMap<String, String> getDefaultParametersWithType() {
            return defaultParametersWithType;
        }

        public LinkedHashMap<String, String> getNamedParameters() {
            return namedParameters;
        }

        /**
         * getTypeParams
         *
         * @return typeParams
         */
        public ArrayList<String> getTypeParams() {
            return typeParams;
        }
    }
}
