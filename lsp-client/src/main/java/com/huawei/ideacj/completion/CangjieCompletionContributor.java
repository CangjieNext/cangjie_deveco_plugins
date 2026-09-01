/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.completion;

import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;

import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.lsp.utils.Constants;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;

import cjgrammar.parser.CharLexer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.Icon;

/**
 * For completionCon of cangjie
 * support keyword  completion, identifier completion, dot completion
 *
 * @author s30009628
 * @since 2021-07-21
 */
public class CangjieCompletionContributor extends CompletionContributor {
    static String myPrefix = "";

    CangjieCompletionContributor() {
        myPrefix = "";
    }

    static class IdentifiernProvider extends CompletionProvider<CompletionParameters> {
        /**
         * SNIPPET_PLACEHOLDER_REGEX
         */
        public static final String SNIPPET_PLACEHOLDER_REGEX = "(\\$\\{\\d+:?([^{^}]*)}|\\$\\d+)";

        /**
         * create lookup item
         *
         * @param item CompletionItem
         * @return LookupElement
         */
        public Optional<LookupElement> createLookupItem(CompletionItem item) {
            Icon icon = AllIcons.Nodes.UpLevel;
            if (item.getKind() == CompletionItemKind.Variable) {
                icon = AllIcons.Nodes.Variable;
            }

            String insertText = item.getInsertText();
            String label = item.getLabel();
            Either<TextEdit, InsertReplaceEdit> textEdit = item.getTextEdit();
            String lookupString = null;
            if (textEdit != null) {
                lookupString = textEdit.getLeft().getNewText();
            } else if (StringUtils.isNotEmpty(insertText)) {
                lookupString = insertText;
            } else {
                if (StringUtils.isNotEmpty(label)) {
                    lookupString = label;
                }
            }
            if (StringUtils.isEmpty(lookupString)) {
                return Optional.empty();
            }
            // Fixes IDEA internal assertion failure in windows.
            lookupString = lookupString.replace(DocumentUtils.WIN_SEPARATOR, DocumentUtils.LINUX_SEPARATOR);

            LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(lookupString,
                    getLookupStringWithoutPlaceholders(item, lookupString));
            if (item.getKind() == CompletionItemKind.Keyword) {
                lookupElementBuilder = lookupElementBuilder.withBoldness(true);
            }

            String detail = item.getDetail();
            String tailText = (detail != null) ? detail : "";
            String presentableText = StringUtils.isNotEmpty(label) ? label : (insertText != null) ? insertText : "";
            return Optional.of(lookupElementBuilder.withPresentableText(presentableText).withTypeText(tailText, true)
                    .withIcon(icon).withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT));
        }

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
            @NotNull CompletionResultSet result) {
            List<LookupElement> lookupItems = new ArrayList<>();
            if (!".".equals(myPrefix)) {
                addKeyword(lookupItems);
            }

            addIdentifier(parameters, lookupItems);
            result.addAllElements(lookupItems);
        }

        private static class CangJiePsiRecursiveElementVisitor extends PsiRecursiveElementVisitor {
            private Set<String> identifierSet;

            public CangJiePsiRecursiveElementVisitor(Set<String> identifierSet) {
                this.identifierSet = identifierSet;
            }

            @Override
            public void visitElement(PsiElement element) {
                IElementType elementType = element.getNode().getElementType();
                if (CangJieTypes.IDENTIFIER.equals(elementType)) {
                    identifierSet.add(element.getText());
                }
                super.visitElement(element);
            }
        }

        private void addKeyword(List<LookupElement> lookupItems) {
            int startTokenType = CharLexer.INT8;
            int endTokenType = CharLexer.REDEF;
            for (int tokenType = startTokenType; tokenType <= endTokenType; tokenType++) {
                String keyword = CharLexer.VOCABULARY.getLiteralName(tokenType);
                if (keyword == null || keyword.isEmpty()) {
                    continue;
                }
                // remove "'" of "'var'"
                keyword = keyword.substring(1, keyword.length() - 1);
                CompletionItem item = new CompletionItem();
                item.setKind(CompletionItemKind.Keyword);
                item.setLabel(keyword);
                item.setInsertTextFormat(InsertTextFormat.forValue(1));
                item.setFilterText(keyword);
                item.setInsertText(updateLookupString(keyword));
                Optional<LookupElement> lookupElement = createLookupItem(item);
                lookupElement.ifPresent(lookupItems::add);
            }
        }

        private String updateLookupString(String lookupString) {
            return Constants.KEYWORD_WITH_SPACE.contains(lookupString) ? lookupString + " " : lookupString;
        }

        private void addIdentifier(@NotNull CompletionParameters parameters, List<LookupElement> lookupItems) {
            PsiElement position = parameters.getPosition();
            PsiFile psiFile = position.getContainingFile();
            Set<String> identifierSet = new HashSet<String>();
            getIdent(psiFile, identifierSet);
            for (var identifier : identifierSet) {
                if (identifier.isEmpty()) {
                    continue;
                }
                CompletionItem item = new CompletionItem();
                item.setKind(CompletionItemKind.Variable);
                item.setLabel(identifier);
                item.setInsertTextFormat(InsertTextFormat.forValue(1));
                item.setFilterText(identifier);
                item.setInsertText(identifier);
                Optional<LookupElement> lookupElement = createLookupItem(item);
                lookupElement.ifPresent(lookupItems::add);
            }
        }

        private String convertPlaceHolders(String insertText) {
            return insertText.replaceAll(SNIPPET_PLACEHOLDER_REGEX, "");
        }

        private String getLookupStringWithoutPlaceholders(CompletionItem item, String lookupString) {
            if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
                return convertPlaceHolders(lookupString);
            } else {
                return lookupString;
            }
        }

        private void getIdent(PsiFile psiFile, Set<String> stringSet) {
            psiFile.accept(new CangJiePsiRecursiveElementVisitor(stringSet));
        }
    }

    /**
     * get completion prefix
     *
     * @param editor Editor
     * @param offset offset of cursor
     * @return CompletionPrefix
     */
    public String getCompletionPrefix(Editor editor, int offset) {
        if (!(editor instanceof EditorEx)) {
            throw new ClassCastException("Forced type conversion failed!");
        }
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset - 1);
        int tokenStart = iterator.getStart();
        List<String> delimiters = new ArrayList<>();
        // add whitespace as delimiter, otherwise forced completion does not work
        delimiters.addAll(Arrays.asList(" \t\n\r".split("")));

        StringBuilder prefix = new StringBuilder();
        String documentText = editor.getDocument().getText();
        for (int i = 0; i < offset - tokenStart; i++) {
            char singleLetter = documentText.charAt(offset - i - 1);
            if (delimiters.contains(String.valueOf(singleLetter))) {
                return prefix.reverse().toString();
            }
            prefix.append(singleLetter);
        }
        return prefix.reverse().toString();
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        Editor editor = parameters.getEditor();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) {
            return;
        }
        Project project = editor.getProject();
        String projectUri = FileUtils.projectToUri(project);
        String ext = file.getExtension();

        LanguageServerWrapper curWrapper = getServerWrappersFor(ext, projectUri);
        if (curWrapper != null && curWrapper.getStatus() == ServerStatus.INITIALIZED) {
            return;
        }

        int offset = parameters.getOffset();
        myPrefix = getCompletionPrefix(editor, offset);
        if (!myPrefix.isEmpty() && !".".equals(myPrefix)) {
            char firstChar = myPrefix.charAt(0);
            if (Character.isDigit(firstChar)) {
                return;
            }
        }

        IdentifiernProvider provider = new IdentifiernProvider();
        provider.addCompletionVariants(parameters, new ProcessingContext(), result.caseInsensitive());
        if (result.isStopped()) {
            return;
        }

        super.fillCompletionVariants(parameters, result);
    }
}
