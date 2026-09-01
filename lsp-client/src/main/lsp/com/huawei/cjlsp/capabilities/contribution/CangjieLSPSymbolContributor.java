/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.contribution;

import static com.huawei.ideacj.lsp.utils.LspConfigUtils.containCangjieModule;

import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;

import io.netty.util.internal.StringUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.symbol.WorkspaceSymbolProvider;
import org.wso2.lsp4intellij.utils.ApplicationUtils;

import java.util.Optional;

/**
 * Cangjie LSPTypedHandler
 *
 * @since 2021-11-10
 */
public class CangjieLSPSymbolContributor implements ChooseByNameContributorEx {
    private final WorkspaceSymbolProvider workspaceSymbolProvider = new WorkspaceSymbolProvider();

    @Override
    public void processNames(@NotNull Processor<? super String> processor,
        @NotNull GlobalSearchScope globalSearchScope, @Nullable IdFilter idFilter) {
        if (!containCangjieModule(globalSearchScope.getProject())) {
            return;
        }
        String queryString = Optional.ofNullable(globalSearchScope.getProject())
                .map(project -> project.getUserData(ChooseByNamePopup.CURRENT_SEARCH_PATTERN))
                .orElse(StringUtil.EMPTY_STRING);
        processor.process(queryString);
    }

    @Override
    public void processElementsWithName(@NotNull String str, @NotNull Processor<? super NavigationItem> processor,
                                        @NotNull FindSymbolParameters findSymbolParameters) {
        if (!containCangjieModule(findSymbolParameters.getProject())) {
            return;
        }
        GlobalSearchScope scope = findSymbolParameters.getSearchScope().isSearchInLibraries()
                ? new EverythingGlobalScope() : findSymbolParameters.getSearchScope();
        ApplicationUtils.pool(() -> workspaceSymbolProvider.workspaceSymbols(str, findSymbolParameters.getProject(),
                        WorkspaceSymbolProvider.QueryKind.SYMBOLS)
                .stream()
                .filter(ni -> scope.accept(ni.getFile())).forEach(processor::process));
    }

    @Override
    public String toString() {
        return "CangjieLSPSymbolContributor{"
                + "workspaceSymbolProvider=" + workspaceSymbolProvider + '}';
    }
}