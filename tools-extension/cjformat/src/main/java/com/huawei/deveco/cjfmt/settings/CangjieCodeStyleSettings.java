/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.lang.Language;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * class CangjieCodeStyleSettings
 *
 * @since 2024 -11-7
 */
public class CangjieCodeStyleSettings extends CustomCodeStyleSettings {
    /**
     * The constant INDENT_WIDTH_DEFAULT.
     */
    public static final int INDENT_WIDTH_DEFAULT = 4;

    /**
     * The constant LINE_LIMIT_LENGTH_DEFAULT.
     */
    public static final int LINE_LIMIT_LENGTH_DEFAULT = 120;

    /**
     * The constant MULTIPLE_LINE_METHOD_CHAIN_LEVEL_DEFAULT.
     */
    public static final int MULTIPLE_LINE_METHOD_CHAIN_LEVEL_DEFAULT = 2;

    /**
     * INDENT_WIDTH
     */
    public int indentWidth = INDENT_WIDTH_DEFAULT;

    /**
     * LINE_LIMIT_LENGTH
     */
    public int lineLimitLength = LINE_LIMIT_LENGTH_DEFAULT;

    /**
     * LINE_BREAK_TYPE
     */
    public LineBreakTypeStyle lineBreakType = LineBreakTypeStyle.LF;

    /**
     * ALLOW_MULTI_LINE_METHOD_CHAIN
     */
    public boolean allowMultiLineMethodChain = true;

    /**
     * MULTIPLE_LINE_METHOD_CHAIN_LEVEL
     */
    public int methodChainLevel = MULTIPLE_LINE_METHOD_CHAIN_LEVEL_DEFAULT;

    /**
     * MULTIPLE_LINE_METHOD_CHAIN_OVER_LINE_LENGTH
     */
    public boolean methodChainOverLength = true;

    /**
     * Persisted mirror of the commenter option LINE_COMMENT_AT_FIRST_COLUMN.
     * Private on purpose (G.OBJ.01): the value is serialized manually in
     * {@link #readExternal}/{@link #writeExternal} because the platform
     * reflection-based serializer only sees public fields.
     */
    private boolean lineCommentAtFirstColumn = false;

    /**
     * Persisted mirror of the commenter option LINE_COMMENT_ADD_SPACE, see
     * {@link #lineCommentAtFirstColumn}.
     */
    private boolean lineCommentAddSpace = false;

    /**
     * Instantiates a new Cangjie code style settings.
     *
     * @param settings the settings
     */
    public CangjieCodeStyleSettings(CodeStyleSettings settings) {
        super("CangjieCodeStyleSettings", settings);
    }

    public boolean isLineCommentAtFirstColumn() {
        return lineCommentAtFirstColumn;
    }

    public void setLineCommentAtFirstColumn(boolean value) {
        lineCommentAtFirstColumn = value;
    }

    public boolean isLineCommentAddSpace() {
        return lineCommentAddSpace;
    }

    public void setLineCommentAddSpace(boolean value) {
        lineCommentAddSpace = value;
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        // The platform passes the scheme ROOT element here; the platform base
        // implementation locates this language's own node by myTagName, and so
        // must the manual reader — the two commenter options live inside the
        // <CangjieCodeStyleSettings> child node, not among the root options.
        Element node = element.getChild(getTagName());
        if (node == null) {
            return;
        }
        // Private fields are invisible to the reflection-based serializer:
        // read the two commenter options manually. Do NOT mirror them into the
        // common settings here: the per-language common settings map is rebuilt
        // afterwards and would wipe the mirrored values — see afterLoaded().
        for (Element option : node.getChildren("option")) {
            String name = option.getAttributeValue("name");
            if ("lineCommentAtFirstColumn".equals(name)) {
                lineCommentAtFirstColumn = Boolean.parseBoolean(option.getAttributeValue("value"));
            }
            if ("lineCommentAddSpace".equals(name)) {
                lineCommentAddSpace = Boolean.parseBoolean(option.getAttributeValue("value"));
            }
        }
    }

