/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.surround;

import static com.intellij.psi.util.PsiTreeUtil.skipParentsOfType;

import com.huawei.ideacj.language.psi.othersnode.CjEnd;
import com.huawei.ideacj.language.psi.packagenode.CjPackageHeader;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputExprWithoutParens;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.lang.folding.CustomFoldingProvider;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.lang.surroundWith.ModCommandSurrounder;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModCommand;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

/**
 * CangJieDefaultSurroundDescriptor
 *
 * @since 2026-05-08
 */
public class CangJieCustomFoldingSurroundDescriptor implements SurroundDescriptor {
    /**
     * INSTANCE
     */
    public static final CangJieCustomFoldingSurroundDescriptor INSTANCE = new CangJieCustomFoldingSurroundDescriptor();

    private static final String DEFAULT_DESC_TEXT = "Description";

    @Override
    public PsiElement @NotNull [] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        if (!(file.getLanguage() instanceof CangJieLanguage)) {
            return PsiElement.EMPTY_ARRAY;
        }
        if (startOffset >= endOffset) {
            return PsiElement.EMPTY_ARRAY;
        }
        Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(file.getLanguage());
        if (commenter == null || commenter.getLineCommentPrefix() == null
                && (commenter.getBlockCommentPrefix() == null || commenter.getBlockCommentSuffix() == null)) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement startElement = file.findElementAt(startOffset);
        PsiElement endElement = file.findElementAt(endOffset - 1);
        if (endElement instanceof CjEnd || endElement.getNode().getElementType().getDebugName().equals("NL")) {
            endElement = file.findElementAt(endOffset - 2);
        }
        Document document = file.getFileDocument();
        if (startElement instanceof PsiWhiteSpace) {
            if (startElement == endElement) {
                return PsiElement.EMPTY_ARRAY;
            }
            startElement = startElement.getNextSibling();
        }
        if (endElement instanceof PsiWhiteSpace) {
            endElement = endElement.getPrevSibling();
        }
        if (startElement == null || endElement == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        int firstLineStart = document.getLineStartOffset(document.getLineNumber(startOffset));
        int lastLine = document.getLineNumber(endOffset - 1);
        int lastLineEnd = lastLine < document.getLineCount() - 1
                ? document.getLineStartOffset(lastLine + 1)
                : document.getTextLength();
        startElement = findClosestParentAfterLineBreak(startElement, document);
        if (startElement == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        endElement = findClosestParentBeforeLineBreak(endElement, document);
        if (endElement == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement[] normalized = normalizeToSameLevel(startElement, endElement);
        if (normalized != null) {
            startElement = normalized[0];
            endElement = normalized[1];
        }
        return adjustRange(startElement, endElement, firstLineStart, lastLineEnd);
    }

    @Nullable
    private static PsiElement[] normalizeToSameLevel(PsiElement start, PsiElement end) {
        PsiElement commonAncestor = PsiTreeUtil.findCommonParent(start, end);
        if (commonAncestor == null) {
            return null;
        }
        PsiElement normalizedStart = findDirectChildOf(commonAncestor, start);
        PsiElement normalizedEnd = findDirectChildOf(commonAncestor, end);
        if (normalizedStart != null && normalizedEnd != null) {
            return new PsiElement[] {normalizedStart, normalizedEnd};
        }
        return null;
    }

    private static PsiElement @NotNull [] adjustRange(@NotNull PsiElement start, @NotNull PsiElement end,
        int firstLineStart, int lastLineEnd) {
        PsiElement lowerStart = lowerStartElementIfNeeded(start, end);
        PsiElement lowerEnd = lowerEndElementIfNeeded(start, end);
        if (lowerStart == null || lowerEnd == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement commonParent = findCommonAncestorForWholeRange(lowerStart, lowerEnd);
        if (commonParent != null) {
            if (commonParent instanceof CjMacroInputExprWithoutParens || commonParent instanceof CjPackageHeader) {
                return PsiElement.EMPTY_ARRAY;
            }
            return checkResultRange(lowerStart, lowerEnd, firstLineStart, lastLineEnd);
        }
        PsiElement newStartParent = getParent(lowerStart);
        PsiElement newEndParent = getParent(lowerEnd);
        boolean canExpandTogether = newStartParent != null
            && newStartParent == newEndParent
            && newStartParent.getFirstChild() == lowerStart
            && newEndParent.getLastChild().getPrevSibling() == lowerEnd
            && newEndParent.getLastChild() instanceof CjEnd;
        if (canExpandTogether) {
            lowerStart = newStartParent;
            lowerEnd = newEndParent;
        }
        if (newStartParent.getNode().getElementType().getDebugName().equals("ifExpression")
                && newEndParent.getNode().getElementType().getDebugName().equals("ifExpression")) {
            return checkResultRange(newStartParent, newEndParent, firstLineStart, lastLineEnd);
        }
        if (getParent(lowerStart) == getParent(lowerEnd)) {
            if (lowerEnd.getLastChild() instanceof CjEnd) {
                lowerEnd = lowerEnd.getLastChild().getPrevSibling();
            }
            return checkResultRange(lowerStart, lowerEnd, firstLineStart, lastLineEnd);
        }
        return PsiElement.EMPTY_ARRAY;
    }

    @Nullable
    private static PsiElement findIfExpression(PsiElement start) {
        PsiElement current = start;
        while (current != null) {
            if (current.getChildren().length > 1) {
                if ("ifExpression".equals(current.getNode().getElementType().getDebugName())) {
                    return current;
                }
                break;
            }
            PsiElement firstChild = current.getFirstChild();
            if (firstChild == null) {
                break;
            }
            current = firstChild;
        }
        return null;
    }

    private static PsiElement @NotNull [] checkResultRange(@NotNull PsiElement resultStart,
                                                           @NotNull PsiElement resultEnd,
        int firstLineStart, int lastLineEnd) {
        PsiElement ifExpression = findIfExpression(resultStart.getFirstChild());
        if (ifExpression != null) {
            return new PsiElement[] {resultStart, resultEnd};
        }
        int resultStartOffset = resultStart.getTextRange().getStartOffset();
        int resultEndOffset = resultEnd.getTextRange().getEndOffset();
        if (resultStartOffset < firstLineStart || resultEndOffset > lastLineEnd) {
            return PsiElement.EMPTY_ARRAY;
        }
        return new PsiElement[] {resultStart, resultEnd};
    }

    @Nullable
    private static PsiElement getParent(@Nullable PsiElement psiElement) {
        return psiElement instanceof PsiFile
            ? psiElement
            : skipParentsOfType(psiElement, GeneratedParserUtilBase.DummyBlock.class);
    }

    @Nullable
    private static PsiElement lowerEndElementIfNeeded(@NotNull PsiElement elementStart,
        @NotNull PsiElement elementEnd) {
        if (PsiTreeUtil.isAncestor(elementEnd, elementStart, true)) {
            PsiElement lastChild = elementEnd.getLastChild();
            while (lastChild != null && lastChild.getParent() != elementStart.getParent()) {
                PsiElement last = lastChild.getLastChild();
                if (last == null) {
                    return lastChild;
                }
                lastChild = last;
            }
            return lastChild;
        }
        return elementEnd;
    }

    @Nullable
    private static PsiElement lowerStartElementIfNeeded(@NotNull PsiElement elementStart,
        @NotNull PsiElement elementEnd) {
        if (PsiTreeUtil.isAncestor(elementStart, elementEnd, true)) {
            PsiElement firstChild = elementStart.getFirstChild();
            while (firstChild != null && firstChild.getParent() != elementEnd.getParent()) {
                PsiElement first = firstChild.getFirstChild();
                if (first == null) {
                    return firstChild;
                }
                firstChild = first;
            }
            return firstChild;
        }
        return elementStart;
    }

    @Nullable
    private static PsiElement findCommonAncestorForWholeRange(@NotNull PsiElement start, @NotNull PsiElement end) {
        PsiElement newEnd = end;
        if (start.getContainingFile() != end.getContainingFile()) {
            return null;
        }
        if (end.getNextSibling() instanceof CjEnd) {
            newEnd = end.getNextSibling();
        }
        final PsiElement parent = PsiTreeUtil.findCommonParent(start, newEnd);
        if (parent == null) {
            return null;
        }
        final TextRange parentRange = parent.getTextRange();
        if (parentRange.getStartOffset() == start.getTextRange().getStartOffset()
                && parentRange.getEndOffset() == newEnd.getTextRange().getEndOffset()) {
            return parent;
        }
        return null;
    }

    @Nullable
    private static PsiElement findDirectChildOf(PsiElement ancestor, PsiElement descendant) {
        if (descendant == ancestor) {
            return descendant;
        }
        PsiElement current = descendant;
        while (current != null) {
            PsiElement parent = current.getParent();
            if (parent == ancestor) {
                return current;
            }
            if (parent == null || parent instanceof PsiFileSystemItem) {
                break;
            }
            current = parent;
        }
        return descendant;
    }

    @Nullable
    private static PsiElement findClosestParentAfterLineBreak(PsiElement element, Document document) {
        int line = document.getLineNumber(element.getTextRange().getStartOffset());
        int lineStart = document.getLineStartOffset(line);
        PsiElement parent = element;
        while (parent != null && !(parent instanceof PsiFileSystemItem)) {
            PsiElement prev = parent.getPrevSibling();
            while (prev != null) {
                if (prev.getTextRange().getStartOffset() < lineStart) {
                    return parent;
                }
                prev = prev.getPrevSibling();
            }
            if (parent.getTextRange().getStartOffset() < lineStart && parent.getParent().getChildren().length != 1) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    private static PsiElement findClosestParentBeforeLineBreak(PsiElement element, Document document) {
        int line = document.getLineNumber(element.getTextRange().getEndOffset());
        int lineEnd = document.getLineEndOffset(line);
        int documentEnd = document.getLineEndOffset(document.getLineCount() - 1);
        PsiElement parent = element;
        while (parent != null && !(parent instanceof PsiFileSystemItem)) {
            PsiElement next = parent.getNextSibling();
            while (next != null) {
                if (next.getTextRange().getEndOffset() > lineEnd) {
                    return parent;
                }
                next = next.getNextSibling();
            }
            if ((parent.getTextRange().getEndOffset() > lineEnd || parent.getTextRange().getEndOffset() == documentEnd)
                    && parent.getParent().getChildren().length != 1) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Override
    public Surrounder @NotNull [] getSurrounders() {
        // noinspection TestOnlyProblems
        return getAllSurrounders().toArray(new CangJieCustomFoldingSurroundDescriptor.CustomFoldingRegionSurrounder[0]);
    }

    /**
     * getAllSurrounders
     *
     * @return List<Surrounder>
     */
    @TestOnly
    @NotNull
    public static List<Surrounder> getAllSurrounders() {
        return ContainerUtil.map(
                CustomFoldingProvider.getAllProviders(),
                provider -> new CangJieCustomFoldingSurroundDescriptor.CustomFoldingRegionSurrounder(provider));
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    private static final class CustomFoldingRegionSurrounder extends ModCommandSurrounder {
        private final CustomFoldingProvider customFoldingProvider;

        CustomFoldingRegionSurrounder(@NotNull CustomFoldingProvider provider) {
            customFoldingProvider = provider;
        }

        @Override
        public String getTemplateDescription() {
            return customFoldingProvider.getDescription();
        }

        @Override
        public boolean isApplicable(@NotNull PsiElement @NotNull [] elements) {
            if (elements.length == 0) {
                return false;
            }
            if (elements[0].getContainingFile() instanceof PsiCodeFragment) {
                return false;
            }
            Language language = elements[0].getLanguage();
            if (!customFoldingProvider.isSupported(language)) {
                return false;
            }
            for (FoldingBuilder each : LanguageFolding.INSTANCE.allForLanguage(language)) {
                if (customFoldingProvider.isSupportedBy(each)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @NotNull
        public ModCommand surroundElements(@NotNull ActionContext context, @NotNull PsiElement @NotNull [] elements) {
            return ModCommand.psiUpdate(context,
                    updater -> doSurround(context, ContainerUtil.map(elements, updater::getWritable), updater));
        }

        private void doSurround(@NotNull ActionContext context,
                                @NotNull List<@NotNull PsiElement> elements, @NotNull ModPsiUpdater updater) {
            if (elements.isEmpty()) {
                return;
            }
            PsiElement firstElement = elements.get(0);
            PsiElement lastElement = elements.get(elements.size() - 1);
            PsiFile psiFile = firstElement.getContainingFile();
            String linePrefix;
            String lineSuffix;
            Language language = psiFile.getLanguage();
            if (customFoldingProvider.wrapStartEndMarkerTextInLanguageSpecificComment()) {
                Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
                if (commenter == null) {
                    return;
                }
                linePrefix = commenter.getLineCommentPrefix();
                lineSuffix = "";
                if (linePrefix == null) {
                    linePrefix = commenter.getBlockCommentPrefix();
                    lineSuffix = StringUtil.notNullize(commenter.getBlockCommentSuffix());
                }
                if (linePrefix == null) {
                    return;
                }
            } else {
                linePrefix = "";
                lineSuffix = "";
            }
            int prefixLength = linePrefix.length();

            int insertionStartPoint = firstElement.getTextRange().getStartOffset();
            final Document document = firstElement.getContainingFile().getFileDocument();
            final int startLineNumber = document.getLineNumber(insertionStartPoint);
            final String startIndent =
                    document.getText(new TextRange(document.getLineStartOffset(startLineNumber), insertionStartPoint));
            int insertionEndPoint = lastElement.getTextRange().getEndOffset();
            int totalCharsAdded = 0;
            TextRange newCursorSelection = TextRange.create(insertionStartPoint, insertionStartPoint);
            String startFoldingTemplate = customFoldingProvider.getStartString();
            int placeholderIndex = startFoldingTemplate.indexOf("?");
            if (placeholderIndex >= 0) {
                startFoldingTemplate = startFoldingTemplate.replace("?", DEFAULT_DESC_TEXT);
                newCursorSelection = TextRange.from(insertionStartPoint + placeholderIndex, DEFAULT_DESC_TEXT.length());
            }

            String fullStartBlock = linePrefix + startFoldingTemplate + lineSuffix + "\n" + startIndent;
            String fullEndBlock = "\n" + startIndent + linePrefix + customFoldingProvider.getEndString() + lineSuffix;
            document.insertString(insertionEndPoint, fullEndBlock);
            totalCharsAdded += fullEndBlock.length();
            document.insertString(insertionStartPoint, fullStartBlock);
            totalCharsAdded += fullStartBlock.length();

            RangeMarker finalSelectionMarker = document.createRangeMarker(newCursorSelection.shiftRight(prefixLength));
            Project project = context.project();
            PsiDocumentManager.getInstance(project).commitDocument(document);
            adjustLineIndent(project, psiFile, language,
                    TextRange.from(insertionEndPoint + totalCharsAdded - fullEndBlock.length(), fullEndBlock.length()));
            adjustLineIndent(project, psiFile, language, TextRange.from(insertionStartPoint, fullStartBlock.length()));
            newCursorSelection =
                TextRange.create(finalSelectionMarker.getStartOffset(), finalSelectionMarker.getEndOffset());
            finalSelectionMarker.dispose();
            updater.select(newCursorSelection);
        }

        private static void adjustLineIndent(
                @NotNull Project project, PsiFile file, Language language, TextRange range) {
            CodeStyleSettings settings = CodeStyle.getSettings(file);
            CodeStyleSettings cloneSettings = CodeStyleSettingsManager.getInstance(project).cloneSettings(settings);
            CommonCodeStyleSettings formatSettings = cloneSettings.getCommonSettings(language);
            formatSettings.KEEP_FIRST_COLUMN_COMMENT = false;
            CodeStyle.runWithLocalSettings(project, cloneSettings,
                    () -> CodeStyleManager.getInstance(project).adjustLineIndent(file, range));
        }
    }
}