/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.syntaxhighlighter;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.PSIElementTypeFactory;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.diagnostic.PluginException;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.TokenIterator;
import com.intellij.lexer.RestartableLexer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.ExceptionWithAttachments;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.PrioritizedDocumentListener;
import com.intellij.openapi.editor.ex.util.DataStorage;
import com.intellij.openapi.editor.ex.util.IntBasedStorage;
import com.intellij.openapi.editor.ex.util.ShortBasedStorage;
import com.intellij.openapi.editor.ex.util.LayeredTextAttributes;
import com.intellij.openapi.editor.ex.util.SegmentArrayWithData;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.EditorDocumentPriorities;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.ImmutableCharSequence;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CangjieEditorHighlighter
 *
 * PrioritizedDocumentListener:
 * Comparator that sorts DocumentListener objects by their priorities (if any).
 *
 * @since 2024-03-16
 */
public class CangjieEditorHighlighter implements EditorHighlighter, PrioritizedDocumentListener {
    private static final Logger LOG = Logger.getInstance(CangjieEditorHighlighter.class);
    private static final int LEXER_INCREMENTALITY_THRESHOLD = 200;
    private static final Set<Class<?>> ourNonIncrementalLexers = new HashSet<>();

    /**
     * thisText
     */
    protected CharSequence thisText;
    private final int thisInitialState;
    private final Lexer thisLexer;
    private final SyntaxHighlighter thisHighlighter;
    private final Map<IElementType, TextAttributes> thisAttributesMap = new HashMap<>();
    private final Map<IElementType, TextAttributesKey[]> thisKeysMap = new HashMap<>();
    private HighlighterClient thisEditor;
    private SegmentArrayWithData thisSegments;
    @NotNull
    private EditorColorsScheme thisScheme;

    /**
     * CangjieEditorHighlighter
     *
     * @param highlighter highlighter
     * @param scheme scheme
     */
    public CangjieEditorHighlighter(@NotNull SyntaxHighlighter highlighter, @NotNull EditorColorsScheme scheme) {
        thisLexer = highlighter.getHighlightingLexer();
        thisLexer.start(Strings.EMPTY_CHAR_SEQUENCE);
        thisInitialState = thisLexer.getState();
        thisHighlighter = highlighter;
        thisScheme = scheme;
        thisSegments = createSegments();
    }

    /**
     * createSegments
     *
     * @return SegmentArrayWithData
     */
    @NotNull
    private SegmentArrayWithData createSegments() {
        return new SegmentArrayWithData(createStorage());
    }

    /**
     * createStorage
     *
     * @return DataStorage
     */
    @NotNull
    protected DataStorage createStorage() {
        return thisLexer instanceof RestartableLexer ? new IntBasedStorage() : new ShortBasedStorage();
    }

    /**
     * getDocument
     *
     * @return Document
     */
    @Nullable
    protected final Document getDocument() {
        return thisEditor != null ? thisEditor.getDocument() : null;
    }

    /**
     * getScheme
     *
     * @return EditorColorsScheme
     */
    @NotNull
    public EditorColorsScheme getScheme() {
        return thisScheme;
    }

    /**
     * setEditor
     *
     * @param editor  editor
     */
    @Override
    public void setEditor(@NotNull HighlighterClient editor) {
        LOG.assertTrue(thisEditor == null, "Highlighters cannot be reused with different editors");
        thisEditor = editor;
    }

    /**
     * setColorScheme
     *
     * @param scheme scheme
     */
    @Override
    public void setColorScheme(@NotNull EditorColorsScheme scheme) {
        thisScheme = scheme;
        thisAttributesMap.clear();
    }

