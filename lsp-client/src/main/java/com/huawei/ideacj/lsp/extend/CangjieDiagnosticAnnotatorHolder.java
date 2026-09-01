/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend;

import com.huawei.ideacj.lsp.extend.params.CangjieCodeActionFix;
import com.huawei.ideacj.lsp.extend.params.ExtendDiagnostic;
import com.huawei.ideacj.lsp.extend.params.NavigateToRelatedFix;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.IntentionsUI;
import com.intellij.codeInsight.daemon.impl.TrafficLightRenderer;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.multiverse.CodeInsightContext;
import com.intellij.codeInsight.multiverse.CodeInsightContexts;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.instanceContainer.internal.ContainerDisposedException;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CangjieDiagnosticAnnotatorHolder
 *
 * @since 2024-01-26
 */
public class CangjieDiagnosticAnnotatorHolder {
    /**
     * record current opened file diagnostics
     */
    public static final Map<VirtualFile, List<ExtendDiagnostic>> diagnosticsRecord = new ConcurrentHashMap<>();

    /**
     * diagnostic2HighlightInfoTypeMap to change DiagnosticSeverity to HighlightInfo
     */
    protected static final Map<DiagnosticSeverity, HighlightInfoType> DIAGNOSTIC_SEVERITY_TO_HIGHLIGHT_INFO_TYPE =
            Map.of(
                    DiagnosticSeverity.Error, HighlightInfoType.ERROR,
                    DiagnosticSeverity.Warning, HighlightInfoType.WARNING,
                    DiagnosticSeverity.Information, HighlightInfoType.INFORMATION,
                    DiagnosticSeverity.Hint, HighlightInfoType.WEAK_WARNING
            );

    /**
     * diagnostic2HighlightInfoTypeMap to change tags to HighlightInfo
     */
    protected static final Map<DiagnosticTag, HighlightInfoType> DIAGNOSTIC_TAGS_TO_HIGHLIGHT_INFO_TYPE =
            Map.of(
                    DiagnosticTag.Deprecated, HighlightInfoType.DEPRECATED,
                    DiagnosticTag.Unnecessary, HighlightInfoType.UNUSED_SYMBOL
            );

    private static final Logger LOG = Logger.getInstance(CangjieDiagnosticAnnotatorHolder.class);

    private static final List<String> SUPPRESSED_UNUSED_PARAM_PREFIXES = List.of(
            "Parameter 'elmtId' is declared but never used",
            "Parameter 'isInitialRender' is declared but never used"
    );

