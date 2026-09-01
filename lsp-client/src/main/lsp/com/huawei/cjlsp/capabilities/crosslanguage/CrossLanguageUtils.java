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
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
 * CangjieLspUtils
 *
 * @since 2025/09/02
 */
public class CrossLanguageUtils {
    private static final Pattern DYNAMIC_BINARY_PATTERN = Pattern.compile("^lib(.*?)\\.so$");

    private static final Pattern LIB_SO_PATTERN = Pattern.compile("^lib(.+?)\\.so$");

    /**
     * get rename item by lsp server
     *
     * @param source  ets element
     * @param result  element result
     * @param project project
     * @return List<CrossLanguageRegisterItem> item list
     */
    public static List<CrossLanguageRegisterItem> getRenameItem(PsiElement source, CheckCppElementResult result,
                                                                Project project) {
        String resultName = result.getPackageName();
        if (StringUtils.isEmpty(resultName)) {
            return new ArrayList<>();
        }
        Matcher matcher = DYNAMIC_BINARY_PATTERN.matcher(resultName);
        if (!matcher.find()) {
            return new ArrayList<>();
        }
        if (StringUtils.isEmpty(result.getInterfaceName())) {
            return new ArrayList<>();
        }
        String[] interfaceNameArray = result.getInterfaceName().split("\\.");
        String interfaceName;
        String outerName;
        if (interfaceNameArray.length > 1) {
            interfaceName = interfaceNameArray[1];
            outerName = interfaceNameArray[0];
        } else {
            interfaceName = interfaceNameArray[0];
            outerName = "";
        }
        if (StringUtils.isEmpty(matcher.group(1)) || StringUtils.isEmpty(interfaceName)) {
            return new ArrayList<>();
        }
        boolean isCombined = false;
        ModuleModel module = ApplicationManager.getApplication().runReadAction((Computable<ModuleModel>) () -> {
            return ModuleUtils.findModuleModelByPsiElement(source);
        });
        if (isDynamicCombined(module)) {
            isCombined = true;
        }
        CrossLanguageDefinitionParam param = new CrossLanguageDefinitionParam(matcher.group(1), interfaceName,
            outerName, isCombined);
        LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION, FileUtils.projectToUri(project));
        if (lspWrapper == null || !(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            return new ArrayList<>();
        }
        CompletableFuture<List<CrossLanguageRegisterItem>> request = requestManager.crossLanguageRegister(param);
        if (request == null) {
            return new ArrayList<>();
        }
        List<CrossLanguageRegisterItem> definitions = new ArrayList<>();
        try {
            definitions = request.get(50000, TimeUnit.MILLISECONDS); // 50s
            lspWrapper.notifySuccess(DEFINITION);
            if (CollectionUtils.isEmpty(definitions)) {
                return new ArrayList<>();
            }
        } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
            requestManager.checkStatus();
            lspWrapper.notifyFailure(DEFINITION);
        }
        return definitions;
    }

    /**
     * extract package name from lib so name
     *
     * @param libSoName lib so name
     * @return String package name
     */
    public static String extractPackageName(String libSoName) {
        if (libSoName == null || libSoName.isEmpty()) {
            return "";
        }
        String packageName = "";
        Matcher matcher = LIB_SO_PATTERN.matcher(libSoName);
        if (matcher.matches()) {
            packageName = matcher.group(1);
        }
        return packageName;
    }
}


