/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;
import static com.huawei.ideacj.language.CangJieTypes.RULE_MACRO_TOKENS;
import static com.huawei.ideacj.language.CangJieTypes.RULE_QUOTE_TOKEN;
import static com.intellij.patterns.PlatformPatterns.psiFile;
import static com.intellij.patterns.StandardPatterns.instanceOf;

import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.utils.ResourceUtils;
import com.huawei.ideacj.filetypes.CangjieCodeFile;
import com.huawei.ideacj.language.psi.othersnode.CjLiteralConstant;
import com.huawei.ideacj.language.psi.othersnode.CjQuoteToken;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjieResourceUtil
 *
 * @since 2024/10/10
 */
public class CangjieResourceUtil {
    /**
     * APP
     */
    public static final String APP = "app";

    /**
     * PREFIX_APP
     */
    public static final String PREFIX_APP = "app.";

    /**
     * SYS
     */
    public static final String SYS = "sys";

    /**
     * PREFIX_SYS
     */
    public static final String PREFIX_SYS = "sys.";

    /**
     * RESOURCE_SEPARATOR
     */
    public static final String RESOURCE_SEPARATOR = ".";

    /**
     * RESOURCE_SEPARATOR_REG_REGULAR
     */
    public static final String RESOURCE_SEPARATOR_REG_REGULAR = "\\.";

    /**
     * RESOURCE_MACRO_EXP_DEEP
     */
    public static final int RESOURCE_MACRO_EXP_DEEP = 4;

    /**
     * RAW_FILE_MACRO_EXP_DEEP
     */
    public static final int RAW_FILE_MACRO_EXP_DEEP = 2;

    /**
     * MACRO_TOKENS_DEEP
     */
    public static final int MACRO_TOKENS_DEEP = 2;

    /**
     * RESOURCE_SIZE
     */
    public static final int RESOURCE_SIZE = 3;

    /**
     * RESOURCE_DECL_PREFIX_COUNT
     */
    public static final int RESOURCE_DECL_PREFIX_COUNT = 4;

    /**
     * RESOURCE_NAME_INDEX
     */
    public static final int RESOURCE_NAME_TOKEN_INDEX = 4;

    /**
     * RAW_FILE
     */
    public static final String RAW_FILE = "rawfile";

    /**
     * COMMA
     */
    public static final String COMMA = ",";

    /**
     * CANGJIE_FILE_PATTERN
     */
    public static final ElementPattern<PsiFile> CANGJIE_FILE_PATTERN = psiFile().withFileType(
            instanceOf(CangjieCodeFile.class)).with(new PatternCondition<>("cangjie resource reference") {
        @Override
        public boolean accepts(@NotNull PsiFile psiFile, ProcessingContext context) {
            OhosModuleModel module = getSelectFileOhosModuleModel(psiFile.getProject(),
                    psiFile.getOriginalFile().getVirtualFile());
            return !ResourceUtils.isInOhosTestPsiElement(psiFile) && LspConfigUtils.isCangjieModule(module);
        }
    });

    /**
     * RESOURCE_PATTERN, Use in Completion, Goto Definition
     */
    public static final PsiElementPattern.Capture<PsiElement> RESOURCE_COMPLETION_PATTERN =
            PlatformPatterns.psiElement()
                    .inFile(CANGJIE_FILE_PATTERN)
                    .withParent(CjQuoteToken.class);

    /**
     * QUOTE_TOKEN_PATTERN
     */
    public static final PsiElementPattern.Capture<PsiElement> QUOTE_TOKEN_PATTERN =
            PlatformPatterns.psiElement()
                    .inFile(CANGJIE_FILE_PATTERN)
                    .withElementType(RULE_QUOTE_TOKEN);

    /**
     * MACRO_TOKENS_RESOURCE_PATTERN, Use in Reference
     */
    public static final PsiElementPattern.Capture<PsiElement> MACRO_TOKENS_RESOURCE_REFERENCE_PATTERN =
            PlatformPatterns.psiElement()
                    .inFile(CANGJIE_FILE_PATTERN)
                    .withElementType(RULE_MACRO_TOKENS)
                    .withChild(QUOTE_TOKEN_PATTERN);

    /**
     * match ../ ./ /
     */
    public static final Pattern PATH_REGEX = Pattern.compile("^[./].*");

    private static final Pattern RESOURCE_INPUT_PATTERN = Pattern.compile("@r\\((.*?)\\)");

    private static final Pattern RAWFILE_INPUT_PATTERN = Pattern.compile("@rawfile\\((.*?)\\)");

    private static final String DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";

