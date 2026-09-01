/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.parser;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.PSIElementTypeFactory;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.RuleIElementType;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CJParseTreeToPSIConverter
 *
 * @since 2024/03/20
 */
public class CJParseTreeToPSIConverter implements ParseTreeListener {
    /**
     * The Builder.
     */
    protected final PsiBuilder builder;

    /**
     * The Syntax errors.
     */
    protected List<SyntaxError> syntaxErrors;

    /**
     * The Markers.
     */
    protected final Deque<PsiBuilder.Marker> markers = new ArrayDeque<>();

    /**
     * The Rule element types.
     */
    protected final List<RuleIElementType> ruleElementTypes;

    /**
     * The Token to error map.
     */
    protected Map<Integer, SyntaxError> tokenToErrorMap = new HashMap<>();

    /**
     * Instantiates a new Antlr parse tree to psi converter.
     *
     * @param language the language
     * @param parser   the parser
     * @param builder  the builder
     */
    public CJParseTreeToPSIConverter(Language language, Parser parser, PsiBuilder builder) {
        this.builder = builder;
        this.ruleElementTypes = PSIElementTypeFactory.getRuleIElementTypes(language);

        for (ANTLRErrorListener listener : parser.getErrorListeners()) {
            if (!(listener instanceof SyntaxErrorListener)) {
                continue;
            }
            syntaxErrors = ((SyntaxErrorListener) listener).getSyntaxErrors();
            for (SyntaxError error : syntaxErrors) {
                // record first error per token
                int startIndex = error.getOffendingSymbol().getStartIndex();
                if (!tokenToErrorMap.containsKey(startIndex)) {
                    tokenToErrorMap.put(startIndex, error);
                }
            }
        }
    }

    /**
     * Gets builder.
     *
     * @return the builder
     */
    protected final PsiBuilder getBuilder() {
        return builder;
    }

    /**
     * Gets rule element types.
     *
     * @return the rule element types
     */
    protected final List<RuleIElementType> getRuleElementTypes() {
        return ruleElementTypes;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        builder.advanceLexer();
    }

    /**
     * visitErrorNode
     *
     * @param node ErrorNode
     */
    public void visitErrorNode(ErrorNode node) {
        ProgressIndicatorProvider.checkCanceled();

        Token badToken = node.getSymbol();
        boolean isConjuredToken = badToken.getTokenIndex() < 0;
        int index = badToken.getStartIndex();
        SyntaxError error = tokenToErrorMap.get(index);

        if (error == null) {
            if (isConjuredToken) {
                PsiBuilder.Marker eMarker = builder.mark();
                eMarker.error(badToken.getText()); // says "<missing X>" or similar
            } else {
                // must be a real token consumed during recovery; just consume w/o highlighting it as an error
                builder.advanceLexer();
            }
        } else {
            PsiBuilder.Marker eMarker = builder.mark();
            if (badToken.getStartIndex() >= 0 && badToken.getType() != Token.EOF && !isConjuredToken) {
                // we advance lexer if error occurred at a real token
                // Missing tokens should highlight the token at the missing position
                // but can't consume a token that does not exist.
                builder.advanceLexer();
            }
            String message = String.format("%s%n", error.getMessage());
            eMarker.error(message);
        }
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        ProgressIndicatorProvider.checkCanceled();
        markers.push(getBuilder().mark());
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        ProgressIndicatorProvider.checkCanceled();
        var marker = markers.pop();
        marker.done(getRuleElementTypes().get(ctx.getRuleIndex()));
    }
}
