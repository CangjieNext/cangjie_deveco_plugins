/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend;

import com.huawei.cjlsp.capabilities.crosslanguage.CrossLanguageRegisterItem;
import com.huawei.cjlsp.capabilities.exports.ExportsItem;
import com.huawei.cjlsp.capabilities.exports.ExportsNameParam;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieFileRefactorParam;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUpdateInfo;
import com.huawei.ideacj.lsp.extend.params.OverridableMethods;
import com.huawei.ideacj.lsp.extend.params.OverrideMethodsParams;
import com.huawei.cjlsp.capabilities.crosslanguage.CrossLanguageDefinitionParam;

import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * No add existing rpc (initialize etc.) must to care about, only can add new rpc message
 *
 * @author rice
 * @since 2020-06-08
 */
public interface ExtendLanguageServer extends LanguageServer {
    /**
     * Override Methods
     *
     * @param params OverrideMethodsParams
     * @return CompletableFuture<List<OverridableMethods>>
     */
    @JsonRequest(value = "codeGenerator/overrideMethods", useSegment = false)
    default CompletableFuture<List<OverridableMethods>> requestOverrideMethods(OverrideMethodsParams params) {
        throw new UnsupportedOperationException();
    }

    /**
     * The textDocument/crossJumpDefinition request
     *
     * @param params provide current query name
     * @return CompletableFuture<List <Location>>
     */
    @JsonRequest(value = "textDocument/crossLanguageDefinition", useSegment = false)
    default CompletableFuture<List<Location>> crossLanguageDefinition(CrossLanguageDefinitionParam params) {
        throw new UnsupportedOperationException();
    }

    /**
     * The textDocument/findFileReferences request
     *
     * @param params provide current query name
     * @return CompletableFuture<List <Location>>
     */
    @JsonRequest(value = "textDocument/findFileReferences", useSegment = false)
    default CompletableFuture<List<Location>> findFileReferences(DocumentLinkParams params) {
        throw new UnsupportedOperationException();
    }

    /**
     * The textDocument/exportsName request
     *
     * @param params provide current query name
     * @return CompletableFuture<ExportsItem>
     */
    @JsonRequest(value = "textDocument/exportsName", useSegment = false)
    default CompletableFuture<ExportsItem> exportsName(ExportsNameParam params) {
        throw new UnsupportedOperationException();
    }

    /**
     * The textDocument/crossLanguageRegister request
     *
     * @param params provide current query name
     * @return CompletableFuture<List <CrossLanguageRegisterItem>>
     */
    @JsonRequest(value = "textDocument/crossLanguageRegister", useSegment = false)
    default CompletableFuture<List<CrossLanguageRegisterItem>> crossLanguageRegister(
            CrossLanguageDefinitionParam params) {
        throw new UnsupportedOperationException();
    }

    /**
     * The textDocument/fileRefactor request
     *
     * @param params provide current query name
     * @return CompletableFuture<CangjieMoveUpdateInfo>
     */
    @JsonRequest(value = "textDocument/fileRefactor", useSegment = false)
    default CompletableFuture<CangjieMoveUpdateInfo> fileRefactor(CangjieFileRefactorParam params) {
        throw new UnsupportedOperationException();
    }
}
