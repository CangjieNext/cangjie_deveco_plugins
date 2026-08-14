/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.capabilities.lsp;

import static com.huawei.idea.lsp.utils.CommonUtils.timeoutCrashCheck;

import com.huawei.idea.lsp.utils.CrashLogPackager;
import com.huawei.idea.trace.TraceUtils;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;
import org.wso2.lsp4intellij.utils.LogThreshold;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * this is extension of Lsp GotoDeclaration
 *
 * @since 2024-05-31
 */
public class CangjieGotoDeclaration extends GotoDeclarationHandlerBase {
    private static final Logger LOG = Logger.getInstance(CangjieGotoDeclaration.class);

    private final HashSet<String> operation = new HashSet<>(Arrays.asList("[]", "!", "**", "*", "%", "/", "+", "-",
            "<<", ">>", "<", "<=", ">", ">=", "==", "!=", "&", "^", "|"));

    @Nullable
    private PsiElement turnLocation2PsiElement(Location location, Editor editor) {
        String uri = location.getUri();
        String filePath;
        try {
            filePath = (new URI(uri)).getPath();
        } catch (URISyntaxException e) {
            LOG.warn("URI syntax exception");
            return null;
        }
        if (filePath == null) {
            return null;
        }
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }
        return ReadAction.compute(() -> {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (virtualFile == null) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null || psiFile == null || !psiFile.isValid() || !isLocationValid(location, document)) {
                return null;
            }
            Position start = location.getRange().getStart();
            int elementOffset = document.getLineStartOffset(start.getLine()) + start.getCharacter();
            return psiFile.findElementAt(elementOffset);
        });
    }

    /**
     * Gets goto definition target.
     *
     * @param sourceElement the source element
     * @param editor        the editor
     * @return the goto definition target
     */
    @Nullable
    public PsiElement getGotoDefinitionTarget(@Nullable PsiElement sourceElement, Editor editor) {
        if (this.isEditorInvalid(editor) || sourceElement == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        int offset = sourceElement.getTextRange().getStartOffset();
        Optional<Location> location = this.getDefinition(offset, editor);
        if (location.isEmpty() || location.get().getRange() == null) {
            // if definition not found, find declaration
            location = this.getDeclaration(offset, editor);
            long duration = System.currentTimeMillis() - startTime;
            if (duration > LogThreshold.REQUEST_DECLARATION.getThreshold()) {
                LOG.info(String.format(Locale.ROOT, "Get declaration took %d ms", duration));
            }
            if (location.isEmpty() || location.get().getRange() == null) {
                return null;
            }
        }
        return turnLocation2PsiElement(location.get(), editor);
    }

    /**
     * getGotoDeclarationTarget
     *
     * @param sourceElement PsiElement
     * @param editor Editor
     * @return PsiElement
     */
    @Nullable
    public PsiElement getGotoDeclarationTarget(@Nullable PsiElement sourceElement, Editor editor) {
        if (this.isEditorInvalid(editor) || sourceElement == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        int offset = sourceElement.getTextRange().getStartOffset();
        Optional<Location> location = this.getDeclaration(offset, editor);
        if (location.isEmpty() || location.get().getRange() == null) {
            // if declaration not found, find definition
            location = this.getDefinition(offset, editor);
            long duration = System.currentTimeMillis() - startTime;
            if (duration > LogThreshold.REQUEST_DEFINITION.getThreshold()) {
                LOG.info(String.format(Locale.ROOT, "Get definition took %d ms", duration));
            }
            if (location.isEmpty() || location.get().getRange() == null) {
                return null;
            }
        }
        PsiElement definitionElement = turnLocation2PsiElement(location.get(), editor);
        if (definitionElement == null) {
            return null;
        }
        boolean isInvalidOperationDef = operation.contains(sourceElement.getText())
                && definitionElement.getText() != null
                && !definitionElement.getText().contains(sourceElement.getText());
        if (isInvalidOperationDef) {
            return null;
        }
        return definitionElement;
    }

    private boolean isEditorInvalid(Editor editor) {
        return editor == null || editor.isDisposed() || editor.getProject() == null;
    }

    /**
     * this is for inline debugger to
     * get the declaration from identifier element
     *
     * @param sourceElement the source identifier element
     * @return null,                           if no declaration found
     * the first declaration element   if declaration found
     */
    @Nullable
    public PsiElement getDeclarationTarget(PsiElement sourceElement) {
        Editor editor = FileUtils.editorFromPsiFile(sourceElement.getContainingFile());
        return this.isEditorInvalid(editor) ? null : this.getGotoDeclarationTarget(sourceElement, editor);
    }

    /**
     * this is for inline debugger to
     * get the definition from identifier element
     *
     * @param sourceElement the source identifier element
     * @return null,                           if no definition found
     * the first definition element   if definition found
     */
    @Nullable
    public PsiElement getDefinitionTarget(PsiElement sourceElement) {
        Editor editor = FileUtils.editorFromPsiFile(sourceElement.getContainingFile());
        if (isEditorInvalid(editor)) {
            return null;
        }
        return getGotoDefinitionTarget(sourceElement, editor);
    }

    @Nullable
    public String getActionText(@NotNull DataContext context) {
        return null;
    }

    private boolean canProvideDeclaration(EditorEventManager eventManager) {
        Optional<ServerCapabilities> capabilities = this.getServerCapabilities(eventManager);
        return capabilities.isPresent() && capabilities.get().getDeclarationProvider() != null;
    }

    /**
     *  getDeclaration
     *
     * @param offset offset
     * @param editor editor
     * @return Optional<Location>
     */
    @Nullable
    public Optional<Location> getDeclaration(int offset, Editor editor) {
        EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
        if (eventManager == null || !this.canProvideDeclaration(eventManager)
                || eventManager.wrapper == null) {
            return Optional.empty();
        }
        LanguageServer languageServer = eventManager.wrapper.getServer();
        if (languageServer == null) {
            return Optional.empty();
        }
        Position position = DocumentUtils.offsetToLSPPos(editor, offset);
        DeclarationParams params = new DeclarationParams(eventManager.getIdentifier(), position);
        TextDocumentService textDocumentService = languageServer.getTextDocumentService();
        if (textDocumentService == null) {
            return Optional.empty();
        }
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> request =
                textDocumentService.declaration(params);
        if (request == null) {
            return Optional.empty();
        }
        try {
            Either<List<? extends Location>, List<? extends LocationLink>> declaration =
                    request.get(200, TimeUnit.MILLISECONDS);
            if (declaration != null && declaration.isLeft()
                    && !(declaration.getLeft()).isEmpty()) {
                return Optional.ofNullable((declaration.getLeft()).get(0));
            }
        } catch (TimeoutException e) {
            LOG.warn("Get declaration timeout");
        } catch (InterruptedException | JsonRpcException | ExecutionException e) {
            LOG.warn("Get declaration error");
            eventManager.wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return Optional.empty();
    }

    private boolean canProvideDefinition(EditorEventManager eventManager) {
        Optional<ServerCapabilities> capabilities = this.getServerCapabilities(eventManager);
        return capabilities.isPresent() && capabilities.get().getDefinitionProvider() != null;
    }

    private Optional<ServerCapabilities> getServerCapabilities(EditorEventManager eventManager) {
        if (eventManager != null && eventManager.wrapper != null
                && eventManager.wrapper.getStatus() == ServerStatus.INITIALIZED) {
            return Optional.ofNullable(eventManager.wrapper.getServerCapabilities());
        }
        return Optional.empty();
    }

    /**
     *  getDefinition
     *
     * @param offset offset
     * @param editor editor
     * @return Optional<Location>
     */
    public Optional<Location> getDefinition(int offset, Editor editor) {
        EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
        if (eventManager == null || !this.canProvideDefinition(eventManager)
                || eventManager.wrapper.getServer() == null) {
            return Optional.empty();
        }
        Position position = DocumentUtils.offsetToLSPPos(editor, offset);
        DefinitionParams params = new DefinitionParams(eventManager.getIdentifier(), position);
        RequestManager requestManager = eventManager.getRequestManager();
        if (requestManager == null) {
            LOG.warn(String.format(Locale.ROOT, "requestManager in wrapper[%s] is null",
                    eventManager.wrapper.serverDefinition.ext));
            return Optional.empty();
        }
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> request =
                requestManager.definition(params);
        if (request == null) {
            LOG.warn("definition fail: request is null");
            return Optional.empty();
        }
        try {
            Either<List<? extends Location>, List<? extends LocationLink>> definition =
                    request.get(200, TimeUnit.MILLISECONDS);
            if (definition != null && definition.isLeft() && !(definition.getLeft()).isEmpty()) {
                return Optional.ofNullable((definition.getLeft()).get(0));
            }
        } catch (TimeoutException e) {
            LOG.warn("Get definition timeout");
            timeoutCrashCheck(requestManager);
        } catch (JsonRpcException | ExecutionException | InterruptedException e) {
            LOG.warn("Get definition error");
            eventManager.wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return Optional.empty();
    }

    /**
     * isLocationValid
     *
     * @param location location
     * @param document document
     * @return boolean
     */
    public boolean isLocationValid(Location location, Document document) {
        // end line is not valid.
        if (document.getLineCount() <= location.getRange().getEnd().getLine()) {
            return false;
        }
        // end offset is valid.
        int endOffset = document.getLineStartOffset(location.getRange().getEnd().getLine())
                + location.getRange().getEnd().getCharacter();
        return document.getTextLength() >= endOffset;
    }
}
