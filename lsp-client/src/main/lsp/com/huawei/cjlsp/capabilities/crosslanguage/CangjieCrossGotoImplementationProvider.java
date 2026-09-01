/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.isDynamicCombined;
import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;
import static org.wso2.lsp4intellij.requests.Timeouts.DEFINITION;

import com.huawei.ace.lsp.extensions.CheckCppElementResult;
import com.huawei.ace.lsp.extensions.client.CrossLanguageGotoImplementationProvider;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjieCrossGotoImplementationProvider
 *
 * @since 2025/06/23
 */
public class CangjieCrossGotoImplementationProvider implements CrossLanguageGotoImplementationProvider {
    private static final Pattern DYNAMIC_BINARY_PATTERN = Pattern.compile("^lib(.*?)\\.so$");

    @Override
    public boolean isTargetElement(PsiElement source, CheckCppElementResult result, Project project) {
        if (result == null || StringUtils.isEmpty(result.getDtsPath())) {
            return false;
        }
        return result.getDtsPath().contains("src/main/cangjie");
    }

    @Override
    public Pair<Boolean, List<Location>> getLocationInfo(PsiElement source, CheckCppElementResult result,
                                                         Project project) {
        String resultName = result.getPackageName();
        if (StringUtils.isEmpty(resultName)) {
            return Pair.create(true, new ArrayList<>());
        }
        Matcher matcher = DYNAMIC_BINARY_PATTERN.matcher(resultName);
        if (!matcher.find()) {
            return Pair.create(true, new ArrayList<>());
        }
        String packageName = matcher.group(1);
        if (StringUtils.isEmpty(result.getInterfaceName())) {
            return Pair.create(true, new ArrayList<>());
        }
        String[] interfaceName = result.getInterfaceName().split("\\.");
        String name;
        String outerName;
        if (interfaceName.length > 1) {
            name = interfaceName[1];
            outerName = interfaceName[0];
        } else {
            name = interfaceName[0];
            outerName = "";
        }
        if (StringUtils.isEmpty(packageName) || StringUtils.isEmpty(name)) {
            return Pair.create(true, new ArrayList<>());
        }
        boolean isCombined = false;
        ModuleModel module = ApplicationManager.getApplication().runReadAction((Computable<ModuleModel>) () -> {
            return ModuleUtils.findModuleModelByPsiElement(source);
        });
        if (isDynamicCombined(module)) {
            isCombined = true;
        }
        CrossLanguageDefinitionParam param = new CrossLanguageDefinitionParam(packageName, name, outerName, isCombined);
        LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION, FileUtils.projectToUri(project));
        if (lspWrapper == null || !(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            return Pair.create(true, new ArrayList<>());
        }
        CompletableFuture<List<Location>> request = requestManager.crossLanguageDefinition(param);
        if (request == null) {
            return Pair.create(true, new ArrayList<>());
        }
        List<Location> definitions = new ArrayList<>();
        try {
            definitions = request.get(50000, TimeUnit.MILLISECONDS); // 50s
            lspWrapper.notifySuccess(DEFINITION);
            if (CollectionUtils.isEmpty(definitions)) {
                return Pair.create(true, new ArrayList<>());
            }
        } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
            requestManager.checkStatus();
            lspWrapper.notifyFailure(DEFINITION);
        }
        return Pair.create(true, definitions);
    }
}
