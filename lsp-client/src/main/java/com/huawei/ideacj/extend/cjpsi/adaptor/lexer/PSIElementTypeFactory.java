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
import com.intellij.psi.tree.TokenSet;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PSIElementTypeFactory
 *
 * @since 2024/03/20
 */
public class PSIElementTypeFactory {
    private static final Map<Language, List<TokenElementType>> tokenEleTypeCache = new HashMap<>();
    private static final Map<Language, List<RuleIElementType>> ruleEleTypeCache = new HashMap<>();
    private static final Map<Language, Map<String, Integer>> tokenNames = new HashMap<>();
    private static final Map<Language, Map<String, Integer>> ruleNames = new HashMap<>();
    private static final Map<Language, TokenElementType> eofEleTypeCache = new HashMap<>();

    /**
     * Define language i element types.
     *
     * @param language   the language
     * @param tokenNames the token names
     * @param ruleNames  the rule names
     */
    public static void defineLanguageIElementTypes(Language language, String[] tokenNames, String[] ruleNames) {
        synchronized (PSIElementTypeFactory.class) {
            if (tokenEleTypeCache.get(language) == null) {
                List<TokenElementType> types = tokenEleTypeCache.get(language);
                if (types == null) {
                    types = createTokenIElementTypes(language, tokenNames);
                    tokenEleTypeCache.put(language, types);
                }
            }
            if (ruleEleTypeCache.get(language) == null) {
                List<RuleIElementType> result = ruleEleTypeCache.get(language);
                if (result == null) {
                    result = createRuleIElementTypes(language, ruleNames);
                    ruleEleTypeCache.put(language, result);
                }
            }
            if (PSIElementTypeFactory.tokenNames.get(language) == null) {
                PSIElementTypeFactory.tokenNames.put(language, createTokenTypeMap(tokenNames));
            }
            if (PSIElementTypeFactory.ruleNames.get(language) == null) {
                PSIElementTypeFactory.ruleNames.put(language, createRuleIndexMap(ruleNames));
            }
        }
    }

    /**
     * Gets eof element type.
     *
     * @param language the language
     * @return the eof element type
     */
    public static TokenElementType getEofElementType(Language language) {
        TokenElementType result = eofEleTypeCache.get(language);
        if (result == null) {
            result = new TokenElementType(Token.EOF, "EOF", language);
            eofEleTypeCache.put(language, result);
        }

        return result;
    }

    /**
     * Gets token i element types.
     *
     * @param language the language
     * @return the token i element types
     */
    public static List<TokenElementType> getTokenIElementTypes(Language language) {
        return tokenEleTypeCache.get(language);
    }

    /**
     * Gets rule i element types.
     *
     * @param language the language
     * @return the rule i element types
     */
    public static List<RuleIElementType> getRuleIElementTypes(Language language) {
        return ruleEleTypeCache.get(language);
    }

    /**
     * Gets rule name to index map.
     *
     * @param language the language
     * @return the rule name to index map
     */
    public static Map<String, Integer> getRuleNameToIndexMap(Language language) {
        return ruleNames.get(language);
    }

    /**
     * Gets token name to type map.
     *
     * @param language the language
     * @return the token name to type map
     */
    public static Map<String, Integer> getTokenNameToTypeMap(Language language) {
        return tokenNames.get(language);
    }

    /**
     * Create token type map map.
     *
     * @param tokenNames the token names
     * @return the map
     */
    public static Map<String, Integer> createTokenTypeMap(String[] tokenNames) {
        return Utils.toMap(tokenNames);
    }

    /**
     * Create rule index map map.
     *
     * @param ruleNames the rule names
     * @return the map
     */
    public static Map<String, Integer> createRuleIndexMap(String[] ruleNames) {
        return Utils.toMap(ruleNames);
    }

    /**
     * Create token i element types list.
     *
     * @param language   the language
     * @param names the token names
     * @return the list
     */
    @NotNull
    public static List<TokenElementType> createTokenIElementTypes(Language language, String[] names) {
        List<TokenElementType> result;
        TokenElementType[] elementTypes = new TokenElementType[names.length];
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }
            elementTypes[i] = new TokenElementType(i, names[i], language);
        }

        result = Collections.unmodifiableList(Arrays.asList(elementTypes));
        return result;
    }

    /**
     * Create rule i element types list.
     *
     * @param language  the language
     * @param names the rule names
     * @return the list
     */
    @NotNull
    public static List<RuleIElementType> createRuleIElementTypes(Language language, String[] names) {
        List<RuleIElementType> result;
        RuleIElementType[] types = new RuleIElementType[names.length];
        for (int i = 0; i < names.length; i++) {
            types[i] = new RuleIElementType(i, names[i], language);
        }

        result = Collections.unmodifiableList(Arrays.asList(types));
        return result;
    }

    /**
     * Create token set token set.
     *
     * @param language the language
     * @param types    the types
     * @return the token set
     */
    public static TokenSet createTokenSet(Language language, int... types) {
        List<TokenElementType> tokenElementTypes = getTokenIElementTypes(language);

        IElementType[] elementTypes = new IElementType[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Token.EOF) {
                elementTypes[i] = getEofElementType(language);
                continue;
            }
            if (tokenElementTypes != null) {
                elementTypes[i] = tokenElementTypes.get(types[i]);
            }
        }

        return TokenSet.create(elementTypes);
    }
}
