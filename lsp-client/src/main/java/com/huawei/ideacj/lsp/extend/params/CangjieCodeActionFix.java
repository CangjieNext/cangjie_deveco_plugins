/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import com.huawei.ideacj.dialog.CjRemoveImportConfirmDialog;
import com.huawei.ideacj.lsp.extend.ExtendWorkspaceEditHandler;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.IntentionsUI;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CodeAction;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.fixes.LSPCodeActionFix;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.requests.WorkspaceEditHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjieCodeActionFix
 *
 * @since 2024-12-22
 */
public class CangjieCodeActionFix extends LSPCodeActionFix {
    /**
     * REMOVE_ALL_IMPORT
     */
    public static final String REMOVE_ALL_IMPORT = "Remove all unused imports";

    /**
     * IMPORT_ALL_SYMBOLS
     */
    public static final String IMPORT_ALL_SYMBOLS = "import all symbols";

    /**
     * IMPLEMENT_MEMBERS
     */
    public static final String IMPLEMENT_MEMBERS = "Implement members";

    private static final String IMPORT_STRING = "import";

    private final Pattern pattern = Pattern.compile("'(.*?)'");

    /**
     * cangjie code action fix
     *
     * @param uri        uri
     * @param codeAction code action
     */
    public CangjieCodeActionFix(String uri, @NotNull CodeAction codeAction) {
        super(uri, codeAction);
    }

    /**
     * CodeAction Kind
     */
    public enum CodeActionKind {
        QUICKFIX_ADD_IMPORT("quickfix.addImport"),
        QUICKFIX_REMOVE_IMPORT("quickfix.removeImport"),
        QUICKFIX_REMOVE_UNUSED_SYMBOL("quickfix.removeUnusedSymbol"),
        QUICKFIX_IMPLEMENT_MEMBERS("quickfix.implementMembers"),
        REFACTOR("refactor"),
        INFO("info");

        private final String kind;

        CodeActionKind(String kind) {
            this.kind = kind;
        }

        public String getKind() {
            return kind;
        }
    }

    /**
     * sort code action
     *
     * @param other other action
     * @return order
     */
    public int compareTo(@NotNull IntentionAction other) {
        boolean thisPreferred = getIsPreferred(this);
        boolean otherPreferred = getIsPreferred(other);

        if (thisPreferred && !otherPreferred) {
            return -1;
        }
        if (!thisPreferred && otherPreferred) {
            return 1;
        }

        int weight1 = this.getWeight(this.getText());
        int weight2 = this.getWeight(other.getText());

        return weight1 == weight2 ? Comparing.compare(this.getText(), other.getText())
                : weight2 - weight1;
    }

    private int getWeight(String displayString) {
        return StringUtils.containsIgnoreCase(displayString, IMPORT_STRING) ? 1 : 0;
    }

    private boolean getIsPreferred(IntentionAction action) {
        if (!(action instanceof CangjieCodeActionFix cangjieCodeAction)) {
            return false;
        }
        return cangjieCodeAction.codeAction.getIsPreferred() != null && cangjieCodeAction.codeAction.getIsPreferred();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) {
        if (Objects.equals(codeAction.getKind(), CodeActionKind.QUICKFIX_REMOVE_IMPORT.getKind())) {
            String removeImport;
            if (codeAction.getTitle().equals(REMOVE_ALL_IMPORT)) {
                removeImport = "unused imports";
            } else {
                Matcher matcher = pattern.matcher(codeAction.getTitle());
                if (matcher.find()) {
                    removeImport = matcher.group(1);
                } else {
                    removeImport = "";
                }
            }
            if (StringUtils.isEmpty(removeImport)) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                boolean shouldExecute = CjRemoveImportConfirmDialog.createConfirmDialog(project, removeImport);
                if (!shouldExecute) {
                    return;
                }
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    executeAction(project, editor);
                });
            });
        } else if (Objects.equals(codeAction.getKind(), CodeActionKind.QUICKFIX_REMOVE_UNUSED_SYMBOL.getKind())
                || Objects.equals(codeAction.getKind(), CodeActionKind.QUICKFIX_IMPLEMENT_MEMBERS.getKind())) {
            // Direct deletion without confirmation
            WriteCommandAction.runWriteCommandAction(project, () -> {
                executeAction(project, editor);
            });
        } else {
            executeAction(project, editor);
        }
    }

    private void executeAction(@NotNull Project project, Editor editor) {
        if (codeAction.getEdit() != null) {
            if (Objects.equals(codeAction.getKind(), CodeActionKind.QUICKFIX_REMOVE_IMPORT.getKind())) {
                ExtendWorkspaceEditHandler.removeImportsApplyEdit(codeAction.getEdit(), codeAction.getTitle());
            } else if (codeAction.getTitle() != null && codeAction.getTitle().equals(IMPLEMENT_MEMBERS)) {
                ExtendWorkspaceEditHandler.implementMembersApplyEdit(codeAction.getEdit(), codeAction.getTitle());
            } else {
                WorkspaceEditHandler.applyEdit(codeAction.getEdit(), codeAction.getTitle());
            }
        }
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (manager != null) {
            manager.executeCommands(Collections.singletonList(codeAction.getCommand()));
        }

        // clean old intention information, avoid recurrence
        IntentionsUI.getInstance(project).invalidate();
        UpdateHighlightersUtil.setHighlightersToEditor(project, editor.getDocument(), 0,
                editor.getDocument().getTextLength(), new ArrayList<HighlightInfo>(), null, 0);
    }
}
