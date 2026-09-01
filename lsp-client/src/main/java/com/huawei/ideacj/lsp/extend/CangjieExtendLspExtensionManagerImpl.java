/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend;

import com.huawei.ideacj.edit.CangjieLSPCaretListenerImpl;
import com.huawei.cjlsp.capabilities.icon.CangjieIconProvider;
import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.lsp.listener.CangjieDocumentListenerImpl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.contributors.label.LSPDefaultLabelProvider;
import org.wso2.lsp4intellij.contributors.label.LSPLabelProvider;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.listeners.EditorMouseListenerImpl;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.utils.FileUtils;

/**
 * 功能描述
 *
 * @since 2020-06-08
 */
public class CangjieExtendLspExtensionManagerImpl implements LSPExtensionManager {
    private static final String CANGJIE_SUFFIX = "/entry";

    private static final char LINUX_PATH_SEPARATOR_CHAR = '/';

    private static final char WIN_PATH_SEPARATOR_CHAR = '\\';

    private LanguageClient client;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DefaultRequestManager> T getExtendedRequestManagerFor(LanguageServerWrapper languageServerWrapper,
        LanguageServer languageServer, LanguageClient languageClient, ServerCapabilities serverCapabilities) {
        return (T) new ExtendRequestManager(languageServerWrapper, languageServer, languageClient, serverCapabilities);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EditorEventManager> T getExtendedEditorEventManagerFor(Editor editor,
        DocumentListener documentListener, EditorMouseListenerImpl editorMouseListener,
        EditorMouseMotionListenerImpl editorMouseMotionListener, LSPCaretListenerImpl lspCaretListener,
        RequestManager requestManager, ServerOptions serverOptions, LanguageServerWrapper languageServerWrapper) {
        return (T) new CangjieEditorEventManager(editor, new CangjieDocumentListenerImpl(), editorMouseListener,
            editorMouseMotionListener, new CangjieLSPCaretListenerImpl(),
            requestManager, serverOptions, languageServerWrapper);
    }

    @Override
    public Class<? extends LanguageServer> getExtendedServerInterface() {
        return ExtendLanguageServer.class;
    }

    @Override
    public LanguageClient getExtendedClientFor(ClientContext clientContext) {
        client = new ExtendLanguageClient(clientContext);
        return client;
    }

    /**
     * Return IconProvider
     *
     * @return ExtendedCangjieIconProvider
     */
    public CangjieIconProvider getIconProvider() {
        return new CangjieIconProvider();
    }

    @Override
    public boolean isFileContentSupported(@NotNull PsiFile file) {
        return LSPExtensionManager.super.isFileContentSupported(file);
    }

    /**
     * Return LabelProvider for search everywhere
     *
     * @return SymbolLabelProvider
     */
    @Override
    public LSPLabelProvider getLabelProvider() {
        return new SymbolLabelProvider();
    }

    private static class SymbolLabelProvider extends LSPDefaultLabelProvider {
        @Override
        @Nullable
        public String symbolLocationFor(@NotNull SymbolInformation symbolInformation,
                                        @NotNull Project project) {
            return buildContainer(symbolInformation);
        }

        @Nullable
        private String buildContainer(@NotNull SymbolInformation symbolInformation) {
            final String dot = " • ";
            String containerName = symbolInformation.getContainerName();
            String uri = symbolInformation.getLocation().getUri();
            VirtualFile file = FileUtils.uriToVfs(uri);

            if (containerName == null || containerName.isEmpty()) {
                if (file != null) {
                    return file.getName();
                } else {
                    return "";
                }
            } else {
                if (file != null) {
                    int indexOfSrcDir = file.getPath().indexOf(CANGJIE_SUFFIX);
                    if (indexOfSrcDir == -1) {
                        return containerName;
                    }
                    String srcOrPackage = file.getPath().substring(indexOfSrcDir)
                            .replace(LINUX_PATH_SEPARATOR_CHAR, WIN_PATH_SEPARATOR_CHAR);
                    return containerName + dot + srcOrPackage;
                }
            }
            return containerName;
        }
    }

    @Override
    public String toString() {
        return "CangjieExtendLspExtensionManagerImpl{" + "client=" + client + '}';
    }
}

