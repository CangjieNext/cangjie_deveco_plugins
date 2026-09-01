/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.highlightersetting;

import static com.huawei.ideacj.highlightersetting.CangjieSemanticTokenHighlighter.colorOf;

import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.syntaxhighlighter.CangjieSyntaxHighlighter;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.semantic.SemanticToken;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

/**
 * Set a color setting page
 *
 * @author t30009182
 * @since 2021-03-26
 */
public class CangjieColorSettingsPage implements ColorSettingsPage {
    private static final Map<String, TextAttributesKey> MYMAP = new HashMap<String, TextAttributesKey>() {
        {
            put("STRING", colorOf(SemanticToken.STRING));
            put("NUMBER", colorOf(SemanticToken.NUMBER));
            put("KEYWORD", colorOf(SemanticToken.KEYWORD));
            put("FUNCTION", colorOf(SemanticToken.FUNCTION));
            put("VARIABLE", colorOf(SemanticToken.VARIABLE));
            put("COMMENTS", colorOf(SemanticToken.COMMENT));
            put("EVENT", colorOf(SemanticToken.EVENT));
            put("CLASS", colorOf(SemanticToken.CLASS));
            put("ESCAPE", CangjieSemanticTokenHighlighter.ESCAPE);
            put("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
            put("BRACES", DefaultLanguageHighlighterColors.BRACES);
            put("DOT", DefaultLanguageHighlighterColors.DOT);
            put("SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
            put("COMMA", DefaultLanguageHighlighterColors.COMMA);
            put("BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
            put("PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
        }
    };

    private static final AttributesDescriptor[] DESCRIPTORS =
            new AttributesDescriptor[]{
                    new AttributesDescriptor("Package", colorOf(SemanticToken.EVENT)),
                    new AttributesDescriptor("Class\\Enum\\Type\\Struct\\Interface", colorOf(SemanticToken.CLASS)),
                    new AttributesDescriptor("Variable", colorOf(SemanticToken.VARIABLE)),
                    new AttributesDescriptor("Function", colorOf(SemanticToken.FUNCTION)),
                    new AttributesDescriptor("Keyword", colorOf(SemanticToken.KEYWORD)),
                    new AttributesDescriptor("String", colorOf(SemanticToken.STRING)),
                    new AttributesDescriptor("Number", colorOf(SemanticToken.NUMBER)),
                    new AttributesDescriptor("Comment", colorOf(SemanticToken.COMMENT)),
                    new AttributesDescriptor("Escape sequence", CangjieSemanticTokenHighlighter.ESCAPE),
                    new AttributesDescriptor("Braces and Operator//Braces",
                            DefaultLanguageHighlighterColors.BRACES),
                    new AttributesDescriptor("Braces and Operator//Dot",
                            DefaultLanguageHighlighterColors.DOT),
                    new AttributesDescriptor("Braces and Operator//SemiColon",
                            DefaultLanguageHighlighterColors.SEMICOLON),
                    new AttributesDescriptor("Braces and Operator//Comma",
                            DefaultLanguageHighlighterColors.COMMA),
                    new AttributesDescriptor("Braces and Operator//Brackets",
                            DefaultLanguageHighlighterColors.BRACKETS),
                    new AttributesDescriptor("Braces and Operator//Parentheses",
                            DefaultLanguageHighlighterColors.PARENTHESES),
                    new AttributesDescriptor("Braces and Operator//Operator",
                            DefaultLanguageHighlighterColors.OPERATION_SIGN)
            };

    private static final String DEMO_TEXT = "<KEYWORD>package</KEYWORD> <EVENT>test.pkg</EVENT>"
            + LspConfigUtils.SEPARATOR
            + "<KEYWORD>import</KEYWORD> <EVENT>pkgA.classA</EVENT>"
            + LspConfigUtils.SEPARATOR + LspConfigUtils.SEPARATOR
            + "<KEYWORD>class</KEYWORD> <CLASS>MyClass</CLASS> <BRACES>{}</BRACES>" + LspConfigUtils.SEPARATOR
            + "<KEYWORD>func</KEYWORD> <FUNCTION>multiValues</FUNCTION>"
            + "<PARENTHESES>(</PARENTHESES><VARIABLE>a</VARIABLE><OPERATOR>:</OPERATOR> <KEYWORD>Int32</KEYWORD>"
            + "<COMMA>,</COMMA> <VARIABLE>b</VARIABLE><OPERATOR>:</OPERATOR> <KEYWORD>Int32</KEYWORD>"
            + "<PARENTHESES>)</PARENTHESES><OPERATOR>:</OPERATOR> <PARENTHESES>(</PARENTHESES>"
            + "<KEYWORD>Int32</KEYWORD><COMMA>,</COMMA> <KEYWORD>Int32</KEYWORD>"
            + "<PARENTHESES>)</PARENTHESES> <BRACES>{</BRACES>" + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>return</KEYWORD> <PARENTHESES>(</PARENTHESES><VARIABLE>a</VARIABLE> "
            + "<OPERATOR>+</OPERATOR> <VARIABLE>b</VARIABLE><COMMA>,</COMMA> <VARIABLE>a</VARIABLE> "
            + "<OPERATOR>-</OPERATOR> <VARIABLE>b</VARIABLE><PARENTHESES>)</PARENTHESES><SEMICOLON>;</SEMICOLON>"
            + LspConfigUtils.SEPARATOR
            + "<BRACES>}</BRACES>" + LspConfigUtils.SEPARATOR
            + "<KEYWORD>main</KEYWORD><PARENTHESES>()</PARENTHESES> <BRACES>{</BRACES>" + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>var</KEYWORD> <PARENTHESES>(</PARENTHESES>"
            + "<VARIABLE>x</VARIABLE><COMMA>,</COMMA> <VARIABLE>y</VARIABLE><PARENTHESES>)</PARENTHESES> "
            + "<OPERATOR>=</OPERATOR> <FUNCTION>multiValues</FUNCTION><PARENTHESES>(</PARENTHESES><NUMBER>8</NUMBER>"
            + "<COMMA>,</COMMA> <NUMBER>24</NUMBER><PARENTHESES>)</PARENTHESES>" + LspConfigUtils.SEPARATOR
            + "    <FUNCTION>print</FUNCTION><PARENTHESES>(</PARENTHESES>"
            + "<STRING>\"x=</STRING><BRACES>${</BRACES><VARIABLE>x</VARIABLE><BRACES>}</BRACES><ESCAPE>\\n</ESCAPE>"
            + "<STRING>\"</STRING> <PARENTHESES>)</PARENTHESES> <COMMENTS>// output: 32</COMMENTS>"
            + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>let</KEYWORD> <VARIABLE>array9</VARIABLE> <OPERATOR>=</OPERATOR>"
            + " <BRACKETS>[</BRACKETS><NUMBER>0</NUMBER><BRACKETS>]</BRACKETS>" + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>let</KEYWORD> <VARIABLE>size1</VARIABLE> <OPERATOR>=</OPERATOR>"
            + " <VARIABLE>array9</VARIABLE><DOT>.</DOT><VARIABLE>size</VARIABLE>"
            + " <COMMENTS>// size1 = 1</COMMENTS>" + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>let</KEYWORD> <VARIABLE>map</VARIABLE> <OPERATOR>=</OPERATOR> <CLASS>HashMap</CLASS>"
            + "<OPERATOR><</OPERATOR><CLASS>String</CLASS><COMMA>,</COMMA> <KEYWORD>Int32</KEYWORD>"
            + "<OPERATOR>></OPERATOR><PARENTHESES>()</PARENTHESES>" + LspConfigUtils.SEPARATOR
            + "    <KEYWORD>return</KEYWORD> <NUMBER>0</NUMBER>" + LspConfigUtils.SEPARATOR
            + "<BRACES>}</BRACES>" + LspConfigUtils.SEPARATOR;

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return new CangjieSyntaxHighlighter();
    }

    @Override
    @NotNull
    public String getDemoText() {
        return DEMO_TEXT;
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return MYMAP;
    }

    @Override
    @NotNull
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    @NotNull
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Cangjie";
    }
}