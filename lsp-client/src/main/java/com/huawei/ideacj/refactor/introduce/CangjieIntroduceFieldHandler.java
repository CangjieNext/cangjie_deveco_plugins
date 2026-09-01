/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.introduce;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.refactor.RefactorBaseHandler;
import com.huawei.ideacj.refactor.extract.CangjieCodeBlock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.actionSystem.DataContext;
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
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CangjieIntroduceFieldHandler
 *
 * @since 2026-05-12
 */
public class CangjieIntroduceFieldHandler extends RefactorBaseHandler {
    private static final Pattern CANGJIE_IDENTIFIER_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private static final int ERROR_FAIL_GET_ROOT_EXPR = 2;
    private static final int ERROR_INVALID_EXPR = 3;
    private static final int ERROR_INVALID_CODE_SEGMENT = 4;
    private static final int ERROR_INVALID_SCOPE = 5;
    private static final int ERROR_INVALID_TYPE = 6;
    private static final int ERROR_INVALID_CONST_INITIALIZER = 7;
    private static final int ERROR_INVALID_LET_PATTERN_DESTRUCTOR = 8;
    private static final int ERROR_IMMUTABLE_STRUCT_MEMBER_FIELD_ASSIGNMENT = 9;
    private static final int ERROR_INVALID_COMPOUND_ASSIGNMENT = 10;
    private static final int ERROR_MEMBER_ASSIGN_IN_CONSTRUCTOR = 11;

    private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<>() {
        {
            put(ERROR_FAIL_GET_ROOT_EXPR, "Select a complete expression to refactor.");
            put(ERROR_INVALID_EXPR, "The selected range is not a valid expression.");
            put(ERROR_INVALID_CODE_SEGMENT, "The selected expression cannot be refactored.");
            put(ERROR_INVALID_SCOPE, "Introduce field is only supported inside class or struct member functions.");
            put(ERROR_INVALID_TYPE, "Cannot infer the selected expression type.");
            put(ERROR_INVALID_CONST_INITIALIZER,
                    "Cannot introduce field from a const initializer because it must remain a compile-time "
                    + "constant expression.");
            put(ERROR_INVALID_LET_PATTERN_DESTRUCTOR,
                    "Cannot introduce field from a let pattern condition because it is not a standalone expression.");
            put(ERROR_IMMUTABLE_STRUCT_MEMBER_FIELD_ASSIGNMENT, "Cannot introduce field in immutable struct function.");
            put(ERROR_INVALID_COMPOUND_ASSIGNMENT, "Cannot introduce field from a compound assignment expression.");
            put(ERROR_MEMBER_ASSIGN_IN_CONSTRUCTOR,
                    "Cannot introduce field because selected is a member-assign expression in constructor.");
        }
    };

    public CangjieIntroduceFieldHandler() {
        setTweak("Introduce expression to field");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
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
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, @Nullable DataContext dataContext) {
        if (file == null || editor == null) {
            return;
        }
        CangjieCodeBlock codeBlock = new CangjieCodeBlock(project, editor);
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
        if (hasMacroInType(psiElement)) {
            reportError(codeBlock,
                "Cannot introduce field because the selected type contains macro.");
            return;
        }
        executeCodeAction(editor, codeBlock, start, end);
    }

    @Override
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock, int start, int end) {
        EditorEventManager editorEventManager = EditorEventManagerBase.forEditor(editor);
        CangjieEditorEventManager manager = null;
        if (editorEventManager instanceof CangjieEditorEventManager) {
            manager = (CangjieEditorEventManager) editorEventManager;
        }
        if (manager == null) {
            return;
        }

        List<Either<Command, CodeAction>> actions = manager.codeAction4Refactor(start, end);
        for (Either<Command, CodeAction> either : actions) {
            Command command = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (command != null && command.getTitle().startsWith(tweak)) {
                if (!checkCommand(codeBlock, command)) {
                    return;
                }
                handleIntroduceFieldCommand(codeBlock, manager, command);
                return;
            }
        }
        reportError(codeBlock, "No available introduce field refactoring for the selected expression.");
    }

    private void handleIntroduceFieldCommand(CangjieCodeBlock codeBlock, CangjieEditorEventManager manager,
            Command command) {
        JsonObject extraOptions = new JsonObject();
        String fieldName = requestIdentifier(codeBlock.getProject(), "Introduce Field", "Field name:", "newField");
        if (fieldName == null) {
            return;
        }
        extraOptions.addProperty("suggestName", fieldName);

        if (command.getArguments() != null && !command.getArguments().isEmpty()) {
            JsonObject newProperty = JsonParser.parseString(command.getArguments().get(0).toString()).getAsJsonObject();

            newProperty.addProperty("suggestName", fieldName);
            newProperty.add("extraOptions", extraOptions);

            Map<?, ?> finalMap = new Gson().fromJson(newProperty, Map.class);
            command.setArguments(new ArrayList<>(List.of(finalMap)));
        }

        executeRefactorCommandDirectly(manager, command);
    }

    private void executeRefactorCommandDirectly(CangjieEditorEventManager manager, Command command) {
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    private boolean checkCommand(CangjieCodeBlock codeBlock, Command command) {
        JsonObject payload = getCommandPayload(command);
        if (payload == null || !payload.has("ErrorCode")) {
            return true;
        }
        int errorCode = payload.get("ErrorCode").getAsInt();
        reportError(codeBlock, ERROR_CODE_MAP.getOrDefault(errorCode, "The selected expression cannot be refactored."));
        return false;
    }

    @Override
    protected void executeCommands(CangjieEditorEventManager manager, Command command) {
    }

    private JsonObject getCommandPayload(Command command) {
        if (command.getArguments() == null || command.getArguments().isEmpty()) {
            return new JsonObject();
        }
        Object arg0 = command.getArguments().getFirst();
        return (arg0 instanceof JsonObject)
                ? (JsonObject) arg0
                : JsonParser.parseString(arg0.toString()).getAsJsonObject();
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
            Messages.showErrorDialog(project, "Enter a valid Cangjie Field.", title);
        }
    }
}
