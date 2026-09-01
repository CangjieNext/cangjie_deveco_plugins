/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel.classnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

/**
 * ClassModifierList
 *
 * @since 2021-07-31
 */
public class CjClassModifierList extends CJPsiNode {
    public CjClassModifierList(@NotNull ASTNode node) {
        super(node);
    }
}
