/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import com.huawei.ideacj.language.CangJieTypes;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;

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
public class CangjieCommenter implements CodeDocumentationAwareCommenter {
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

    @Override
    public IElementType getLineCommentTokenType() {
        return CangJieTypes.LINE_COMMENT;
    }

    @Override
    public IElementType getBlockCommentTokenType() {
        return CangJieTypes.BLOCK_COMMENT;
    }

    @Override
    public IElementType getDocumentationCommentTokenType() {
        return CangJieTypes.BLOCK_COMMENT;
    }

    @Nullable
    @Override
    public String getDocumentationCommentPrefix() {
        return "/**";
    }

    @Nullable
    @Override
    public String getDocumentationCommentLinePrefix() {
        return "*";
    }

    @Nullable
    @Override
    public String getDocumentationCommentSuffix() {
        return " */";
    }

    @Override
    public boolean isDocumentationComment(PsiComment element) {
        return element != null
                && element.getTokenType() == CangJieTypes.BLOCK_COMMENT
                && element.getText().startsWith("/**");
    }
}
