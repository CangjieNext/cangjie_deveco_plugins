/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor;

import com.huawei.cjlsp.capabilities.lsp.CangjieFindUsagesHandler;
import com.huawei.cjlsp.capabilities.lsp.CangjieFindUsagesHandlerFactory;
import com.huawei.cjlsp.capabilities.lsp.CangjieGotoDeclarationHandler;
import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.CangjieDeclarationPsiFileRoot;
import com.huawei.ideacj.language.psi.CangjieNamedElement;
import com.huawei.ideacj.language.psi.othersnode.CjEnumPattern;
import com.huawei.ideacj.language.psi.othersnode.CjExceptionTypePattern;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.othersnode.CjLambdaParameter;
import com.huawei.ideacj.language.psi.othersnode.CjResourceSpecification;
import com.huawei.ideacj.language.psi.othersnode.CjTypePattern;
import com.huawei.ideacj.language.psi.othersnode.CjVarBindingPattern;
import com.huawei.ideacj.language.psi.toplevel.CjType;
import com.huawei.ideacj.language.psi.toplevel.CjTypeParameters;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOverloadedOperators;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputExprWithParens;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputExprWithoutParens;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;
import com.huawei.ideacj.lsp.utils.CangjiePsiUtils;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegateBase;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;

import org.eclipse.lsp4j.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cangjie File Safe Delete Processor Delegate
 *
 * @since 2025/03/26
 */
public class CangjieSafeDeleteProcessorDelegate extends SafeDeleteProcessorDelegateBase {
    private static final Logger LOG = Logger.getInstance(CangjieSafeDeleteProcessorDelegate.class);

    private PsiElement originalElement;

    /**
     * determine whether delegates can process element.
     *
     * @param element an element selected for deletion.
     * @return true if delegates can process element.
     */
    @Override
    public boolean handlesElement(PsiElement element) {
        return element instanceof CJPsiNode || element instanceof CJPsiLeafNode;
    }

