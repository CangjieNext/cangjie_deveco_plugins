/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.lsp.listener;

import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidChange;
import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidChangeWatchFile;
import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;

import com.huawei.idea.edit.CangjieLSPCaretListenerImpl;

import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.DocumentEventManager;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * CangjieCommandListener
 *
 * @since 2024/11/11
 */
public class CangjieCommandListener implements CommandListener, DocumentListener {
    private OperationState currentOperationState = OperationState.NONE;

    private final Set<String> unOpenedFiles = new HashSet<>();

    private final Project project;

    public CangjieCommandListener(Project project) {
        this.project = project;
    }

    @Override
    public void commandStarted(@NotNull CommandEvent event) {
        currentOperationState = getCurrentOperationState(event);
    }

    @Override
    public void commandFinished(@NotNull CommandEvent event) {
        if (currentOperationState == getCurrentOperationState(event)) {
            currentOperationState = OperationState.NONE;
            doSendFileChange();
        }
        unOpenedFiles.clear();
    }

    private OperationState getCurrentOperationState(@NotNull CommandEvent event) {
        if (project == null) {
            return OperationState.NONE;
        }
        String commandName = event.getCommandName();
        if (commandName == null) {
            return OperationState.NONE;
        }
        if (commandName.startsWith("Deleting")) {
            return OperationState.DELE;
        }
        if (commandName.startsWith("Move")) {
            return OperationState.MOVE;
        }
        UndoManager undoManager = UndoManager.getInstance(project);
        if (undoManager == null) {
            return OperationState.NONE;
        }
        if (!undoManager.isUndoInProgress() && !undoManager.isRedoInProgress()) {
            return OperationState.NONE;
        }
        return isUndoOrRedo(commandName);
    }

    private OperationState isUndoOrRedo(String commandName) {
        if ("Undo Rename".equals(commandName)) {
            return OperationState.UNDO;
        }
        if (commandName.startsWith("Undo Deleting")) {
            return OperationState.UNDO;
        }
        if (commandName.startsWith("Redo Deleting")) {
            return OperationState.REDO;
        }
        if (commandName.startsWith("Undo Move")) {
            return OperationState.UNDO;
        }
        if (commandName.startsWith("Redo Move")) {
            return OperationState.REDO;
        }
        if (commandName.startsWith("Undo LSP")) {
            return OperationState.UNDO;
        }
        if (commandName.startsWith("Redo LSP")) {
            return OperationState.REDO;
        }
        if (commandName.startsWith("Undo Extract")) {
            return OperationState.UNDO;
        }
        if (commandName.startsWith("Redo Extract")) {
            return OperationState.REDO;
        }
        return OperationState.NONE;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
        int oldLen = event.getOldLength();
        int newLen = event.getNewLength();

        CangjieLSPCaretListenerImpl.setInputAChar(newLen - oldLen == 1);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        DocumentListener.super.documentChanged(event);
        if (currentOperationState == OperationState.NONE) {
            return;
        }
        Document document = event.getDocument();
        String uri = FileUtils.documentToUri(document);
        boolean hasEditor = EditorFactory.getInstance().getEditors(document).length != 0;
        if (hasEditor) {
            return;
        }
        unOpenedFiles.add(uri);
        DocumentEventManager documentEventManager = DocumentEventManager.getDocumentEventManager(uri);
        if (documentEventManager == null) {
            documentChange(uri, event);
        } else {
            documentChange(documentEventManager, event);
        }
    }

    private void documentChange(String uri, DocumentEvent event) {
        DidChangeTextDocumentParams changesParams = new DidChangeTextDocumentParams(
                new VersionedTextDocumentIdentifier(), Collections.singletonList(new TextDocumentContentChangeEvent()));
        changesParams.getTextDocument().setUri(uri);
        changesParams.getTextDocument().setVersion(0);
        doSendChange(changesParams, event);
    }

    private void documentChange(DocumentEventManager documentEventManager, DocumentEvent event) {
        DidChangeTextDocumentParams changesParams = new DidChangeTextDocumentParams(
                new VersionedTextDocumentIdentifier(), Collections.singletonList(new TextDocumentContentChangeEvent()));
        changesParams.getTextDocument().setUri(documentEventManager.getIdentifier().getUri());
        changesParams.getTextDocument().setVersion(documentEventManager.getDocumentVersion() + 1);
        doSendChange(changesParams, event);
    }

    private void doSendChange(DidChangeTextDocumentParams changesParams, DocumentEvent event) {
        LanguageServerWrapper wrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
        if (wrapper == null) {
            return;
        }

        TextDocumentContentChangeEvent changeEvent = changesParams.getContentChanges().get(0);
        CharSequence newText = event.getNewFragment();
        int offset = event.getOffset();
        Position lspPosition = DocumentUtils.offsetToLSPPos(event.getDocument(), offset);
        int startLine = lspPosition.getLine();
        int startColumn = lspPosition.getCharacter();
        CharSequence oldText = event.getOldFragment();

        // if text was deleted/replaced, calculate the end position of inserted/deleted text
        int endLine;
        int endColumn;
        if (!oldText.isEmpty()) {
            endLine = startLine + StringUtil.countNewLines(oldText);
            String oldTextStr = oldText.toString();
            String[] oldLines = oldTextStr.split("\n");
            int oldTextLength = oldLines.length == 0 ? 0 : oldLines[oldLines.length - 1].length();
            endColumn = oldTextStr.endsWith("\n") ? 0 : oldLines.length == 1
                    ? startColumn + oldTextLength : oldTextLength;
        } else { // if insert or no text change, the end position is the same
            endLine = startLine;
            endColumn = startColumn;
        }
        Range range = new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));
        changeEvent.setRange(range);
        changeEvent.setText(newText.toString());
        notifyDidChange(wrapper, changesParams.getTextDocument(), changesParams.getContentChanges());
    }

    private void doSendFileChange() {
        LanguageServerWrapper wrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
        if (wrapper == null) {
            return;
        }
        for (String fileUri : unOpenedFiles) {
            notifyDidChangeWatchFile(wrapper, fileUri, FileChangeType.Changed);
        }
    }

    private enum OperationState {
        NONE,
        UNDO,
        REDO,
        MOVE,
        DELE;

        private OperationState() {
        }
    }
}
