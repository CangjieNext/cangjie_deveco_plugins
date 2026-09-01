/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend;

import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.formatter.CangjieFormatCodeHandler;
import com.huawei.ideacj.formatter.LineRange;
import com.huawei.ideacj.lsp.extend.params.CompletionTipParams;
import com.huawei.ideacj.lsp.extend.params.ExtendDiagnostic;
import com.huawei.ideacj.lsp.extend.params.ExtendPublishDiagnosticsParams;
import com.huawei.ideacj.lsp.extend.params.IndexingProgressUpdateParams;

import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkDoneProgressReport;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.awt.Point;
import java.awt.Component;
import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;

/**
 * 功能描述
 *
 * @author rice
 * @since 2020-06-08
 */
public class ExtendLanguageClient extends DefaultLanguageClient {
    private static final Logger LOG = Logger.getInstance(ExtendLanguageClient.class);

    public ExtendLanguageClient(ClientContext context) {
        super(context);
    }

    @Override
    public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
        return CompletableFuture.supplyAsync(ArrayList::new);
    }

    private boolean isValidURI(String uriString) {
        try {
            new URI(uriString);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Replace textDocument/PublishDiagnostics
     *
     * @param diagnosticsParams diagnosticsParams with code actions
     */
    @JsonNotification("textDocument/extendPublishDiagnostics")
    void extendPublishDiagnostics(ExtendPublishDiagnosticsParams diagnosticsParams) {
        if (!isValidURI(diagnosticsParams.getUri())) {
            return;
        }
        String uri = FileUtils.sanitizeURI(diagnosticsParams.getUri());
        List<ExtendDiagnostic> diagnostics = List.copyOf(diagnosticsParams.getDiagnostics());
        for (Editor editor : FileUtils.editorsForFile(FileUtils.uriToVfs(uri))) {
            EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
            if (manager instanceof CangjieEditorEventManager) {
                CangjieDiagnosticAnnotatorHolder.doAnnotation(manager.editor, diagnostics);
            }
        }
    }

    /**
     * Replace textDocument/PublishDiagnostics
     *
     * @param completionTip completion tip
     */
    @JsonNotification("textDocument/publishCompletionTip")
    void publishCompletionTip(CompletionTipParams completionTip) {
        if (!isValidURI(completionTip.getUri())) {
            return;
        }

        String uri = FileUtils.sanitizeURI(completionTip.getUri());
        for (Editor editor : FileUtils.editorsForFile(FileUtils.uriToVfs(uri))) {
            ApplicationManager.getApplication().invokeLater(() -> {
                Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
                Component component = editor.getComponent();
                JLabel label = new JLabel(completionTip.getTip());
                LogicalPosition logicalPosition = editor.getCaretModel().getPrimaryCaret().getLogicalPosition();
                Point point = editor.logicalPositionToXY(logicalPosition);
                point.y = point.y - visibleArea.y;
                point.x = point.x + 50;
                IdeTooltip tooltip = new IdeTooltip(component, point, label);
                IdeTooltipManager.getInstance().show(tooltip, true);
            });
        }
    }

    @JsonNotification("progress/UpdateIndexingProgress")
    void updateIndexingProgress(IndexingProgressUpdateParams params) {
        Project project = getContext().getProject();
        if (project == null) {
            LOG.warn("onIndexingProgressUpdate error, project is null or closed");
            return;
        }

        LanguageServerWrapper wrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
        if (wrapper == null) {
            LOG.warn("wrapper is null");
            return;
        }
        int total = params.getTotal();
        int current = params.getCurrent();
        if (total <= 0 || current > total) {
            LOG.warn(String.format(Locale.ROOT,
                    "onIndexingProgressUpdate result params, total: %d, increment: %d", total,
                    current));
            return;
        }

        wrapper.onIndexingProgressUpdate(String.format(Locale.ROOT, "%d/%d packages indexed", current, total),
                (double) current / total);
    }

    @Override
    public void notifyProgress(ProgressParams params) {
        Project project = getContext().getProject();
        if (project == null || project.isDisposed()) {
            LOG.warn("onIndexingProgressUpdate error, project is null or closed");
            return;
        }

        LanguageServerWrapper wrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
        if (wrapper == null) {
            LOG.warn("wrapper is null");
            return;
        }
        if (params.getValue().getLeft() != null) {
            if (params.getValue().getLeft() instanceof WorkDoneProgressReport workDoneProgressReport) {
                wrapper.onIndexingProgressUpdate(workDoneProgressReport.getMessage(),
                        (double) workDoneProgressReport.getPercentage() / 100.0);
            }
        }
    }

    /**
     * applyEdit
     *
     * @param params ApplyWorkspaceEditParams
     * @return ApplyWorkspaceEditResponse
     */
    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(@NotNull ApplyWorkspaceEditParams params) {
        CompletableFuture<Boolean> response = ExtendWorkspaceEditHandler.applyEdit(params.getEdit());
        return response.thenApply(result -> new ApplyWorkspaceEditResponse(result));
    }

    /**
     * refactor applyEdit后格式化代码
     *
     * @param workspaceEdit workspaceEdit
     */
    public static void refactorFormatCode(@NotNull WorkspaceEdit workspaceEdit) {
        Map<String, List<TextEdit>> changes = workspaceEdit.getChanges();
        Map<Editor, Collection<LineRange>> formatMap = new HashMap<>();
        AtomicReference<Project> project = new AtomicReference<>();
        changes.forEach((uri, edits) -> {
            Editor editor = FileUtils.editorForFile(FileUtils.uriToVfs(uri));
            if (project.get() == null) {
                project.set(editor.getProject());
            }
            sortEdits(edits);
            List<LineRange> lines = new ArrayList<>();
            AtomicInteger changeLines = new AtomicInteger();
            edits.forEach(edit -> {
                if (edit.getRange() == null || edit.getRange().getStart() == null || edit.getRange().getEnd() == null) {
                    return;
                }
                String[] split = edit.getNewText().split("\n");
                LineRange lineRange;
                if (edit.getRange().getStart().equals(edit.getRange().getEnd())) {
                    int startLine = edit.getRange().getStart().getLine() + 1 + changeLines.get();
                    int endLine = edit.getRange().getStart().getLine() + split.length + 1;
                    lineRange = LineRange.create(startLine, endLine);
                    changeLines.addAndGet(split.length);
                } else {
                    int startLine = edit.getRange().getStart().getLine() + 1 + changeLines.get();
                    int endLine = edit.getRange().getEnd().getLine() + 1 + changeLines.get();
                    lineRange = LineRange.create(startLine, endLine);
                    int originLineCount = edit.getRange().getEnd().getLine() - edit.getRange().getStart().getLine();
                    changeLines.addAndGet(split.length - originLineCount);
                }
                lines.add(lineRange);
            });
            formatMap.put(editor, lines);
        });
        if (project.get() == null || formatMap.isEmpty()) {
            return;
        }
        formatMap.forEach((editor, lines) -> {
            CangjieFormatCodeHandler.executeCodeFormat(project.get(), editor, lines);
        });
    }

    private static void sortEdits(List<TextEdit> edits) {
        edits.sort(Comparator
                .comparingInt((TextEdit edit) -> edit.getRange().getStart().getLine())
                .thenComparingInt((TextEdit edit) -> edit.getRange().getStart().getCharacter()));
    }

    /**
     * format code after remove unused imports
     *
     * @param workspaceEdit workspaceEdit
     */
    public static void removeImportsFormatCode(@NotNull WorkspaceEdit workspaceEdit) {
        Map<String, List<TextEdit>> changes = workspaceEdit.getChanges();
        AtomicReference<Project> project = new AtomicReference<>();
        AtomicReference<Editor> formatEditor = new AtomicReference<>();
        AtomicReference<Integer> startLine = new AtomicReference<>(Integer.MAX_VALUE);
        AtomicReference<Integer> endLine = new AtomicReference<>(-1);
        changes.forEach((uri, edits) -> {
            Editor editor = FileUtils.editorForFile(FileUtils.uriToVfs(uri));
            if (editor.isDisposed()) {
                return;
            }
            formatEditor.compareAndSet(null, editor);
            project.compareAndSet(null, editor.getProject());
            edits.forEach(edit -> {
                if (edit.getRange() == null || edit.getRange().getStart() == null || edit.getRange().getEnd() == null) {
                    return;
                }
                if (edit.getRange().getStart().getLine() < startLine.get()) {
                    startLine.set(edit.getRange().getStart().getLine() + 1);
                }
                if (edit.getRange().getEnd().getLine() >= endLine.get()) {
                    endLine.set(edit.getRange().getEnd().getLine() + 1);
                }
            });
        });
        if (formatEditor.get() == null || project.get() == null) {
            return;
        }
        if (startLine.get() - 1 >= 1) {
            startLine.set(startLine.get() - 1);
        }
        if (endLine.get() + 1 <= formatEditor.get().getDocument().getLineCount()) {
            endLine.set(endLine.get() + 1);
        }
        CangjieFormatCodeHandler.executeCodeFormat(project.get(), formatEditor.get(),
                Collections.singleton(new LineRange(startLine.get(), endLine.get())));
    }

    /**
     * format code after implement members, including surrounding blank lines
     *
     * @param workspaceEdit workspaceEdit
     */
    public static void implementMembersFormatCode(@NotNull WorkspaceEdit workspaceEdit) {
        Map<String, List<TextEdit>> changes = workspaceEdit.getChanges();
        if (changes == null || changes.isEmpty()) {
            return;
        }

        AtomicReference<Project> project = new AtomicReference<>();
        AtomicReference<Editor> formatEditor = new AtomicReference<>();
        AtomicReference<Integer> startLine = new AtomicReference<>(Integer.MAX_VALUE);
        AtomicReference<Integer> endLine = new AtomicReference<>(-1);

        changes.forEach((uri, edits) -> {
            if (edits == null || edits.isEmpty()) {
                return;
            }

            Editor editor = FileUtils.editorForFile(FileUtils.uriToVfs(uri));
            if (editor == null || editor.isDisposed()) {
                return;
            }

            TextEdit edit = edits.get(0);
            if (edit.getRange() == null || edit.getRange().getStart() == null
                    || edit.getRange().getEnd() == null) {
                return;
            }

            formatEditor.compareAndSet(null, editor);
            project.compareAndSet(null, editor.getProject());

            Document document = editor.getDocument();
            int insertStartLine = edit.getRange().getStart().getLine();
            int newLineCount = countLines(edit.getNewText());
            int insertEndLine = insertStartLine + newLineCount;

            int[] range = calculateFormatRange(document, insertStartLine, insertEndLine);
            startLine.set(range[0]);
            endLine.set(range[1]);
        });

        if (formatEditor.get() == null || project.get() == null) {
            return;
        }

        CangjieFormatCodeHandler.executeCodeFormat(project.get(), formatEditor.get(),
                Collections.singleton(new LineRange(startLine.get(), endLine.get())));
    }

    private static int countLines(String text) {
        if (text == null) {
            return 0;
        }
        return (int) text.chars().filter(ch -> ch == '\n').count();
    }

    /**
     * calculate format range for implement members
     *
     * @param document document
     * @param insertStartLine insert start line
     * @param insertEndLine insert end line
     * @return int[2] - [startLine, endLine]
     */
    private static int[] calculateFormatRange(Document document, int insertStartLine, int insertEndLine) {
        int documentLineCount = document.getLineCount();
        int extendStartLine = insertStartLine;
        int extendEndLine = insertEndLine;

        // extend backward to first non-empty line
        while (extendStartLine > 0) {
            // do not refactor code above inserted code
            String lineText = getLineText(document, extendStartLine).trim();
            if (!lineText.isEmpty()) {
                break;
            }
            extendStartLine--;
        }

        // extend forward to first empty line
        while (extendEndLine < documentLineCount - 1) {
            String lineText = getLineText(document, extendEndLine).trim();
            if (!lineText.isEmpty()) {
                break;
            }
            extendEndLine++;
        }

        return new int[]{extendStartLine + 1, extendEndLine + 1};
    }

    private static String getLineText(Document document, int line) {
        return document.getText(new com.intellij.openapi.util.TextRange(
                document.getLineStartOffset(line),
                document.getLineEndOffset(line)));
    }
}