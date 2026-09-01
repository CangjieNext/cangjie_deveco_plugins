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
import com.huawei.ideacj.language.psi.CangjieMacrocallPsiFileRoot;
import com.huawei.ideacj.lsp.utils.CangjieMacrocallLanguage;

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

import cjgrammar.parser.CharLexer;
import cjgrammar.parser.CharParser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * CangjieMacrocallParseDefinition
 *
 * @since 2024/04/30
 */
public class CangjieMacrocallParseDefinition implements ParserDefinition {
    private static final IFileElementType FILE = new IFileElementType(CangjieMacrocallLanguage.INSTANCE);

    /**
     * Definition of empty nodes
     */
    private static final TokenSet WHITESPACE =
            PSIElementTypeFactory.createTokenSet(CangjieMacrocallLanguage.INSTANCE, CharLexer.WS);

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(
            CangjieMacrocallLanguage.INSTANCE, CharParser.tokenNames, CharParser.ruleNames);
    }

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        CharLexer lexer = new CharLexer(null);
        lexer.isGarbled = false;
        FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (editor == null) {
            return new CJLexerAdaptor(CangjieMacrocallLanguage.INSTANCE, lexer);
        }
        VirtualFile file = editor.getFile();
        if (file == null) {
            return new CJLexerAdaptor(CangjieMacrocallLanguage.INSTANCE, lexer);
        }
        try {
            String content = new String(file.contentsToByteArray());
            Charset charset = file.getCharset();
            String newContent = new String(content.getBytes(charset), StandardCharsets.UTF_8);
            if (!content.equals(newContent)) {
                lexer.isGarbled = true;
            }
        } catch (IOException e) {
            return new CJLexerAdaptor(CangjieMacrocallLanguage.INSTANCE, lexer);
        }
        // ANTLRLexerAdaptor is an adapter for antlr intellij atapter to
        // bind the developer's custom language with the underlying ANTLR lexical analyzer
        return new CJLexerAdaptor(CangjieMacrocallLanguage.INSTANCE, lexer);
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        final CharParser parser = new CharParser(null);
        // ANTLRParserAdaptor is an adapter for antlr intellij atapter to
        // bind the developer's custom language with the underlying ANTLR parser
        return new CJParserAdaptor(CangjieMacrocallLanguage.INSTANCE, parser) {
            @Override
            @Nullable
            protected ParseTree parse(Parser parser, IElementType root) {
                if (!(parser instanceof CharParser)) {
                    return null;
                }
                // parse entry
                return ((CharParser) parser).translationUnit();
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
        return TokenSet.EMPTY;
    }

    /**
     * Returns the set of types treated as strings by the PSI builder.
     *
     * @return String node type
     */
    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
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
        return new CangjieMacrocallPsiFileRoot(viewProvider);
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
