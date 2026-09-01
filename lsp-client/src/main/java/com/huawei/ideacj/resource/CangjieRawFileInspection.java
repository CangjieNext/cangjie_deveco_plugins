/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ideacj.resource.CangjieResourceUtil.PATH_REGEX;
import static com.huawei.ideacj.resource.CangjieResourceUtil.isResourceDecl;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createSourceEmptyErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createRawFileUnsupportedErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createRawFileInvalidErrorInfo;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.deveco.res.ohos.utils.ProjectUtils;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.language.visitor.CangjieBasePsiVisitor;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * CangjieRawFileInspection
 *
 * @since 2024/10/22
 */
public class CangjieRawFileInspection extends LocalInspectionTool {
    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new CangjieBasePsiVisitor() {
            @Override
            public void visitMacroTokens(CjMacroTokens cjElement) {
                if (!ProjectUtils.isHvigorProject(cjElement.getProject())) {
                    return;
                }
                ModuleModel module = ModuleUtils.findModuleModelByPsiElement(cjElement);
                if (!(module instanceof OhosModuleModel)
                        || !LspConfigUtils.isCangjieModule((OhosModuleModel) module)) {
                    return;
                }
                if (!isResourceDecl(cjElement, true)) {
                    return;
                }
                handleReferenceValue(holder, cjElement);
            }
        };
    }

    private void handleReferenceValue(ProblemsHolder holder, @NotNull CjMacroTokens cjElement) {
        String rawFileText = StringUtil.unquoteString(cjElement.getText());
        if (StringUtil.isEmpty(rawFileText)) {
            holder.registerProblem(cjElement.getParent(), createSourceEmptyErrorInfo());
            return;
        }
        String path = Optional.of(rawFileText)
                .filter(text -> PATH_REGEX.matcher(text).matches())
                .orElse("");
        if (StringUtil.isNotEmpty(path)) {
            holder.registerProblem(cjElement, createRawFileUnsupportedErrorInfo());
            return;
        }
        PsiReference[] references = cjElement.getReferences();
        boolean canAnyMatch = references.length == 0 || Arrays.stream(references)
                .anyMatch(ref -> ref.resolve() == null);
        if (canAnyMatch) {
            holder.registerProblem(cjElement, createRawFileInvalidErrorInfo());
        }
    }
}