    /**
     * createIterator
     *
     * @param startOffset startOffset
     * @return HighlighterIterator
     */
    @NotNull
    @Override
    public HighlighterIterator createIterator(int startOffset) {
        synchronized (this) {
            if (!isInSyncWithDocument() && getDocument() != null) {
                Document document = getDocument();
                doSetText(document.getImmutableCharSequence());
            }

            int latestValidOffset = thisSegments.getLastValidOffset();
            return new HighlighterIteratorImpl(Math.max(0, Math.min(startOffset, latestValidOffset)));
        }
    }

    /**
     * isInSyncWithDocument
     *
     * @return isInSyncWithDocument
     */
    private boolean isInSyncWithDocument() {
        if (thisSegments.getSegmentCount() > 0) {
            return true;
        }
        Document doc = getDocument();
        return doc == null || doc.getTextLength() == 0;
    }

    private boolean isInitialState(int data) {
        if (thisLexer instanceof RestartableLexer restartableLexer) {
            return restartableLexer.isRestartableState(thisSegments.unpackStateFromData(data));
        }
        return data >= 0;
    }

    int incrementalUpdate(int eventOffset, int eventOldLength, int eventNewLength, @NotNull Document document) {
        CharSequence text = document.getImmutableCharSequence();
        if (thisSegments.getSegmentCount() == 0) {
            setText(text);
            return text.length();
        }
        thisText = text;
        int oldStartIndex = 0;
        try {
            oldStartIndex = Math.max(0, thisSegments.findSegmentIndex(eventOffset) - 2);
        } catch (IllegalStateException e) {
            LOG.warn("Lexer error in find segment for change pos: " + e.getMessage());
        }
        int startIndex = oldStartIndex;
        int data;
        do {
            data = thisSegments.getSegmentData(startIndex);
            if (isInitialState(data) || startIndex == 0) {
                break;
            }
            startIndex--;
        } while (true);
        int startOffset = thisSegments.getSegmentStart(startIndex);
        startLexer(startOffset, text, startIndex);
        Lexer lexerWrapper = new SafeLexerWrapper(thisLexer);
        for (IElementType type = lexerWrapper.getTokenType(); type != null; type = lexerWrapper.getTokenType()) {
            if (startIndex >= oldStartIndex) {
                break;
            }
            data = thisSegments.packData(type, lexerWrapper.getState(), canRestart(lexerWrapper.getState()));
            if (thisSegments.getSegmentStart(startIndex) != lexerWrapper.getTokenStart()
                    || thisSegments.getSegmentEnd(startIndex) != lexerWrapper.getTokenEnd()
                    || thisSegments.getSegmentData(startIndex) != data) {
                break;
            }
            startIndex++;
            try {
                lexerWrapper.advance();
            } catch (PluginException e) {
                LOG.warn("Lexer error in ValidatingLexerWrapper POS1:" + e.getMessage());
            }
        }
        if (ApplicationManager.getApplication().isInternal()
                && startOffset == 0 && startIndex > LEXER_INCREMENTALITY_THRESHOLD) {
            if (!ourNonIncrementalLexers.contains(thisLexer.getClass())) {
                LOG.warn(String.format(Locale.ROOT,
                        "%s is probably not incremental: no initial state throughout %d tokens",
                        thisLexer.getClass().getName(), startIndex));

                ourNonIncrementalLexers.add(thisLexer.getClass());
            }
        }
        startOffset = thisSegments.getSegmentStart(startIndex);
        return getRepaintEnd(new Info(eventOffset, eventOldLength, eventNewLength, startIndex, startOffset),
            lexerWrapper, data, new SegmentArrayWithData(thisSegments.createStorage()), text);
    }

    private static class Info {
        int eventOffset;
        int eventOldLength;
        int eventNewLength;
        int startIndex;
        int startOffset;

        public Info(int eventOffset, int eventOldLength, int eventNewLength, int startIndex, int startOffset) {
            this.eventOffset = eventOffset;
            this.eventOldLength = eventOldLength;
            this.eventNewLength = eventNewLength;
            this.startIndex = startIndex;
            this.startOffset = startOffset;
        }
    }

