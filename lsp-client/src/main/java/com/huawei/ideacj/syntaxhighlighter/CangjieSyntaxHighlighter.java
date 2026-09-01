/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.syntaxhighlighter;

import static com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter.colorOf;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.CJLexerAdaptor;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.PSIElementTypeFactory;
import com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.lsp.utils.CangjieMacrocallLanguage;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;

import cjgrammar.parser.CharLexer;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Syntax highlight: client syntax
 *
 * @since 2020-06-08
 */
public class CangjieSyntaxHighlighter extends SyntaxHighlighterBase {
    /**
     * keyword type
     */
    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("Cangjie_KEY", colorOf(SemanticToken.KEYWORD));

    /**
     * string type
     */
    public static final TextAttributesKey STRING =
            createTextAttributesKey("Cangjie_STRING", colorOf(SemanticToken.STRING));

    /**
     * OPERATOR type
     */
    public static final TextAttributesKey OPERATOR =
            createTextAttributesKey("Cangjie_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    /**
     * BRACES type
     */
    public static final TextAttributesKey BRACES =
            createTextAttributesKey("Cangjie_BRACES", DefaultLanguageHighlighterColors.BRACES);

    /**
     * DOT type
     */
    public static final TextAttributesKey DOT =
            createTextAttributesKey("Cangjie_DOT", DefaultLanguageHighlighterColors.DOT);

    /**
     * SEMICOLON type
     */
    public static final TextAttributesKey SEMICOLON =
            createTextAttributesKey("Cangjie_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);

    /**
     * COMMA type
     */
    public static final TextAttributesKey COMMA =
            createTextAttributesKey("Cangjie_COMMA", DefaultLanguageHighlighterColors.COMMA);

    /**
     * BRACKETS type
     */
    public static final TextAttributesKey BRACKETS =
            createTextAttributesKey("Cangjie_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);

    /**
     * string type
     */
    public static final TextAttributesKey PARENTHESES =
            createTextAttributesKey("Cangjie_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);

    /**
     * comment type
     */
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("Cangjie_COMMENT", colorOf(SemanticToken.COMMENT));

    /**
     * number type
     */
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("Cangjie_NUMBER", colorOf(SemanticToken.NUMBER));

    /**
     * default identifier type
     */
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("DEFAULT_IDENTIFIER", HighlighterColors.TEXT);

    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[] {KEYWORD};

    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[] {STRING};

    private static final TextAttributesKey[] ESCAPE_SEQ_KEYS =
            new TextAttributesKey[] {CangjieSemanticTokenHighlighter.ESCAPE};

    private static final TextAttributesKey[] OPERATOR_KEYS = new TextAttributesKey[] {OPERATOR};

    private static final TextAttributesKey[] BRACES_KEYS = new TextAttributesKey[] {BRACES};

    private static final TextAttributesKey[] DOT_KEYS = new TextAttributesKey[] {DOT};

    private static final TextAttributesKey[] SEMICOLON_KEYS = new TextAttributesKey[] {SEMICOLON};

    private static final TextAttributesKey[] COMMA_KEYS = new TextAttributesKey[] {COMMA};

    private static final TextAttributesKey[] BRACKETS_KEYS = new TextAttributesKey[] {BRACKETS};

    private static final TextAttributesKey[] PARENTHESES_KEYS = new TextAttributesKey[] {PARENTHESES};

    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[] {NUMBER};

    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[] {IDENTIFIER};

    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[] {COMMENT};

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(
                CangJieLanguage.INSTANCE, cjgrammar.parser.CharParser.tokenNames,
            cjgrammar.parser.CharParser.ruleNames);
        PSIElementTypeFactory.defineLanguageIElementTypes(
                CangjieMacrocallLanguage.INSTANCE, cjgrammar.parser.CharParser.tokenNames,
            cjgrammar.parser.CharParser.ruleNames);
    }

    IElementType[] elementTypes = {
        CangJieTypes.INT8,
        CangJieTypes.INT16,
        CangJieTypes.INT32,
        CangJieTypes.INT64,
        CangJieTypes.UINT8,
        CangJieTypes.UINT16,
        CangJieTypes.UINT32,
        CangJieTypes.UINT64,
        CangJieTypes.FLOAT16,
        CangJieTypes.FLOAT32,
        CangJieTypes.FLOAT64,
        CangJieTypes.RUNE,
        CangJieTypes.BOOLEAN,
        CangJieTypes.UNIT,
        CangJieTypes.STRUCT,
        CangJieTypes.ENUM,
        CangJieTypes.THISTYPE
    };

    IElementType[] elementKeyWords = {
        CangJieTypes.PACKAGE,
        CangJieTypes.CLASS,
        CangJieTypes.EXTEND,
        CangJieTypes.IMPORT,
        CangJieTypes.INTERFACE,
        CangJieTypes.FUNC,
        CangJieTypes.LET,
        CangJieTypes.VAR,
        CangJieTypes.TYPE_ALIAS,
        CangJieTypes.THIS,
        CangJieTypes.INIT,
        CangJieTypes.SUPER,
        CangJieTypes.WHILE,
        CangJieTypes.IF,
        CangJieTypes.ELSE,
        CangJieTypes.RETURN,
        CangJieTypes.CASE,
        CangJieTypes.TRY,
        CangJieTypes.CATCH,
        CangJieTypes.IN,
        CangJieTypes.FOR,
        CangJieTypes.DO,
        CangJieTypes.THROW,
        CangJieTypes.CONTINUE,
        CangJieTypes.BREAK,
        CangJieTypes.AS,
        CangJieTypes.FINALLY,
        CangJieTypes.MATCH,
        CangJieTypes.INTERNAL,
        CangJieTypes.WHERE,
        CangJieTypes.SPAWN,
        CangJieTypes.IS,
        CangJieTypes.SYNCHRONIZED,
        CangJieTypes.FALSE,
        CangJieTypes.TRUE,
        CangJieTypes.INTNATIVE,
        CangJieTypes.UINTNATIVE,
        CangJieTypes.QUOTE,
        CangJieTypes.NOTHING,
        CangJieTypes.CONST,
        CangJieTypes.MAIN,
        CangJieTypes.UNSAFE,
        CangJieTypes.GRAD,
        CangJieTypes.ADJOINTOF,
        CangJieTypes.DIFFERENTIABLE,
        CangJieTypes.ADJOINT,
        CangJieTypes.VAL_WITH_GRAD,
        CangJieTypes.PRIMAL,
        CangJieTypes.EXCEPT,
        CangJieTypes.INCLUDE,
        CangJieTypes.STAGE,
        CangJieTypes.VJP,
        CangJieTypes.VARRAY
    };

    IElementType[] elementEscape = {CangJieTypes.ESCAPE_SEQ};

    IElementType[] elementBraces = {CangJieTypes.LCURL, CangJieTypes.RCURL, CangJieTypes.LINE_STR_EXPR_START};

    IElementType[] elementParentheses = {CangJieTypes.LPAREN, CangJieTypes.RPAREN};

    IElementType[] elementBrackets = {CangJieTypes.LSQUARE, CangJieTypes.RSQUARE};

    IElementType[] elementOperators = {
        CangJieTypes.EXP,
        CangJieTypes.MUL,
        CangJieTypes.MOD,
        CangJieTypes.DIV,
        CangJieTypes.ADD,
        CangJieTypes.SUB,
        CangJieTypes.PIPELINE,
        CangJieTypes.COMPOSITION,
        CangJieTypes.INC,
        CangJieTypes.DEC,
        CangJieTypes.AND,
        CangJieTypes.OR,
        CangJieTypes.NOT,
        CangJieTypes.BITAND,
        CangJieTypes.BITOR,
        CangJieTypes.BITXOR,
        CangJieTypes.LSHIFT,
        CangJieTypes.COLON,
        CangJieTypes.ASSIGN,
        CangJieTypes.ADD_ASSIGN,
        CangJieTypes.SUB_ASSIGN,
        CangJieTypes.MUL_ASSIGN,
        CangJieTypes.EXP_ASSIGN,
        CangJieTypes.DIV_ASSIGN,
        CangJieTypes.MOD_ASSIGN,
        CangJieTypes.ARROW,
        CangJieTypes.DOUBLE_ARROW,
        CangJieTypes.ELLIPSIS,
        CangJieTypes.CLOSEDRANGEOP,
        CangJieTypes.RANGEOP,
        CangJieTypes.HASH,
        CangJieTypes.QUEST,
        CangJieTypes.UPPERBOUND,
        CangJieTypes.LT,
        CangJieTypes.GT,
        CangJieTypes.LE,
        CangJieTypes.NOTEQUAL,
        CangJieTypes.EQUAL,
        CangJieTypes.WILDCARD,
        CangJieTypes.BACKSLASH
    };

    IElementType[] elementModifiers = {
        CangJieTypes.PUBLIC,
        CangJieTypes.PRIVATE,
        CangJieTypes.PROTECTED,
        CangJieTypes.STATIC,
        CangJieTypes.OVERRIDE,
        CangJieTypes.ABSTRACT,
        CangJieTypes.OPEN,
        CangJieTypes.OPERATOR,
        CangJieTypes.FOREIGN,
        CangJieTypes.MACRO,
        CangJieTypes.MUT,
        CangJieTypes.PROP,
        CangJieTypes.REDEF,
        CangJieTypes.AT,
        CangJieTypes.SEALED
    };

    IElementType[] elementConstraints = {
        CangJieTypes.FLOAT_LITERAL,
        CangJieTypes.INTEGER_LITERAL,
        CangJieTypes.BYTE_STRING_LITERAL,
        CangJieTypes.CHARACTER_BYTE_LITERAL
    };

    IElementType[] elementStrings = {
        CangJieTypes.MULTI_LINE_STR_TEXT,
        CangJieTypes.MULTI_LINE_RAW_STRING_LITERAL,
        CangJieTypes.LINE_STR_TEXT,
        CangJieTypes.CHARACTER_LITERAL,
        CangJieTypes.TRIPLE_QUOTE_CLOSE,
        CangJieTypes.TRIPLE_QUOTE_OPEN,
        CangJieTypes.QUOTE_CLOSE,
        CangJieTypes.QUOTE_OPEN,
        CangJieTypes.SINGLE_QUOTE_OPEN,
        CangJieTypes.SINGLE_QUOTE_CLOSE,
        CangJieTypes.TRIPLE_SINGLE_QUOTE_OPEN,
        CangJieTypes.TRIPLE_SINGLE_QUOTE_CLOSE,
        CangJieTypes.MULTI_LINE_STRING_QUOTE,
        CangJieTypes.MULTI_LINE_STRING_SINGLE_QUOTE
    };

    IElementType elementIdentifier = CangJieTypes.IDENTIFIER;

    Set<IElementType> setTypes = new HashSet<>(Arrays.asList(elementTypes));

    Set<IElementType> setKeywords = new HashSet<>(Arrays.asList(elementKeyWords));

    Set<IElementType> setModifiers = new HashSet<>(Arrays.asList(elementModifiers));

    Set<IElementType> setConstraints = new HashSet<>(Arrays.asList(elementConstraints));

    Set<IElementType> setStrings = new HashSet<>(Arrays.asList(elementStrings));

    IElementType elementLine = CangJieTypes.LINE_COMMENT;

    IElementType elementBlock = CangJieTypes.BLOCK_COMMENT;

    Set<IElementType> setEscape = new HashSet<>(Arrays.asList(elementEscape));

    Set<IElementType> setBraces = new HashSet<>(Arrays.asList(elementBraces));

    Set<IElementType> setOperators = new HashSet<>(Arrays.asList(elementOperators));

    IElementType elementDot = CangJieTypes.DOT;

    IElementType elementComma = CangJieTypes.COMMA;

    IElementType elementSemiColon = CangJieTypes.SEMI;

    Set<IElementType> setBrackets = new HashSet<>(Arrays.asList(elementBrackets));

    Set<IElementType> setParentheses = new HashSet<>(Arrays.asList(elementParentheses));

    Map<IElementType, TextAttributesKey[]> strategies = new HashMap() {{
        put(elementDot, Arrays.copyOf(DOT_KEYS, DOT_KEYS.length));
        put(elementSemiColon, Arrays.copyOf(SEMICOLON_KEYS, SEMICOLON_KEYS.length));
        put(elementComma, Arrays.copyOf(COMMA_KEYS, COMMA_KEYS.length));
        put(elementLine, Arrays.copyOf(COMMENT_KEYS, COMMENT_KEYS.length));
        put(elementBlock, Arrays.copyOf(COMMENT_KEYS, COMMENT_KEYS.length));
        put(elementIdentifier, Arrays.copyOf(IDENTIFIER_KEYS, IDENTIFIER_KEYS.length));
    }};

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        CharLexer charLexer = new CharLexer(null);
        return new CJLexerAdaptor(CangJieLanguage.INSTANCE, charLexer);
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (setTypes.contains(tokenType) || setKeywords.contains(tokenType) || setModifiers.contains(tokenType)) {
            return Arrays.copyOf(KEYWORD_KEYS, KEYWORD_KEYS.length);
        } else if (setConstraints.contains(tokenType)) {
            return Arrays.copyOf(NUMBER_KEYS, NUMBER_KEYS.length);
        } else if (setStrings.contains(tokenType)) {
            return Arrays.copyOf(STRING_KEYS, STRING_KEYS.length);
        } else if (setOperators.contains(tokenType)) {
            return Arrays.copyOf(OPERATOR_KEYS, OPERATOR_KEYS.length);
        } else if (setEscape.contains(tokenType)) {
            return Arrays.copyOf(ESCAPE_SEQ_KEYS, ESCAPE_SEQ_KEYS.length);
        } else if (setBraces.contains(tokenType)) {
            return Arrays.copyOf(BRACES_KEYS, BRACES_KEYS.length);
        } else if (setBrackets.contains(tokenType)) {
            return Arrays.copyOf(BRACKETS_KEYS, BRACKETS_KEYS.length);
        } else if (setParentheses.contains(tokenType)) {
            return Arrays.copyOf(PARENTHESES_KEYS, PARENTHESES_KEYS.length);
        } else if (strategies.containsKey(tokenType)) {
            return strategies.get(tokenType);
        } else {
            return Arrays.copyOf(EMPTY_KEYS, EMPTY_KEYS.length);
        }
    }
}
