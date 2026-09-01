/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ideacj.resource.CangjieResourceUtil.getResourceInput;
import static com.huawei.ideacj.resource.CangjieResourceUtil.getWholeResourceDecl;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.reference.base.ReferenceResourceValue;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.ProcessingContext;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * CangjieResourceReferenceProvider
 *
 * @since 2024/10/10
 */
public class CangjieResourceReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement,
                                                           @NotNull ProcessingContext processingContext) {
        PsiElement terminalElement = psiElement;
        if (!(terminalElement instanceof LeafPsiElement) && psiElement.getFirstChild() != null) {
            terminalElement = psiElement.getFirstChild().getFirstChild();
        }
        if (!(terminalElement instanceof LeafPsiElement)) {
            return PsiReference.EMPTY_ARRAY;
        }
        String input = getResourceInput(terminalElement, false);
        if (!isValidAppReference(input)) {
            return PsiReference.EMPTY_ARRAY;
        }
        String[] values = input.split(CangjieResourceUtil.RESOURCE_SEPARATOR_REG_REGULAR);
        if (values.length != CangjieResourceUtil.RESOURCE_SIZE) {
            return PsiReference.EMPTY_ARRAY;
        }
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(psiElement);
        return getPsiReferences(getWholeResourceDecl(terminalElement), values, module,
                CangjieResourceUtil.getRangeInElement(psiElement));
    }

    private PsiReference[] getPsiReferences(@NotNull PsiElement element,
                                            String[] values,
                                            ModuleModel module,
                                            TextRange range) {
        String source = values[0];
        if (CangjieResourceUtil.APP.equals(source)) {
            String type = values[1];
            if (!ResourceType.contains(type)) {
                return PsiReference.EMPTY_ARRAY;
            }
            String resName = values[2];
            ReferenceResourceValue resourceValue = ReferenceResourceValue.reference(resName, false);
            resourceValue.setResourceType(type);
            CangjieAppResourceReference appReference =
                    new CangjieAppResourceReference(element, range, resourceValue, module);
            return new PsiReference[] {appReference};
        } else if (CangjieResourceUtil.SYS.equals(source)) {
            CangjieSysResourceReference systemReference =
                    new CangjieSysResourceReference(element, range, module);
            return new PsiReference[] {systemReference};
        } else {
            return PsiReference.EMPTY_ARRAY;
        }
    }

    private boolean isValidAppReference(@NotNull String name) {
        return Optional.of(name)
                .map(currName -> currName.split(CangjieResourceUtil.RESOURCE_SEPARATOR_REG_REGULAR))
                .filter(names -> names.length == CangjieResourceUtil.RESOURCE_SIZE
                        && (CangjieResourceUtil.APP.equals(names[0]) || CangjieResourceUtil.SYS.equals(names[0])))
                .isPresent();
    }
}
