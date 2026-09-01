/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.edit;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

import com.huawei.ideacj.language.psi.othersnode.CjLineStringContent;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.toplevel.CjNamedParameter;
import com.huawei.ideacj.language.psi.toplevel.CjType;
import com.huawei.ideacj.language.psi.toplevel.CjUnnamedParameter;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjAtomicType;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjSuperClass;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjUserType;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputDecl;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputType;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.syntaxhighlighter.CangjieSyntaxHighlighter;
import com.huawei.ace.utils.MarkdownProcessor;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Object used to process Hover responses.
 *
 * @since 2024-04-18
 */
public class CangjieHoverHandler {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Pattern SIGN_PATTERN = Pattern.compile("^\\(.*?\\)(.*)");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("(?s)```(?:cangjie|Cangjie)?\\s*\\n(.*?)\\n```");
    private static final CangjieSyntaxHighlighter HIGHLIGHTER = new CangjieSyntaxHighlighter();
    private static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("DEFAULT_IDENTIFIER", HighlighterColors.TEXT);

    /**
     * Returns the hover string corresponding to a Hover response.
     *
     * @param hover The Hover
     * @param project project
     * @return The string response
     */
    public static String getHoverString(@NonNull Hover hover, Project project) {
        if (hover == null || hover.getContents() == null || !hover.getContents().isRight()) {
            return "";
        }
        return genString(hover.getContents().getRight(), project);
    }

    private static String genString(MarkupContent content, Project project) {
        if (content == null || content.getValue() == null || content.getValue().isEmpty()) {
            return "";
        }
        String markdown = normalizeLineSeparator(content.getValue());
        Matcher matcher = CODE_FENCE_PATTERN.matcher(markdown);
        if (!matcher.find()) {
            return "<html>" + renderMarkdown(project, markdown) + "</html>";
        }

        String declareInfo = markdown.substring(0, matcher.start()).trim();
        String codeContent = matcher.group(1).trim();
        String comments = stripMarkdownSeparator(markdown.substring(matcher.end()).trim());

        StringBuilder result = new StringBuilder("<html>");
        if (!declareInfo.isEmpty()) {
            result.append(renderMarkdown(project, declareInfo));
        }
        result.append(renderCodeContent(codeContent, project));
        if (!comments.isEmpty()) {
            result.append(renderMarkdown(project, comments));
        }
        result.append("</html>");
        return result.toString();
    }

    private static String renderCodeContent(String codeContent, Project project) {
        String[] lines = codeContent.split("\n");
        int apiKeyLineIndex = IntStream.range(0, lines.length)
                .filter(i -> lines[i].startsWith("apiKey:"))
                .findFirst()
                .orElse(-1);
        if (apiKeyLineIndex < 0) {
            return highLight(codeContent, project);
        }

        int signatureLineIndex = Math.max(apiKeyLineIndex - 1, 0);
        String declareInfo = signatureLineIndex > 0
                ? String.join("\n", Arrays.copyOfRange(lines, 0, signatureLineIndex))
                : "";
        String signatureContent = lines[signatureLineIndex];
        Matcher matcher = SIGN_PATTERN.matcher(signatureContent);
        String codeContentWithoutKind = signatureContent;
        if (matcher.find() && matcher.group(1) != null) {
            codeContentWithoutKind = matcher.group(1).trim();
        }

        String renderedDeclareInfo = !declareInfo.isEmpty()
                ? "<pre>" + StringUtil.escapeXmlEntities(declareInfo) + "</pre>"
                : "";
        String highLightCodeContent = highLight(codeContentWithoutKind, project);
        String apiKeyLine = StringUtil.escapeXmlEntities(lines[apiKeyLineIndex]);
        String apiLevelInfo = lines.length > apiKeyLineIndex + 1
                ? String.join("\n", Arrays.copyOfRange(lines, apiKeyLineIndex + 1, lines.length))
                : "";
        String renderedApiLevelInfo = !apiLevelInfo.isEmpty()
                ? "<pre>" + StringUtil.escapeXmlEntities(apiLevelInfo) + "</pre><hr>"
                : "";
        return renderedDeclareInfo + highLightCodeContent + LINE_SEPARATOR + apiKeyLine + LINE_SEPARATOR
                + renderedApiLevelInfo;
    }