    /**
     * Find usages of the element and fill result with them.
     *
     * @param element              an element selected for deletion.
     * @param allElementsToDelete  all elements selected for deletion.
     * @param result               list of {@link UsageInfo} to store found usages
     * @return null if element should not be searched in text occurrences/comments though corresponding settings were
     *         enabled, otherwise bean with the information how to detect if an element is inside all elements
     *         to delete.
     */
    @Override
    @Nullable
    public NonCodeUsageSearchInfo findUsages(
        @NotNull PsiElement element,
        PsiElement @NotNull [] allElementsToDelete,
        @NotNull List<? super UsageInfo> result) {
        TextRange range = element.getTextRange();
        CangjieFindUsagesHandlerFactory findUsagesHandlerFactory = new CangjieFindUsagesHandlerFactory();
        FindUsagesHandler findUsagesHandler = findUsagesHandlerFactory
                .createFindUsagesHandler(originalElement, false);
        if (!(findUsagesHandler instanceof CangjieFindUsagesHandler cangjieFindUsagesHandler)) {
            LOG.warn("Find Usages Not Available.");
            return null;
        }
        Collection<PsiReference> references = cangjieFindUsagesHandler.getReferences(originalElement);
        references.forEach(reference -> {
            final PsiElement psiElement = reference.getElement();
            if (!psiElement.getTextRange().contains(range)) {
                result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(psiElement, element, false));
            }
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
        originalElement = psiElement;
        Optional<PsiElement> curElementOption = getCurElement(psiElement);
        if (curElementOption.isEmpty()) {
            LOG.warn("cur element is empty");
            return Collections.emptyList();
        }
        PsiElement curElement = curElementOption.get();
        Editor editor = FileEditorManager.getInstance(psiElement.getProject()).getSelectedTextEditor();
        CangjieGotoDeclarationHandler handler = new CangjieGotoDeclarationHandler();
        PsiElement definition = handler.getGotoDeclarationTarget(curElement, editor);
        // curElement is definition
        if (definition == null) {
            definition = curElement;
        }
        if (!isLegal(curElement, editor)) {
            CommonRefactoringUtil.showErrorMessage("Error", String.format(
                    "Refactoring cannot be performed %s Cannot find definition of %s",
                    System.lineSeparator(),
                    curElement.getText()),
                null, psiElement.getProject());
            return null;
        }
        if (definition.getContainingFile() instanceof CangjieDeclarationPsiFileRoot) {
            CommonRefactoringUtil.showErrorMessage("Error", String.format(
                    "Refactoring cannot be performed %s File %s is read-only.",
                    System.lineSeparator(),
                    definition.getContainingFile().getVirtualFile().getPath()),
                    null, psiElement.getProject());
            return null;
        }
        PsiElement typeElement = PsiTreeUtil.getParentOfType(definition, CjType.class);
        if (typeElement != null) {
            CommonRefactoringUtil.showErrorMessage("Error", String.format(
                            "Refactoring cannot be performed %s %s is built-in type.",
                            System.lineSeparator(),
                            typeElement.getText()),
                    null, psiElement.getProject());
            return null;
        }
        return Collections.singleton(definition);
    }

    /**
     * Returns the list of additional elements to be deleted. Called after the refactoring dialog is shown. May show UI
     * to ask the user if some additional elements should be deleted along with the specified selected element.
     *
     * @param element an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @param shouldAskUser user confirm
     * @return additional elements to search for usages, or null if no additional elements were chosen.
     */
    @Override
    @Nullable
    public Collection<PsiElement> getAdditionalElementsToDelete(
        @NotNull PsiElement element,
        @NotNull Collection<? extends PsiElement> allElementsToDelete,
        boolean shouldAskUser) {
        return null;
    }

    /**
     * Detects usages which are not safe to delete.
     *
     * @param element an element selected for deletion.
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
     * @param usages all usages to be processed by the refactoring.
     * @return the filtered list of usages, or null if the user has cancelled the refactoring.
     */
    @Override
    @Nullable
    public UsageInfo [] preprocessUsages(
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
        PsiElement toDelete = getParentToDelete(element);
        if (toDelete == null) {
            LOG.warn("Cannot Find Element To Delete");
            return;
        }

        while (isSingleChild(toDelete) && toDelete.getParent() != null) {
            toDelete = toDelete.getParent();
        }

        PsiElement curElement = toDelete.getParent().getFirstChild();
        while (curElement.getNextSibling() != null) {
            curElement = curElement.getNextSibling();
            if (curElement.getNode().getElementType() == CangJieTypes.COMMA) {
                CangjiePsiUtils.deleteCommaSeparatedElement(toDelete);
                return;
            }
        }

        PsiElement semiElement = toDelete.getNextSibling();
        if (semiElement != null && semiElement.getChildren().length > 0
                && semiElement.getChildren()[0].getNode().getElementType() == CangJieTypes.SEMI) {
            semiElement.delete();
        }
        Document document = FileDocumentManager.getInstance()
                .getDocument(toDelete.getContainingFile().getVirtualFile());
        toDelete.delete();
        if (document == null) {
            return;
        }
        PsiDocumentManager.getInstance(element.getProject()).doPostponedOperationsAndUnblockDocument(document);
        FileDocumentManager.getInstance().saveDocument(document);
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
     * @param isEnabled enable
     */
    @Override
    public void setToSearchInComments(PsiElement element, boolean isEnabled) {
        RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS = isEnabled;
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
     * @param isEnabled enable
     */
    @Override
    public void setToSearchForTextOccurrences(PsiElement element, boolean isEnabled) {}

    private Optional<PsiElement> getCurElement(PsiElement element) {
        Project project = element.getProject();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            LOG.warn("Cannot find editor");
            return Optional.empty();
        }
        int offset = editor.getCaretModel().getOffset();
        return Optional.ofNullable(PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()))
                .map(psiFile -> psiFile.findElementAt(offset));
    }

    private PsiElement getParentToDelete(PsiElement element) {
        PsiElement cjPsiNode = element.getParent();

        if (cjPsiNode instanceof CjOverloadedOperators) {
            return cjPsiNode.getParent();
        }

        if (!(cjPsiNode instanceof CjIdentifier) || cjPsiNode.getParent() instanceof CjTypeParameters) {
            return cjPsiNode;
        }

        if (cjPsiNode.getParent() instanceof CjFunctionDefinition) {
            PsiElement foreignMember = cjPsiNode.getParent().getParent();
            while (foreignMember.getPrevSibling() != null) {
                foreignMember = foreignMember.getPrevSibling();
                if (foreignMember.getNode().getElementType() == CangJieTypes.FOREIGN) {
                    return foreignMember.getParent();
                }
            }
        }

        if (cjPsiNode.getParent() instanceof CjVarBindingPattern) {
            PsiElement varElement = PsiTreeUtil.getParentOfType(cjPsiNode, CjVariableDeclaration.class);
            if (varElement == null) {
                return cjPsiNode;
            }
        }

        if (shouldReturnParent(cjPsiNode)) {
            return cjPsiNode.getParent();
        }

        PsiElement toDelete = PsiTreeUtil.getParentOfType(cjPsiNode, CangjieNamedElement.class);
        if (toDelete == null) {
            toDelete = cjPsiNode;
        }

        if (toDelete.getParent() instanceof CjMacroInputExprWithParens
                || toDelete.getParent() instanceof CjMacroInputExprWithoutParens) {
            return PsiTreeUtil.getParentOfType(cjPsiNode, CjMacroExpression.class);
        }
        return toDelete;
    }

    private boolean isPattern(PsiElement element) {
        return element instanceof CjTypePattern
                || element instanceof CjEnumPattern;
    }

    private boolean shouldReturnParent(PsiElement cjPsiNode) {
        return isPattern(cjPsiNode.getParent())
                || cjPsiNode.getParent() instanceof CjResourceSpecification
                || cjPsiNode.getParent() instanceof CjExceptionTypePattern
                || cjPsiNode.getParent() instanceof CjLambdaParameter;
    }

    private boolean isSingleChild(PsiElement element) {
        PsiElement currentElement = element.getNextSibling();
        while (currentElement != null) {
            if (currentElement.getNode().getElementType() != CangJieTypes.RULE_END) {
                return false;
            }
            currentElement = currentElement.getNextSibling();
        }
        currentElement = element.getPrevSibling();
        while (currentElement != null) {
            if (currentElement.getNode().getElementType() != CangJieTypes.RULE_END) {
                return false;
            }
            currentElement = currentElement.getPrevSibling();
        }
        return true;
    }

    private boolean isLegal(PsiElement sourceElement, Editor editor) {
        CangjieGotoDeclarationHandler handler = new CangjieGotoDeclarationHandler();
        int offset = sourceElement.getTextRange().getStartOffset();
        Optional<Location> location = handler.getDeclaration(offset, editor);
        if (location.isPresent() && location.get().getRange() != null) {
            return true;
        }
        location = handler.getDefinition(offset, editor);
        return location.isPresent() && location.get().getRange() != null;
    }
}
