/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;

import com.alibaba.fastjson2.JSONObject;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEvent;
import com.intellij.codeInsight.lookup.LookupListener;
import com.intellij.openapi.project.Project;

import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

/**
 * CangjieLookupListener
 *
 * @since 2024-11-10
 */
public class CangjieLookupListener implements LookupListener {
    private final Project project;

    public CangjieLookupListener(Project project) {
        this.project = project;
    }

    @Override
    public void itemSelected(@NotNull LookupEvent event) {
        Lookup lookup = event.getLookup();
        if (lookup != null) {
            LookupElement currentItem = lookup.getCurrentItem();
            if (currentItem == null) {
                return;
            }
            String item = currentItem.getLookupString();
            LanguageServerWrapper wrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
            if (wrapper == null) {
                return;
            }
            if (wrapper.getStatus() != ServerStatus.INITIALIZED) {
                return;
            }
            LanguageServer server = wrapper.getServer();
            JSONObject completionParams = new JSONObject();
            completionParams.put("label", item.trim());
            if (server instanceof Endpoint endpoint) {
                endpoint.notify("textDocument/trackCompletion", completionParams);
            }
        }
    }
}
