/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.highlightersetting;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;
import org.wso2.lsp4intellij.contributors.semantic.SemanticTokenHighlighter;

import java.util.EnumMap;
import java.util.Locale;

/**
 * define Cangjie SemanticToken Highlighter Color
 *
 * @author t30009182
 * @since 2021-03-26
 */
public class CangjieSemanticTokenHighlighter implements SemanticTokenHighlighter {
    /**
     * ESCAPE
     */
    public static final TextAttributesKey ESCAPE = TextAttributesKey.createTextAttributesKey(
            "CANGJIE_ESCAPE_SEQ", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
    );

    static final TextAttributesKey TYPE =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.TYPE.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.CLASS_NAME);
    static final TextAttributesKey CLASS =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.CLASS.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.CLASS_NAME);
    static final TextAttributesKey ENUM =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.ENUM.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.CLASS_NAME);
    static final TextAttributesKey INTERFACE =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.INTERFACE.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.CLASS_NAME);
    static final TextAttributesKey STRUCT =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.STRUCT.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.CLASS_NAME);
    static final TextAttributesKey TYPE_PARAMETER =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.TYPE_PARAMETER.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    static final TextAttributesKey PARAMETER =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.PARAMETER.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    static final TextAttributesKey VARIABLE =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.VARIABLE.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    static final TextAttributesKey PROPERTY =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.PROPERTY.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    static final TextAttributesKey ENUM_MEMBER =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.ENUM_MEMBER.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    static final TextAttributesKey EVENT =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.EVENT.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.STATIC_FIELD);
    static final TextAttributesKey FUNCTION =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.FUNCTION.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    static final TextAttributesKey METHOD =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.METHOD.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    static final TextAttributesKey MACRO =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.MACRO.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.STATIC_FIELD);
    static final TextAttributesKey KEYWORD =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.KEYWORD.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.KEYWORD);
    static final TextAttributesKey MODIFIER =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.MODIFIER.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.METADATA);
    static final TextAttributesKey COMMENT =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.COMMENT.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    static final TextAttributesKey STRING =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.STRING.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.STRING);
    static final TextAttributesKey NUMBER =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.NUMBER.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.NUMBER);
    static final TextAttributesKey REGEXP =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.REGEXP.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.STRING);
    static final TextAttributesKey OPERATOR =
            TextAttributesKey.createTextAttributesKey(
                    "SEMANTIC_" + SemanticToken.OPERATOR.name.toUpperCase(Locale.ROOT),
                    DefaultLanguageHighlighterColors.OPERATION_SIGN);

    private static final EnumMap<SemanticToken, TextAttributesKey> COLORS;

    static {
        COLORS = new EnumMap<>(SemanticToken.class);
        COLORS.put(SemanticToken.TYPE, TYPE);
        COLORS.put(SemanticToken.CLASS, CLASS);
        COLORS.put(SemanticToken.ENUM, ENUM);
        COLORS.put(SemanticToken.INTERFACE, CLASS);
        COLORS.put(SemanticToken.STRUCT, STRUCT);
        COLORS.put(SemanticToken.TYPE_PARAMETER, TYPE_PARAMETER);
        COLORS.put(SemanticToken.PARAMETER, PARAMETER);
        COLORS.put(SemanticToken.VARIABLE, VARIABLE);
        COLORS.put(SemanticToken.PROPERTY, PROPERTY);
        COLORS.put(SemanticToken.ENUM_MEMBER, ENUM_MEMBER);
        COLORS.put(SemanticToken.EVENT, EVENT);
        COLORS.put(SemanticToken.FUNCTION, FUNCTION);
        COLORS.put(SemanticToken.METHOD, METHOD);
        COLORS.put(SemanticToken.MACRO, EVENT);
        COLORS.put(SemanticToken.KEYWORD, KEYWORD);
        COLORS.put(SemanticToken.MODIFIER, MODIFIER);
        COLORS.put(SemanticToken.COMMENT, COMMENT);
        COLORS.put(SemanticToken.STRING, STRING);
        COLORS.put(SemanticToken.NUMBER, NUMBER);
        COLORS.put(SemanticToken.REGEXP, REGEXP);
        COLORS.put(SemanticToken.OPERATOR, OPERATOR);
    }

    /**
     * This method is used for exposing TextAttributesKeys to ColorSettingsPage, where the TextAttributesKeys are
     * accessed in a class-static context
     *
     * @param token SemanticToken
     * @return TextAttributesKey
     */
    public static TextAttributesKey colorOf(SemanticToken token) {
        return COLORS.get(token);
    }

    @Override
    public TextAttributesKey colorFor(SemanticToken token) {
        return COLORS.get(token);
    }
}