    @Override
    protected void afterLoaded() {
        super.afterLoaded();
        // Called by the platform after the whole CodeStyleSettings.readExternal
        // finishes — the per-language common settings map has been rebuilt by
        // then, so mirroring here survives the re-initialization.
        CommonCodeStyleSettings common = getContainer().getCommonSettings(CangJieLanguage.INSTANCE);
        common.LINE_COMMENT_AT_FIRST_COLUMN = lineCommentAtFirstColumn;
        common.LINE_COMMENT_ADD_SPACE = lineCommentAddSpace;
    }

    @Override
    public void writeExternal(@NotNull Element element, @NotNull CustomCodeStyleSettings defaultSettings)
        throws WriteExternalException {
        super.writeExternal(element, defaultSettings);
        // Private fields are invisible to the reflection-based serializer:
        // append the two commenter options manually into this language's node.
        Element node = element.getChild(getTagName());
        if (node == null) {
            node = new Element(getTagName());
            element.addContent(node);
        }
        addBooleanOption(node, "lineCommentAtFirstColumn", lineCommentAtFirstColumn);
        addBooleanOption(node, "lineCommentAddSpace", lineCommentAddSpace);
    }

    private static void addBooleanOption(Element element, String name, boolean value) {
        Element option = new Element("option");
        option.setAttribute("name", name);
        option.setAttribute("value", Boolean.toString(value));
        element.addContent(option);
    }

    /**
     * Copy settings.
     *
     * @param lang the lang
     * @param rootSettings the root settings
     * @param targetSettings the target settings
     */
    public static void copySettings(Language lang, CodeStyleSettings rootSettings, CodeStyleSettings targetSettings) {
        CommonCodeStyleSettings sourceCommonSettings = rootSettings.getCommonSettings(lang);
        CangjieCodeStyleSettings targetCommonSettings =
            targetSettings.getCustomSettings(CangjieCodeStyleSettings.class);
        if (sourceCommonSettings.getIndentOptions() != null) {
            if (CangjieCodeStyleSettings.validateIndentWidth(sourceCommonSettings.getIndentOptions().INDENT_SIZE)) {
                targetCommonSettings.indentWidth = sourceCommonSettings.getIndentOptions().INDENT_SIZE;
            }
        }
        if (CangjieCodeStyleSettings.validateLineLimitLength(sourceCommonSettings.RIGHT_MARGIN)) {
            targetCommonSettings.lineLimitLength = sourceCommonSettings.RIGHT_MARGIN;
        }
    }

    /**
     * Validate indent width boolean.
     *
     * @param indentWidthInput the indent width input
     * @return the boolean
     */
    public static boolean validateIndentWidth(int indentWidthInput) {
        return indentWidthInput >= 0 && indentWidthInput <= 8;
    }

    /**
     * Validate line limit length boolean.
     *
     * @param lineLimitLengthInput the line limit length input
     * @return the boolean
     */
    public static boolean validateLineLimitLength(int lineLimitLengthInput) {
        return lineLimitLengthInput >= 1 && lineLimitLengthInput <= 120;
    }

    /**
     * Validate chain level boolean.
     *
     * @param chainLevelInput the chain level input
     * @return the boolean
     */
    public static boolean validateChainLevel(int chainLevelInput) {
        return chainLevelInput >= 2 && chainLevelInput <= 10;
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indentWidth, lineLimitLength, lineBreakType, allowMultiLineMethodChain, methodChainLevel,
            methodChainOverLength);
    }

    /**
     * Write config to file.
     *
     * @param settings the settings
     * @param cjfmtTomlPath the cjfmt toml path
     * @throws IOException the io exception
     */
    public static void writeConfigToFile(CangjieCodeStyleSettings settings, Path cjfmtTomlPath) throws IOException {
        if (cjfmtTomlPath == null) {
            return;
        }
        List<String> configList = new ArrayList<>();
        configList.add("indentWidth = " + settings.indentWidth);
        configList.add("linelimitLength = " + settings.lineLimitLength);
        configList.add("lineBreakType = \"" + settings.lineBreakType.getBreakType() + "\"");
        configList.add("allowMultiLineMethodChain = " + settings.allowMultiLineMethodChain);
        configList.add("multipleLineMethodChainLevel = " + settings.methodChainLevel);
        configList.add("multipleLineMethodChainOverLineLength = " + settings.methodChainOverLength);
        if (cjfmtTomlPath.getParent() != null) {
            Files.createDirectories(cjfmtTomlPath.getParent());
        }
        Files.write(cjfmtTomlPath, configList);
    }
}
