/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor;

import com.huawei.idea.edit.CangjieEditorEventManager;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroAttrDecl;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroAttrExpr;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.idea.refactor.extract.CangjieCodeBlock;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * RefactorBaseHandler
 *
 * @since 2025-06-10
 */
public abstract class RefactorBaseHandler implements RefactoringActionHandler {
    private static final Logger LOG = Logger.getInstance(RefactorBaseHandler.class);

    /**
     * tweak type
     */
    protected String tweak = "";

    /**
     * warning message when failed to execute command
     */
    protected String warningMessage = "";

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        CangjieCodeBlock codeBlock = new CangjieCodeBlock(project, editor);
        Selected selected = getValidSelected(editor, codeBlock);
        if (selected == null) {
            return;
        }

        try {
            if (selected.selectionModel().hasSelection()) {
                executeCodeAction(editor, codeBlock, selected.validStart(), selected.validEnd());
            } else {
                reportError(codeBlock, "Select a code segment first.");
            }
        } catch (ExecutionException e) {
            LOG.debug(e);
        }
    }

    @Nullable
    private RefactorBaseHandler.Selected getValidSelected(Editor editor, CangjieCodeBlock codeBlock) {
        SelectionModel selectionModel = getSelectionModel(editor);
        int startCaret = editor.getSelectionModel().getSelectionStart();
        int endCaret = editor.getSelectionModel().getSelectionEnd();
        String selectedText = selectionModel.getSelectedText();
        if (selectedText == null) {
            reportError(codeBlock, "Select a code segment first.");
            return null;
        }
        int leadingWhitespace = 0;
        for (int i = 0; i < selectedText.length(); i++) {
            if (!Character.isWhitespace(selectedText.charAt(i))) {
                break;
            }
            leadingWhitespace++;
        }

        int trailingWhitespace = 0;
        for (int i = selectedText.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(selectedText.charAt(i))) {
                break;
            }
            trailingWhitespace++;
        }
        int validStart = startCaret + leadingWhitespace;
        int validEnd = endCaret - trailingWhitespace;
        if (validEnd <= validStart) {
            reportError(codeBlock, "Select a code segment first.");
            return null;
        }
        return new Selected(selectionModel, validStart, validEnd);
    }

    private record Selected(SelectionModel selectionModel, int validStart, int validEnd) {
    }

    @NotNull
    private static SelectionModel getSelectionModel(Editor editor) {
        return editor.getSelectionModel();
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {
    }

    /**
     * executeCodeAction
     *
     * @param editor editor
     * @param codeBlock codeBlock
     * @param start start
     * @param end end
     * @throws ExecutionException ExecutionException
     */
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock,
                                     int start, int end) throws ExecutionException {
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager)) {
            reportError(codeBlock, "Manager is null");
            return;
        }
        CangjieEditorEventManager extendedManager = (CangjieEditorEventManager) manager;
        List<Either<Command, CodeAction>> res = extendedManager.codeAction4Refactor(start, end);
        if (res.isEmpty()) {
            reportError(codeBlock, this.tweak + " failed. " + warningMessage);
            return;
        }
        Iterator<Either<Command, CodeAction>> iterator = res.iterator();
        while (iterator.hasNext()) {
            Either<Command, CodeAction> either = iterator.next();
            Command command = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (command.getTitle().startsWith(tweak)) {
                executeCommands(extendedManager, command);
                return;
            }
        }
        reportError(codeBlock, this.tweak + " failed. " + warningMessage);
    }

    public void setTweak(String tweak) {
        this.tweak = tweak;
    }

    public void setWarningMessage(String message) {
        this.warningMessage = message;
    }

    /**
     * execute commands
     *
     * @param manager manager
     * @param command command
     */
    protected abstract void executeCommands(CangjieEditorEventManager manager, Command command);

    @Override
    public String toString() {
        return "RefactorBaseHandler{" + "tweak='" + tweak + '\'' + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (obj instanceof RefactorBaseHandler) {
            RefactorBaseHandler that = (RefactorBaseHandler) obj;
            return Objects.equals(tweak, that.tweak);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tweak);
    }

    /**
     * reportError
     *
     * @param block   CangjieCodeBlock
     * @param message String
     */
    @Contract("_, _ -> fail")
    protected void reportError(CangjieCodeBlock block, String message) {
        CommonRefactoringUtil.showErrorHint(block.getProject(), block.getEditor(), message,
                this.tweak + " failed", null);
        LOG.debug(message);
    }

    /**
     * Check if the type definition contains any macro defintions or macro expressions.
     * Traverse up from the current node to check if the element or its parents are macros.
     *
     * @param element the type definition element to check
     * @return true if the element or its parents contain macros, false otherwise
     */
    public boolean hasMacroInType(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        PsiElement current = element;
        while (current != null) {
            if (isMacroElement(current)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Check if an element is a macro-related PSI element
     *
     * @param element the element to check
     * @return true if the element is a macro definition or expression
     */
    public boolean isMacroElement(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        return element instanceof CjMacroDefinition
                || element instanceof CjMacroExpression
                || element instanceof CjMacroAttrDecl
                || element instanceof CjMacroAttrExpr;
    }
}
