/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.CJLexerAdaptor;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.PSIElementTypeFactory;
import com.huawei.ideacj.extend.cjpsi.adaptor.parser.CJParserAdaptor;
import com.huawei.ideacj.language.psi.CangjieDeclarationPsiFileRoot;
import com.huawei.ideacj.lsp.utils.CangjieDeclarationLanguage;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

import cjgrammar.parser.DeclarationLexer;
import cjgrammar.parser.DeclarationParser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * definition Cangjie Declaration parser class
 *
 * @since 2025-02-17
 */
public class CangjieDeclarationParserDefinition implements ParserDefinition {
    private static final IFileElementType FILE;

    /**
     * Definition of comment node
     */
    private static final TokenSet COMMENTS;

    /**
     * Definition of empty nodes
     */
    private static final TokenSet WHITESPACE;

    /**
     * The definition of string nodes, all string types need to be added
     */
    private static final TokenSet STRING;

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(
                CangjieDeclarationLanguage.INSTANCE, DeclarationParser.tokenNames, DeclarationParser.ruleNames);
        FILE = new IFileElementType(CangjieDeclarationLanguage.INSTANCE);
        COMMENTS = PSIElementTypeFactory.createTokenSet(CangjieDeclarationLanguage.INSTANCE,
                DeclarationLexer.DelimitedComment, DeclarationLexer.LineComment);
        WHITESPACE = PSIElementTypeFactory.createTokenSet(CangjieDeclarationLanguage.INSTANCE, DeclarationLexer.WS);
        STRING = PSIElementTypeFactory.createTokenSet(CangjieDeclarationLanguage.INSTANCE, DeclarationLexer.LineString,
                DeclarationLexer.MultiLineRawStringLiteral);
    }

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        DeclarationLexer lexer = new DeclarationLexer(null);
        lexer.isGarbled = false;
        FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (editor == null) {
            return new CJLexerAdaptor(CangjieDeclarationLanguage.INSTANCE, lexer);
        }
        VirtualFile file = editor.getFile();
        if (file == null) {
            return new CJLexerAdaptor(CangjieDeclarationLanguage.INSTANCE, lexer);
        }
        try {
            String content = new String(file.contentsToByteArray());
            Charset charset = file.getCharset();
            String newContent = new String(content.getBytes(charset), StandardCharsets.UTF_8);
            if (!content.equals(newContent)) {
                lexer.isGarbled = true;
            }
        } catch (IOException e) {
            return new CJLexerAdaptor(CangjieDeclarationLanguage.INSTANCE, lexer);
        }
        // ANTLRLexerAdaptor is an adapter for antlr intellij atapter to
        // bind the developer's custom language with the underlying ANTLR lexical analyzer
        return new CJLexerAdaptor(CangjieDeclarationLanguage.INSTANCE, lexer);
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        final DeclarationParser parser = new DeclarationParser(null);
        // ANTLRParserAdaptor is an adapter for antlr intellij atapter to
        // bind the developer's custom language with the underlying ANTLR parser
        return new CJParserAdaptor(CangjieDeclarationLanguage.INSTANCE, parser) {
            @Override
            @Nullable
            protected ParseTree parse(Parser parser, IElementType root) {
                if (!(parser instanceof DeclarationParser)) {
                    return null;
                }
                // parse entry
                return ((DeclarationParser) parser).translationUnit();
            }
        };
    }

    /**
     * Returns the set of types that are considered annotations by the PSI builder.
     *
     * @return Empty node type
     */
    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    /**
     * Returns the set of types that are considered annotations by the PSI builder.
     *
     * @return Comment node type
     */
    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    /**
     * Returns the set of types treated as strings by the PSI builder.
     *
     * @return String node type
     */
    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRING;
    }

    /**
     * What is the IFileElementType of the root parse tree node? It
     * is called from {@link #createFile(FileViewProvider)} at least.
     *
     * @return IFileElementType
     */
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    /**
     * Create the root node of the PSI tree (a PsiFile).
     *
     * @param viewProvider FileViewProvider
     * @return class CjPSIFileRoot inherits from PsiFileBase
     */
    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CangjieDeclarationPsiFileRoot(viewProvider);
    }

    /**
     * Convert the AST tree converted by ANTLR into PSI node.
     *
     * @param node ASTNode
     * @return The custom node inherited from ANTLRPsiNode, generally the same name as the AST node
     */
    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return CjPsiNodeCreateUtils.INSTANCE.create(node);
    }
}
