/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language;

import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.PSIElementTypeFactory;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.RuleIElementType;
import com.huawei.ideacj.extend.cjpsi.adaptor.lexer.TokenElementType;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import cjgrammar.parser.CharLexer;
import cjgrammar.parser.CharParser;

/**
 * Cangjie types
 *
 * @since 2021 -07-07
 */
public class CangJieTypes {
    /**
     * The constant DELIMITED_COMMENT.
     */
    public static final TokenElementType BLOCK_COMMENT;

    /**
     * The constant LINE_COMMENT.
     */
    public static final TokenElementType LINE_COMMENT;

    /**
     * EDITOR_FOLD_START
     */
    public static final TokenElementType EDITOR_FOLD_START;

    /**
     * EDITOR_FOLD_END
     */
    public static final TokenElementType EDITOR_FOLD_END;

    /**
     * The constant WS.
     */
    public static final TokenElementType WS;

    /**
     * The constant NL.
     */
    public static final TokenElementType NL;

    /**
     * The constant DOT.
     */
    public static final TokenElementType DOT;

    /**
     * The constant COMMA.
     */
    public static final TokenElementType COMMA;

    /**
     * The constant LPAREN.
     */
    public static final TokenElementType LPAREN;

    /**
     * The constant RPAREN.
     */
    public static final TokenElementType RPAREN;

    /**
     * The constant LSQUARE.
     */
    public static final TokenElementType LSQUARE;

    /**
     * The constant RSQUARE.
     */
    public static final TokenElementType RSQUARE;

    /**
     * The constant LCURL.
     */
    public static final TokenElementType LCURL;

    /**
     * The constant LineStrExprStart.
     */
    public static final TokenElementType LINE_STR_EXPR_START;

    /**
     * The constant MultiLineStrExprStart.
     */
    public static final TokenElementType MULTI_LINE_STR_EXPR_START;

    /**
     * The constant RCURL.
     */
    public static final TokenElementType RCURL;

    /**
     * The constant EXP.
     */
    public static final TokenElementType EXP;

    /**
     * The constant MUL.
     */
    public static final TokenElementType MUL;

    /**
     * The constant MOD.
     */
    public static final TokenElementType MOD;

    /**
     * The constant DIV.
     */
    public static final TokenElementType DIV;

    /**
     * The constant ADD.
     */
    public static final TokenElementType ADD;

    /**
     * The constant SUB.
     */
    public static final TokenElementType SUB;

    /**
     * The constant PIPELINE.
     */
    public static final TokenElementType PIPELINE;

    /**
     * The constant COMPOSITION.
     */
    public static final TokenElementType COMPOSITION;

    /**
     * The constant INC.
     */
    public static final TokenElementType INC;

    /**
     * The constant DEC.
     */
    public static final TokenElementType DEC;

    /**
     * The constant AND.
     */
    public static final TokenElementType AND;

    /**
     * The constant OR.
     */
    public static final TokenElementType OR;

    /**
     * The constant NOT.
     */
    public static final TokenElementType NOT;

    /**
     * The constant BITAND.
     */
    public static final TokenElementType BITAND;

    /**
     * The constant BITOR.
     */
    public static final TokenElementType BITOR;

    /**
     * The constant BITXOR.
     */
    public static final TokenElementType BITXOR;

    /**
     * The constant LSHIFT.
     */
    public static final TokenElementType LSHIFT;

    /**
     * The constant COLON.
     */
    public static final TokenElementType COLON;

    /**
     * The constant SEMI.
     */
    public static final TokenElementType SEMI;

    /**
     * The constant ASSIGN.
     */
    public static final TokenElementType ASSIGN;

    /**
     * The constant ADD_ASSIGN.
     */
    public static final TokenElementType ADD_ASSIGN;

    /**
     * The constant SUB_ASSIGN.
     */
    public static final TokenElementType SUB_ASSIGN;

