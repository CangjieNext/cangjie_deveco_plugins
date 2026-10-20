/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.capabilities.lsp;

import com.huawei.capabilities.apidocs.ApiDocsManager;
import com.huawei.capabilities.apidocs.ApiDocsToolWindow;

import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.content.Content;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.Objects;
import java.util.Set;

/**
 * CangjieDocumentationProvider
 * quick documentation when hover on
 * need to override generateDoc generateHoverDoc getCustomDocumentationElement
 *
 * @since 2024-01-17
 */
public class CangjieDocumentationProvider extends DocumentationProviderEx {
    private static final String LINE_SEPARATOR = "\n";

    private final Set<String> apiPackages =
            Set.of("std", "ohos", "compress", "crypto", "encoding", "fuzz", "log", "net", "logger", "serialization");

    /**
     * generate hover documentation
     *
     * @param element element
     * @param originalElement original element
     * @return document result
     */
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        String hoverResult = generateHoverDoc(element, originalElement);
        return StringUtil.isEmpty(hoverResult) ? null : hoverResult;
    }

    /**
     * get hover documentation
     *
     * @param element         psi element
     * @param originalElement original element
     * @return document result
     */
    @Override
    public String generateHoverDoc(@NotNull PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof PsiComment) {
            return null;
        }
        Editor editor = FileUtils.editorFromPsiFile(element.getContainingFile());
        if (editor == null || editor.isDisposed() || editor.getProject() == null) {
            return null;
        }
        EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
        if (eventManager == null) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(element.getProject())
            .getDocument(element.getContainingFile());
        String uri = FileUtils.documentToUri(document);
        TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri);
        if (isTextOffsetInvalidInDoc(document, element.getTextOffset())) {
            return null;
        }
        Position position = DocumentUtils.offsetToLSPPos(document, element.getTextOffset());

        StringBuilder sb = new StringBuilder();

        // 添加文档内容
        String docContent = eventManager.getQuickDocString(identifier, position);

        String[] lines = docContent.split(LINE_SEPARATOR);
        StringBuilder cleanDoc = new StringBuilder();
        String apiKey = "";
        for (String line : lines) {
            if (line.startsWith("apiKey:")) {
                apiKey = line.substring("apiKey:".length())
                        .replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                // 将HTML实体反转义为字符
                apiKey = Jsoup.parse(apiKey).text();
                continue;
            }
            cleanDoc.append(line);
            if (!cleanDoc.isEmpty()) {
                cleanDoc.append(LINE_SEPARATOR);
            }
        }
        sb.append(cleanDoc);
        ApiDocsManager apiDocsManager = new ApiDocsManager(element.getProject());
        ApiDocsManager.initializeMapping();

        if (Objects.equals("", apiDocsManager.getDocumentationUrl(element, apiKey))) {
            return sb.toString();
        }
        addApiHref(apiKey, sb);
        return sb.toString();
    }

    @Override
    public @Nullable
    PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        if (link.startsWith("apiName:")) {
            // 处理点击事件
            String apiName = link.substring("apiName:".length());
            Project project = context.getProject();
            openApiDocumentation(context, project, apiName);
            return null;
        }
        return super.getDocumentationElementForLink(psiManager, link, context);
    }

    /**
     * get custom documentation element
     *
     * @param editor editor
     * @param file file
     * @param contextElement context element
     * @param targetOffset target
     * @return custom element
     */
    @Override
    @Nullable
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                    @Nullable PsiElement contextElement, int targetOffset) {
        PsiElement element = super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
        return element == null ? contextElement : element;
    }

    private boolean isTextOffsetInvalidInDoc(Document document, int textOffset) {
        return document == null || textOffset < 0 || textOffset > document.getTextLength();
    }

    private void addApiHref(String apiKey, StringBuilder sb) {
        if (apiKey.isEmpty()) {
            return;
        }
        String packageName = "";
        if (apiKey.indexOf('/') != -1) {
            packageName = apiKey.substring(0, apiKey.indexOf('/'));
        }
        if (packageName.indexOf('.') != -1) {
            packageName = packageName.substring(0, packageName.indexOf('.'));
        }
        if (!apiPackages.contains(packageName)) {
            return;
        }
        // 添加Show in API Reference操作按钮
        sb.append("<div style=\"width=200px\">");
        sb.append("<a href=\"psi_element://apiName:").append(StringUtil.escapeXmlEntities(apiKey))
                .append("\" style=\"float: right;\">Show in API Reference</a>");
        sb.append("</div>");
    }

    private void openApiDocumentation(PsiElement element, Project project, String apiQualifiedName) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Cangjie API");
            if (toolWindow == null) {
                return;
            }
            toolWindow.show(() -> {
                Content content = toolWindow.getContentManager().getContent(0);
                if (content != null && content.getComponent() instanceof ApiDocsToolWindow apiDocsWindow) {
                    apiDocsWindow.navigateToElement(element, apiQualifiedName);
                }
            });
        });
    }
}
