/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.othersnode;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

/**
 * CjLiteralConstant
 *
 * @since 2024/10/26
 */
public class CjLiteralConstant extends CJPsiNode implements CangjieGetID {
    public CjLiteralConstant(@NotNull ASTNode node) {
        super(node);
    }
}
