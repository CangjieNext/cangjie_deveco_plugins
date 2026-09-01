/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;
import com.huawei.ideacj.language.psi.toplevel.expressionnode.CjExpression;

import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * CjDefaultParameter
 *
 * @since 2020-08-09
 */
public class CjDefaultParameter extends CangjieNamedElement implements CangjieGetID {
    private String parameterName = null;

    private String parameterType = null;

    private String defaultValue = null;

    public CjDefaultParameter(@NotNull ASTNode node) {
        super(node);
    }

    public String getParameterType() {
        initParameterInfo();
        return parameterType;
    }

    public String getParameterName() {
        initParameterInfo();
        return parameterName;
    }

    public String getDefaultValue() {
        if (defaultValue == null) {
            initParameterInfo();
        }
        return defaultValue;
    }

    private void initParameterInfo() {
        new CjDefaultParameterHandler().processAllElement(this.getChildren());
    }

    private class CjDefaultParameterHandler extends PsiElementInfoHandler {
        public CjDefaultParameterHandler() {
            initHandlers();
        }

        @Override
        public final void initHandlers() {
            handlers.put(
                    LeafPsiElement.class,
                    element -> {
                        if (CjPsiUtils.INSTANCE.isIdentifier(element)) {
                            parameterName = element.getText();
                        }
                    });
            handlers.put(CjType.class, element -> parameterType = element.getText());
            handlers.put(CjExpression.class, element -> defaultValue = element.getText().trim());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CjDefaultParameter that = (obj instanceof CjDefaultParameter) ? (CjDefaultParameter) obj : null;
        return Objects.equals(parameterName, that.parameterName)
                && Objects.equals(parameterType, that.parameterType)
                && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, parameterType, defaultValue);
    }
}
