/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.contribution;

import static com.huawei.ideacj.lsp.utils.LspConfigUtils.containCangjieModule;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.symbol.WorkspaceSymbolProvider;
import org.wso2.lsp4intellij.utils.ApplicationUtils;

/**
 * Use lsp to find class names
 *
 * @since 2021-12-14
 */
public class CangjieClassNameContributor extends CangjieLSPSymbolContributor {
    private final WorkspaceSymbolProvider workspaceSymbolProvider = new WorkspaceSymbolProvider();

    @Override
    public void processElementsWithName(@NotNull String str,
                                        @NotNull Processor<? super NavigationItem> processor,
                                        @NotNull FindSymbolParameters findSymbolParameters) {
        if (!containCangjieModule(findSymbolParameters.getProject())) {
            return;
        }
        GlobalSearchScope scope = findSymbolParameters.getSearchScope().isSearchInLibraries()
                ? new EverythingGlobalScope() : findSymbolParameters.getSearchScope();
        ApplicationUtils.pool(() -> workspaceSymbolProvider.workspaceSymbols(str, findSymbolParameters.getProject(),
                        WorkspaceSymbolProvider.QueryKind.CLASSES).stream().filter(ni -> scope.accept(ni.getFile()))
                .forEach(processor::process));
    }

    @Override
    public String toString() {
        return "CangjieClassNameContributor{"
                + "workspaceSymbolProvider=" + workspaceSymbolProvider + '}';
    }
}