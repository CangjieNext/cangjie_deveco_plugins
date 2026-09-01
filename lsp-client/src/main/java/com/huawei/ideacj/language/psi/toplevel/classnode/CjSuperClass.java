/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.Optional;

/**
 * SuperClass
 *
 * @since 2021-07-31
 */
public class CjSuperClass extends CJPsiNode implements CangjieGetID {
    public CjSuperClass(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return ID PsiElement
     *
     * @return PsiElement
     */
    public Optional<PsiElement> getIDPsi() {
        PsiElement element = PsiTreeUtil.getChildOfType(this, CjClassType.class);
        if (element == null) {
            return Optional.empty();
        }
        PsiElement identifier = element.getFirstChild();
        if (identifier != null) {
            return Optional.of(identifier);
        }
        return Optional.empty();
    }

    @Override
    @Nullable
    public TextAttributesKey getColor() {
        return CangjieSemanticTokenHighlighter.colorOf(SemanticToken.CLASS);
    }
}
