/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.parser;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;

/**
 * Adapt ANTLR's DefaultErrorStrategy so that we add error nodes
 * for EOF if reached at start of resync's consumeUntil().
 * Also set start/stop of missing token to always be the current token,
 * even if that's EOF.
 *
 * @since 2024/03/20
 */
public class ErrorStrategyAdaptor extends DefaultErrorStrategy {
    @Override
    protected void consumeUntil(Parser recognizer, IntervalSet set) {
        Token currentToken = recognizer.getCurrentToken();
        if (currentToken.getType() == Token.EOF) {
            ErrorNodeImpl errorNode = new ErrorNodeImpl(currentToken);
            recognizer.getRuleContext().addErrorNode(errorNode);
        }
        super.consumeUntil(recognizer, set);
    }

    /** By default ANTLR makes the start/stop -1/-1 for invalid tokens
     *  which is reasonable but here we want to highlight the
     *  current position indicating that is where we lack a token.
     *  if no input, highlight at position 0.
     *
     * @param recognizer Parser
     * @return Token
     */
    protected Token getMissingSymbol(Parser recognizer) {
        Token missingSymbol = super.getMissingSymbol(recognizer);
        // alter the default missing symbol.
        if (missingSymbol instanceof CommonToken) {
            Token current = recognizer.getCurrentToken();
            int start = current.getStartIndex();
            int stop = current.getStopIndex();
            ((CommonToken) missingSymbol).setStartIndex(start);
            ((CommonToken) missingSymbol).setStopIndex(stop);
        }
        return missingSymbol;
    }
    @Override
    public void sync(Parser recognizer) throws RecognitionException {

    }
}
