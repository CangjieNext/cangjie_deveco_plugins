/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.bracematcher;

import com.huawei.ideacj.language.CangJieTypes;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.codeInsight.highlighting.BraceMatcher;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.CharArrayUtil;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieEnterAfterUnmatchedBraceHandler
 *
 * @since 2025/07/30
 */
public class CangjieEnterAfterUnmatchedBraceHandler extends EnterHandlerDelegateAdapter {
    private static final Logger LOG = Logger.getInstance(CangjieEnterAfterUnmatchedBraceHandler.class);

    public CangjieEnterAfterUnmatchedBraceHandler() {
    }

    @Override
    public EnterHandlerDelegate.Result preprocessEnter(@NotNull PsiFile file,
                                                       @NotNull Editor editor,
                                                       @NotNull Ref<Integer> caretOffsetRef,
                                                       @NotNull Ref<Integer> caretAdvance,
                                                       @NotNull DataContext dataContext,
                                                       EditorActionHandler originalHandler) {
        int caretOffset = caretOffsetRef.get();
        if (!this.isApplicable(file, caretOffset)) {
            return Result.Continue;
        }
        int maxRBraceCount = this.getMaxRBraceCount(file, editor, caretOffset);
        if (maxRBraceCount > 0) {
            this.insertRBraces(file, editor, caretOffset,
                    this.getRBraceOffset(file, editor, caretOffset),
                    this.generateStringToInsert(editor, caretOffset, maxRBraceCount));
            return Result.DefaultForceIndent;
        }
        return Result.Continue;
    }

    public boolean isApplicable(@NotNull PsiFile file, int caretOffset) {
        return true;
    }

    private int getMaxRBraceCount(@NotNull PsiFile file, @NotNull Editor editor, int caretOffset) {
        return !CodeInsightSettings.getInstance().INSERT_BRACE_ON_ENTER
                ? 0 : Math.max(0, getUnmatchedLBracesNumberBefore(editor, caretOffset, file.getFileType()));
    }

    @NotNull
    private String generateStringToInsert(@NotNull Editor editor, int caretOffset, int maxRBraceCount) {
        CharSequence text = editor.getDocument().getCharsSequence();
        int bracesToInsert = 0;

        for (int i = caretOffset - 1; i >= 0 && bracesToInsert < maxRBraceCount; --i) {
            char c = text.charAt(i);
            if (c == '{') {
                ++bracesToInsert;
                continue;
            }
            if (this.isStopChar(c)) {
                break;
            }
        }
        return StringUtil.repeatSymbol('}', Math.max(bracesToInsert, 1));
    }

    private boolean isStopChar(char c) {
        return " \n\t".indexOf(c) < 0;
    }

    private int getRBraceOffset(@NotNull PsiFile file, @NotNull Editor editor, int caretOffset) {
        CharSequence text = editor.getDocument().getCharsSequence();
        int offset = CharArrayUtil.shiftForward(text, caretOffset, " \t");
        int fileLength = text.length();
        if (offset < fileLength && ")];,%<?".indexOf(text.charAt(offset)) < 0) {
            offset = this.calculateOffsetToInsertClosingBrace(file, text, offset).second;
        }

        return Math.min(offset, fileLength);
    }

    private void insertRBraces(@NotNull PsiFile file,
                               @NotNull Editor editor,
                               int caretOffset, int rBracesInsertOffset, String generatedRBraces) {
        Document document = editor.getDocument();
        this.insertRBracesAtPosition(document, caretOffset, rBracesInsertOffset, generatedRBraces);
        this.formatCodeFragmentBetweenBraces(
                file, document, editor, caretOffset, rBracesInsertOffset);
    }

    private void insertRBracesAtPosition(Document document, int caretOffset, int rBracesInsertOffset,
                                         String generatedRBraces) {
        document.insertString(rBracesInsertOffset, "\n" + generatedRBraces);
        document.insertString(caretOffset, "\n");
    }

