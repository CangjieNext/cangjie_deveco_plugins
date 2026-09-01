/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.completion;

import static java.util.Objects.requireNonNull;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.edit.CangjieLSPCaretListenerImpl;
import com.huawei.ideacj.edit.PsiElementFinder;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.lsp.utils.CommonUtils;
import com.huawei.ideacj.lsp.utils.Constants;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.Optional;

/**
 * The completion contributor for the LSP
 *
 * @author xiewen
 * @since 2021-08-18
 */

class LSPCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(LSPCompletionContributor.class);

    class CangjieCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
            @NotNull CompletionResultSet result) {
            try {
                ApplicationUtil.runWithCheckCanceled(() -> {
                    Editor editor = parameters.getEditor();
                    int offset = parameters.getOffset();
                    if (!(EditorEventManagerBase.forEditor(editor) instanceof CangjieEditorEventManager manager)) {
                        return null;
                    }
                    // psi parsed and try to check the first char legal or not
                    Optional<PsiElement> psiElement =
                            PsiElementFinder.findPsiElementAtCaret(editor.getProject(), editor);
                    if (psiElement.isEmpty()) {
                        return null;
                    }
                    String text = psiElement.get().getText();
                    if (!text.isEmpty() && Character.isDigit(text.charAt(0))) {
                        return null;
                    }
                    Document doc = editor.getDocument();
                    String lineStr = doc.getText().substring(doc.getLineStartOffset(doc.getLineNumber(offset)),
                            doc.getLineEndOffset(doc.getLineNumber(offset)));
                    PsiFile psiFile = ApplicationUtils.computableReadAction(
                            () -> PsiDocumentManager.getInstance(requireNonNull(editor.getProject()))
                                    .getPsiFile(editor.getDocument()));
                    PsiElement element = psiFile.findElementAt(offset - 1);
                    var generic = isInGeneric(element, psiFile);
                    manager.setIsInGeneric(generic);
                    var results = CangjieLSPCaretListenerImpl.getCompletionResult(editor, lineStr);
                    result.addAllElements(results);
                    return null;
                }, requireNonNull(ProgressIndicatorProvider.getGlobalProgressIndicator()));
            } catch (ProcessCanceledException ignored) {
                LOG.info("LSP canceled the completion request.");
            } catch (Exception e) {
                LOG.warn("LSP Completions ended with an error.");
            }
        }
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        CompletionProvider<CompletionParameters> provider = new CangjieCompletionProvider();
        Editor editor = parameters.getEditor();
        if (!(EditorEventManagerBase.forEditor(editor) instanceof CangjieEditorEventManager)) {
            return;
        }
        provider.addCompletionVariants(parameters, new ProcessingContext(), result);
        if (result.isStopped()) {
            return;
        }
        CommonUtils.closeCompletionToolTip();

        super.fillCompletionVariants(parameters, result);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        final VirtualFile file = position.getContainingFile().getVirtualFile();
        if (!FileUtils.isFileSupported(file)) {
            return false;
        }

        String uri = FileUtils.VFSToURI(file);
        if (!(EditorEventManagerBase.forUri(uri) instanceof CangjieEditorEventManager manager)) {
            return false;
        }
        for (String triggerChar : manager.completionTriggers) {
            if (triggerChar != null && triggerChar.length() == 1 && triggerChar.charAt(0) == typeChar) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if the position in generic
     *
     * @param element psi element in the pos
     * @param psiFile psi element in the psifile
     * @return boolean is in generic
     */
    public boolean isInGeneric(PsiElement element, PsiFile psiFile) {
        String ws = "WHITE_SPACE";
        if (element == null) {
            return false;
        }
        if (element.getNode().getElementType() == CangJieTypes.LT) {
            return true;
        }
        Optional<PsiElement> prev = getPrevElement(element, psiFile);
        int identifier = 0;
        while (prev.isPresent()) {
            var prevElement = prev.get();
            var type = prevElement.getNode().getElementType();
            if (type == CangJieTypes.COMMA) {
                identifier++;
            } else if (this.isIdentifier(type)) {
                identifier--;
                if (identifier < 0) {
                    return false;
                }
            } else if (type == CangJieTypes.LT) {
                return true;
            } else {
                if (type != CangJieTypes.NL && !type.toString().equals(ws)) {
                    return false;
                }
            }
            prev = getPrevElement(prevElement, psiFile);
            if (prev.isEmpty()) {
                return false;
            }
        }
        return false;
    }

    private Optional<PsiElement> getPrevElement(PsiElement element, PsiFile psiFile) {
        if (element == null) {
            return Optional.empty();
        }
        TextRange textRange = element.getTextRange();
        if (textRange == null) {
            return Optional.empty();
        }
        int startOffset = textRange.getStartOffset();
        if (startOffset > 0) {
            return Optional.ofNullable(psiFile.findElementAt(startOffset - 1));
        } else {
            return Optional.empty();
        }
    }

    private boolean isIdentifier(IElementType type) {
        if (type == CangJieTypes.IDENTIFIER) {
            return true;
        }
        if (Constants.KEYWORD_WITH_SPACE.contains(type.toString().replace("'", ""))) {
            return true;
        }
        return false;
    }
}