    private static String renderMarkdown(@NotNull Project project, String markdown) {
        // The bundled Markdown renderer treats a single line feed as a soft break and
        // collapses it to a space. Render each source line separately and add an explicit
        // HTML break so documentation comments retain their layout in the IntelliJ popup.
        String[] lines = markdown.split("\\n", -1);
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                html.append(MarkdownProcessor.toHtml(project, lines[i]));
            }
            if (i + 1 < lines.length) {
                html.append("<br>");
            }
        }
        return html.toString();
    }

    private static String normalizeLineSeparator(String text) {
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static String stripMarkdownSeparator(String text) {
        return text.replaceAll("(?m)^\\s*---\\s*$", "").trim();
    }

    private static String highLight(String codeContent, Project project) {
        return ReadAction.compute(() -> {
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getActiveVisibleScheme();
            if (scheme == null) {
                scheme = EditorColorsManager.getInstance().getGlobalScheme();
            }

            PsiFile psiFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("signature.cj", CangJieLanguage.INSTANCE, codeContent);

            StringBuilder html = new StringBuilder();
            traverseAST(psiFile.getNode(), html, scheme);
            return wrapInPre(scheme, html.toString());
        });
    }

    private static void traverseAST(ASTNode node, StringBuilder html, EditorColorsScheme scheme) {
        ASTNode child = node.getFirstChildNode();
        if (child == null) {
            renderNode(node, html, scheme);
            return;
        }

        while (child != null) {
            traverseAST(child, html, scheme);
            child = child.getTreeNext();
        }
    }

    private static void renderNode(ASTNode node, StringBuilder html, EditorColorsScheme scheme) {
        String text = node.getText();
        if (StringUtils.isEmpty(text)) {
            return;
        }

        PsiElement psi = node.getPsi();
        if (psi instanceof PsiWhiteSpace) {
            html.append(StringUtil.escapeXmlEntities(text));
            return;
        }

        if (!(psi instanceof LeafPsiElement)) {
            return;
        }

        TextAttributesKey key = getSemanticKey(psi);
        TextAttributes attrs = (key != null) ? scheme.getAttributes(key) : null;
        String style = buildStyle(attrs);
        String escapedText = StringUtil.escapeXmlEntities(text);
        if (style.isEmpty()) {
            html.append(escapedText);
        } else {
            html.append("<span style=\"").append(style).append("\">").append(escapedText).append("</span>");
        }
    }

    private static TextAttributesKey getSemanticKey(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        TextAttributesKey[] keys = HIGHLIGHTER.getTokenHighlights(type);

        if (keys.length > 0 && keys[0] != IDENTIFIER) {
            return keys[0];
        }

        PsiElement parent = element.getParent();
        while (parent != null) {
            TextAttributesKey key = switch (parent) {
                case CjLineStringContent p -> p.getColor();
                case CjNamedParameter p -> p.getColor();
                case CjUnnamedParameter p -> p.getColor();
                case CjAtomicType p -> p.getColor();
                case CjUserType p -> p.getColor();
                case CjMacroInputType p -> p.getColor();
                case CjSuperClass p -> p.getColor();
                case CjClassDefinition p -> p.getColor();
                case CjInterfaceDefinition p -> p.getColor();
                case CjStructDefinition p -> p.getColor();
                case CjEnumDefinition p -> p.getColor();
                case CjType p -> p.getColor();
                case CjFunctionDefinition p -> p.getColor();
                case CjMacroDefinition p -> p.getColor();
                case CjPostfixExpression p -> p.getColor();
                case CjVariableDeclaration p -> p.getColor();
                case CjMacroInputDecl p -> p.getColor();
                default -> null;
            };

            if (key != null) {
                return key;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static String buildStyle(TextAttributes attrs) {
        if (attrs == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        Color fg = attrs.getForegroundColor();
        if (fg != null) {
            sb.append(String.format(Locale.ROOT, "color:#%02x%02x%02x;", fg.getRed(), fg.getGreen(), fg.getBlue()));
        }
        return sb.toString();
    }

    private static String wrapInPre(EditorColorsScheme scheme, String content) {
        Color bgColor = scheme.getColor(EditorColors.GUTTER_BACKGROUND);
        if (bgColor == null) {
            bgColor = JBColor.border();
        }
        String bgHex = ColorUtil.toHtmlColor(bgColor);

        return String.format(Locale.ROOT,
                "<div class='code-block' style='background-color:%s; padding:10px;'>"
                        + "<pre style=\"font-family:'%s'; font-size:%dpt; margin:0;\">%s</pre>"
                        + "</div><hr/>",
                bgHex, scheme.getEditorFontName(), scheme.getEditorFontSize(), content);
    }
}