    /**
     * The constant MUL_ASSIGN.
     */
    public static final TokenElementType MUL_ASSIGN;

    /**
     * The constant EXP_ASSIGN.
     */
    public static final TokenElementType EXP_ASSIGN;

    /**
     * The constant DIV_ASSIGN.
     */
    public static final TokenElementType DIV_ASSIGN;

    /**
     * The constant MOD_ASSIGN.
     */
    public static final TokenElementType MOD_ASSIGN;

    /**
     * The constant ARROW.
     */
    public static final TokenElementType ARROW;

    /**
     * The constant DOUBLE_ARROW.
     */
    public static final TokenElementType DOUBLE_ARROW;

    /**
     * The constant ELLIPSIS.
     */
    public static final TokenElementType ELLIPSIS;

    /**
     * The constant CLOSEDRANGEOP.
     */
    public static final TokenElementType CLOSEDRANGEOP;

    /**
     * The constant RANGEOP.
     */
    public static final TokenElementType RANGEOP;

    /**
     * The constant HASH.
     */
    public static final TokenElementType HASH;

    /**
     * The constant AT.
     */
    public static final TokenElementType AT;

    /**
     * The constant QUEST.
     */
    public static final TokenElementType QUEST;

    /**
     * The constant UPPERBOUND.
     */
    public static final TokenElementType UPPERBOUND;

    /**
     * The constant LT.
     */
    public static final TokenElementType LT;

    /**
     * The constant GT.
     */
    public static final TokenElementType GT;

    /**
     * The constant LE.
     */
    public static final TokenElementType LE;

    /**
     * The constant NOTEQUAL.
     */
    public static final TokenElementType NOTEQUAL;

    /**
     * The constant EQUAL.
     */
    public static final TokenElementType EQUAL;

    /**
     * The constant WILDCARD.
     */
    public static final TokenElementType WILDCARD;

    /**
     * The constant BACKSLASH.
     */
    public static final TokenElementType BACKSLASH;

    /**
     * The constant QUOTESYMBOL.
     */
    public static final TokenElementType QUOTESYMBOL;

    /**
     * The constant DOLLAR.
     */
    public static final TokenElementType DOLLAR;

    /**
     * The constant INT8.
     */
    public static final TokenElementType INT8;

    /**
     * The constant INT16.
     */
    public static final TokenElementType INT16;

    /**
     * The constant INT32.
     */
    public static final TokenElementType INT32;

    /**
     * The constant INT64.
     */
    public static final TokenElementType INT64;

    /**
     * The constant INTNATIVE.
     */
    public static final TokenElementType INTNATIVE;

    /**
     * The constant UINT8.
     */
    public static final TokenElementType UINT8;

    /**
     * The constant UINT16.
     */
    public static final TokenElementType UINT16;

    /**
     * The constant UINT32.
     */
    public static final TokenElementType UINT32;

    /**
     * The constant UINT64.
     */
    public static final TokenElementType UINT64;

    /**
     * The constant UINTNATIVE.
     */
    public static final TokenElementType UINTNATIVE;

    /**
     * The constant FLOAT16.
     */
    public static final TokenElementType FLOAT16;

    /**
     * The constant FLOAT32.
     */
    public static final TokenElementType FLOAT32;

    /**
     * The constant FLOAT64.
     */
    public static final TokenElementType FLOAT64;

    /**
     * The constant RUNE.
     */
    public static final TokenElementType RUNE;

    /**
     * The constant BOOLEAN.
     */
    public static final TokenElementType BOOLEAN;

    /**
     * The constant UNIT.
     */
    public static final TokenElementType UNIT;

    /**
     * The constant NOTHING.
     */
    public static final TokenElementType NOTHING;

    /**
     * The constant STRUCT.
     */
    public static final TokenElementType STRUCT;

    /**
     * The constant ENUM.
     */
    public static final TokenElementType ENUM;

