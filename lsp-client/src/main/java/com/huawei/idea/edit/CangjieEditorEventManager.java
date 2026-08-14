/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.edit;

import static com.huawei.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.positionToOffset;
import static com.huawei.idea.lsp.utils.CommonUtils.timeoutCrashCheck;
import static com.huawei.idea.lsp.utils.LspConfigUtils.addContentChanges;
import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidChange;
import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidChangeWatchFile;
import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidCloseFile;
import static com.huawei.idea.lsp.utils.LspConfigUtils.notifyDidOpenFile;
import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.EXECUTE_COMMAND;
import static org.wso2.lsp4intellij.requests.Timeouts.HOVER;
import static org.wso2.lsp4intellij.utils.ApplicationUtils.invokeLater;
import static org.wso2.lsp4intellij.utils.ApplicationUtils.pool;

import com.huawei.idea.dialog.CangjieRenameDialog;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.idea.lsp.extend.CangjieDiagnosticAnnotatorHolder;
import com.huawei.idea.lsp.listener.CangjieDocumentListenerImpl;
import com.huawei.idea.lsp.utils.CangjieBundle;
import com.huawei.idea.lsp.utils.Constants;
import com.huawei.idea.lsp.utils.CrashLogPackager;
import com.huawei.idea.lsp.utils.LSPThreadPoolManager;
import com.huawei.idea.trace.TraceUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.Alarm;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.contributors.icon.LSPIconProvider;
import org.wso2.lsp4intellij.contributors.psi.LSPPsiElement;
import org.wso2.lsp4intellij.contributors.rename.LSPRenameProcessor;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.requests.Timeout;
import org.wso2.lsp4intellij.requests.Timeouts;
import org.wso2.lsp4intellij.requests.completion.CompletionData;
import org.wso2.lsp4intellij.requests.completion.CompletionDataHandler;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;
import org.wso2.lsp4intellij.utils.GUIUtils;
import org.wso2.lsp4intellij.utils.LogThreshold;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

/**
 * expand EditorEventManager
 *
 * @since 2022-7-21
 */
public class CangjieEditorEventManager extends EditorEventManager {
    private static final int TIME_COMPLETION = 6000;
    private static final int TIME_REFERENCES = 6000;
    private static final String RENAME_FAILURE_MESSAGE = "The element can't be renamed.";

    private static final String CANGJIE_RESOURCE_IDENTIFIER = "r";
    private static final Logger LOG = Logger.getInstance(EditorEventManager.class);
    private static final int DOCUMENT_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 50;
    private static final Gson GSON = new Gson();

    private static final Set<String> INVALID_TOKENS = new HashSet<>() {
        {
            add("(");
            add(")");
            add("[");
            add("]");
            add("{");
            add("}");
        }
    };

    private RequestManager requestManager;
    private Project project;
    private Alarm hoverAlarm = new Alarm();
    private CompletionDataHandler completionDataHandler = new CompletionDataHandler();
    private List<RangeHighlighter> highlighterList = new ArrayList();
    private String isGeneric = "";
    private boolean isRemoved = false;

    /**
     * CangjieEditorEventManager
     *
     * @param editor              Editor
     * @param documentListener    DocumentListener
     * @param mouseListener       EditorMouseListener
     * @param mouseMotionListener EditorMouseMotionListener
     * @param caretListener       LSPCaretListenerImpl
     * @param requestManager      RequestManager
     * @param serverOptions       ServerOptions
     * @param wrapper             LanguageServerWrapper
     */
    public CangjieEditorEventManager(Editor editor, DocumentListener documentListener,
        EditorMouseListener mouseListener, EditorMouseMotionListener mouseMotionListener,
        LSPCaretListenerImpl caretListener, RequestManager requestManager, ServerOptions serverOptions,
        LanguageServerWrapper wrapper) {
        super(editor, documentListener, mouseListener, mouseMotionListener, caretListener, requestManager,
                serverOptions, wrapper);
        this.requestManager = requestManager;
        this.project = editor.getProject();
        caretListener.setDocumentEventManager(this.documentEventManager);
        caretListener.setEditorEventManager(this);
        if (documentListener instanceof CangjieDocumentListenerImpl) {
            ((CangjieDocumentListenerImpl) documentListener).setDocumentEventManager(this.documentEventManager);
            ((CangjieDocumentListenerImpl) documentListener).setEditorEventManager(this);
        }
    }

    static class SnippetVariable {
        String lspSnippetText;
        int startIndex;
        int endIndex;
        String variableValue;

        SnippetVariable(String text, int start, int end) {
            this.lspSnippetText = text;
            this.startIndex = start;
            this.endIndex = end;
            this.variableValue = this.getVariableValue(text);
        }

        private String getVariableValue(String lspVarSnippet) {
            return lspVarSnippet.contains(":") ? lspVarSnippet.substring(lspVarSnippet.indexOf(58) + 1,
                    lspVarSnippet.lastIndexOf(125)) : "";
        }
    }

    @Override
    @NotNull
    public String getQuickDocString(@NotNull TextDocumentIdentifier identifier, @NotNull Position position) {
        String hoverString = ERROR_DOC;
        try {
            hoverString = LSPThreadPoolManager.pool(() -> requestForHoverString(identifier, position))
                    .get(HOVER.getDefaultTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Get quick doc error");
            wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        } catch (TimeoutException e) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Get quick doc timeout");
            wrapper.notifyFailure(Timeouts.HOVER);
        }
        return hoverString;
    }

