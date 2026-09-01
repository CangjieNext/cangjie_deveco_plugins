/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.syntaxhighlighter;

import com.intellij.diagnostic.PluginException;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type Safe lexer wrapper.
 *
 * @since 2026 -05-27
 */
public class SafeLexerWrapper extends Lexer {
    private final Lexer customLexer;

    private int cachedState;

    private int cachedStartOffset;

    private int cachedEndOffset;

    private IElementType cachedTokenType;

    private boolean isCacheValid;

    /**
     * Instantiates a new Safe lexer wrapper.
     *
     * @param delegate the delegate
     */
    public SafeLexerWrapper(@NotNull Lexer delegate) {
        customLexer = delegate;
    }

    @Override
    @NotNull
    public CharSequence getTokenSequence() {
        return customLexer.getTokenSequence();
    }

    @Override
    @NotNull
    public String getTokenText() {
        return customLexer.getTokenText();
    }

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        customLexer.start(buffer, startOffset, endOffset, initialState);
        isCacheValid = false;
    }

    @Override
    public int getState() {
        return isCacheValid ? cachedState : customLexer.getState();
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        if (!isCacheValid) {
            cachedTokenType = customLexer.getTokenType();
            if (cachedTokenType != null) {
                cachedState = customLexer.getState();
                cachedStartOffset = customLexer.getTokenStart();
                cachedEndOffset = customLexer.getTokenEnd();
            }
            isCacheValid = true;
        }
        return cachedTokenType;
    }

    @Override
    public int getTokenStart() {
        return isCacheValid ? cachedStartOffset : customLexer.getTokenStart();
    }

    @Override
    public int getTokenEnd() {
        return isCacheValid ? cachedEndOffset : customLexer.getTokenEnd();
    }

    @Override
    public void advance() {
        customLexer.advance();
        int preStartOffset = 0;
        int preEndOffset = 0;
        int preState = 0;
        IElementType preTokenType = null;
        if (isCacheValid) {
            preStartOffset = cachedStartOffset;
            preEndOffset = cachedEndOffset;
            preState = cachedState;
            preTokenType = cachedTokenType;
        }
        isCacheValid = false;
        getTokenType();
        if (preTokenType == null || cachedTokenType == null) {
            return;
        }
        if (cachedStartOffset > cachedEndOffset) {
            throwCustomMsgException("The lexer returns an incorrect token offset. "
                + "The start position is greater than the end position.");
        }
        if (cachedStartOffset != preEndOffset) {
            throwCustomMsgException("The lexer generates a discontinuous token sequence. "
                + "The current Start is not equal to the previous End.");
        }
        if (cachedEndOffset == cachedStartOffset && preEndOffset == preStartOffset && cachedState == preState
            && cachedTokenType == preTokenType) {
            throwCustomMsgException("The Lexer does not advance after advance() is called. "
                + "There is a risk of infinite loop.");
        }
    }

    @Override
    @NotNull
    public LexerPosition getCurrentPosition() {
        return customLexer.getCurrentPosition();
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        customLexer.restore(position);
        isCacheValid = false;
    }

    @Override
    @NotNull
    public CharSequence getBufferSequence() {
        return customLexer.getBufferSequence();
    }

    @Override
    public int getBufferEnd() {
        return customLexer.getBufferEnd();
    }

    private void throwCustomMsgException(@NotNull String message) {
        String detailedMessage;
        Class<?> reportingClass;
        if (customLexer instanceof FlexAdapter flexAdapter) {
            detailedMessage = message + ": " + flexAdapter.toString();
            reportingClass = flexAdapter.getFlex().getClass();
        } else {
            detailedMessage = message + ": " + customLexer.getClass().getName();
            reportingClass = customLexer.getClass();
        }
        throw PluginException.createByClass(detailedMessage, null, reportingClass);
    }
}