    /**
     * The constant THISTYPE.
     */
    public static final TokenElementType THISTYPE;

    /**
     * The constant PACKAGE.
     */
    public static final TokenElementType PACKAGE;

    /**
     * The constant IMPORT.
     */
    public static final TokenElementType IMPORT;

    /**
     * The constant CLASS.
     */
    public static final TokenElementType CLASS;

    /**
     * The constant INTERFACE.
     */
    public static final TokenElementType INTERFACE;

    /**
     * The constant FUNC.
     */
    public static final TokenElementType FUNC;

    /**
     * The constant LET.
     */
    public static final TokenElementType LET;

    /**
     * The constant VAR.
     */
    public static final TokenElementType VAR;

    /**
     * The constant TYPE_ALIAS.
     */
    public static final TokenElementType TYPE_ALIAS;

    /**
     * The constant INIT.
     */
    public static final TokenElementType INIT;

    /**
     * The constant THIS.
     */
    public static final TokenElementType THIS;

    /**
     * The constant SUPER.
     */
    public static final TokenElementType SUPER;

    /**
     * The constant IF.
     */
    public static final TokenElementType IF;

    /**
     * The constant ELSE.
     */
    public static final TokenElementType ELSE;

    /**
     * The constant CASE.
     */
    public static final TokenElementType CASE;

    /**
     * The constant TRY.
     */
    public static final TokenElementType TRY;

    /**
     * The constant CATCH.
     */
    public static final TokenElementType CATCH;

    /**
     * The constant FINALLY.
     */
    public static final TokenElementType FINALLY;

    /**
     * The constant FOR.
     */
    public static final TokenElementType FOR;

    /**
     * The constant DO.
     */
    public static final TokenElementType DO;

    /**
     * The constant WHILE.
     */
    public static final TokenElementType WHILE;

    /**
     * The constant THROW.
     */
    public static final TokenElementType THROW;

    /**
     * The constant RETURN.
     */
    public static final TokenElementType RETURN;

    /**
     * The constant CONTINUE.
     */
    public static final TokenElementType CONTINUE;

    /**
     * The constant BREAK.
     */
    public static final TokenElementType BREAK;

    /**
     * The constant IS.
     */
    public static final TokenElementType IS;

    /**
     * The constant AS.
     */
    public static final TokenElementType AS;

    /**
     * The constant IN.
     */
    public static final TokenElementType IN;

    /**
     * The constant MATCH.
     */
    public static final TokenElementType MATCH;

    /**
     * The constant INTERNAL.
     */
    public static final TokenElementType INTERNAL;

    /**
     * The constant WHERE.
     */
    public static final TokenElementType WHERE;

    /**
     * The constant EXTEND.
     */
    public static final TokenElementType EXTEND;

    /**
     * The constant SPAWN.
     */
    public static final TokenElementType SPAWN;

    /**
     * The constant SYNCHRONIZED.
     */
    public static final TokenElementType SYNCHRONIZED;

    /**
     * The constant MACRO.
     */
    public static final TokenElementType MACRO;

    /**
     * The constant QUOTE.
     */
    public static final TokenElementType QUOTE;

    /**
     * The constant TRUE.
     */
    public static final TokenElementType TRUE;

    /**
     * The constant FALSE.
     */
    public static final TokenElementType FALSE;

    /**
     * The constant PROP.
     */
    public static final TokenElementType PROP;

    /**
     * The constant MUT.
     */
    public static final TokenElementType MUT;

    /**
     * The constant INTERNAL.
     */
    public static final TokenElementType CONST;

    /**
     * The constant VAL_WITH_GRAD.
     */
    public static final TokenElementType VAL_WITH_GRAD;

    /**
     * The constant EXCEPT.
     */
    public static final TokenElementType EXCEPT;

    /**
     * The constant INCLUDE.
     */
    public static final TokenElementType INCLUDE;

    /**
     * The constant INCLUDE.
     */
    public static final TokenElementType STAGE;

