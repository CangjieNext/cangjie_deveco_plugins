/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import com.intellij.lang.Commenter;

import org.jetbrains.annotations.Nullable;

/**
 * Place the cursor at the website line.
 * Select Code | Comment with Line Comment.
 * The line is converted to a comment.
 * Select Code | Comment with Line Comment again,
 * and the comment is converted back to active code.
 *
 * @author x30009917
 * @since 2021-06-2
 */
public class CangjieCommenter implements Commenter {
    @Nullable
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Nullable
    @Override
    public String getBlockCommentPrefix() {
        return "/**";
    }

    @Nullable
    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }
}