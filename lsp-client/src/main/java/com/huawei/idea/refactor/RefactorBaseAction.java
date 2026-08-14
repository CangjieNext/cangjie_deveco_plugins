/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor;

import com.huawei.idea.edit.CangjieEditorEventManager;
import com.huawei.idea.language.psi.operatornode.CJAdditiveOperator;
import com.huawei.idea.language.psi.operatornode.CJMultiplicativeOperator;
import com.huawei.idea.lsp.utils.CangJieLanguage;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BasePlatformRefactoringAction;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.ArrayList;
import java.util.List;

/**
 * RefactorBaseAction
 *
 * @since 2025-06-09
 */
public abstract class RefactorBaseAction extends BasePlatformRefactoringAction {
    /**
     * Command
     */
    @NotNull
    protected Command command;

    /**
     * title
     */
    protected String title;

    /**
     * validCommands
     */
    protected ArrayList<Command> validCommands;

    @Override
    @Nullable
    protected abstract RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider);

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(false);

        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            return;
        }
        if (!isAvailableForFile(file) || file instanceof PsiCompiledElement && disableOnCompiledElement()) {
            return;
        }

        DataContext dataContext = e.getDataContext();
        PsiElement element = findRefactoringTargetInEditor(dataContext, this::isAvailableForLanguage);
        if (element == null) {
            return;
        }

        if (enable(e) && isAvailableOnElementInEditorAndFile(element, editor, file, dataContext, e.getPlace())) {
            presentation.setEnabledAndVisible(true);
        }
    }

    /**
     * refactor action default enable method
     *
     * @param event action event
     * @return enable
     */
    protected boolean enable(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return false;
        }
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        Command fromTweaks = getCommandFromTweaks(editor, selectionStart, selectionEnd);
        if (fromTweaks == null) {
            return false;
        }
        command = fromTweaks;
        return command.getCommand() != null;
    }

    /**
     * 判断选中元素是否是同一层级，是否符合优先级
     * 3 * 9 + base, 比如选中 9 + base 是不符合要求的
     *
     * @param event event
     * @return isSameLevelPsi
     */
    protected boolean isSameLevelPsi(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || file == null) {
            return false;
        }
        SelectionModel selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            return false;
        }

        int startOffset = selectionModel.getSelectionStart();
        int endOffset = selectionModel.getSelectionEnd();
        if (startOffset >= endOffset) {
            return false;
        }
        PsiElement startElement = file.findElementAt(startOffset);
        PsiElement endElement = file.findElementAt(endOffset - 1);
        if (startElement == null || endElement == null) {
            return false;
        }
        if (startElement == endElement) {
            return true;
        }

        PsiElement commonParent = PsiTreeUtil.findCommonParent(startElement, endElement);
        if (commonParent == null) {
            return false;
        }
        return isSameLevelSelection(commonParent, startOffset, endOffset);
    }

    private static boolean isSameLevelSelection(@NotNull PsiElement parent, int startOffset, int endOffset) {
        int selectedItemCount = 0;
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isIgnoredSelectionElement(child)) {
                continue;
            }
            TextRange range = child.getTextRange();
            if (range == null || !range.intersectsStrict(startOffset, endOffset)) {
                continue;
            }
            if (startOffset > range.getStartOffset() || endOffset < range.getEndOffset()) {
                return false;
            }
            selectedItemCount++;
        }
        return selectedItemCount > 0;
    }

    private static boolean isIgnoredSelectionElement(@NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace
                || element instanceof CJAdditiveOperator
                || element instanceof CJMultiplicativeOperator;
    }

    /**
     * getCommandFromTweaks
     * check if CodeAction Response contains tweak id
     *
     * @param editor editor
     * @param startOffset startOffset
     * @param endOffset endOffset
     * @return Command
     */
    @Nullable
    protected Command getCommandFromTweaks(Editor editor, int startOffset, int endOffset) {
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager extendManager)) {
            return new Command();
        }
        List<Either<Command, CodeAction>> res = extendManager.codeAction4Refactor(startOffset, endOffset);
        for (Either<Command, CodeAction> either : res) {
            Command cmd = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (cmd.getTitle().startsWith(title)) {
                return cmd;
            }
        }
        return new Command();
    }

    @Override
    protected boolean isAvailableForFile(PsiFile file) {
        return file.getLanguage().is(CangJieLanguage.INSTANCE);
    }
}
