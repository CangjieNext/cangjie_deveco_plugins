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

import com.huawei.ace.ohos.reference.EtsResourcesReferenceUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.ohos.json.gotodeclaration.ResourceJsonToPsiResolver;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.language.psi.othersnode.CjLiteralConstant;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * CangjieResourceGotoDeclarationHandler
 *
 * @since 2024/10/10
 */
public class CangjieResourceGotoDeclarationHandler implements GotoDeclarationHandler {
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor) {
        if (psiElement == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(psiElement);
        if (!(module instanceof OhosModuleModel)
                || !LspConfigUtils.isCangjieModule((OhosModuleModel) module)) {
            return PsiElement.EMPTY_ARRAY;
        }
        String input = getResourceInput(psiElement, false);
        // input element is leaf element
        if (!isValidAppReference(input, psiElement)) {
            return PsiElement.EMPTY_ARRAY;
        }
        return handleAppResourceValue(getWholeResourceDecl(psiElement), input);
    }

    @NotNull
    private PsiElement[] handleAppResourceValue(@NotNull PsiElement element, @NotNull String elementText) {
        // 将形如app.media.icon的字符串处理成$media:icon，使用harmony editor的功能来跳转
        return Optional.of(elementText)
                .map(this::toHosName)
                .map(name -> ResourceJsonToPsiResolver.INSTANCE.getReferenceGotoDeclarationTargets(name, element))
                .orElse(PsiElement.EMPTY_ARRAY);
    }

    // 将形如app.media.icon的字符串处理成$media:icon
    private String toHosName(@NotNull String name) {
        String mainName = StringUtils.substring(name, CangjieResourceUtil.PREFIX_APP.length());
        int firstIndexOfDot = mainName.indexOf(CangjieResourceUtil.RESOURCE_SEPARATOR);
        if (firstIndexOfDot < 0) {
            return StringUtils.EMPTY;
        }
        return EtsResourcesReferenceUtil.DOLLAR + mainName.substring(0, firstIndexOfDot)
                + EtsResourcesReferenceUtil.COLON + mainName.substring(firstIndexOfDot + 1);
    }

    private boolean isValidAppReference(@NotNull String name, PsiElement psiElement) {
        String text = psiElement.getText();
        if (psiElement.getParent() instanceof CjLiteralConstant && !StringUtils.isEmpty(text)) {
            text = text.substring(1);
        }
        String finalText = text;
        return Optional.of(name)
                .map(currName -> currName.split(CangjieResourceUtil.RESOURCE_SEPARATOR_REG_REGULAR))
                .filter(names -> names.length == CangjieResourceUtil.RESOURCE_SIZE
                        && CangjieResourceUtil.APP.equals(names[0]) && names[2].equals(finalText))
                .isPresent();
    }
}
