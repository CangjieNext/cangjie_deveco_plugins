/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CJLexerAdaptor
 *
 * @since 2024/03/20
 */
public class CJLexerAdaptor extends com.intellij.lexer.LexerBase {
    private final List<? extends IElementType> tokenElementTypes;

    private final Lexer lexer;
    private final Map<CJLexerState, Integer> stateCacheMap = new HashMap<CJLexerState, Integer>();
    private final List<CJLexerState> stateCache = new ArrayList<CJLexerState>();
    private CharSequence buffer;
    private int endOffset;
    private CJLexerState curState;
    private Token curToken;

    /**
     * Constructor
     *
     * @param language The language.
     * @param lexer    The underlying lexer.
     */
    public CJLexerAdaptor(Language language, Lexer lexer) {
        this.tokenElementTypes = PSIElementTypeFactory.getTokenIElementTypes(language);
        this.lexer = lexer;
    }

    @Override
    public void start(CharSequence buffer, int start, int end, int initialState) {
        this.buffer = buffer;
        this.endOffset = end;

        CharStream in = new CharSequenceCharStream(buffer, end, IntStream.UNKNOWN_SOURCE_NAME);
        in.seek(start);
        CJLexerState state = start == 0 && initialState == 0 ? getInitialState() : toLexerState(initialState);
        applyLexerState(in, state);
        advance();
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        return getTokenType(curToken.getType());
    }

    /**
     * Gets token type.
     *
     * @param tokenType the token type
     * @return the token type
     */
    @Nullable
    public IElementType getTokenType(int tokenType) {
        if (tokenType == Token.EOF) {
            // return null when lexing is finished
            return null;
        }
        return tokenElementTypes.get(tokenType);
    }

    @Override
    public void advance() {
        curState = getLexerState(lexer);
        curToken = lexer.nextToken();
    }

    @Override
    public int getState() {
        CJLexerState state = curState != null ? curState : getInitialState();
        Integer existing = stateCacheMap.get(state);
        if (existing != null) {
            return existing;
        }
        existing = stateCache.size();
        stateCache.add(state);
        stateCacheMap.put(state, existing);
        return existing;
    }

    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    @Override
    public int getTokenStart() {
        return curToken.getStartIndex();
    }

    @Override
    public int getTokenEnd() {
        return curToken.getStopIndex() + 1;
    }

    /**
     * Update the current lexer to use the specified {@code input}
     * stream starting in the specified {@code state}.
     *
     * @param input The new input stream for the lexer.
     * @param state A {@code CJLexerState} instance containing the starting state for the lexer.
     */
    protected void applyLexerState(CharStream input, CJLexerState state) {
        lexer.setInputStream(input);
        state.apply(lexer);
    }

    /**
     * Get the initial {@code CJLexerState} of the lexer.
     *
     * @return a {@code CJLexerState} instance representing the state of the lexer at the beginning of an input.
     */
    protected CJLexerState getInitialState() {
        return new CJLexerState(Lexer.DEFAULT_MODE, null);
    }

    /**
     * Get a {@code CJLexerState} instance representing the current state
     * of the specified lexer.
     *
     * @param lexer The lexer.
     * @return A {@code CJLexerState} instance containing the current state of the lexer.
     */
    protected CJLexerState getLexerState(Lexer lexer) {
        if (lexer._modeStack.isEmpty()) {
            return new CJLexerState(lexer._mode, null);
        }

        return new CJLexerState(lexer._mode, lexer._modeStack);
    }

    /**
     * Gets the {@code CJLexerState} corresponding to the specified IntelliJ {@code state}.
     *
     * @param state The lexer state provided by IntelliJ.
     * @return The {@code CJLexerState} instance corresponding to the specified state.
     */
    protected CJLexerState toLexerState(int state) {
        return stateCache.get(state);
    }
}
