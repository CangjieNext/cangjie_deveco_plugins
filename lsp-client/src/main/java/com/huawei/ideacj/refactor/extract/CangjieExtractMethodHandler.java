/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.refactor.RefactorBaseHandler;
import com.huawei.ideacj.refactor.extract.dialog.CangjieExtractFunctionDialog;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CangjieExtractMethodHandler
 *
 * @since 2025-06-10
 */
public class CangjieExtractMethodHandler extends RefactorBaseHandler {
    private static final int TWEAK_FAIL = 0;
    private static final int ERROR_AST = 1;
    private static final int INVALID_CODE_SEGMENT = 2;
    private static final int PARTIAL_SELECTION = 3;
    private static final int MULTI_RETURN_VALUE = 4;
    private static final int PARTIAL_IF_EXPR = 5;
    private static final int PARTIAL_JUMP_EXPR = 6;
    private static final int MULTI_EXIT_POINT = 7;
    private static final int GLOBAL_MEMBER_VAR = 8;
    private static final int CONTAIN_MEMBER_VAR_INIT = 9;
    private static final int PARTIAL_TRY_EXPR = 10;
    private static final int PARTIAL_MATCH_EXPR = 11;
    private static final int CANNOT_EXTRACT = 12;

    private static final Map<Integer, String> errorCodeMap = new HashMap<>() {
        {
            put(TWEAK_FAIL, "The selected range cannot refactor.");
            put(ERROR_AST, "The selected range exist syntax error.");
            put(INVALID_CODE_SEGMENT, "An Invalid code segment.");
            put(PARTIAL_SELECTION, "Some nodes are partially selected.");
            put(MULTI_RETURN_VALUE, "Selected range exist multiple return values.");
            put(PARTIAL_IF_EXPR, "The if expression is partially selected.");
            put(PARTIAL_JUMP_EXPR, "The jump expression need its loop expression be completed.");
            put(MULTI_EXIT_POINT, "The selected range exist multiple exit points.");
            put(GLOBAL_MEMBER_VAR, "Cannot extract function of global variable or member variable.");
            put(CONTAIN_MEMBER_VAR_INIT, "Cannot extract function because of contain member variable initialize.");
            put(PARTIAL_TRY_EXPR, "The try expression is partially selected.");
            put(PARTIAL_MATCH_EXPR, "The match expression is partially selected.");
            put(CANNOT_EXTRACT, "Cannot extract the selected code.");
        }
    };

    /**
     * CangjieExtractMethodHandler
     */
    public CangjieExtractMethodHandler() {
        this.tweak = "Extract to function";
    }

    /**
     * executeCodeAction
     *
     * @param editor editor
     * @param codeBlock codeBlock
     * @param start start
     * @param end end
     * @throws ExecutionException exception
     */
    @Override
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock, int start, int end)
            throws ExecutionException {
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager)) {
            reportError(codeBlock, "Manager is null");
            return;
        }
        CangjieEditorEventManager extendedManager = (CangjieEditorEventManager) manager;
        List<Either<Command, CodeAction>> res = extendedManager.codeAction4Refactor(start, end);
        for (Either<Command, CodeAction> either : res) {
            Command cmd = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (!cmd.getTitle().startsWith(tweak)) {
                continue;
            }
            if (!checkCommand(codeBlock, cmd)) {
                return;
            }
            executeCommands(extendedManager, cmd);
            return;
        }
        reportError(codeBlock, this.tweak + " failed. " + warningMessage);
    }

    /**
     * executeCommands
     *
     * @param manager manager
     * @param command command
     */
    @Override
    protected void executeCommands(CangjieEditorEventManager manager, Command command) {
        FileDocumentManager.getInstance().saveAllDocuments();
        customizedFuncName(manager, command);
    }

    private void customizedFuncName(CangjieEditorEventManager manager, Command command) {
        Project project = manager.getProject();
        CangjieExtractFunctionDialog dialog = new CangjieExtractFunctionDialog(project,
                CangjieBundle.message("lsp.refactor.extract.method.dialog.title"));
        dialog.show();
        if (!dialog.isDoOK()) {
            return;
        }
        if (command.getArguments() == null || command.getArguments().isEmpty()) {
            return;
        }

        String rawArgsStr = command.getArguments().getFirst().toString().trim();
        if (rawArgsStr.isEmpty()) {
            return;
        }

        JsonObject jsonObject = JsonParser.parseString(rawArgsStr).getAsJsonObject();

        JsonObject newName = new JsonObject();
        newName.addProperty("functionName", dialog.getFunctionName());
        jsonObject.add("extraOptions", newName);

        Map<?, ?> finalMap = new Gson().fromJson(jsonObject, Map.class);

        command.setArguments(new ArrayList<>(List.of(finalMap)));
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    private boolean checkCommand(CangjieCodeBlock codeBlock, Command command) {
        if (command.getArguments() == null || command.getArguments().isEmpty()
                || command.getArguments().get(0) == null) {
            return false;
        }
        if (command.getArguments().isEmpty()) {
            return false;
        }
        JsonObject jsonObject = JsonParser.parseString(command.getArguments().get(0).toString()).getAsJsonObject();
        if (jsonObject.get("ErrorCode") == null) {
            return true;
        }
        Integer errCode = jsonObject.get("ErrorCode").getAsInt();
        reportError(codeBlock,
                errorCodeMap.get(errCode) == null ? errorCodeMap.get(CANNOT_EXTRACT) : errorCodeMap.get(errCode));
        return false;
    }
}
