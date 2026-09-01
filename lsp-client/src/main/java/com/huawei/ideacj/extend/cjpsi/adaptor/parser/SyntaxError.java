/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.parser;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

/**
 * A syntax error from parsing language of plugin. These are
 * created by SyntaxErrorListener.
 *
 * @since 2024/03/20
 */
public class SyntaxError {
    private final Token offendingSymbol;
    private final int line;
    private final String message;
    private final RecognitionException recognitionException;

    /**
     * Instantiates a new Syntax error.
     *
     * @param offendingSymbol    the offending symbol
     * @param line               the line
     * @param msg                the msg
     * @param recognitionException                  the e
     */
    public SyntaxError(
            Token offendingSymbol,
            int line,
            String msg,
            RecognitionException recognitionException) {
        this.offendingSymbol = offendingSymbol;
        this.line = line;
        this.message = msg;
        this.recognitionException = recognitionException;
    }

    /**
     * Gets offending symbol.
     *
     * @return the offending symbol
     */
    public Token getOffendingSymbol() {
        if (recognitionException instanceof NoViableAltException) {
            return ((NoViableAltException) recognitionException).getStartToken();
        }
        return offendingSymbol;
    }

    /**
     * Gets line.
     *
     * @return the line
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets exception.
     *
     * @return the exception
     */
    public RecognitionException getException() {
        return recognitionException;
    }
}
