/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.Objects;

/**
 * CjNamedParameter
 *
 * @since 2020-08-09
 */
public class CjNamedParameter extends CangjieNamedElement implements CangjieGetID {
    private String parameterName = null;

    private String parameterType = null;

    public CjNamedParameter(@NotNull ASTNode node) {
        super(node);
    }

    public String getParameterName() {
        initParameterInfo();
        return parameterName;
    }

    public String getParameterType() {
        if (parameterType == null) {
            initParameterInfo();
        }
        return parameterType;
    }

    private void initParameterInfo() {
        PsiElement[] elements = this.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                parameterName = childElement.getText().trim();
                break;
            }
        }
        new CjNamedParameterHandler().processAllElement(this.getChildren());
    }

    private class CjNamedParameterHandler extends PsiElementInfoHandler {
        public CjNamedParameterHandler() {
            initHandlers();
        }

        @Override
        public final void initHandlers() {
            handlers.put(
                    CjIdentifier.class,
                    element -> {
                        if (CjPsiUtils.INSTANCE.isIdentifier(element)) {
                            parameterName = element.getText();
                        }
                    });
            handlers.put(CjType.class, element -> parameterType = element.getText());
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
        CjNamedParameter that = (obj instanceof CjNamedParameter) ? (CjNamedParameter) obj : null;
        return Objects.equals(parameterName, that == null ? null : that.parameterName)
                && Objects.equals(parameterType, that == null ? null : that.parameterType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, parameterType);
    }

    @Override
    public TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }
}
