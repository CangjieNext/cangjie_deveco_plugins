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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a token in the language of the plug-in. The "token type" of
 * leaf nodes in jetbrains PSI tree. Corresponds to ANTLR's int token type.
 * Intellij lexer token types are instances of IElementType:
 * <p>
 * "Interface for token types returned from lexical analysis and for types
 * of nodes in the AST tree."
 * <p>
 * We differentiate between parse tree subtree roots and tokens with
 * {@link RuleIElementType} and {@link TokenElementType}, respectively.
 *
 * @since 2024/03/20
 */
public class TokenElementType extends IElementType {
    private final int antlrTokenType;

    /**
     * Instantiates a new Token i element type.
     *
     * @param antlrTokenType the antlr token type
     * @param debugName      the debug name
     * @param language       the language
     */
    public TokenElementType(int antlrTokenType, @NotNull @NonNls String debugName, @Nullable Language language) {
        super(debugName, language);
        this.antlrTokenType = antlrTokenType;
    }

    /**
     * Gets antlr token type.
     *
     * @return the antlr token type
     */
    public int getANTLRTokenType() {
        return antlrTokenType;
    }
}
