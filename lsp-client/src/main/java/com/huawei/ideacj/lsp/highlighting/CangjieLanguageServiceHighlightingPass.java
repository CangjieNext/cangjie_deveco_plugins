/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.highlighting;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 功能描述: 打开的文件有error时 目录树飘红
 *
 * @since 2024-01-27
 */
public class CangjieLanguageServiceHighlightingPass extends TextEditorHighlightingPass {
    private static final String ACTUAL_TEXT = "actual:";
    private static final Object CANGJIE_UI_SERVICE_SOURCE = "cangjie";

    @NotNull
    private final PsiFile myFile;

    @NotNull
    private final Project myProject;

    @NotNull
    private final Editor myEditor;

    @NotNull
    private final List<HighlightInfo> myHighlightInfoList = new ArrayList<>();

    CangjieLanguageServiceHighlightingPass(@NotNull Project project, @NotNull PsiFile file, @NotNull Editor editor,
                                           @NotNull Document document) {
        super(project, document, false);
        this.myFile = file;
        this.myProject = project;
        this.myEditor = editor;
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        setResults();
    }

    private void setResults() {
        ApplicationManager.getApplication().invokeLater(() -> {
            MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, true);
            RangeHighlighter[] allHighlighters = markupModel.getAllHighlighters();
            Set<HighlightInfo> allHighlightInfo = getAllHighlightInfo(allHighlighters);
            myHighlightInfoList.clear();
            myHighlightInfoList.addAll(allHighlightInfo);
        });
    }

    @NotNull
    private Set<HighlightInfo> getAllHighlightInfo(RangeHighlighter[] allHighlighters) {
        if (allHighlighters.length == 0) {
            return Collections.emptySet();
        }
        return Arrays.stream(allHighlighters)
            .filter(highlighter -> !myDocument.getText(
                new TextRange(highlighter.getStartOffset(), highlighter.getEndOffset())).isEmpty())
            .map(HighlightInfo::fromRangeHighlighter)
            .filter(Objects::nonNull)
            .filter(info -> HighlightSeverity.ERROR.getName().equals(info.getSeverity().getName()))
            .filter(highlightInfo -> !highlightInfo.toString().contains(ACTUAL_TEXT))
            .collect(Collectors.toSet());
    }

    @Override
    public void doApplyInformationToEditor() {
        ApplicationManager.getApplication()
            .executeOnPooledThread(
                () -> updateWolfTheProblemSolver(myFile, CollectionUtils.isNotEmpty(myHighlightInfoList)));
    }

    private void updateWolfTheProblemSolver(@NotNull PsiFile file, boolean hasError) {
        Project project = file.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        VirtualFile virtualFile = file.getVirtualFile();
        WolfTheProblemSolver wolfTheProblemSolver = WolfTheProblemSolver.getInstance(project);
        if (document == null) {
            wolfTheProblemSolver.clearProblemsFromExternalSource(virtualFile, CANGJIE_UI_SERVICE_SOURCE);
            return;
        }
        ReadAction.computeBlocking(() -> {
            if (hasError) {
                wolfTheProblemSolver.reportProblemsFromExternalSource(virtualFile, CANGJIE_UI_SERVICE_SOURCE);
            } else {
                wolfTheProblemSolver.clearProblemsFromExternalSource(virtualFile, CANGJIE_UI_SERVICE_SOURCE);
            }
            return null;
        });
    }

    @Override
    @NotNull
    public List<HighlightInfo> getInfos() {
        return myHighlightInfoList;
    }
}