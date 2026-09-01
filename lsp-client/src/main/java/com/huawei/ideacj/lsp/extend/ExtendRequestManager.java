/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend;

import static com.huawei.ideacj.lsp.utils.LspConfigUtils.removeEditorListeners;
import static org.wso2.lsp4intellij.utils.FileUtils.getAllOpenedEditors;

import com.huawei.cjlsp.capabilities.crosslanguage.CrossLanguageDefinitionParam;
import com.huawei.cjlsp.capabilities.crosslanguage.CrossLanguageRegisterItem;
import com.huawei.cjlsp.capabilities.exports.ExportsItem;
import com.huawei.cjlsp.capabilities.exports.ExportsNameParam;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieFileRefactorParam;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUpdateInfo;
import com.huawei.ideacj.highlightersetting.CangjieSemanticHighlight;
import com.huawei.ideacj.lsp.extend.params.OverridableMethods;
import com.huawei.ideacj.lsp.extend.params.OverrideMethodsParams;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.lsp.utils.LSPThreadPoolManager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.openapi.editor.Editor;

import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 功能描述
 *
 * @author rice
 * @since 2020-06-08
 */
public class ExtendRequestManager extends DefaultRequestManager {
    private volatile boolean isRestarting = false;
    private ExtendLanguageServer server;
    private int traceCount = TraceUtils.ActionThreshold.SEMANTICS_HIGHLIGHT;

    /**
     * ExtendedRequestManager
     *
     * @param wrapper LanguageServerWrapper
     * @param server LanguageServer
     * @param client LanguageClient
     * @param serverCapabilities ServerCapabilities
     */
    public ExtendRequestManager(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client,
                                ServerCapabilities serverCapabilities) {
        super(wrapper, server, client, serverCapabilities);
        if (server instanceof ExtendLanguageServer) {
            this.server = (ExtendLanguageServer) server;
        }
    }

    /**
     * set isRestarting
     *
     * @param restarting boolean
     */
    public void setRestarting(boolean restarting) {
        isRestarting = restarting;
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        if (traceCount >= TraceUtils.ActionThreshold.SEMANTICS_HIGHLIGHT) {
            TraceUtils.trace(TraceUtils.Action.SEMANTICS_HIGHLIGHT);
            traceCount = 0;
        } else {
            traceCount++;
        }
        return super.semanticTokensFull(params);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        CangjieSemanticHighlight.removeHighlighter(params.getTextDocument().getUri());
        super.didClose(params);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        return super.initialize(params);
    }

    @Override
    public void initialized(InitializedParams params) {
        super.initialized(params);
        LanguageServerWrapper wrapper = getWrapper();
        if (wrapper != null && wrapper.getStatus() == ServerStatus.STARTED) {
            List<Editor> allOpenedEditors = getAllOpenedEditors(wrapper.getProject());
            allOpenedEditors.forEach(IntellijLanguageClient::editorOpened);
            wrapper.stopTaskForStartServer(true);
        }
    }

    @Override
    public CompletableFuture<List<CallHierarchyOutgoingCall>>
        callHierarchyOutgoingCalls(CallHierarchyOutgoingCallsParams params) {
        if (this.checkStatus()) {
            LanguageServerWrapper wrapper = getWrapper();
            if (wrapper == null || wrapper.getServer() == null) {
                return null;
            }
            return wrapper.getServer().getTextDocumentService().callHierarchyOutgoingCalls(params);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ExtendRequestManager{" + "server=" + server + '}';
    }

    @Override
    public boolean checkStatus(boolean isNeedCheckStatusOfUpdatingModules) {
        synchronized (this.getWrapper()) {
            if (!getWrapper().isServerProcessAlive() && !isRestarting) {
                isRestarting = true;
                LSPThreadPoolManager.pool(() -> {
                    removeEditorListeners(this.getWrapper());
                    getWrapper().crashed(new Exception());
                    isRestarting = false;
                    if (this.getWrapper().getCrashCount() > 5) {
                        TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
                    }
                    CrashLogPackager.packageLogFiles();
                });
                return false;
            }
        }
        return super.checkStatus(isNeedCheckStatusOfUpdatingModules);
    }

    /**
     * Request Override Method
     *
     * @param params OverrideMethodsParams
     * @return CompletableFuture<List<OverridableMethods>>
     */
    @Nullable
    public CompletableFuture<List<OverridableMethods>> requestOverrideMethods(OverrideMethodsParams params) {
        if (checkStatus()) {
            try {
                return server.requestOverrideMethods(params);
            } catch (UnsupportedOperationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * cross language definition
     *
     * @param params cross language definition request param
     * @return definition
     */
    @Nullable
    public CompletableFuture<List<Location>> crossLanguageDefinition(CrossLanguageDefinitionParam params) {
        if (checkStatus()) {
            try {
                return server.crossLanguageDefinition(params);
            } catch (UnsupportedOperationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * find file references
     *
     * @param params find file reference request param
     * @return definition
     */
    @Nullable
    public CompletableFuture<List<Location>> findFileReference(DocumentLinkParams params) {
        if (!checkStatus()) {
            return null;
        }
        try {
            return server.findFileReferences(params);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * get exports name for register function
     *
     * @param params exports name param
     * @return exports function name
     */
    @Nullable
    public CompletableFuture<ExportsItem> exportsName(ExportsNameParam params) {
        if (checkStatus()) {
            try {
                return server.exportsName(params);
            } catch (UnsupportedOperationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * get cross language register item
     *
     * @param params cross language definition request param
     * @return cross language register item
     */
    @Nullable
    public CompletableFuture<List<CrossLanguageRegisterItem>> crossLanguageRegister(
            CrossLanguageDefinitionParam params) {
        if (checkStatus()) {
            try {
                return server.crossLanguageRegister(params);
            } catch (UnsupportedOperationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * find file references for move refactor
     *
     * @param params find file reference request param
     * @return definition
     */
    @Nullable
    public CompletableFuture<CangjieMoveUpdateInfo> fileRefactor(CangjieFileRefactorParam params) {
        if (!checkStatus()) {
            return null;
        }
        try {
            return server.fileRefactor(params);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}