    /**
     * The constant P.
     */
    public static final TokenElementType PRIMAL;

    /**
     * The constant GRAD.
     */
    public static final TokenElementType GRAD;

    /**
     * The constant @vjp.
     */
    public static final TokenElementType VJP;

    /**
     * The constant ADJOINTOF.
     */
    public static final TokenElementType ADJOINTOF;

    /**
     * The constant DIFFERENTIABLE.
     */
    public static final TokenElementType DIFFERENTIABLE;

    /**
     * The constant ADJOINT
     */
    public static final TokenElementType ADJOINT;

    /**
     * The constant EXTERNAL.
     */
    public static final TokenElementType MAIN;

    /**
     * The constant STATIC.
     */
    public static final TokenElementType STATIC;

    /**
     * The constant PUBLIC.
     */
    public static final TokenElementType PUBLIC;

    /**
     * The constant PRIVATE.
     */
    public static final TokenElementType PRIVATE;

    /**
     * The constant PROTECTED.
     */
    public static final TokenElementType PROTECTED;

    /**
     * The constant OVERRIDE.
     */
    public static final TokenElementType OVERRIDE;

    /**
     * The constant ABSTRACT.
     */
    public static final TokenElementType ABSTRACT;

    /**
     * The constant OPEN.
     */
    public static final TokenElementType OPEN;

    /**
     * The constant SEALED.
     */
    public static final TokenElementType SEALED;

    /**
     * The constant OPERATOR.
     */
    public static final TokenElementType OPERATOR;

    /**
     * The constant FOREIGN.
     */
    public static final TokenElementType FOREIGN;

    /**
     * The constant REDEF.
     */
    public static final TokenElementType REDEF;

    /**
     * The constant UNSAFE.
     */
    public static final TokenElementType UNSAFE;

    /**
     * The constant FLOAT_LITERAL.
     */
    public static final TokenElementType FLOAT_LITERAL;

    /**
     * The constant INTEGER_LITERAL.
     */
    public static final TokenElementType INTEGER_LITERAL;

    /**
     * The constant IDENTIFIER.
     */
    public static final TokenElementType IDENTIFIER;

    /**
     * The constant CHARACTER_LITERAL.
     */
    public static final TokenElementType CHARACTER_LITERAL;

    /**
     * The constant MULTI_LINE_RAW_STRING_LITERAL.
     */
    public static final TokenElementType MULTI_LINE_RAW_STRING_LITERAL;

    /**
     * The constant QUOTE_OPEN.
     */
    public static final TokenElementType QUOTE_OPEN;

    /**
     * The constant TRIPLE_QUOTE_OPEN.
     */
    public static final TokenElementType TRIPLE_QUOTE_OPEN;

    /**
     * The constant SINGLE_QUOTE_CLOSE.
     */
    public static final TokenElementType SINGLE_QUOTE_CLOSE;

    /**
     * The constant SINGLE_QUOTE_OPEN.
     */
    public static final TokenElementType SINGLE_QUOTE_OPEN;

    /**
     * The constant TRIPLE_SINGLE_QUOTE_OPEN.
     */
    public static final TokenElementType TRIPLE_SINGLE_QUOTE_OPEN;

    /**
     * The constant TRIPLE_SINGLE_QUOTE_CLOSE.
     */
    public static final TokenElementType TRIPLE_SINGLE_QUOTE_CLOSE;

    /**
     * The constant QUOTE_CLOSE.
     */
    public static final TokenElementType QUOTE_CLOSE;

    /**
     * The constant LINE_STR_TEXT.
     */
    public static final TokenElementType LINE_STR_TEXT;

    /**
     * The const Escape_Seq
     */
    public static final TokenElementType ESCAPE_SEQ;

    /**
     * The constant TRIPLE_QUOTE_CLOSE.
     */
    public static final TokenElementType TRIPLE_QUOTE_CLOSE;