    private class GetRepaintEndInfo {
        int insertSegmentCount;
        int oldEndIndex;
        int repaintEnd;
        SegmentArrayWithData insertSegments;

        public GetRepaintEndInfo(int insertSegmentCount, int oldEndIndex, int repaintEnd,
            SegmentArrayWithData insertSegments) {
            this.insertSegmentCount = insertSegmentCount;
            this.oldEndIndex = oldEndIndex;
            this.repaintEnd = repaintEnd;
            this.insertSegments = insertSegments;
        }
    }

    private int getRepaintEnd(Info info, Lexer lexerWrapper, int data, SegmentArrayWithData insertSegments,
        CharSequence text) {
        int repaintEnd = -1;
        int insertSegmentCount = 0;
        int oldEndIndex = -1;
        int shift = info.eventNewLength - info.eventOldLength;
        int newEndOffset = info.eventOffset + info.eventNewLength;
        int lastSegmentOffset = thisSegments.getLastValidOffset();
        int localData = data;
        for (IElementType type = lexerWrapper.getTokenType(); type != null; type = lexerWrapper.getTokenType()) {
            int lexerState = lexerWrapper.getState();
            int start = lexerWrapper.getTokenStart();
            insertSegmentCount = handleInvalidChar(info, insertSegments, insertSegmentCount, start, lexerState);
            localData = thisSegments.packData(type, lexerState, canRestart(lexerState));
            if (start >= newEndOffset && start - shift < lastSegmentOffset && canRestart(lexerState)) {
                int i = thisSegments.findSegmentIndex(start - shift);
                if (thisSegments.getSegmentStart(i) == start - shift && thisSegments.getSegmentData(i) == data) {
                    repaintEnd = start;
                    oldEndIndex = i;
                    break;
                }
            }
            insertSegments.setElementAt(insertSegmentCount, start, lexerWrapper.getTokenEnd(), localData);
            insertSegmentCount++;
            try {
                lexerWrapper.advance();
            } catch (PluginException e) {
                LOG.warn("Lexer error in ValidatingLexerWrapper POS2:" + e.getMessage());
            }
        }
        GetRepaintEndInfo para = new GetRepaintEndInfo(insertSegmentCount, oldEndIndex, repaintEnd, insertSegments);
        if (repaintEnd > 0) {
            extracted(info, para, shift);
        }
        para.repaintEnd = para.repaintEnd == -1 ? text.length() : para.repaintEnd;
        para.oldEndIndex = para.oldEndIndex < 0 ? thisSegments.getSegmentCount() : para.oldEndIndex;
        thisSegments.shiftSegments(para.oldEndIndex, shift);
        thisSegments.replace(info.startIndex, para.oldEndIndex, para.insertSegments);
        boolean flag =
            para.insertSegmentCount != 0 && (para.oldEndIndex != info.startIndex + 1 || para.insertSegmentCount != 1
                || localData != thisSegments.getSegmentData(info.startIndex));
        if (flag) {
            thisEditor.repaint(info.startOffset, para.repaintEnd);
        }
        return para.repaintEnd;
    }

    private int handleInvalidChar(Info info, SegmentArrayWithData insertSegments, int insertSegmentCount,
                                  int start, int lexerState) {
        IElementType invalidCharacterType = PSIElementTypeFactory
                .getTokenIElementTypes(CangJieLanguage.INSTANCE).getLast();
        int cnt = insertSegmentCount;
        int localData;
        if (cnt > 0 && insertSegments.getSegmentCount() == cnt
                && start > insertSegments.getSegmentEnd(cnt - 1)) {
            localData = thisSegments.packData(invalidCharacterType, lexerState, canRestart(lexerState));
            insertSegments.setElementAt(cnt,
                    insertSegments.getSegmentEnd(cnt - 1), start, localData);
            cnt++;
        }
        if (cnt == 0 && start > info.startOffset) {
            localData = thisSegments.packData(invalidCharacterType, lexerState, canRestart(lexerState));
            insertSegments.setElementAt(cnt, info.startOffset, start, localData);
            cnt++;
        }
        return cnt;
    }

