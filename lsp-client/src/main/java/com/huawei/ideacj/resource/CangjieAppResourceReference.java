/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.ohos.reference.base.BaseResourceReference;
import com.huawei.deveco.res.ohos.reference.base.ReferenceResourceValue;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * CangjieAppResourceReference
 *
 * @since 2024/10/15
 */
public class CangjieAppResourceReference extends BaseResourceReference {
    public CangjieAppResourceReference(@NotNull PsiElement element,
                                       @Nullable TextRange range,
                                       @NotNull ReferenceResourceValue referenceResourceValue,
                                       @NotNull ModuleModel module) {
        super(element, range, referenceResourceValue, module);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        if (super.myElement instanceof CjMacroTokens) {
            return new MyManipulator()
                    .handleContentChange(super.myElement.getParent().getParent(),
                            super.getRangeInElement(), newElementName);
        }
        return super.handleElementRename(newElementName);
    }

    private static class MyManipulator extends AbstractElementManipulator<PsiElement> {
        @Override
        @Nullable
        public PsiElement handleContentChange(@NotNull PsiElement element,
                                              @NotNull TextRange range,
                                              String newContent) throws IncorrectOperationException {
            String originalContent = element.getText();
            TextRange noQuotes = this.getRangeInElement(element);
            String replacement = String.format(
                    Locale.ROOT,
                    "%s%s%s%s",
                    StringUtils.substring(originalContent, 0, 3),
                    StringUtils.substring(originalContent, noQuotes.getStartOffset() + 3, range.getStartOffset() + 3),
                    newContent,
                    StringUtils.substring(originalContent, originalContent.length() - 1)
            );
            PsiFile dummyFile = PsiFileFactory.getInstance(element.getProject())
                    .createFileFromText(CangJieLanguage.INSTANCE, replacement);
            return element.replace(dummyFile.getFirstChild());
        }
    }
}
