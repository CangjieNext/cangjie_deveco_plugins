/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage.diagnostic;

import com.huawei.ace.crosslanguage.CrossLanguageDiagnosticProvider;
import com.huawei.ace.language.psi.JavaScriptNamedElement;
import com.huawei.ace.lsp.extensions.CheckCppElementResult;
import com.huawei.ace.lsp.extensions.client.CrossLanguageGotoImplementationProvider;
import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;
import java.util.Objects;

/**
 * CangjieCrossLanguageDiagnosticProvider
 *
 * @since 2025/12/23
 */
public class CangjieCrossLanguageDiagnosticProvider implements CrossLanguageDiagnosticProvider {
    private static final String ARK_INTEROP_API_SO_NAME = "libark_interop_api.so";

    private static final String CANGJIE_SRC_PATH = "/src/main/cangjie/";

    @Override
    public boolean isValidModule(PsiFile dtsPsiFile) {
        return isCangjieDts(dtsPsiFile);
    }

    @Override
    public boolean isFunctionExist(@NotNull String soName, @NotNull PsiElement psiElement,
        @NotNull CheckCppElementResult cppResult) {
        if (ARK_INTEROP_API_SO_NAME.equals(soName)) {
            return true;
        }

        for (CrossLanguageGotoImplementationProvider provider : CrossLanguageGotoImplementationProvider
            .PROVIDER_EXTENSION_LIST.getExtensionList()) {
            Pair<Boolean, List<Location>> locations = provider.getLocationInfo(psiElement, cppResult,
                psiElement.getProject());
            if (locations != null && locations.first && !locations.second.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerProblem(ProblemsHolder holder, JavaScriptNamedElement psiElement,
        CheckCppElementResult cppResult, List<String> parameterTypes, String returnType) {
        String message = CangjieBundle.message("cross.language.function.has.no.native.implementation.general",
            Objects.requireNonNull(psiElement.getName()));
        holder.registerProblem(psiElement, message,
            new GenerateImplementationQuickFix(psiElement, cppResult, psiElement.getProject(), parameterTypes,
                returnType));
    }

    private boolean isCangjieDts(PsiFile dtsPsiFile) {
        if (dtsPsiFile == null) {
            return false;
        }
        String dtsFileUri = FileUtils.VFSToURI(dtsPsiFile.getVirtualFile());
        if (dtsFileUri == null) {
            return false;
        }
        return dtsFileUri.contains(CANGJIE_SRC_PATH);
    }
}