    private void extracted(Info info, GetRepaintEndInfo para, int shift) {
        while (para.insertSegmentCount > 0 && para.oldEndIndex > info.startIndex) {
            if (!segmentsEqual(para.oldEndIndex - 1, thisSegments,
                    para.insertSegmentCount - 1, para.insertSegments, shift) || hasAdditionalData()) {
                break;
            }
            para.insertSegmentCount--;
            para.oldEndIndex--;
            para.repaintEnd = para.insertSegments.getSegmentStart(para.insertSegmentCount);
            para.insertSegments.remove(para.insertSegmentCount, para.insertSegmentCount + 1);
        }
    }

    private void startLexer(int offset, CharSequence text, int startIndex) {
        int initialState;
        if (offset == 0 && thisLexer instanceof RestartableLexer) {
            initialState = ((RestartableLexer) thisLexer).getStartState();
            thisLexer.start(text, offset, text.length(), initialState);
        } else {
            if (thisLexer instanceof RestartableLexer) {
                initialState = thisSegments.unpackStateFromData(thisSegments.getSegmentData(startIndex));
                ((RestartableLexer) thisLexer).start(text, offset,
                        text.length(), initialState, createTokenIterator(startIndex));
            } else {
                initialState = thisInitialState;
                thisLexer.start(text, offset, text.length(), initialState);
            }
        }
    }

    /**
     * documentChanged
     *
     * @param documentEvent the event containing the information about the change.
     */
    @Override
    public synchronized void documentChanged(@NotNull DocumentEvent documentEvent) {
        try {
            Document document = documentEvent.getDocument();

            if (document.isInBulkUpdate()) {
                thisText = null;
                thisSegments.removeAll();
                return;
            }
            incrementalUpdate(documentEvent.getOffset(), documentEvent.getOldLength(),
                    documentEvent.getNewLength(), document);
        } catch (ProcessCanceledException ex) {
            thisText = null;
            thisSegments.removeAll();
            throw ex;
        } catch (RuntimeException ex) {
            LOG.info("Error updating  after ", ex);
        }
    }

    @NotNull
    private TokenIterator createTokenIterator(int start) {
        return new TokenIterator() {
            @Override
            @NotNull
            public IElementType getType(int index) {
                int data = thisSegments.getSegmentData(index);
                return thisSegments.unpackTokenFromData(data);
            }

            @Override
            public int getState(int index) {
                int data = thisSegments.getSegmentData(index);
                return thisSegments.unpackStateFromData(data);
            }

            @Override
            public int getTokenCount() {
                return thisSegments.getSegmentCount();
            }

            @Override
            public int getStartOffset(int index) {
                return thisSegments.getSegmentStart(index);
            }

            @Override
            public int getEndOffset(int index) {
                return thisSegments.getSegmentEnd(index);
            }

            @Override
            public int initialTokenIndex() {
                return start;
            }
        };
    }

    private boolean canRestart(int lexerState) {
        if (thisLexer instanceof RestartableLexer restartableLexer) {
            return restartableLexer.isRestartableState(lexerState);
        }
        return lexerState == thisInitialState;
    }

    /**
     * hasAdditionalData
     *
     * @return Additional has Data return true
     */
    protected boolean hasAdditionalData() {
        return false;
    }

    /**
     * getPriority
     *
     * @return int EditorDocumentPriorities.LEXER_EDITOR
     */
    @Override
    public int getPriority() {
        return EditorDocumentPriorities.LEXER_EDITOR;
    }

