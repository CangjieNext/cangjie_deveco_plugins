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
 * Represents a specific ANTLR rule invocation in the language of the plug-in and is the
 * intellij "token type" of an interior PSI tree node. The IntelliJ equivalent
 * of ANTLR RuleNode.getRuleIndex() method or maybe RuleNode itself.
 * <p>
 * Intellij Lexer token types are instances of IElementType.
 * We differentiate between parse tree subtree roots and tokens with
 * {@link RuleIElementType} and {@link TokenElementType}.
 *
 * @since 2024/03/20
 */
public class RuleIElementType extends IElementType {
    private final int ruleIndex;

    /**
     * Instantiates a new Rule i element type.
     *
     * @param ruleIndex the rule index
     * @param debugName the debug name
     * @param language  the language
     */
    public RuleIElementType(int ruleIndex, @NotNull @NonNls String debugName, @Nullable Language language) {
        super(debugName, language);
        this.ruleIndex = ruleIndex;
    }

    /**
     * Gets rule index.
     *
     * @return the rule index
     */
    public int getRuleIndex() {
        return ruleIndex;
    }
}