    @NotNull
    private String requestForHoverString(@NotNull TextDocumentIdentifier identifier, @NotNull Position position) {
        String hoverResult = ERROR_DOC;
        CompletableFuture<Hover> request = requestManager.hover(new HoverParams(identifier, position));
        if (request == null) {
            return hoverResult;
        }
        try {
            Hover hover = request.get(getTimeout(HOVER), TimeUnit.MILLISECONDS);
            wrapper.notifySuccess(Timeouts.HOVER);
            if (hover == null) {
                LOG.debug(String.format(Locale.ROOT, "Hover is null for file %s and pos (%d;%d)",
                        identifier.getUri(), position.getLine(), position.getCharacter()));
            }
            hoverResult = CangjieHoverHandler.getHoverString(hover, project);
            if (StringUtils.isEmpty(hoverResult)) {
                LOG.warn(String.format(Locale.ROOT, "Hover string returned is empty for file %s and pos (%d;%d)",
                        identifier.getUri(), position.getLine(), position.getCharacter()));
            }
            return hoverResult;
        } catch (TimeoutException e) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Hover timeout");
            wrapper.notifyFailure(Timeouts.HOVER);
        } catch (InterruptedException | JsonRpcException | ExecutionException e) {
            LOG.warn("Hover error");
            wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return hoverResult;
    }

    @Override
    public void highlight() {
        invokeLater(() -> {
            if (!EditorEventManagerBase.EDITOR_TO_MANAGER.containsKey(editor)) {
                return;
            }
            MarkupModel markupModelEx = editor.getMarkupModel();
            for (RangeHighlighter lighter : markupModelEx.getAllHighlighters()) {
                if (lighter.getLayer() == DOCUMENT_HIGHLIGHT_LAYER) {
                    markupModelEx.removeHighlighter(lighter);
                }
            }
            highlighterList.clear();
            TextAttributes textAttributes = editor.getColorsScheme().getAttributes(SELECTION);
            int currentOffset = editor.getCaretModel().getOffset();
            LSPThreadPoolManager.pool(() -> {
                boolean shouldReturn = ReadAction.computeBlocking(() -> {
                    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                    if (psiFile != null) {
                        PsiElement preEle = psiFile.findElementAt(currentOffset - 1);
                        if (preEle != null && INVALID_TOKENS.contains(preEle.getText())) {
                            return true;
                        }
                    }
                    return false;
                });
                if (shouldReturn) {
                    return;
                }
                Position position = DocumentUtils.offsetToLSPPos(editor, currentOffset);
                DocumentHighlightParams params = new DocumentHighlightParams(getIdentifier(), position);
                documentHighlight(params, textAttributes, markupModelEx);
            });
        });
    }

