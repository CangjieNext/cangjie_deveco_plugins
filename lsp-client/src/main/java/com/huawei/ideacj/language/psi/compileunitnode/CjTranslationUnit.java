/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.compileunitnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

/**
 * The base class of the duplicated node class is PsiElementBase, which
 * implements the PsiElement interface.
 * The caller can directly treat this node as a normal PsiElement.
 * In addition, since the replicated node inherits from ASTWrapperPsiElement,
 * Therefore, ASTNode is an attribute of this class that can be used to traverse
 * the AST tree at any time
 *
 * @since 2021/07/20
 */
public class CjTranslationUnit extends CJPsiNode {
    public CjTranslationUnit(@NotNull ASTNode node) {
        super(node);
    }
}
