/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi;

import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CjPsiUtils;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.ArrayList;
import java.util.List;

/**
 * CangjieGetID
 *
 * @since 2022-05-05
 */
public interface CangjieGetID {
    /**
     * Return ID PsiElement
     *
     * @param myElement PsiElement
     * @return List<PsiElement>
     */
    default List<PsiElement> getIDPsi(PsiElement myElement) {
        List<PsiElement> aimElement = new ArrayList<>();
        PsiElement[] elements = myElement.getChildren();
        for (PsiElement childElement : elements) {
            if (CjPsiUtils.INSTANCE.isIdentifier(childElement)) {
                aimElement.add(childElement);
            }
        }
        return aimElement;
    }

    /**
     * according to element set elementColor
     *
     * @return TextAttributesKey
     */
    @Nullable
    default TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.VARIABLE);
    }
}