    private static boolean segmentsEqual(int idx1, @NotNull SegmentArrayWithData a1,
                                         int idx2, @NotNull SegmentArrayWithData a2,
                                         int offsetShift) {
        return a1.getSegmentStart(idx1) + offsetShift == a2.getSegmentStart(idx2)
                && a1.getSegmentEnd(idx1) + offsetShift == a2.getSegmentEnd(idx2)
                && a1.getSegmentData(idx1) == a2.getSegmentData(idx2);
    }

    /**
     * getClient
     *
     * @return HighlighterClient
     */
    public HighlighterClient getClient() {
        return thisEditor;
    }

    /**
     * setText
     *
     * @param text text
     */
    @Override
    public void setText(@NotNull CharSequence text) {
        synchronized (this) {
            doSetText(text);
        }
    }

    /**
     * interface TokenProcessor
     */
    protected interface TokenProcessor {
        /**
         * addToken
         *
         * @param tokenIndex tokenIndex
         * @param startOffset startOffset
         * @param endOffset endOffset
         * @param data data
         * @param tokenType tokenType
         */
        void addToken(int tokenIndex, int startOffset, int endOffset, int data, @NotNull IElementType tokenType);

        /**
         * finish
         */
        default void finish() {
        }
    }

    private void doSetText(@NotNull CharSequence text) {
        if (Comparing.equal(thisText, text)) {
            return;
        }
        CharSequence text1 = ImmutableCharSequence.asImmutable(text);

        SegmentArrayWithData tempSegments = createSegments();
        TokenProcessor processor = createTokenProcessor(tempSegments);
        int textLength = text1.length();
        Lexer lexerWrapper = new SafeLexerWrapper(thisLexer);

        lexerWrapper.start(text1, 0, textLength,
                thisLexer instanceof RestartableLexer
                        ? ((RestartableLexer) thisLexer).getStartState() : thisInitialState);
        int index = 0;
        while (true) {
            IElementType tokenType = lexerWrapper.getTokenType();
            if (tokenType == null) {
                break;
            }

            int state = lexerWrapper.getState();
            int data = tempSegments.packData(tokenType, state, canRestart(state));
            processor.addToken(index, lexerWrapper.getTokenStart(), lexerWrapper.getTokenEnd(), data, tokenType);
            index++;
            if (index % 1024 == 0) {
                ProgressManager.checkCanceled();
            }
            try {
                lexerWrapper.advance();
            } catch (PluginException e) {
                LOG.warn("Lexer error in doSetText:" + e.getMessage());
            }
        }

        thisText = text1;
        thisSegments = tempSegments;
        processor.finish();

        if (textLength > 0 && (thisSegments.getSegmentCount() == 0
                || thisSegments.getSegmentEnd(thisSegments.getSegmentCount() - 1) != textLength)) {
            throw new IllegalStateException("Unexpected termination offset for lexer " + thisLexer);
        }

        if (thisEditor != null && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
            UIUtil.invokeLaterIfNeeded(() -> thisEditor.repaint(0, textLength));
        }
    }

    /**
     * createTokenProcessor
     *
     * @param segments segments
     * @return CangjieEditorHighlighter.TokenProcessor
     */
    @NotNull
    protected CangjieEditorHighlighter.TokenProcessor createTokenProcessor(@NotNull SegmentArrayWithData segments) {
        return (tokenIndex, startOffset, endOffset, data,
                tokenType) -> segments.setElementAt(tokenIndex, startOffset, endOffset, data);
    }

    @NotNull
    private TextAttributes getAttributes(@NotNull IElementType tokenType) {
        TextAttributes attrs = thisAttributesMap.get(tokenType);
        if (attrs == null) {
            // let's fetch syntax highlighter attributes for token
            // and merge them with "TEXT" attribute of current color scheme
            attrs = convertAttributes(getAttributesKeys(tokenType));
            thisAttributesMap.put(tokenType, attrs);
        }
        return attrs;
    }

