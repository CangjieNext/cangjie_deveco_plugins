/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.lexer;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

/**
 * CJPSITokenSource
 *
 * @since 2024/03/20
 */
public class CJPSITokenSource implements TokenSource {
    /**
     * The Builder.
     */
    protected PsiBuilder psiBuilder;

    /**
     * The Token factory.
     */
    protected TokenFactory factory = CommonTokenFactory.DEFAULT;

    /**
     * Instantiates a new Psi token source.
     *
     * @param builder the builder
     */
    public CJPSITokenSource(PsiBuilder builder) {
        this.psiBuilder = builder;
    }

    @Override
    public int getCharPositionInLine() {
        return 0;
    }

    @Override
    public Token nextToken() {
        ProgressIndicatorProvider.checkCanceled();
        TokenElementType tokenType = null;
        if (psiBuilder.getTokenType() instanceof TokenElementType) {
            tokenType = (TokenElementType) psiBuilder.getTokenType();
        }
        int type = tokenType != null ? tokenType.getANTLRTokenType() : Token.EOF;

        int channel = Token.DEFAULT_CHANNEL;
        Pair<TokenSource, CharStream> source = new Pair<TokenSource, CharStream>(this, null);
        String text = psiBuilder.getTokenText();
        int startOffset = psiBuilder.getCurrentOffset();
        int length = text != null ? text.length() : 0;
        int stop = startOffset + length - 1;
        // PsiBuilder doesn't provide line, column info
        int line = 0;
        int charPositionInLine = 0;
        Token token = factory.create(source, type, text, channel, startOffset, stop, line, charPositionInLine);
        psiBuilder.advanceLexer();
        return token;
    }

    @Override
    public int getLine() {
        return 0;
    }

    @Override
    public void setTokenFactory(TokenFactory<?> tokenFactory) {
        this.factory = tokenFactory;
    }

    @Override
    public TokenFactory<?> getTokenFactory() {
        return factory;
    }

    @Override
    public CharStream getInputStream() {
        CharSequence text = psiBuilder.getOriginalText();
        return new CharSequenceCharStream(text, text.length(), getSourceName());
    }

    @Override
    public String getSourceName() {
        return CharStream.UNKNOWN_SOURCE_NAME;
    }
}
