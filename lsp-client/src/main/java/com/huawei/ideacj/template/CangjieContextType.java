/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.template;

import static com.huawei.ace.language.template.TemplateContextUtils.getContextElement;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.importnode.CjImportList;
import com.huawei.ideacj.language.psi.othersnode.CjStringLiteral;
import com.huawei.ideacj.lsp.utils.CangjiePsiUtils;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieContextType
 *
 * @since 2025-11-20
 */
public class CangjieContextType extends TemplateContextType {
    protected CangjieContextType(@NlsContexts.Label @NotNull String presentableName) {
        super(presentableName);
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        PsiFile file = templateActionContext.getFile().getOriginalFile();
        boolean isCangjieFile = CangjiePsiUtils.isCangjieFile(file);
        if (!isCangjieFile) {
            return false;
        }
        PsiElement element = getContextElement(templateActionContext);
        if (element == null || element instanceof PsiWhiteSpace) {
            return false;
        }
        CjImportList parent = PsiTreeUtil.getParentOfType(element, CjImportList.class);
        if (parent != null) {
            return false;
        }
        return !isInStringOrComment(element);
    }

    /**
     * 当前元素是否是在string或者注释中
     *
     * @param element 当前元素
     * @return 是否是在string或者注释中
     */
    public static boolean isInStringOrComment(@NotNull PsiElement element) {
        // 1. 优先检查当前节点是否就是 String 或 Comment，或者是其子节点
        if (PsiTreeUtil.getNonStrictParentOfType(element, PsiComment.class, CjStringLiteral.class) != null) {
            return true;
        }

        // 2. 处理 OriginalElement 的特定类型检查 (例如 Editorfold)
        PsiElement original = element.getOriginalElement();
        if (original instanceof CJPsiLeafNode) {
            IElementType type = ((CJPsiLeafNode) original).getElementType();
            if (type == CangJieTypes.EDITOR_FOLD_START) {
                return true;
            }
        }

        // 3. 处理特殊的父级兄弟节点逻辑 (针对特定的注释附着情况)
        PsiElement parent = element.getParent();
        if (parent != null) {
            PsiElement prevSibling = parent.getPrevSibling();
            if (prevSibling instanceof PsiComment) {
                return true;
            }
        }

        return false;
    }
}
