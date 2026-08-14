/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor.introduce;

import com.huawei.idea.edit.CangjieEditorEventManager;
import com.huawei.idea.refactor.RefactorBaseHandler;
import com.huawei.idea.refactor.extract.CangjieCodeBlock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * CangjieIntroduceParameterHandler handles the refactoring logic for introducing
 * an expression as a function parameter within the Cangjie language plugin.
 *
 * @since 2026-05-12
 */
public class CangjieIntroduceParameterHandler extends RefactorBaseHandler {
    private static final Pattern CANGJIE_IDENTIFIER_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private static final int ERROR_FAIL_GET_ROOT_EXPR = 2;
    private static final int ERROR_INVALID_EXPR = 3;
    private static final int ERROR_INVALID_CODE_SEGMENT = 4;
    private static final int ERROR_INVALID_SCOPE = 5;
    private static final int ERROR_INVALID_TYPE = 6;
    private static final int ERROR_PUBLIC_DECL_USES_NON_PUBLIC_TYPE = 7;
    private static final int ERROR_MEMBER_ASSIGN_IN_CONSTRUCTOR = 8;
    private static final int ERROR_INVALID_CONST_INITIALIZER = 9;
    private static final int ERROR_INVALID_LET_PATTERN_DESTRUCTOR = 10;

    /**
     * Error mapping constants to localized notification strings.
     */
    private static final Map<Integer, String> ERROR_CODE_MAP = Map.of(
            ERROR_FAIL_GET_ROOT_EXPR, "Select a complete expression to refactor.",
            ERROR_INVALID_EXPR, "The selected range is not a valid expression.",
            ERROR_INVALID_CODE_SEGMENT, "The selected expression cannot be refactored.",
            ERROR_INVALID_SCOPE, "Introduce parameter is only supported inside function bodies.",
            ERROR_INVALID_TYPE, "Cannot infer the selected expression type.",
            ERROR_PUBLIC_DECL_USES_NON_PUBLIC_TYPE,
                "Cannot introduce parameter because a public declaration cannot use a non-public type.",
            ERROR_MEMBER_ASSIGN_IN_CONSTRUCTOR,
                "Cannot introduce parameter because selected is a member-assign expression in constructor.",
            ERROR_INVALID_CONST_INITIALIZER,
                "Cannot introduce parameter from a const initializer because it must remain a compile-time "
                    + "constant expression",
            ERROR_INVALID_LET_PATTERN_DESTRUCTOR,
                "Cannot introduce parameter from a let pattern condition because it is not a standalone expression."
    );

    /**
     * Initializes a new instance of CangjieIntroduceParameterHandler with predefined tweaks.
     */
    public CangjieIntroduceParameterHandler() {
        setTweak("Introduce expression to parameter");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements,
                       @Nullable com.intellij.openapi.actionSystem.DataContext dataContext) {
        if (elements == null || elements.length == 0) {
            return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        invoke(project, editor, elements[0].getContainingFile(), dataContext);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file,
                       @Nullable com.intellij.openapi.actionSystem.DataContext dataContext) {
        if (file == null || editor == null) {
            return;
        }
        CangjieCodeBlock codeBlock = new CangjieCodeBlock(project, editor);
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
        if (hasMacroInType(psiElement)) {
            reportError(codeBlock, "Cannot introduce parameter because the selected type contains macro.");
            return;
        }
        executeCodeAction(editor, codeBlock, start, end);
    }

    @Override
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock,
                                     int start, int end) {
        if (!(EditorEventManagerBase.forEditor(editor) instanceof CangjieEditorEventManager manager)) {
            return;
        }

        List<Either<Command, CodeAction>> actions = manager.codeAction4Refactor(start, end);
        for (Either<Command, CodeAction> either : actions) {
            Command command = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (command != null && command.getTitle().startsWith(tweak)) {
                if (!checkCommand(codeBlock, command)) {
                    return;
                }
                handleIntroduceParameterCommand(codeBlock, manager, command);
                return;
            }
        }
        reportError(codeBlock, "No available introduce parameter refactoring for the selected expression.");
    }

    private void handleIntroduceParameterCommand(CangjieCodeBlock codeBlock, CangjieEditorEventManager manager,
                                                 Command command) {
        JsonObject extraOptions = new JsonObject();
        String parameterName = requestIdentifier(codeBlock.getProject(), "Introduce Parameter",
                "Parameter name:", "newParameter");
        if (parameterName == null) {
            return;
        }
        extraOptions.addProperty("suggestName", parameterName);

        if (command.getArguments() != null && !command.getArguments().isEmpty()) {
            JsonObject newProperty = JsonParser
                    .parseString(command.getArguments().get(0).toString())
                    .getAsJsonObject();

            newProperty.addProperty("suggestName", parameterName);
            newProperty.add("extraOptions", extraOptions);

            Map<?, ?> finalMap = new com.google.gson.Gson().fromJson(newProperty, Map.class);
            command.setArguments(new ArrayList<>(List.of(finalMap)));
        }

        executeRefactorCommandDirectly(manager, command);
    }

    private void executeRefactorCommandDirectly(CangjieEditorEventManager manager, Command command) {
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    /**
     * Checks the structural command feedback for server-side rule errors.
     *
     * @param codeBlock The active source file block wrapper.
     * @param command   The targeted refactoring command execution block.
     * @return True if the configuration validation is clear; false otherwise.
     */
    protected boolean checkCommand(CangjieCodeBlock codeBlock, Command command) {
        JsonObject payload = getCommandPayload(command);
        if (payload == null || !payload.has("ErrorCode")) {
            return true;
        }
        int errorCode = payload.get("ErrorCode").getAsInt();
        reportError(codeBlock, ERROR_CODE_MAP.getOrDefault(errorCode,
                "The selected expression cannot be refactored."));
        return false;
    }

    @Override
    protected void executeCommands(CangjieEditorEventManager manager, Command command) {
    }

    private void executeCommands(CangjieEditorEventManager manager, Command command, JsonObject extraOptions) {
        if (command.getArguments() == null || command.getArguments().isEmpty()) {
            return;
        }
        JsonObject payload = getCommandPayload(command);
        payload.add("extraOptions", extraOptions);
        command.setArguments(Collections.singletonList(payload));
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    private JsonObject getCommandPayload(Command command) {
        if (command.getArguments() == null || command.getArguments().isEmpty()) {
            return new JsonObject();
        }
        Object arg0 = command.getArguments().get(0);
        if (arg0 instanceof JsonObject) {
            return (JsonObject) arg0;
        }
        try {
            return JsonParser.parseString(arg0.toString()).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new JsonObject();
        }
    }

    private String requestIdentifier(Project project, String title, String message, String defaultValue) {
        while (true) {
            String value = Messages.showInputDialog(project, message, title, null, defaultValue, null);
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (CANGJIE_IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
            Messages.showErrorDialog(project, "Enter a valid Cangjie parameter.", title);
        }
    }
}