    /**
     * The constant MULTI_LINE_STR_TEXT.
     */
    public static final TokenElementType MULTI_LINE_STR_TEXT;

    /**
     * The constant MULTI_LINE_STRING_QUOTE.
     */
    public static final TokenElementType MULTI_LINE_STRING_QUOTE;

    /**
     * The constant MULTI_LINE_STRING_SINGLE_QUOTE.
     */
    public static final TokenElementType MULTI_LINE_STRING_SINGLE_QUOTE;

    /**
     * The constant BYTE_STRING_LITERAL.
     */
    public static final TokenElementType BYTE_STRING_LITERAL;

    /**
     * The constant CHARACTER_BYTE_LITERAL.
     */
    public static final TokenElementType CHARACTER_BYTE_LITERAL;

    /**
     * The constant VARRAY.
     */
    public static final TokenElementType VARRAY;

    /**
     * The constant RULE_TRANSLATION_UNIT.
     */
    public static final RuleIElementType RULE_TRANSLATION_UNIT;

    /**
     * The constant RULE_IMPORT_LIST.
     */
    public static final RuleIElementType RULE_IMPORT_LIST;

    /**
     * The constant RULE_PREAMBLE.
     */
    public static final RuleIElementType RULE_PREAMBLE;

    /**
     * The constant RULE_END.
     */
    public static final RuleIElementType RULE_END;

    /**
     * The constant RULE_IMPORT_ALL_OR_SPECIFIED.
     */
    public static final RuleIElementType RULE_IMPORT_ALL_OR_SPECIFIED;

    /**
     * The constant RULE_QUOTE_TOKEN.
     */
    public static final RuleIElementType RULE_QUOTE_TOKEN;

    /**
     * The constant RULE_QUOTE_TOKEN.
     */
    public static final RuleIElementType RULE_MACRO_TOKENS;

    /**
     * The constant RULE_LITERAL_CONSTANT.
     */
    public static final RuleIElementType RULE_LITERAL_CONSTANT;

    /**
     * The constant RULE_STRING_LITERAL.
     */
    public static final RuleIElementType RULE_STRING_LITERAL;

    /**
     * The constant RULE_LINE_STRING_LITERAL.
     */
    public static final RuleIElementType RULE_LINE_STRING_LITERAL;

    /**
     * The constant RULE_LINE_STRING_CONTENT.
     */
    public static final RuleIElementType RULE_LINE_STRING_CONTENT;

    /**
     * The constant RULE_CLASS_DEFINITION.
     */
    public static final RuleIElementType RULE_CLASS_DEFINITION;

    /**
     * The constant RULE_PACKAGE_HEADER.
     */
    public static final RuleIElementType RULE_PACKAGE_HEADER;


    /**
     *  The constant RCURL_INTERP.
     */
    public static final TokenElementType CHARACTER_STR_EXPR_START;

    static {
        PSIElementTypeFactory.defineLanguageIElementTypes(CangJieLanguage.INSTANCE, CharParser.tokenNames,
                CharParser.ruleNames);

        BLOCK_COMMENT =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DelimitedComment);