    private TextAttributesKey @NotNull [] getAttributesKeys(@NotNull IElementType tokenType) {
        TextAttributesKey[] attributesKeys = thisKeysMap.get(tokenType);
        if (attributesKeys == null) {
            attributesKeys = thisHighlighter.getTokenHighlights(tokenType);
            thisKeysMap.put(tokenType, attributesKeys);
        }
        return attributesKeys;
    }

    @NotNull
    TextAttributes convertAttributes(TextAttributesKey @NotNull [] keys) {
        return LayeredTextAttributes.create(thisScheme, keys);
    }

    /**
     * toString
     *
     * @return String
     */
    @Override
    @NonNls
    public String toString() {
        return getClass().getName() + "("
                + (thisLexer.getClass() == FlexAdapter.class ? thisLexer.toString()
                : thisLexer.getClass().getName()) + "): '" + thisLexer.getBufferSequence() + "'";
    }

    /**
     * HighlighterIteratorImpl
     */
    public class HighlighterIteratorImpl implements HighlighterIterator {
        private int thisSegmentIndex;

        HighlighterIteratorImpl(int startOffset) {
            if (startOffset < 0 || startOffset > thisSegments.getLastValidOffset()) {
                throw new IllegalArgumentException("Invalid offset: "
                        + startOffset + "; thisSegments.getLastValidOffset()="
                        + thisSegments.getLastValidOffset());
            }
            try {
                thisSegmentIndex = thisSegments.findSegmentIndex(startOffset);
            } catch (IllegalStateException e) {
                LOG.info("wrong state", e);
            }
        }

        @Override
        public TextAttributes getTextAttributes() {
            IElementType ty = getTokenType();
            return getAttributes(ty);
        }

        @Override
        public TextAttributesKey @NotNull [] getTextAttributesKeys() {
            IElementType ty = getTokenType();
            return getAttributesKeys(ty);
        }

        @Override
        public int getStart() {
            return thisSegments.getSegmentStart(thisSegmentIndex);
        }

        @Override
        public int getEnd() {
            return thisSegments.getSegmentEnd(thisSegmentIndex);
        }

        @Override
        public IElementType getTokenType() {
            try {
                int data = thisSegments.getSegmentData(thisSegmentIndex);
                return thisSegments.unpackTokenFromData(data);
            } catch (IllegalStateException e) {
                throw new InvalidStateException(CangjieEditorHighlighter.this, "wrong state", e);
            }
        }

        @Override
        public void advance() {
            thisSegmentIndex++;
        }

        @Override
        public void retreat() {
            thisSegmentIndex--;
        }

        @Override
        public boolean atEnd() {
            return thisSegmentIndex >= thisSegments.getSegmentCount() || thisSegmentIndex < 0;
        }

        @Override
        public Document getDocument() {
            return CangjieEditorHighlighter.this.getDocument();
        }

        /**
         * getClient
         *
         * @return HighlighterClient
         */
        public HighlighterClient getClient() {
            return CangjieEditorHighlighter.this.getClient();
        }
    }

    /**
     * SegmentArrayWithData
     *
     * @return thisSegments
     */
    @NotNull
    public SegmentArrayWithData getSegments() {
        return thisSegments;
    }

    /**
     * InvalidStateException
     */
    public static final class InvalidStateException extends RuntimeException implements ExceptionWithAttachments {
        private final Attachment[] myAttachments;

        private InvalidStateException(CangjieEditorHighlighter highlighter, String message, Throwable cause) {
            super(highlighter.getClass().getName() + "("
                    + (highlighter.thisLexer.getClass() == FlexAdapter.class ? highlighter.thisLexer.toString()
                    : highlighter.thisLexer.getClass().getName()) + "): " + message, cause);
            myAttachments = new Attachment[]{new Attachment("content.txt",
                    highlighter.thisLexer.getBufferSequence().toString())};
        }

        @Override
        public Attachment @NotNull [] getAttachments() {
            return myAttachments;
        }
    }
}