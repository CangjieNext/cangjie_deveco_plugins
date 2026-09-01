/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.refactor.RefactorBaseHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;

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
 * CangjieExtractVariableHandler
 *
 * @since 2025-06-19
 */
public class CangjieExtractVariableHandler extends RefactorBaseHandler {
    private static final int TWEAK_FAIL = 0;
    private static final int ERROR_AST = 1;
    private static final int FAIL_GET_ROOT_EXPR = 2;
    private static final int FAIL_MATCH_EXPR = 3;
    private static final int INVALID_EXPR = 4;
    private static final int INVALID_CODE_SEGMENT = 5;
    private static final int CANNOT_EXTRACT = 6;

    private static final Map<Integer, String> errorCodeMap = new HashMap<>() {
        {
            put(TWEAK_FAIL, "The selected range cannot refactor.");
            put(ERROR_AST, "The selected range exist syntax error.");
            put(FAIL_GET_ROOT_EXPR, "The selected code is not an expression. Select an expression to extract.");
            put(FAIL_MATCH_EXPR, "Failed to find an expression match based on the selected code.");
            put(INVALID_EXPR, "The selected code contains an invalid expression.");
            put(INVALID_CODE_SEGMENT, "An Invalid code segment.");
            put(CANNOT_EXTRACT, "Cannot extract the selected code.");
        }
    };

    /**
     * CangjieExtractVariableHandler
     */
    public CangjieExtractVariableHandler() {
        setTweak("Extract expression to variable");
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
            if (cmd.getTitle().startsWith(tweak)) {
                if (!checkCommand(codeBlock, cmd)) {
                    return;
                }
                executeCommands(extendedManager, cmd);
                return;
            }
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
        if (command.getArguments() == null || command.getArguments().isEmpty()) {
            return;
        }
        JsonObject newName = new JsonObject();
        newName.addProperty("suggestName", getSuggestName());

        String rawArgsStr = command.getArguments().getFirst().toString().trim();
        if (rawArgsStr.isEmpty()) {
            return;
        }

        JsonObject newProperty = JsonParser.parseString(rawArgsStr).getAsJsonObject();

        newProperty.add("extraOptions", newName);

        Map<?, ?> finalMap = new Gson().fromJson(newProperty, Map.class);

        command.setArguments(new ArrayList<>(List.of(finalMap)));
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    private String getSuggestName() {
        return "newVariable";
    }

    /**
     * check Command and report extract method error
     *
     * @param codeBlock CangjieCodeBlock
     * @param command Command
     * @return is void
     */
    protected boolean checkCommand(CangjieCodeBlock codeBlock, Command command) {
        if (command.getArguments() == null || command.getArguments().isEmpty()
                || command.getArguments().get(0) == null) {
            return false;
        }
        JsonObject jsonObject = JsonParser.parseString(command.getArguments().get(0).toString()).getAsJsonObject();
        if (jsonObject.get("ErrorCode") != null) {
            Integer errCode = jsonObject.get("ErrorCode").getAsInt();
            reportError(codeBlock,
                    errorCodeMap.get(errCode) == null ? errorCodeMap.get(CANNOT_EXTRACT) : errorCodeMap.get(errCode));
            return false;
        }
        return true;
    }
}
