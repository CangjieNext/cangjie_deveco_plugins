/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.extendnode;

import com.huawei.ideacj.language.psi.CangjieNamedElement;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

/**
 * CjExtendMemberDeclaration
 *
 * @since 2022-02-18
 */
public class CjExtendMemberDeclaration extends CangjieNamedElement {
    public CjExtendMemberDeclaration(@NotNull ASTNode node) {
        super(node);
    }
}
