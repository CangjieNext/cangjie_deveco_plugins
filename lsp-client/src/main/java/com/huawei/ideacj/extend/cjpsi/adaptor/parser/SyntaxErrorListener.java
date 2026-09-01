/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Traps errors from parsing language of plugin. E.g., for a Java plugin,
 * this would catch errors when people type invalid Java code into .java file.
 * This swallows the errors as the PSI tree has error nodes.
 *
 * @since 2024/03/20
 */
public class SyntaxErrorListener extends BaseErrorListener {
    private final List<SyntaxError> syntaxErrors = new ArrayList<SyntaxError>();

    /**
     * Gets syntax errors.
     *
     * @return the syntax errors
     */
    public List<SyntaxError> getSyntaxErrors() {
        return syntaxErrors;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException recognitionException) {
        if (offendingSymbol instanceof Token) {
            syntaxErrors.add(new SyntaxError((Token) offendingSymbol, line,
                msg, recognitionException));
        }
    }

    @Override
    public String toString() {
        return Utils.join(syntaxErrors.iterator(), "\n");
    }
}
