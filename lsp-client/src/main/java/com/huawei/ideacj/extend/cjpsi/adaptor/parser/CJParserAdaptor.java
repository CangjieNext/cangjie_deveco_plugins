/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.extend.cjpsi.adaptor.parser;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.CJPSITokenSource;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.tree.IElementType;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;

/**
 * The type Antlr parser adaptor.
 *
 * @since 2024/03/20
 */
public abstract class CJParserAdaptor implements PsiParser {
    /**
     * The Language.
     */
    protected final Language language;

    /**
     * The Parser.
     */
    protected final Parser parser;

    /**
     * Create a jetbrains adaptor for an ANTLR parser object. When
     * the IDE requests a {@link #parse(IElementType, PsiBuilder)},
     * the token stream will be set on the parser.
     *
     * @param language the language
     * @param parser   the parser
     */
    public CJParserAdaptor(Language language, Parser parser) {
        this.language = language;
        this.parser = parser;
    }

    /**
     * Gets language.
     *
     * @return the language
     */
    public Language getLanguage() {
        return language;
    }

    @NotNull
    @Override
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        ProgressIndicatorProvider.checkCanceled();

        TokenSource source = new CJPSITokenSource(builder);
        TokenStream tokens = new CommonTokenStream(source);
        parser.setTokenStream(tokens);
        parser.setErrorHandler(new ErrorStrategyAdaptor());
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener());
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        ParseTree parseTree;
        PsiBuilder.Marker rollbackMarker = builder.mark();
        try {
            parseTree = parse(parser, root);
        } finally {
            rollbackMarker.rollbackTo();
        }
        CJParseTreeToPSIConverter listener = createListener(parser, builder);
        PsiBuilder.Marker rootMarker = builder.mark();
        ParseTreeWalker.DEFAULT.walk(listener, parseTree);
        while (!builder.eof()) {
            ProgressIndicatorProvider.checkCanceled();
            builder.advanceLexer();
        }
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    /**
     * Parse parse tree.
     *
     * @param parser the parser
     * @param root   the root
     * @return the parse tree
     */
    protected abstract ParseTree parse(Parser parser, IElementType root);

    /**
     * Create listener antlr parse tree to psi converter.
     *
     * @param parser  the parser
     * @param builder the builder
     * @return the antlr parse tree to psi converter
     */
    protected CJParseTreeToPSIConverter createListener(Parser parser, PsiBuilder builder) {
        return new CJParseTreeToPSIConverter(language, parser, builder);
    }
}
