/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.compileunitnode.CjPreamble;
import com.huawei.ideacj.language.psi.othersnode.CjArrowParameters;
import com.huawei.ideacj.language.psi.othersnode.CjTupleType;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassFinalizer;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOverloadedOperators;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.refactor.extract.CangjieExtractMethodHandler;

import com.intellij.lang.Language;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieRefactoringSupportProvider
 *
 * @since 2025-06-10
 */
public class CangjieRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public boolean isAvailable(@NotNull PsiElement context) {
        Language language = context.getContainingFile().getLanguage();
        return language instanceof CangJieLanguage;
    }

    @Nullable
    @Override
    public RefactoringActionHandler getExtractMethodHandler() {
        return new CangjieExtractMethodHandler();
    }

    @Override
    public boolean isInplaceIntroduceAvailable(@NotNull PsiElement element, PsiElement context) {
        return true;
    }

    @Override
    public boolean isSafeDeleteAvailable(@NotNull PsiElement tokenish) {
        Editor editor = FileEditorManager.getInstance(tokenish.getProject()).getSelectedTextEditor();
        if (editor == null) {
            return false;
        }
        // 由于传进来的元素是这一行的元素，需要精确到光标所在的元素
        int offset = editor.getCaretModel().getOffset();
        PsiFile psiFile = PsiDocumentManager.getInstance(tokenish.getProject()).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return false;
        }
        PsiElement psiElement = psiFile.findElementAt(offset);
        CJPsiNode element = PsiTreeUtil.getParentOfType(psiElement, CJPsiNode.class);
        if (element == null) {
            return false;
        }

        // init ~init main operator重载可以安全删除
        if (isExtraSupported(element)) {
            return true;
        }

        // 其他非identifier节点不能安全删除
        if (!(psiElement instanceof CJPsiLeafNode leafNode && leafNode.getElementType() == CangJieTypes.IDENTIFIER)) {
            return false;
        }

        // tupleType返回值类型不支持删除
        if (psiElement.getParent().getParent() instanceof CjTupleType
                || psiElement.getParent().getParent() instanceof CjArrowParameters) {
            return false;
        }

        // import包名和类名当前不能findUsage，暂不支持安全删除
        CJPsiNode parent = PsiTreeUtil.getParentOfType(element, CjPreamble.class);
        if (parent != null) {
            return false;
        }

        return true;
    }

    private boolean isExtraSupported(PsiElement element) {
        return element instanceof CjClassFinalizer || element instanceof CjClassInit
                || element instanceof CjMainDefinition || element instanceof CjOverloadedOperators
                || element instanceof CjStructInit;
    }
}