    private void documentHighlight(DocumentHighlightParams params, TextAttributes textAttributes,
                                   MarkupModel markupModelEx) {
        CompletableFuture<List<? extends DocumentHighlight>> request = getRequestManager().documentHighlight(params);
        if (request == null || wrapper.getServer() == null) {
            return;
        }
        List<? extends DocumentHighlight> results = null;
        try {
            results = request.get(getTimeout(HOVER), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("Document highlight error");
        } catch (TimeoutException e) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Timeout warning");
        }
        if (results == null) {
            return;
        }
        final List<? extends DocumentHighlight> finalResult = results;
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        invokeLater(() -> {
            for (DocumentHighlight result : finalResult) {
                Range range = result.getRange();
                Position start = range.getStart();
                Position end = range.getEnd();
                int startOffset = DocumentUtils.lspPosToOffset(editor, start);
                int endOffset = DocumentUtils.lspPosToOffset(editor, end);
                if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) {
                    return;
                }
                if (psiFile != null) {
                    PsiElement elementAtRange = psiFile.findElementAt(startOffset);
                    // 如果是仓颉资源标识符，跳过server的高亮机制
                    if (isMacroR(elementAtRange)) {
                        continue;
                    }
                }
                RangeHighlighter highlighter = markupModelEx.addRangeHighlighter(startOffset, endOffset,
                    DOCUMENT_HIGHLIGHT_LAYER, textAttributes, HighlighterTargetArea.EXACT_RANGE);
                highlighterList.add(highlighter);
            }
        });
    }

    /**
     * completionAction
     *
     * @param lookupItems lookupItems
     * @param res result
     * @param startTime startTime
     */
    public void completionAction(List<LookupElement> lookupItems, Either<List<CompletionItem>,
            CompletionList> res, long startTime) {
        ApplicationUtils.readAction(() -> {
            getLookupItem(lookupItems, res, startTime);
        });
    }

    private boolean isMacroR(PsiElement element) {
        if (element == null) {
            return false;
        }
        // 判断是否是资源标识符
        return CANGJIE_RESOURCE_IDENTIFIER.equals(element.getText())
                && PsiTreeUtil.getParentOfType(element, CjMacroExpression.class) != null;
    }

    private void getLookupItem(List<LookupElement> lookupItems, Either<List<CompletionItem>, CompletionList> res,
        long startTime) {
        Iterator var6;
        CompletionItem item;
        LookupElement lookupElement;
        if (res.getLeft() instanceof List) {
            var6 = ((List) res.getLeft()).iterator();

            while (var6.hasNext()) {
                var next = var6.next();
                item = next instanceof CompletionItem ? (CompletionItem) next : null;
                if (item == null) {
                    continue;
                }
                addTrickyItem(item);
                lookupElement = this.createLookupItem(item);
                if (lookupElement != null) {
                    lookupItems.add(lookupElement);
                }
            }
        } else if (res.getRight() != null) {
            var6 = res.getRight().getItems().iterator();

            while (var6.hasNext()) {
                var next = var6.next();
                item = next instanceof CompletionItem ? (CompletionItem) next : null;
                if (item == null) {
                    continue;
                }
                lookupElement = this.createLookupItem(item);
                if (lookupElement != null) {
                    lookupItems.add(lookupElement);
                }
            }
        } else {
            LOG.warn("Left and Right are null");
        }
        long duration = System.currentTimeMillis() - startTime;
        if (duration > (long) LogThreshold.COMPLETION.getThreshold()) {
            LOG.info(String.format(Locale.ROOT, "Completion successfully, it took %d ms", duration));
        }
    }

    /**
     * async completion handler
     *
     * @param pos trigger position
     * @return completion result
     */
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionAsync(Position pos) {
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> promise = new CompletableFuture<>();
        LSPThreadPoolManager.pool(() -> {
            try {
                CompletableFuture<Either<List<CompletionItem>, CompletionList>> request =
                        this.requestManager.completion(new CompletionParams(super.getIdentifier(), pos));
                if (request == null) {
                    promise.complete(null);
                    return;
                }
                this.completionDataHandler.reset();
                try {
                    promise.complete(request.get(TIME_COMPLETION, TimeUnit.MILLISECONDS));
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    timeoutCrashCheck(requestManager);
                    promise.complete(null);
                }
            } catch (JsonRpcException var8) {
                LOG.warn("Completion error");
                this.wrapper.crashed(var8);
                TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
                CrashLogPackager.packageLogFiles();
                promise.complete(null);
            }
        });
        return promise;
    }

    /**
     * completion for Cangjie LSP
     *
     * @param pos completion position
     * @param isGeneric pos is in generic
     * @return completion result
     */
    public Iterable<? extends LookupElement> completion(Position pos, boolean isGeneric) {
        List<LookupElement> lookupItems = new ArrayList();
        Optional<PsiElement> psiElement = PsiElementFinder.findPsiElementAtCaret(project, editor);
        if (psiElement.isEmpty()) {
            return lookupItems;
        }
        String text = psiElement.get().getText();
        if (!text.isEmpty() && Character.isDigit(text.charAt(0))) {
            return lookupItems;
        }
        this.setIsInGeneric(isGeneric);
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> request =
                this.requestManager.completion(new CompletionParams(super.getIdentifier(), pos));
        if (request == null) {
            return lookupItems;
        }
        this.completionDataHandler.reset();
        long startTime = System.currentTimeMillis();
        try {
            Either<List<CompletionItem>, CompletionList> res = request.get(TIME_COMPLETION, TimeUnit.MILLISECONDS);
            this.wrapper.notifySuccess(Timeouts.COMPLETION);
            if (res == null) {
                return lookupItems;
            }
            completionAction(lookupItems, res, startTime);
            return lookupItems;
        } catch (InterruptedException | TimeoutException var7) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Completion timeout");
            this.wrapper.notifyFailure(Timeouts.COMPLETION);
            return lookupItems;
        } catch (ExecutionException | JsonRpcException var8) {
            LOG.warn("Completion error");
            this.wrapper.crashed(var8);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
            return lookupItems;
        }
    }

    @Override
    public void handleMethodCursor(LookupElement lookupElement) {
    }
    @Override
    public void characterTyped(char chr) {
    }

    @Override
    public LookupElement createLookupItem(@NotNull CompletionItem item) {
        StringBuilder insertText = new StringBuilder(item.getInsertText());
        if (item.getKind() == CompletionItemKind.Keyword) {
            insertText.append(Constants.KEYWORD_WITH_SPACE.contains(insertText.toString()) ? this.isGeneric : "");
        }
        if (item.getKind() == CompletionItemKind.Class) {
            String detail = item.getDetail();
            if (!StringUtils.isEmpty(detail) && detail.startsWith("(struct)")) {
                item.setKind(CompletionItemKind.Struct);
            }
        }
        String label = item.getLabel();

        String lookupString = null;
        if (item.getTextEdit() != null && item.getTextEdit().getLeft() != null) {
            lookupString = item.getTextEdit().getLeft().getNewText();
        } else if (StringUtils.isNotEmpty(insertText)) {
            lookupString = insertText.toString();
        } else if (StringUtils.isNotEmpty(label)) {
            lookupString = label;
        } else {
            return null;
        }
        if (StringUtils.isEmpty(lookupString)) {
            return null;
        }
        String detail = item.getDetail();
        if (StringUtils.isEmpty(detail)) {
            detail = lookupString;
        }

        // handle the completionTriggers Char words
        if (getTriggerChar() == '.' || getTriggerChar() == '`') {
            lookupString = handleCompletionTriggers(lookupString, getTriggerChar());
        }

        // Fixes IDEA internal assertion failure in windows.
        lookupString = lookupString.replace(DocumentUtils.WIN_SEPARATOR, DocumentUtils.LINUX_SEPARATOR);

        CompletionItemKind kind = item.getKind();
        LookupElementBuilder lookupElementBuilder = getLookupElementBuilder(item, lookupString, detail);
        return returnLookupItem(lookupElementBuilder, item, lookupString, kind);
    }

    @NotNull
    private LookupElementBuilder getLookupElementBuilder(CompletionItem item, String lookupString,
        String detail) {
        LookupElementBuilder lookupElementBuilder;
        if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
            lookupElementBuilder = LookupElementBuilder.create(lookupString + "&" + detail,
                    convertPlaceHolders(lookupString));
        } else {
            lookupElementBuilder = LookupElementBuilder.create(detail, lookupString);
        }
        if (item.getDeprecated()) {
            lookupElementBuilder = lookupElementBuilder.withStrikeoutness(true);
        }
        return lookupElementBuilder;
    }

    @Override
    @NotNull
    public String getCompletionPrefix(@NotNull Editor editor, int offset) {
        if (!(editor instanceof EditorEx)) {
            throw new ClassCastException("Forced type conversion failed!");
        }
        return "";
    }

    @Nullable
    private LookupElement returnLookupItem(LookupElementBuilder lookupElementBuilder, CompletionItem item,
        String lookupString, CompletionItemKind kind) {
        // data is from context of LSP completion, containing the location of declaration and definition
        CompletionData data = null;
        try {
            Object obj = item.getData();
            if (obj instanceof JsonElement) {
                data = GSON.fromJson((JsonElement) obj, CompletionData.class);
            }
        } catch (JsonSyntaxException | JsonIOException e) {
            LOG.error("error in location of the completionItem");
        }
        LookupElementBuilder retLookupElementBuilder = addCompletionInsertHandlers(item, lookupElementBuilder,
                lookupString);
        if (data != null) {
            Project editorProject = editor.getProject();
            Location location = data.getDeclaration();
            if (editorProject == null || location == null) {
                return null;
            }
            PsiElement declarationPsiElement = locationToPsiElement(editorProject, location);
            retLookupElementBuilder = retLookupElementBuilder.withPsiElement(declarationPsiElement);
            getCompletionDataHandler().buildMapping(data.getDeclaration(), data.getDefinition());
        }

        if (kind == CompletionItemKind.Keyword) {
            retLookupElementBuilder = retLookupElementBuilder.withBoldness(true);
        }

        String insertText = item.getInsertText();
        String label = item.getLabel();
        LSPIconProvider iconProvider = GUIUtils.getIconProviderFor(wrapper.getServerDefinition());
        Icon icon = iconProvider.getCompletionIcon(kind);
        String presentableText = StringUtils.isNotEmpty(label) ? label : (insertText != null) ? insertText : "";
        String detail = item.getDetail();
        String tailText = (detail != null) ? detail : "";
        InsertHandler<LookupElement> originalHandler = retLookupElementBuilder.getInsertHandler();
        // 重新包装一个 Handler
        retLookupElementBuilder = retLookupElementBuilder.withInsertHandler((context, lookupItem) -> {
            // 先执行原有的逻辑
            if (originalHandler != null) {
                originalHandler.handleInsert(context, lookupItem);
            }

            String insertedText = lookupItem.getLookupString();
            // 只有在非函数补全的情况下，才手动修正光标
            // 这样不会干扰已经启动的函数参数模板（Template）
            if (!insertedText.contains("(") && !insertedText.contains("{")) {
                context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
            }
        });

        // 创建 LookupElement
        LookupElement lookupElement = retLookupElementBuilder.withPresentableText(presentableText)
                .withTypeText(tailText, true)
                .withIcon(icon)
                .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT);

        // 为 LookupElement 赋予权重
        double priority = 999999d - Double.parseDouble(item.getSortText());
        lookupElement = PrioritizedLookupElement.withPriority(lookupElement, priority);

        return lookupElement;
    }

    /**
     * referencesForFindUsages
     *
     * @param offset                   offset
     * @param shouldGetOriginalElement should Include Definitions or not
     * @return {@link Pair}<{@link List}<{@link PsiElement}>,
     * {@link List}<{@link VirtualFile}>>
     */
    public Pair<List<PsiElement>, List<VirtualFile>> referencesForFindUsages(int offset,
            boolean shouldGetOriginalElement) {
        Position lspPos = DocumentUtils.offsetToLSPPos(this.editor, offset);
        ReferenceParams params = new ReferenceParams(super.getIdentifier(), lspPos,
                new ReferenceContext(shouldGetOriginalElement));
        CompletableFuture<List<? extends Location>> request = this.requestManager.references(params);
        return request == null ? new Pair<>(null, null) : findReferences(request);
    }

    private Pair<List<PsiElement>, List<VirtualFile>> findReferences(CompletableFuture<List<? extends Location>> req) {
        try {
            List<? extends Location> res = req.get(TIME_REFERENCES, TimeUnit.MILLISECONDS);
            this.wrapper.notifySuccess(Timeouts.REFERENCES);
            if (res == null || res.isEmpty()) {
                return new Pair<>(null, null);
            }
            List<VirtualFile> openedEdts = new ArrayList<>();
            List<PsiElement> psiElements = new ArrayList<>();
            res.forEach(loc -> {
                Position locStart = loc.getRange().getStart();
                Position locEnd = loc.getRange().getEnd();
                String uri = FileUtils.sanitizeURI(loc.getUri());
                VirtualFile vFile = FileUtils.virtualFileFromURI(uri);
                if (vFile == null) {
                    return;
                }
                PsiFile psiFile = PsiManager.getInstance(this.project).findFile(vFile);
                Document document = FileDocumentManager.getInstance().getDocument(vFile);
                if (document == null || psiFile == null) {
                    return;
                }
                if (locStart.getLine() < 0 || locStart.getLine() >= document.getLineCount()) {
                    return;
                }
                int logicalStart = document.getLineStartOffset(locStart.getLine()) + locStart.getCharacter();
                int logicalEnd = document.getLineStartOffset(locEnd.getLine()) + locEnd.getCharacter();
                if (logicalStart < 0 || logicalEnd > document.getTextLength()) {
                    return;
                }
                String name = document.getText(new TextRange(logicalStart, logicalEnd));
                psiElements.add(new LSPPsiElement(name, this.project, logicalStart, logicalEnd, psiFile));
                openedEdts.add(vFile);
            });
            return new Pair<>(psiElements, openedEdts);
        } catch (TimeoutException timeoutException) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Find references link timeout");
            this.wrapper.notifyFailure(Timeouts.REFERENCES);
            return new Pair<>(null, null);
        } catch (JsonRpcException | ExecutionException | InterruptedException exception) {
            LOG.warn("Find references link error");
            this.wrapper.crashed(exception);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
            return new Pair<>(null, null);
        }
    }

    /**
     * cjSignatureHelp
     *
     * @return SignatureHelp
     */
    @Nullable
    public SignatureHelp cjSignatureHelp() {
        if (!this.editor.isDisposed()) {
            LogicalPosition lPos = this.editor.getCaretModel().getCurrentCaret().getLogicalPosition();
            SignatureHelpParams params =
                new SignatureHelpParams(super.getIdentifier(), DocumentUtils.logicalToLSPPos(lPos, this.editor));
            CompletableFuture<SignatureHelp> future = this.requestManager.signatureHelp(params);
            if (future != null) {
                try {
                    SignatureHelp signatureResp =
                        future.get(Timeout.getTimeout(Timeouts.CJ_SIGNATURE), TimeUnit.MILLISECONDS);
                    this.wrapper.notifySuccess(Timeouts.CJ_SIGNATURE);
                    return signatureResp;
                } catch (TimeoutException var9) {
                    timeoutCrashCheck(requestManager);
                    LOG.warn("Signature help timeout");
                    this.wrapper.notifyFailure(Timeouts.CJ_SIGNATURE);
                } catch (ExecutionException | InterruptedException | JsonRpcException var10) {
                    LOG.warn("Signature help error");
                    this.wrapper.crashed(var10);
                    TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
                    CrashLogPackager.packageLogFiles();
                } catch (Exception var11) {
                    LOG.warn("Internal error occurred when processing signature help");
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CangjieEditorEventManager{" + ", requestManager=" + requestManager
                + ", identifier=" + super.getIdentifier() + ", project=" + project + ", hoverAlarm=" + hoverAlarm
                + ", completionDataHandler=" + completionDataHandler + ", highlighterList=" + highlighterList + '}';
    }

    private void showPopup() {
        CommonRefactoringUtil.showErrorHint(project, this.editor,
                RefactoringBundle.getCannotRefactorMessage(CangjieEditorEventManager.RENAME_FAILURE_MESSAGE),
                RefactoringBundle.message("rename.title"), null);
    }

    private void showPopup(String message) {
        CommonRefactoringUtil.showErrorHint(project, this.editor,
                RefactoringBundle.getCannotRefactorMessage(message),
                RefactoringBundle.message("rename.title"), null);
    }

    @Override
    public void prepareRename() {
        Position pos = DocumentUtils.offsetToLSPPos(this.editor,
                this.editor.getCaretModel().getCurrentCaret().getOffset());
        LSPThreadPoolManager.pool(() -> {
            if (this.editor.isDisposed()) {
                return;
            }
            PrepareRenameParams params = new PrepareRenameParams(super.getIdentifier(), pos);
            CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> request =
                this.requestManager.prepareRename(params);
            if (request == null) {
                return;
            }
            request.thenAccept(this::beforeRename).exceptionally((ex -> {
                String errorMessage;
                Throwable cause = ex.getCause();
                if (cause instanceof ResponseErrorException) {
                    errorMessage = ((ResponseErrorException) cause).getResponseError().getMessage();
                } else {
                    errorMessage = ex.getMessage();
                }

                this.showPopup(errorMessage);
                return null;
            }));
        });
    }

    private void beforeRename(Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> res) {
        if (res == null) {
            this.showPopup();
            return;
        }
        Range range = null;
        String placeholder = "";
        String name = "";
        if (res.isFirst() && res.getLeft() instanceof Range r) {
            range = r;
        }

        if (res.isSecond() && res.getSecond() instanceof PrepareRenameResult prepareRenameResult) {
            range = prepareRenameResult.getRange();
            placeholder = prepareRenameResult.getPlaceholder();
        }

        if (range != null) {
            Position start = range.getStart();
            Position end = range.getEnd();
            int logicalStart = DocumentUtils.lspPosToOffset(this.editor, start);
            int logicalEnd = DocumentUtils.lspPosToOffset(this.editor, end);
            if (logicalStart < 0 || logicalEnd > this.editor.getDocument().getTextLength()) {
                return;
            }
            name = this.editor.getDocument().getText(new TextRange(logicalStart, logicalEnd));
        }

        String initialValue = placeholder.isEmpty() ? name : placeholder;
        ApplicationUtils.invokeLater(() -> {
            CangjieRenameDialog dialog = new CangjieRenameDialog(this.editor.getProject(),
                CangjieBundle.message("lsp.rename.dialog.title"), initialValue);
            if (!dialog.showAndGet()) {
                return;
            }
            String renameTo = dialog.getInput();
            if (renameTo != null && !renameTo.isEmpty()) {
                this.rename(renameTo);
            }
        });
    }

    @Override
    public void rename(String renameTo, int offset) {
        LSPThreadPoolManager.pool(() -> {
            if (editor.isDisposed()) {
                return;
            }
            Position servPos = DocumentUtils.offsetToLSPPos(editor, offset);
            RenameParams params = new RenameParams(super.getIdentifier(), servPos, renameTo);
            CompletableFuture<WorkspaceEdit> request = this.requestManager.rename(params);
            if (request == null) {
                LOG.warn("rename fail: request is null");
                return;
            }
            request.thenAccept(res -> {
                doRename(res);
                LSPRenameProcessor.clearEditors();
            }).exceptionally(e -> {
                LOG.error("Rename error");
                return null;
            });
        });
    }

    private void doRename(WorkspaceEdit edit) {
        ApplicationManager.getApplication().invokeLater(() -> {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    rename(edit);
                });
            }, "Rename", null);
        });
    }

    private void rename(WorkspaceEdit edit) {
        List<Either<TextDocumentEdit, ResourceOperation>> eithers = edit.getDocumentChanges();
        Set<String> unOpenedFiles = new HashSet<>();
        for (Either<TextDocumentEdit, ResourceOperation> either : eithers) {
            TextDocumentEdit textDocumentEdit = either.getLeft();
            if (textDocumentEdit == null) {
                continue;
            }
            String uri = textDocumentEdit.getTextDocument().getUri();
            Path path;
            try {
                path = Path.of(new URI(uri));
            } catch (URISyntaxException e) {
                LOG.error("Rename invalid uri");
                continue;
            }
            VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(path);
            if (file == null) {
                continue;
            }
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                continue;
            }
            TextDocumentItem textDocumentItem = new TextDocumentItem(uri, "Cangjie",
                    textDocumentEdit.getTextDocument().getVersion() == -1
                            ? 0 : textDocumentEdit.getTextDocument().getVersion(), document.getText());
            boolean hasEditor = EditorFactory.getInstance().getEditors(document).length != 0;
            if (!hasEditor) {
                notifyDidOpenFile(wrapper, textDocumentItem);
            }
            WriteAction.run(() -> {
                if (!replaceNewText(document, textDocumentEdit, textDocumentItem, hasEditor)
                        || hasEditor) {
                    return;
                }
                unOpenedFiles.add(uri);
                notifyDidCloseFile(wrapper, uri);
            });
        }
        for (String fileUri : unOpenedFiles) {
            notifyDidChangeWatchFile(wrapper, fileUri, FileChangeType.Changed);
        }
    }

    private boolean replaceNewText(Document document,
                                   TextDocumentEdit textDocumentEdit,
                                   TextDocumentItem textDocumentItem,
                                   boolean hasEditor) {
        boolean isReplaced = false;
        List<TextEdit> textEdits = textDocumentEdit.getEdits();
        if (textEdits.isEmpty()) {
            return isReplaced;
        }
        List<TextEdit> sortedTextEdits = getSortedTextEdits(textEdits);
        List<TextDocumentContentChangeEvent> contentChanges = new ArrayList<>();
        for (TextEdit textEdit : sortedTextEdits) {
            int startOffset = positionToOffset(document, textEdit.getRange().getStart());
            int endOffset = positionToOffset(document, textEdit.getRange().getEnd());
            if (startOffset < 0 || endOffset < 0 || endOffset > document.getTextLength()) {
                continue;
            }
            String newText = textEdit.getNewText();
            if (endOffset - startOffset <= 0) {
                document.insertString(startOffset, newText);
            } else {
                document.replaceString(startOffset, endOffset, newText);
            }
            isReplaced = true;
            if (!hasEditor) {
                addContentChanges(textDocumentItem, textEdit, contentChanges, newText);
            }
        }
        if (!hasEditor) {
            notifyDidChange(wrapper, textDocumentEdit.getTextDocument(), contentChanges);
        }
        return isReplaced;
    }

    /**
     * 按照Position的大小从大到小重排,保证每处修改的文本偏移量都是正确的(先改前面的会导致偏移量变化)
     *
     * @param textEdits textEdits
     * @return sorted textEdits
     */
    @NotNull
    private List<TextEdit> getSortedTextEdits(List<TextEdit> textEdits) {
        return textEdits.stream().sorted((textEdit1, textEdit2) -> {
            Position start1 = textEdit1.getRange().getStart();
            Position start2 = textEdit2.getRange().getStart();
            int line1 = start1.getLine();
            int line2 = start2.getLine();
            int character1 = start1.getCharacter();
            int character2 = start2.getCharacter();
            return line1 == line2 ? character2 - character1 : line2 - line1;
        }).toList();
    }

    private void addTrickyItem(@NotNull CompletionItem item) {
        if (Objects.equals(item.getLabel(), "ForEach(subcomponent: () -> Unit)")
                && Objects.equals(item.getDetail(), "(function) public func init(subcomponent: () -> Unit)")
                && Objects.equals(item.getInsertText(), "ForEach(${1:subcomponent: () -> Unit})")) {
            item.setLabel("ForEach(dataSource: ArrayList<T>, itemGeneratorFunc!: (T, Int64) -> Unit, "
                    + "keyGeneratorFunc!: (T, Int64) -> String): Unit");
            item.setDetail("(function) public func ForEach(dataSource: ArrayList<T>, itemGeneratorFunc!: (T, Int64) "
                    + "-> Unit, keyGeneratorFunc!: (T, Int64) -> String): Unit");
            item.setInsertText("ForEach(${1:dataSource: ArrayList<T>}, itemGeneratorFunc: ${2:(T, Int64) -> Unit}, "
                    + "keyGeneratorFunc: ${3:(T, Int64) -> String})");
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }

        if (Objects.equals(item.getLabel(), "LazyForEach()")
                && Objects.equals(item.getDetail(), "(function) public func init()")
                && Objects.equals(item.getInsertText(), "LazyForEach()")) {
            item.setLabel("LazyForEach(dataSource: IDataSource<T>, itemGeneratorFunc!: (T, Int64) -> Unit, "
                    + "keyGeneratorFunc!: (T, Int64) -> String): Unit");
            item.setDetail("(function) public func LazyForEach(dataSource: IDataSource<T>, itemGeneratorFunc!: (T, "
                    + "Int64) -> Unit, keyGeneratorFunc!: (T, Int64) -> String): Unit");
            item.setInsertText("LazyForEach(${1:dataSource: IDataSource<T>}, itemGeneratorFunc: ${2:(T, Int64) -> "
                    + "Unit}, keyGeneratorFunc: ${3:(T, Int64) -> String})");
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }
    }

    @Override
    public void mouseClicked(EditorMouseEvent event) {
        if (event.getEditor() != editor) {
            LOG.error("Wrong editor for EditorEventManager");
            return;
        }
    }

    @Override
    public void removeListeners() {
        if (!isRemoved) {
            super.removeListeners();
            isRemoved = true;
        }
    }

    /**
     * set the space string
     *
     * @param isGeneric the position in generic
     */
    public synchronized void setIsInGeneric(boolean isGeneric) {
        if (isGeneric) {
            this.isGeneric = "";
        } else {
            this.isGeneric = " ";
        }
    }

    @Override
    public void prepareAndRunSnippet(String insertText) {
        List<SnippetVariable> variables = new ArrayList<>();
        // Extracts variables using placeholder REGEX pattern.
        Matcher varMatcher = Pattern.compile(SNIPPET_PLACEHOLDER_REGEX).matcher(insertText);
        while (varMatcher.find()) {
            variables.add(new SnippetVariable(varMatcher.group(), varMatcher.start(), varMatcher.end()));
        }

        variables.sort(Comparator.comparingInt(obj -> obj.startIndex));
        final String[] finalInsertText = {insertText};
        variables.forEach(var -> finalInsertText[0] = finalInsertText[0].replace(var.lspSnippetText, "$"));

        // Matches $. If consecutive $is encountered, only the second one is matched.
        // text:    VArray<$, $$>
        // match:          ^   ^
        String[] splitInsertText = finalInsertText[0].split("\\$(?!\\$)|(?<=\\$)\\$");
        finalInsertText[0] = String.join("", splitInsertText);

        TemplateManager tm = TemplateManager.getInstance(getProject());
        if (tm.createTemplate(finalInsertText[0], "lsp4intellij") instanceof TemplateImpl) {
            TemplateImpl template = (TemplateImpl) tm.createTemplate(finalInsertText[0], "lsp4intellij");
            template.parseSegments();

            if (variables.isEmpty()) {
                // prevent "smart" indent of next line...
                template.setToIndent(false);
            }
            final int[] varIndex = {0};
            variables.forEach(var -> {
                template.addTextSegment(splitInsertText[varIndex[0]]);
                template.addVariable(varIndex[0] + "_" + var.variableValue, new TextExpression(var.variableValue),
                        new TextExpression(var.variableValue), true, false);
                varIndex[0]++;
            });
            // If the snippet text ends with a placeholder, there will be no string segment left to append
            // after the last variable.
            if (splitInsertText.length != variables.size()) {
                template.addTextSegment(splitInsertText[splitInsertText.length - 1]);
            }
            template.setInline(true);
            EditorModificationUtil.moveCaretRelatively(editor, -template.getTemplateText().length());
            TemplateManager.getInstance(getProject()).startTemplate(editor, template);
        }
    }

    @Override
    public LookupElementBuilder addCompletionInsertHandlers(CompletionItem item, @NotNull LookupElementBuilder builder,
                                                            String lookupString) {
        LookupElementBuilder resultBuilder = builder;
        Command command = item.getCommand();
        List<TextEdit> addTextEdits = item.getAdditionalTextEdits();
        InsertTextFormat format = item.getInsertTextFormat();
        CompletionItemKind kind = item.getKind();
        if (item.getTextEdit() != null && item.getTextEdit().getLeft() != null) {
            if (addTextEdits != null) {
                resultBuilder = setInsertHandler(resultBuilder, item, lookupString);
            } else {
                resultBuilder = getLookupElementBuilder(resultBuilder, lookupString, format, kind, item);
            }
        } else if (addTextEdits != null) {
            resultBuilder = setInsertHandler(resultBuilder, item, lookupString);
        } else if (command != null) {
            resultBuilder = resultBuilder.withInsertHandler((InsertionContext context, LookupElement lookupElement) -> {
                TraceUtils.trace(TraceUtils.Action.COMPLETION);
                applyInitialTextEdit(item, context, lookupString);

                if (format == InsertTextFormat.Snippet) {
                    context.commitDocument();
                    prepareAndRunSnippet(lookupString);
                }
                if (kind == CompletionItemKind.Function || kind == CompletionItemKind.Method) {
                    handleMethodCursor(lookupElement);
                }
                context.commitDocument();
                executeCommands(Collections.singletonList(command));
            });
        } else {
            resultBuilder = getLookupElementBuilder(resultBuilder, lookupString, format, kind, item);
        }
        return resultBuilder;
    }

    @Override
    protected LookupElementBuilder setInsertHandler(LookupElementBuilder builder,
                                                    CompletionItem item, String lookupString) {
        String label = item.getLabel();
        Command command = item.getCommand();
        List<TextEdit> addTextEdits = item.getAdditionalTextEdits();
        InsertTextFormat format = item.getInsertTextFormat();
        CompletionItemKind kind = item.getKind();

        return builder.withInsertHandler((InsertionContext context, LookupElement lookupElement) -> {
            TraceUtils.trace(TraceUtils.Action.COMPLETION);
            applyInitialTextEdit(item, context, lookupString);

            if (format == InsertTextFormat.Snippet) {
                prepareAndRunSnippet(lookupString);
                context.commitDocument();
            }
            if (kind == CompletionItemKind.Function || kind == CompletionItemKind.Method) {
                handleMethodCursor(lookupElement);
            }
            context.commitDocument();
            invokeLater(() -> {
                applyEdit(addTextEdits, "Completion : " + label, false);
                if (command != null) {
                    executeCommands(Collections.singletonList(command));
                }
            });
        });
    }

    boolean applyEdit(List<? extends TextEdit> edits, String name, boolean setCaret) {
        return applyEdit(Integer.MAX_VALUE, edits, name, false, setCaret);
    }

    boolean applyEdit(int version, List<? extends TextEdit> edits,
                      String name, boolean closeAfter, boolean setCaret) {
        return applyEdit(editor.getDocument(), version, edits, name, closeAfter, setCaret);
    }

    private LookupElementBuilder getLookupElementBuilder(@NotNull LookupElementBuilder builder, String lookupString,
                                                         InsertTextFormat format, CompletionItemKind kind,
                                                         CompletionItem item) {
        return builder.withInsertHandler((InsertionContext context, LookupElement lookupElement) -> {
            applyInitialTextEdit(item, context, lookupString);
            TraceUtils.trace(TraceUtils.Action.COMPLETION);
            if (format == InsertTextFormat.Snippet) {
                context.commitDocument();
                prepareAndRunSnippet(lookupString);
            }
            if (kind == CompletionItemKind.Function || kind == CompletionItemKind.Method) {
                handleMethodCursor(lookupElement);
            }
        });
    }

    /**
     * requestAndShowCodeActions
     */
    @Override
    public void requestAndShowCodeActions() { // disable code action
    }

    /**
     * codeAction for Refactor
     *
     * @param startOffset int
     * @param endOffset int
     * @return codeAction results
     */
    public List<Either<Command, CodeAction>> codeAction4Refactor(int startOffset, int endOffset) {
        Range range = new Range(DocumentUtils.offsetToLSPPos(this.editor, startOffset),
                DocumentUtils.offsetToLSPPos(this.editor, endOffset));
        List<Diagnostic> diagnosticContext = Collections.emptyList();
        CodeActionParams params = new CodeActionParams(this.getIdentifier(), range,
                new CodeActionContext(diagnosticContext));
        CompletableFuture<List<Either<Command, CodeAction>>> future = this.getRequestManager().codeAction(params);
        if (future == null) {
            return Collections.emptyList();
        }
        try {
            List<Either<Command, CodeAction>> res =
                    future.get(Timeout.getTimeout(Timeouts.CODEACTION), TimeUnit.MILLISECONDS);
            this.wrapper.notifySuccess(Timeouts.CODEACTION);
            return Objects.requireNonNullElse(res, Collections.emptyList());
        } catch (TimeoutException var9) {
            timeoutCrashCheck(requestManager);
            LOG.warn("Code action timeout");
            this.wrapper.notifyFailure(Timeouts.CODEACTION);
            return Collections.emptyList();
        } catch (JsonRpcException | ExecutionException | InterruptedException var10) {
            LOG.warn("Code action error");
            this.wrapper.crashed(var10);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            return Collections.emptyList();
        }
    }

    /**
     * executeCommands for refactor
     *
     * @param commands commands
     */
    public void executeCommands4Refactor(List<Command> commands) {
        pool(() -> {
            if (editor.isDisposed()) {
                return;
            }
            commands.stream().map(command -> {
                ExecuteCommandParams params = new ExecuteCommandParams(command.getCommand(), command.getArguments());
                return getRequestManager().executeCommand(params);
            }).filter(Objects::nonNull).forEach(file -> {
                try {
                    file.get(getTimeout(EXECUTE_COMMAND), TimeUnit.MILLISECONDS);
                    wrapper.notifySuccess(Timeouts.EXECUTE_COMMAND);
                } catch (TimeoutException te) {
                    timeoutCrashCheck(requestManager);
                    LOG.warn("Apply Refactor timeout");
                    wrapper.notifyFailure(Timeouts.EXECUTE_COMMAND);
                } catch (JsonRpcException | InterruptedException e) {
                    LOG.warn("Apply Refactor error");
                    wrapper.crashed(e);
                    TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
                } catch (ExecutionException e) {
                    String message = e.getMessage().substring(e.getMessage().indexOf(": ") + ": ".length());
                    CommonRefactoringUtil.showErrorHint(getProject(), editor,
                            String.format(Locale.ROOT, message),
                            "Refactoring Error From Server", null);
                    LOG.warn("Apply Refactor error");
                }
            });
        });
    }

    /**
     * documentClosed
     */
    @Override
    public void documentClosed() {
        VirtualFile virtualFile = editor.getVirtualFile();
        if (virtualFile != null) {
            CangjieDiagnosticAnnotatorHolder.diagnosticsRecord.remove(virtualFile);
        }
        super.documentClosed();
    }
}