    private void formatCodeFragmentBetweenBraces(@NotNull PsiFile file,
                                                   @NotNull Document document,
                                                   @NotNull Editor editor,
                                                   int caretOffset, int rBracesInsertOffset) {
        Project project = file.getProject();
        long stamp = document.getModificationStamp();

        boolean closingBraceIndentAdjusted;
        try {
            PsiDocumentManager.getInstance(project).commitDocument(document);
            CodeStyleManager.getInstance(project).adjustLineIndent(file,
                    new TextRange(caretOffset, rBracesInsertOffset + 2));
        } catch (IncorrectOperationException exception) {
            LOG.info("failed to adjustLineIndent");
        } finally {
            closingBraceIndentAdjusted = stamp != document.getModificationStamp();
            document.deleteString(caretOffset, caretOffset + 1);
        }

        if (!closingBraceIndentAdjusted) {
            int line = document.getLineNumber(rBracesInsertOffset);
            StringBuilder buffer = new StringBuilder();
            int start = document.getLineStartOffset(line);
            int end = document.getLineEndOffset(line);
            CharSequence text = document.getCharsSequence();
            for (int i = start; i < end; ++i) {
                char c = text.charAt(i);
                if (c != ' ' && c != '\t') {
                    break;
                }

                buffer.append(c);
            }
            if (!buffer.isEmpty()) {
                document.insertString(rBracesInsertOffset + 1, buffer);
            }
        }
    }

    private Pair<PsiElement, Integer> calculateOffsetToInsertClosingBrace(@NotNull PsiFile file,
                                                                          @NotNull CharSequence text, int offset) {
        PsiElement element = PsiUtilCore.getElementAtOffset(file, offset);
        ASTNode node = element.getNode();
        if (node != null
                && (node.getElementType() == TokenType.WHITE_SPACE || node.getElementType() == CangJieTypes.NL)) {
            return Pair.create(null, CharArrayUtil.shiftForwardUntil(text, offset, "\n"));
        } else {
            for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
                ASTNode parentNode = parent.getNode();
                if (parentNode == null || parentNode.getStartOffset() != offset) {
                    break;
                }

                element = parent;
            }
            return element.getTextOffset() != offset
                    ? Pair.create(null, CharArrayUtil.shiftForwardUntil(text, offset, "\n"))
                    : Pair.create(element, this.calculateOffsetToInsertClosingBraceInsideElement(element));
        }
    }

    private int calculateOffsetToInsertClosingBraceInsideElement(PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    private static int getUnmatchedLBracesNumberBefore(Editor editor, int offset, FileType fileType) {
        if (offset == 0) {
            return -1;
        }
        CharSequence chars = editor.getDocument().getCharsSequence();
        if (chars.charAt(offset - 1) != '{') {
            return -1;
        }
        EditorHighlighter highlighter = editor.getHighlighter();
        HighlighterIterator iterator = highlighter.createIterator(offset - 1);
        BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
        if (braceMatcher.isLBraceToken(iterator, chars, fileType)
                && braceMatcher.isStructuralBrace(iterator, chars, fileType)) {
            Language language = iterator.getTokenType().getLanguage();
            iterator = highlighter.createIterator(0);
            int lBracesBeforeOffset = 0;

            int rBracesAfterOffset;
            for (rBracesAfterOffset = 0; !iterator.atEnd(); iterator.advance()) {
                IElementType tokenType = iterator.getTokenType();
                if (tokenType.getLanguage().equals(language)
                        && braceMatcher.isStructuralBrace(iterator, chars, fileType)) {
                    boolean beforeOffset = iterator.getStart() < offset;
                    if (braceMatcher.isLBraceToken(iterator, chars, fileType)) {
                        if (beforeOffset) {
                            ++lBracesBeforeOffset;
                        } else {
                            --rBracesAfterOffset;
                        }
                        continue;
                    }
                    if (braceMatcher.isRBraceToken(iterator, chars, fileType)) {
                        if (beforeOffset) {
                            if (lBracesBeforeOffset > 0) {
                                --lBracesBeforeOffset;
                            }
                        } else {
                            ++rBracesAfterOffset;
                            if (rBracesAfterOffset == lBracesBeforeOffset) {
                                return 0;
                            }
                        }
                    }
                }
            }
            return lBracesBeforeOffset - rBracesAfterOffset;
        }
        return -1;
    }
}