    /**
     * getResourceInput
     *
     * @param element target element
     * @param isRawFile isRawFile
     * @return resource text
     */
    public static String getResourceInput(PsiElement element, boolean isRawFile) {
        PsiElement marcoExp = element;
        // number is supported, its psi is deeper than identifier
        if (element.getParent() instanceof CjLiteralConstant) {
            marcoExp = element.getParent();
        }
        int searchDeep = isRawFile ? RAW_FILE_MACRO_EXP_DEEP : RESOURCE_MACRO_EXP_DEEP;
        for (int i = 0; i < searchDeep; i++) {
            if (marcoExp == null) {
                return StringUtils.EMPTY;
            }
            marcoExp = marcoExp.getParent();
        }
        if (!(marcoExp instanceof CjMacroExpression)) {
            return StringUtils.EMPTY;
        }

        String beginStr = isRawFile ? "@rawfile" : "@r";
        if (StringUtils.isEmpty(marcoExp.getText()) || !marcoExp.getText().startsWith(beginStr)) {
            return StringUtils.EMPTY;
        }
        Pattern pattern = isRawFile ? RAWFILE_INPUT_PATTERN : RESOURCE_INPUT_PATTERN;
        Matcher matcher = pattern.matcher(marcoExp.getText());
        String input = matcher.find() ? matcher.group(1) : StringUtils.EMPTY;
        // comma as separator
        if (input.contains(COMMA)) {
            input = input.substring(0, input.indexOf(COMMA));
        }
        // "IntellijIdeaRulezzz" will be used as a prefix when complete
        // normal resource remove prefix, rawfile reference completion need this prefix
        int matchIndex = input.contains(DUMMY_IDENTIFIER) && !isRawFile
                ? input.indexOf(DUMMY_IDENTIFIER) : input.length();
        if (isRawFile && !StringUtil.isQuotedString(input)) {
            return StringUtils.EMPTY;
        }
        return StringUtil.unquoteString(input.substring(0, matchIndex));
    }

    /**
     * getWholeResourceDecl
     *
     * @param element quote terminal node
     * @return macro tokens, the whole resource node
     */
    public static PsiElement getWholeResourceDecl(PsiElement element) {
        PsiElement marcoExp = element;
        for (int i = 0; i < MACRO_TOKENS_DEEP; i++) {
            if (marcoExp == null) {
                return element;
            }
            marcoExp = marcoExp.getParent();
        }
        return marcoExp != null ? marcoExp : element;
    }

    /**
     * check is resource decl
     *
     * @param cjElement CjMacroTokens
     * @param isRawFile isRawFile
     * @return is resource decl
     */
    public static boolean isResourceDecl(CjMacroTokens cjElement, boolean isRawFile) {
        if (cjElement == null || cjElement.getParent() == null || cjElement.getParent().getParent() == null) {
            return false;
        }
        PsiElement macroExpr = cjElement.getParent().getParent();
        if (!(macroExpr instanceof CjMacroExpression)) {
            return false;
        }
        Pattern pattern = isRawFile ? RAWFILE_INPUT_PATTERN : RESOURCE_INPUT_PATTERN;
        return pattern.matcher(macroExpr.getText()).matches();
    }

    /**
     * getRangeInElement
     *
     * @param element marcoTokens element
     * @return element range
     */
    public static TextRange getRangeInElement(@NotNull PsiElement element) {
        int offset = 0;
        PsiElement[] elements = element.getChildren();
        if (elements.length < RESOURCE_NAME_TOKEN_INDEX + 1) {
            return TextRange.from(offset, element.getTextLength());
        }
        for (int i = 0; i < RESOURCE_DECL_PREFIX_COUNT; i++) {
            offset += elements[i].getTextLength();
        }
        return TextRange.from(offset, elements[RESOURCE_NAME_TOKEN_INDEX].getTextLength());
    }

    /**
     * createSourceEmptyErrorInfo
     *
     * @return SourceEmptyErrorInfo
     */
    public static String createSourceEmptyErrorInfo() {
        return "Resource source cannot be empty";
    }

    /**
     * createMissingSourceErrorInfo
     *
     * @return MissingSourceErrorInfo
     */
    public static String createMissingSourceErrorInfo() {
        return "Missing resource source";
    }

    /**
     * createMissingTypeErrorInfo
     *
     * @return MissingTypeErrorInfo
     */
    public static String createMissingTypeErrorInfo() {
        return "Missing resource type";
    }

    /**
     * createUnknownSourceErrorInfo
     *
     * @param source input source
     * @return UnknownSourceErrorInfo
     */
    public static String createUnknownSourceErrorInfo(String source) {
        return "Unknown resource source '" + source + "'";
    }

    public static String createUnSupportedSource() {
        return "The resource type 'profile' is not supported.";
    }

    /**
     * createUnknownTypeErrorInfo
     *
     * @param type input type
     * @return UnknownTypeErrorInfo
     */
    public static String createUnknownTypeErrorInfo(String type) {
        return "Unknown resource type '" + type + "'";
    }

    /**
     * createMissingNameErrorInfo
     *
     * @return MissingNameErrorInfo
     */
    public static String createMissingNameErrorInfo() {
        return "Missing resource name";
    }

    /**
     * createUnknownNameErrorInfo
     *
     * @param name input name
     * @return UnknownNameErrorInfo
     */
    public static String createUnknownNameErrorInfo(String name) {
        return "Unknown resource name '" + name + "'";
    }

    /**
     * createInvalidNameErrorInfo
     *
     * @return InvalidNameErrorInfo
     */
    public static String createInvalidNameErrorInfo() {
        return "'.' is not a valid resource name character";
    }

    /**
     * createRawFileUnsupportedErrorInfo
     *
     * @return RawFileUnsupportedErrorInfo
     */
    public static String createRawFileUnsupportedErrorInfo() {
        return "Only resources in the rawfile folder can be referenced";
    }

    /**
     * createRawFileInvalidErrorInfo
     *
     * @return RawFileInvalidErrorInfo
     */
    public static String createRawFileInvalidErrorInfo() {
        return "No such rawfile resource";
    }
}
