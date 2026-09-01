/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.CangjieNamedElement;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

/**
 * CjForeignMemberDeclaration
 *
 * @since 2025/12/31
 */
public class CjForeignMemberDeclaration extends CangjieNamedElement implements CangjieGetID {
    public CjForeignMemberDeclaration(@NotNull ASTNode node) {
        super(node);
    }
}
