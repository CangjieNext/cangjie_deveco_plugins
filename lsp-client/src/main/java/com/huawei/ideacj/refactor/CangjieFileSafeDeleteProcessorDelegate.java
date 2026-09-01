/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor;

import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegateBase;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.contributors.psi.LSPPsiElement;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CangjieFileSafeDeleteProcessorDelegate
 *
 * @since 2025/8/20
 */
public class CangjieFileSafeDeleteProcessorDelegate extends SafeDeleteProcessorDelegateBase {
    private static final Logger LOG = Logger.getInstance(CangjieFileSafeDeleteProcessorDelegate.class);

    private static final int TIMEOUT = 50000;   // 50s

    /**
     * determine whether delegates can process element.
     *
     * @param element an element selected for deletion.
     * @return true if delegates can process element.
     */
    @Override
    public boolean handlesElement(PsiElement element) {
        return element instanceof CangJiePsiFileRoot;
    }

    /**
     * Find usages of the element and fill result with them.
     *
     * @param element             an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @param result              list of {@link UsageInfo} to store found usages
     * @return null if element should not be searched in text occurrences/comments though corresponding settings were
     * enabled, otherwise bean with the information how to detect if an element is inside all elements
     * to delete.
     */
    @Override
    @Nullable
    public NonCodeUsageSearchInfo findUsages(
            @NotNull PsiElement element,
            PsiElement @NotNull [] allElementsToDelete,
            @NotNull List<? super UsageInfo> result) {
        if (!(element instanceof CangJiePsiFileRoot cjFileRoot)) {
            LOG.warn("Not a CangjieFile");
            return null;
        }
        DocumentLinkParams param =
                new DocumentLinkParams(new TextDocumentIdentifier(cjFileRoot.getVirtualFile().getUrl()));
        LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION,
                        FileUtils.projectToUri(cjFileRoot.getProject()));
        if (lspWrapper == null || !(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            LOG.warn("LSP Server Not Ready");
            return null;
        }
        CompletableFuture<List<Location>> request = requestManager.findFileReference(param);
        if (request == null) {
            LOG.warn("Find File Request Is Null");
            return null;
        }
        List<Location> locations = new ArrayList<>();
        try {
            locations = request.get(TIMEOUT, TimeUnit.MILLISECONDS); // 50s
            lspWrapper.notifySuccess(REFERENCES);
        } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
            lspWrapper.notifyFailure(REFERENCES);
        }
        if (CollectionUtils.isEmpty(locations)) {
            return new NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete),
                    element);
        }
        locations.forEach(location -> {
            Optional<PsiElement> reference = getReferencebyLocation(location, element.getProject());
            if (!reference.isPresent()) {
                return;
            }
            result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(reference.get(), element, false));
        });
        return new NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete),
                element);
    }

    /**
     * Returns elements that are searched for usages of the element selected for deletion. Called before the refactoring
     * dialog is shown. May show UI to ask if additional elements should be deleted along with the specified
     * selected element.
     *
     * @param psiElement an element selected for deletion.
     * @param module     module
     * @param collection all elements selected for deletion.
     * @return additional elements to search for usages, or null if the user has cancelled the refactoring.
     */
    @Override
    @Nullable
    public Collection<? extends PsiElement> getElementsToSearch(
            @NotNull PsiElement psiElement,
            @Nullable Module module,
            @NotNull Collection<? extends PsiElement> collection) {
        return Collections.singletonList(psiElement);
    }

    /**
     * Returns the list of additional elements to be deleted. Called after the refactoring dialog is shown. May show UI
     * to ask the user if some additional elements should be deleted along with the specified selected element.
     *
     * @param element             an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @param askUser             user confirm
     * @return additional elements to search for usages, or null if no additional elements were chosen.
     */
    @Override
    @Nullable
    public Collection<PsiElement> getAdditionalElementsToDelete(
            @NotNull PsiElement element,
            @NotNull Collection<? extends PsiElement> allElementsToDelete,
            boolean askUser) {
        return null;
    }

    /**
     * Detects usages which are not safe to delete.
     *
     * @param element             an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @return collection of conflict messages which would be shown to the user before delete can be performed.
     */
    @Override
    @Nullable
    public Collection<@NlsContexts.DialogMessage String> findConflicts(
            @NotNull PsiElement element,
            PsiElement @NotNull [] allElementsToDelete) {
        return null;
    }

    /**
     * Called after the user has confirmed the refactoring. Can filter out some of the usages found by the refactoring.
     * May show UI to ask the user if some of the usages should be excluded.
     *
     * @param project the project where the refactoring happens.
     * @param usages  all usages to be processed by the refactoring.
     * @return the filtered list of usages, or null if the user has cancelled the refactoring.
     */
    @Override
    @Nullable
    public UsageInfo[] preprocessUsages(
            @NotNull Project project,
            UsageInfo @NotNull [] usages) {
        return usages;
    }

    /**
     * Prepares an element for deletion e.g., normalizing declaration so the element declared in the same declaration
     * won't be affected by deletion.
     *
     * @param element an element selected for deletion.
     * @throws IncorrectOperationException Incorrect Operation Exception
     */
    @Override
    public void prepareForDeletion(@NotNull PsiElement element) throws IncorrectOperationException {
        return;
    }

    /**
     * Called to set initial value for "Search in comments" checkbox.
     *
     * @param element an element selected for deletion.
     * @return true if previous safe delete was executed with "Search in comments" option on.
     */
    @Override
    public boolean isToSearchInComments(PsiElement element) {
        return RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS;
    }

    /**
     * Called to save chosen for given element "Search in comments" value.
     *
     * @param element an element selected for deletion.
     * @param enabled enable
     */
    @Override
    public void setToSearchInComments(PsiElement element, boolean enabled) {
        RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS = enabled;
    }

    /**
     * Called to set initial value for "Search for text occurrence" checkbox.
     *
     * @param element an element selected for deletion.
     * @return true if previous safe delete was executed with "Search for test occurrences" option on.
     */
    @Override
    public boolean isToSearchForTextOccurrences(PsiElement element) {
        return false;
    }

    /**
     * Called to save chosen for given element "Search for text occurrences" value.
     *
     * @param element an element selected for deletion.
     * @param enabled enable
     */
    @Override
    public void setToSearchForTextOccurrences(PsiElement element, boolean enabled) {
    }

    private Optional<PsiElement> getReferencebyLocation(Location location, Project project) {
        Position start = location.getRange().getStart();
        Position end = location.getRange().getEnd();
        String uri = FileUtils.sanitizeURI(location.getUri());
        VirtualFile file = FileUtils.virtualFileFromURI(uri);
        if (file == null) {
            return Optional.empty();
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null || psiFile == null) {
            return Optional.empty();
        }
        if (start.getLine() < 0 || start.getLine() >= document.getLineCount()) {
            return Optional.empty();
        }
        int logicalStart = document.getLineStartOffset(start.getLine()) + start.getCharacter();
        int logicalEnd = document.getLineStartOffset(end.getLine()) + end.getCharacter();
        if (logicalStart < 0 || logicalEnd > document.getTextLength()) {
            return Optional.empty();
        }
        String name = document.getText(new TextRange(logicalStart, logicalEnd));
        PsiReference reference = new LSPPsiElement(name, project, logicalStart, logicalEnd, psiFile).getReference();
        if (reference == null) {
            return Optional.empty();
        }
        return Optional.of(reference.getElement());
    }
}