    private static void makeAnnotations(@NotNull Editor editor, @NotNull List<ExtendDiagnostic> diagnostics) {
        List<HighlightInfo> highlightInfos = new ArrayList<HighlightInfo>();
        diagnostics.forEach(diagnostic -> {
            if (!isDiagnosticCodeValid(diagnostic.getCode())) {
                return;
            }
            if (isSuppressedUnusedParamDiagnostic(diagnostic)) {
                return;
            }
            makeAnnotation(diagnostic, editor, highlightInfos);
        });
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 使用 ReadAction 包装所有 PSI 访问
            ReadAction.computeBlocking(() -> {
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

                // invokeLater 部分也需要在 ReadAction 内或使用合适的线程调度
                ApplicationManager.getApplication().invokeLater(() -> {
                    ReadAction.computeBlocking(() -> {
                        try {
                            Document document = editor.getDocument();
                            clearDiagsInMarkupModel(project, document);
                            IntentionsUI.getInstance(editor.getProject()).invalidate();
                            updateErrorStripe(editor, document, project);
                            UpdateHighlightersUtil.setHighlightersToEditor(project, document, 0,
                                    document.getTextLength(), highlightInfos, null, 0);

                            VirtualFile vFile = editor.getVirtualFile();
                            if (vFile == null || file == null) {
                                return null;
                            }

                            if ((!diagnosticsRecord.containsKey(vFile) && diagnostics.isEmpty())
                                    || (diagnosticsRecord.containsKey(vFile)
                                    && CollectionUtils.isEqualCollection(
                                    diagnosticsRecord.get(vFile), diagnostics))) {
                                return null;
                            }
                            diagnosticsRecord.put(vFile, diagnostics);
                            // set 0ms delay for extreme performance
                            DaemonCodeAnalyzerSettings.getInstance().forceUseZeroAutoReparseDelayIn(() -> {
                                DaemonCodeAnalyzer.getInstance(project).restart(file, "Global restart");
                            });
                        } catch (ContainerDisposedException e) {
                            LOG.warn("Container disposed exception", e);
                        }
                        return null;
                    });
                });
                return null;
            });
        });
    }

    private static void clearDiagsInMarkupModel(@NotNull Project project, @NotNull Document document) {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);
        RangeHighlighter[] allHighlighters = markupModel.getAllHighlighters();
        List<RangeHighlighter> diagRangeHighlighter = Arrays.stream(allHighlighters)
                .filter(CangjieDiagnosticAnnotatorHolder::hasErrorOrWarn)
                .toList();
        for (RangeHighlighter rangeHighlighter : diagRangeHighlighter) {
            markupModel.removeHighlighter(rangeHighlighter);
        }
    }

    private static boolean hasErrorOrWarn(RangeHighlighter rangeHighlighter) {
        if (rangeHighlighter == null) {
            return false;
        }
        HighlightSeverity highlightSeverity = Optional.ofNullable(HighlightInfo.fromRangeHighlighter(rangeHighlighter))
                .map(HighlightInfo::getSeverity)
                .orElse(null);
        Object obj = rangeHighlighter.getErrorStripeTooltip();
        if (obj instanceof HighlightInfo highlightInfo) {
            String toolId = highlightInfo.getInspectionToolId();
            if ("CangjieRawFileInspection".equals(toolId) || "CangjieResourceInspection".equals(toolId)) {
                return false;
            }
        }
        return HighlightSeverity.ERROR.equals(highlightSeverity) || HighlightSeverity.WARNING.equals(highlightSeverity);
    }

    private static void updateErrorStripe(@NotNull Editor editor, @NotNull Document document,
        @NotNull Project project) {
        MarkupModel markupModel1 = editor.getMarkupModel();
        if (!(markupModel1 instanceof EditorMarkupModelImpl editorMarkupModel)) {
            return;
        }
        ErrorStripeRenderer errorStripeRenderer = editorMarkupModel.getErrorStripeRenderer();
        if (!(errorStripeRenderer instanceof TrafficLightRenderer trafficLightRenderer)) {
            return;
        }
        try {
            Field errorCountField = TrafficLightRenderer.class.getDeclaredField("errorCount");
            errorCountField.setAccessible(true);
            Object errorCountObj = errorCountField.get(trafficLightRenderer);
            if (errorCountObj == null) {
                return;
            }

            MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);
            AtomicInteger errCount = new AtomicInteger();
            AtomicInteger warnCount = new AtomicInteger();
            RangeHighlighter[] allHighlighters = markupModel.getAllHighlighters();

            Arrays.stream(allHighlighters).forEach(rangeHighlighter -> {
                HighlightSeverity highlightSeverity =
                    Optional.ofNullable(HighlightInfo.fromRangeHighlighter(rangeHighlighter))
                        .map(HighlightInfo::getSeverity).orElse(null);
                if (highlightSeverity == HighlightSeverity.ERROR) {
                    errCount.getAndIncrement();
                }
                if (highlightSeverity == HighlightSeverity.WARNING) {
                    warnCount.getAndIncrement();
                }
            });

            // 获取 incErrorCount 方法
            Method incErrorCountMethod = errorCountObj.getClass().getDeclaredMethod(
                "incErrorCount",
                HighlightSeverity.class,
                CodeInsightContext.class,
                int.class);
            incErrorCountMethod.setAccessible(true);

            // 获取默认 CodeInsightContext
            CodeInsightContext defaultContext = CodeInsightContexts.defaultContext();

            // 先清空错误计数
            Method clearMethod = errorCountObj.getClass().getDeclaredMethod("clear");
            clearMethod.setAccessible(true);
            clearMethod.invoke(errorCountObj);

            // 设置新的错误数量
            incErrorCountMethod.invoke(errorCountObj, HighlightSeverity.ERROR, defaultContext, errCount.get());
            incErrorCountMethod.invoke(errorCountObj, HighlightSeverity.WARNING, defaultContext, warnCount.get());
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOG.warn("CangjieDiagnosticAnnotator update ErrorStripe fail.");
        }
    }


    private static void makeAnnotation(@NotNull ExtendDiagnostic diagnostic, @NotNull Editor editor,
        @NotNull List<HighlightInfo> highlightInfos) {
        DiagnosticSeverity severity = diagnostic.getSeverity();
        HighlightInfoType highlightInfoType =
                DIAGNOSTIC_SEVERITY_TO_HIGHLIGHT_INFO_TYPE.getOrDefault(severity, HighlightInfoType.WEAK_WARNING);
        if (diagnostic.getTags() != null && !diagnostic.getTags().isEmpty()) {
            highlightInfoType = DIAGNOSTIC_TAGS_TO_HIGHLIGHT_INFO_TYPE.getOrDefault(
                    diagnostic.getTags().get(0), highlightInfoType);
        }
        quickFixHighlightDiagnosticInfo(diagnostic, editor, highlightInfos, highlightInfoType);
    }

    /**
     * 显示错误
     *
     * @param editor editor
     * @param diagnostics 错误信息
     */
    public static void doAnnotation(@NotNull Editor editor, @NotNull List<ExtendDiagnostic> diagnostics) {
        EdtExecutorService.getInstance().execute(() -> makeAnnotations(editor, diagnostics));
    }

    /**
     * provide quickfix for diagnostic
     *
     * @param diagnostic            diagnostic
     * @param editor                editor
     * @param highlightInfos        highlightInfos
     * @param highlightInfoType     highlightInfoType
     */
    private static void quickFixHighlightDiagnosticInfo(@NotNull ExtendDiagnostic diagnostic,
                                                        @NotNull Editor editor,
                                                        @NotNull List<HighlightInfo> highlightInfos,
                                                        HighlightInfoType highlightInfoType) {
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        int startOffset = DocumentUtils.lspPosToOffset(editor, diagnostic.getRange().getStart());
        int endOffset = DocumentUtils.lspPosToOffset(editor, diagnostic.getRange().getEnd());
        if (startOffset > endOffset || startOffset < 0) {
            return;
        }
        if (startOffset == endOffset) {
            if (diagnostic.getRange().getStart().getLine() >= editor.getDocument().getLineCount()) {
                return;
            }
            int lineEndOffset = editor.getDocument()
                    .getLineEndOffset(diagnostic.getRange().getStart().getLine());
            if (lineEndOffset >= startOffset + 1 || startOffset == 0) {
                endOffset = startOffset + 1;
            } else {
                // fix pos like `from ohos import ability.`
                startOffset = startOffset - 1;
            }
        }

        TextRange range = new TextRange(startOffset, endOffset);
        // 1. 纯文本，给 Problems 栏使用
        String rawMsg = diagnostic.getMessage();

        // 2. HTML 文本，给悬浮窗使用（先做转义，再拼装 <br>）
        String escapedMsg = rawMsg.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        StringBuilder htmlTooltip = new StringBuilder(escapedMsg);

        addRelatedInfo(diagnostic, htmlTooltip);
        String finalHtmlTooltip = "<html>" + htmlTooltip + "</html>";

        HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(highlightInfoType)
                .range(range)
                .description(rawMsg)
                .escapedToolTip(finalHtmlTooltip);

        if (diagnostic.getCodeActions() != null && !diagnostic.getCodeActions().isEmpty()) {
            setPreferredCodeAction(diagnostic);
            List<CangjieCodeActionFix> fixes = new ArrayList<>(ContainerUtil.map(diagnostic.getCodeActions(),
                    codeAction -> new CangjieCodeActionFix(FileUtils.editorToURIString(editor), codeAction)));
            QuickFixAction.registerQuickFixActions(builder, range, fixes);
        }
        showRelatedInfoNavigate(diagnostic, editor, builder, range);
        HighlightInfo highlightInfo = builder.create();

        highlightInfos.add(highlightInfo);
    }

    private static void setPreferredCodeAction(@NotNull ExtendDiagnostic diagnostic) {
        diagnostic.getCodeActions().forEach(codeAction -> {
            if (CangjieCodeActionFix.CodeActionKind.QUICKFIX_ADD_IMPORT.getKind().equals(codeAction.getKind())
                    && codeAction.getTitle().equals(CangjieCodeActionFix.IMPORT_ALL_SYMBOLS)) {
                codeAction.setIsPreferred(true);
            }
            if (CangjieCodeActionFix.CodeActionKind.QUICKFIX_REMOVE_IMPORT.getKind().equals(codeAction.getKind())
                    && !codeAction.getTitle().equals(CangjieCodeActionFix.REMOVE_ALL_IMPORT)) {
                codeAction.setIsPreferred(true);
            }
        });
    }

    private static void addRelatedInfo(@NotNull ExtendDiagnostic diagnostic, StringBuilder diagnosticMessage) {
        if (!isShowRelatedInformation(diagnostic) || diagnostic.getRelatedInformation() == null
                || diagnostic.getRelatedInformation().isEmpty()) {
            return;
        }
        for (DiagnosticRelatedInformation related : diagnostic.getRelatedInformation()) {
            if (related == null) {
                continue;
            }
            VirtualFile virtualFile = FileUtils.uriToVfs(FileUtils.sanitizeURI(related.getLocation().getUri()));
            if (virtualFile == null) {
                continue;
            }
            String escapedRelatedMsg = related.getMessage().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            diagnosticMessage.append("<br>").append(escapedRelatedMsg);
            String path = virtualFile.getCanonicalPath();
            Range lspRange = related.getLocation().getRange();
            diagnosticMessage.append("<br>").append(path)
                    .append(':').append(lspRange.getStart().getLine())
                    .append(':').append(lspRange.getEnd().getCharacter());
        }
    }

    private static void showRelatedInfoNavigate(@NotNull ExtendDiagnostic diagnostic,
                                                @NotNull Editor editor,
                                                HighlightInfo.Builder builder,
                                                TextRange range) {
        if (!isShowRelatedInformation(diagnostic) || diagnostic.getRelatedInformation() == null
                || diagnostic.getRelatedInformation().isEmpty()) {
            return;
        }
        DiagnosticRelatedInformation related = diagnostic.getRelatedInformation().get(0);
        if (related == null) {
            return;
        }
        String uri = related.getLocation().getUri();
        Range relatedRange = related.getLocation().getRange();
        IntentionAction fix = new NavigateToRelatedFix(editor.getProject(), uri, relatedRange, related.getMessage());
        List<IntentionAction> fixes = List.of(fix);
        QuickFixAction.registerQuickFixActions(builder, range, fixes);
    }

    private static boolean isDiagnosticCodeValid(Either<String, Integer> diagnosticCode) {
        if (diagnosticCode == null) {
            return false;
        }
        String codeStr = diagnosticCode.getLeft();
        Integer codeNum = diagnosticCode.getRight();
        return !StringUtil.isEmptyOrSpaces(codeStr) || codeNum != null;
    }

    private static boolean isSuppressedUnusedParamDiagnostic(@NotNull ExtendDiagnostic diagnostic) {
        if (diagnostic.getTags() == null || !diagnostic.getTags().contains(DiagnosticTag.Unnecessary)) {
            return false;
        }
        String message = diagnostic.getMessage();
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        return SUPPRESSED_UNUSED_PARAM_PREFIXES.stream().anyMatch(message::startsWith);
    }

    private static boolean isShowRelatedInformation(ExtendDiagnostic diagnostic) {
        if (diagnostic == null) {
            return false;
        }
        String message = diagnostic.getMessage();
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        return message.startsWith("ambiguous match")
                || message.startsWith("redefinition of declaration")
                || message.startsWith("ambiguous use");
    }

    @Override
    public String toString() {
        return "CangjieDiagnosticAnnotatorHolder{}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}

