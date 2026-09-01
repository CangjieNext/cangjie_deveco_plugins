/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.listeners.DocumentListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cangjie LSP Doument Listener, send didchange infomation to lspserver
 *
 * @since 2025-09-17
 */
public class CangjieDocumentListenerImpl extends DocumentListenerImpl {
    private Map<Document, List<TextDocumentContentChangeEvent>> bulkUpdatingDocuments = new ConcurrentHashMap<>();

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (!checkDocumentEnabled()) {
            return;
        }
        Document document = event.getDocument();
        if (document.isInBulkUpdate()) {
            bulkUpdatingDocuments.get(document).add(documentEventManager.createContentChangeEvent(event));
        } else {
            documentEventManager.documentChanged(event, false);
        }
    }

    @Override
    public void bulkUpdateStarting(@NotNull Document document) {
        bulkUpdatingDocuments.put(document, new ArrayList<>());
    }

    @Override
    public void bulkUpdateFinished(@NotNull Document document) {
        List<TextDocumentContentChangeEvent> bulkChangeEvents = bulkUpdatingDocuments.remove(document);
        if (CollectionUtils.isNotEmpty(bulkChangeEvents)) {
            documentEventManager.documentChanged(bulkChangeEvents, false);
        }
    }
}
