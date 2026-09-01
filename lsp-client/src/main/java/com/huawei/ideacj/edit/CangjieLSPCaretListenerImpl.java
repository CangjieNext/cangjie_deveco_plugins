/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.edit;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.CaretEvent;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Cangjie LSPCaretListenerImpl
 *
 * @author xiewen
 * @since 2022-09-29
 */
public class CangjieLSPCaretListenerImpl extends LSPCaretListenerImpl {
    private static final Logger LOG = Logger.getInstance(CangjieLSPCaretListenerImpl.class);

    private static final Map<String, CompletableFuture<Either<List<CompletionItem>, CompletionList>>>
            completeRequestMap = new HashMap<>();

    private static boolean inputAChar = false;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    private ScheduledFuture<?> scheduledFuture = null;

    /**
     * set inputAChar flag
     *
     * @param value flag
     */
    public static synchronized void setInputAChar(boolean value) {
        inputAChar = value;
    }

    /**
     * caret position change listener
     *
     * @param event the event containing information about the caret movement.
     */
    @Override
    public void caretPositionChanged(CaretEvent event) {
        try {
            if (this.scheduledFuture != null && !this.scheduledFuture.isCancelled()) {
                this.scheduledFuture.cancel(false);
            }

            if (!(EditorEventManagerBase.forEditor(event.getEditor()) instanceof CangjieEditorEventManager manager)) {
                return;
            }

            if (!shouldRequestCompletion(event)) {
                this.scheduledFuture = this.scheduler.schedule(this::debouncedCaretPositionChanged, 100L,
                        TimeUnit.MILLISECONDS);
                setInputAChar(false);
                return;
            }

            setInputAChar(false);
            for (CompletableFuture<?> future : completeRequestMap.values()) {
                future.cancel(true);
            }
            synchronized (completeRequestMap) {
                completeRequestMap.clear();
            }
            Position pos = getCaretNewPos(event);
            var request = manager.completionAsync(pos);
            Document doc = event.getEditor().getDocument();
            completeRequestMap.put(doc.getText().substring(doc.getLineStartOffset(event.getNewPosition().getLine()),
                    doc.getLineEndOffset(event.getNewPosition().getLine())), request);

            this.scheduledFuture = this.scheduler.schedule(this::debouncedCaretPositionChanged, 100L,
                    TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException | NullPointerException e) {
            LOG.warn("Error occurred when trying to update code actions");
        }
    }

    /**
     * get completion result
     *
     * @param editor Editor
     * @param line trigger line
     * @return lookup Elements
     */
    public static List<LookupElement> getCompletionResult(Editor editor, String line) {
        List<LookupElement> lookupItems = new ArrayList<>();
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> request = null;
        synchronized (completeRequestMap) {
            request = completeRequestMap.get(line);
        }
        if (request != null) {
            try {
                Either<List<CompletionItem>, CompletionList> res = request.get();
                if (res == null) {
                    return lookupItems;
                }
                if (!(EditorEventManagerBase.forEditor(editor) instanceof CangjieEditorEventManager manager)) {
                    return lookupItems;
                }
                long startTime = System.currentTimeMillis();
                manager.completionAction(lookupItems, res, startTime);
                return lookupItems;
            } catch (InterruptedException | ExecutionException e) {
                return lookupItems;
            }
        }
        return lookupItems;
    }

    private void debouncedCaretPositionChanged() {
        if (this.checkEditorEnabled()) {
            this.editorEventManager.requestAndShowCodeActions();
            this.editorEventManager.highlight();
        }
    }

    private boolean shouldRequestCompletion(CaretEvent event) {
        LogicalPosition oldPos = event.getOldPosition();
        LogicalPosition newPos = event.getNewPosition();

        return inputAChar
                && newPos.getLine() == oldPos.getLine()
                && newPos.getColumn() - oldPos.getColumn() == 1;
    }

    private Position getCaretNewPos(CaretEvent event) {
        Editor editor = event.getEditor();
        Document document = editor.getDocument();
        var caret = event.getCaret();
        if (caret == null) {
            return new Position(event.getNewPosition().getLine(), event.getNewPosition().getColumn());
        }

        int offset = caret.getOffset();
        int line = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(line);
        int character = offset - lineStartOffset;
        return new Position(line, character);
    }
}
