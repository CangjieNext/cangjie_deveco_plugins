/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.highlightersetting;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;

/**
 * CangjieExternalAnnotator
 *
 * @since 2022-11-04
 */
public class CangjieExternalAnnotator extends ExternalAnnotator<Editor, Editor> {
    private static final Logger LOG = Logger.getInstance(CangjieExternalAnnotator.class);

    @Nullable
    @Override
    public Editor collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return editor;
    }

    @Nullable
    @Override
    public Editor doAnnotate(Editor collectedInfo) {
        return collectedInfo;
    }

    @Override
    public void apply(@NotNull PsiFile file, @NotNull Editor editor, @NotNull AnnotationHolder holder) {
        AnnotationHolder annotationHolder = holder;
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (manager != null && manager.getAnonHolder() != null) {
            annotationHolder = manager.getAnonHolder();
        }
        if (!(annotationHolder instanceof AnnotationHolderImpl)) {
            return;
        }
        String uri = FileUtils.pathToUri(file.getVirtualFile().getPath());
        if (CangjieSemanticHighlight.getAllRangHighlighter(uri) == null) {
            return;
        }
        List<RangeHighlighter> highlighters = CangjieSemanticHighlight.getAllRangHighlighter(uri);
        if (highlighters == null || highlighters.isEmpty()) {
            return;
        }
        List<RangeHighlighter> highlighterList = null;
        try {
            highlighterList = List.copyOf(highlighters);
        } catch (NullPointerException e) {
            LOG.warn("Failed to copy RangeHighlighters due to a NullPointerException: " + e.getMessage());
        }

        if (highlighterList == null) {
            return;
        }
        for (RangeHighlighter rangeHighlighter : highlighterList) {
            try {
                int start = rangeHighlighter.getStartOffset();
                int end = rangeHighlighter.getEndOffset();
                if (start == -1 || end == -1) {
                    continue;
                }
                TextRange textRange = new TextRange(start, end);
                TextAttributesKey key = rangeHighlighter.getTextAttributesKey();
                if (key == null || isServerSemanticHighlight(start, (AnnotationHolderImpl) annotationHolder)) {
                    continue;
                }

                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .textAttributes(key).range(textRange).create();
            } catch (AssertionError e) {
                LOG.warn("Failed to process RangeHighlighter due to an AssertionError: " + e.getMessage());
            }
        }
    }

    private boolean isServerSemanticHighlight(int rangStart, AnnotationHolderImpl annotationHolder) {
        for (Annotation annotation : annotationHolder) {
            if (rangStart < annotation.getStartOffset() || rangStart > annotation.getEndOffset()) {
                continue;
            }
            return true;
        }
        return false;
    }
}