        LINE_COMMENT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LineComment);

        EDITOR_FOLD_START = PSIElementTypeFactory.getTokenIElementTypes(
                CangJieLanguage.INSTANCE).get(CharParser.EditorfoldStart);

        EDITOR_FOLD_END = PSIElementTypeFactory.getTokenIElementTypes(
                CangJieLanguage.INSTANCE).get(CharLexer.EditorfoldEnd);

        WS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.WS);

        NL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.NL);

        DOT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DOT);

        COMMA = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.COMMA);

        LPAREN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LPAREN);

        RPAREN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RPAREN);

        LSQUARE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LSQUARE);

        RSQUARE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RSQUARE);

        LCURL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LCURL);

        LINE_STR_EXPR_START =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LineStrExprStart);

        CHARACTER_STR_EXPR_START =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                        .get(CharLexer.CharacterStrExprStart);

        MULTI_LINE_STR_EXPR_START =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharLexer.MultiLineStrExprStart);

        RCURL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RCURL);

        EXP = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EXP);

        MUL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MUL);

        MOD = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MOD);

        DIV = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DIV);

        ADD = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ADD);

        SUB = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SUB);

        PIPELINE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PIPELINE);

        COMPOSITION = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.COMPOSITION);

        INC = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INC);

        DEC = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DEC);

        AND = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.AND);

        OR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.OR);

        NOT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.NOT);

        BITAND = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BITAND);

        BITOR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BITOR);

        BITXOR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BITXOR);

        LSHIFT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LSHIFT);

        COLON = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.COLON);

        SEMI = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SEMI);

        ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ASSIGN);

        ADD_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ADD_ASSIGN);

        SUB_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SUB_ASSIGN);

        MUL_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MUL_ASSIGN);

        EXP_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EXP_ASSIGN);

        DIV_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DIV_ASSIGN);

        MOD_ASSIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MOD_ASSIGN);

        ARROW = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ARROW);

        DOUBLE_ARROW =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DOUBLE_ARROW);

        ELLIPSIS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ELLIPSIS);

        CLOSEDRANGEOP =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CLOSEDRANGEOP);

        RANGEOP = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RANGEOP);

        HASH = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.HASH);

        AT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.AT);

        QUEST = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.QUEST);

        UPPERBOUND = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UPPERBOUND);

        LT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LT);

        GT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.GT);

        LE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LE);

        NOTEQUAL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.NOTEQUAL);

        EQUAL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EQUAL);

        WILDCARD = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.WILDCARD);

        BACKSLASH = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BACKSLASH);

        QUOTESYMBOL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.QUOTESYMBOL);

        DOLLAR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DOLLAR);

        INT8 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INT8);

        INT16 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INT16);

        INT32 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INT32);

        INT64 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INT64);

        INTNATIVE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INTNATIVE);

        UINT8 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UINT8);

        UINT16 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UINT16);

        UINT32 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UINT32);

        UINT64 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UINT64);

        UINTNATIVE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UINTNATIVE);

        FLOAT16 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FLOAT16);

        FLOAT32 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FLOAT32);

        FLOAT64 = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FLOAT64);

        RUNE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RUNE);

        BOOLEAN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BOOLEAN);

        UNIT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UNIT);

        NOTHING = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.NOTHING);

        STRUCT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.STRUCT);

        ENUM = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ENUM);

        THISTYPE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.THISTYPE);

        PACKAGE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PACKAGE);

        IMPORT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.IMPORT);

        CLASS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CLASS);

        INTERFACE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INTERFACE);

        FUNC = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FUNC);

        LET = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LET);

        VAR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.VAR);

        TYPE_ALIAS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.TYPE_ALIAS);

        INIT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INIT);

        THIS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.THIS);

        SUPER = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SUPER);

        IF = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.IF);

        ELSE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ELSE);

        CASE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CASE);

        TRY = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.TRY);

        CATCH = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CATCH);

        FINALLY = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FINALLY);

        FOR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FOR);

        DO = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DO);

        WHILE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.WHILE);

        THROW = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.THROW);

        RETURN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.RETURN);

        CONTINUE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CONTINUE);

        BREAK = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.BREAK);

        IS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.IS);

        AS = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.AS);

        IN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.IN);

        MATCH = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MATCH);

        INTERNAL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INTERNAL);

        WHERE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.WHERE);

        EXTEND = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EXTEND);

        SPAWN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SPAWN);

        SYNCHRONIZED =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SYNCHRONIZED);

        MACRO = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MACRO);

        QUOTE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.QUOTE);

        TRUE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.TRUE);

        FALSE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FALSE);

        PROP = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PROP);

        MUT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MUT);

        CONST = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CONST);

        VAL_WITH_GRAD =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.VAlWITHGRAD);

        EXCEPT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EXCEPT);

        INCLUDE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.INCLUDE);

        STAGE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.STAGE);

        PRIMAL = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PRIMAL);

        GRAD = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.GRAD);

        VJP = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.VJP);

        ADJOINTOF = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ADJOINTOF);

        DIFFERENTIABLE =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.DIFFERENTIABLE);

        ADJOINT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ADJOINT);

        MAIN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MAIN);

        STATIC = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.STATIC);

        PUBLIC = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PUBLIC);

        PRIVATE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PRIVATE);

        PROTECTED = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.PROTECTED);

        OVERRIDE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.OVERRIDE);

        ABSTRACT = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ABSTRACT);

        OPEN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.OPEN);

        SEALED = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SEALED);

        OPERATOR = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.OPERATOR);

        FOREIGN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FOREIGN);

        REDEF = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.REDEF);

        UNSAFE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.UNSAFE);

        FLOAT_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.FloatLiteral);

        INTEGER_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.IntegerLiteral);

        IDENTIFIER = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.Identifier);

        CHARACTER_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.CharacterLiteral);

        MULTI_LINE_RAW_STRING_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharLexer.MultiLineRawStringLiteral);

        QUOTE_OPEN = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.QUOTE_OPEN);

        TRIPLE_QUOTE_OPEN =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.TRIPLE_QUOTE_OPEN);

        SINGLE_QUOTE_CLOSE =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SINGLE_QUOTE_CLOSE);

        SINGLE_QUOTE_OPEN =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.SINGLE_QUOTE_OPEN);

        TRIPLE_SINGLE_QUOTE_OPEN =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                        .get(CharLexer.TRIPLE_SINGLE_QUOTE_OPEN);

        TRIPLE_SINGLE_QUOTE_CLOSE =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                        .get(CharLexer.TRIPLE_SINGLE_QUOTE_CLOSE);

        QUOTE_CLOSE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.QUOTE_CLOSE);

        LINE_STR_TEXT =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.LineStrText);

        ESCAPE_SEQ = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.EscapeSeq);

        TRIPLE_QUOTE_CLOSE =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.TRIPLE_QUOTE_CLOSE);

        MULTI_LINE_STR_TEXT =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MultiLineStrText);

        MULTI_LINE_STRING_QUOTE =
            PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.MultiLineStringQuote);

        MULTI_LINE_STRING_SINGLE_QUOTE = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
            .get(CharLexer.MultiLineStringSingleQuote);

        BYTE_STRING_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.ByteStringLiteral);

        CHARACTER_BYTE_LITERAL =
                PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharLexer.CharacterByteLiteral);

        VARRAY = PSIElementTypeFactory.getTokenIElementTypes(CangJieLanguage.INSTANCE).get(CharLexer.VARRAY);

        RULE_TRANSLATION_UNIT =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_translationUnit);

        RULE_IMPORT_LIST =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_importList);

        RULE_PREAMBLE =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_preamble);

        RULE_END = PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_end);

        RULE_IMPORT_ALL_OR_SPECIFIED =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_importAllOrSpecified);

        RULE_QUOTE_TOKEN =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_quoteToken);

        RULE_MACRO_TOKENS =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_macroTokens);

        RULE_LITERAL_CONSTANT =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_literalConstant);

        RULE_STRING_LITERAL =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE).get(CharParser.RULE_stringLiteral);

        RULE_LINE_STRING_LITERAL =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_lineStringLiteral);

        RULE_LINE_STRING_CONTENT =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_lineStringContent);

        RULE_CLASS_DEFINITION =
                PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_classDefinition);

        RULE_PACKAGE_HEADER =
            PSIElementTypeFactory.getRuleIElementTypes(CangJieLanguage.INSTANCE)
                .get(CharParser.RULE_packageHeader);
    }
}
