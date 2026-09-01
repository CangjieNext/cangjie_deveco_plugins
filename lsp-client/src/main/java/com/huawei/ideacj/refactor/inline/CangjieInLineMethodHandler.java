/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.inline;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.refactor.RefactorBaseHandler;
import com.huawei.ideacj.refactor.extract.CangjieCodeBlock;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CangjieInLineMethodHandler
 *
 * @since 2026-05-14
 */
public class CangjieInLineMethodHandler extends RefactorBaseHandler {
    private static final int TWEAK_FAIL = 0;
    private static final int ERROR_AST = 1;
    private static final int NOT_CALL_EXPR = 2;
    private static final int FUNC_DEC_NOT_FOUND = 3;
    private static final int IS_GENERIC_FUNC = 4;
    private static final int IS_EXTEND_FUNC = 5;
    private static final int IS_LAMBDA_FUNC = 6;
    private static final int IS_RECURSIVE_FUNC = 7;
    private static final int HAS_PRIVATE_ACCESS = 8;
    private static final int CANNOT_EXTRACT = 9;


    private static final Map<Integer, String> errorCodeMap = new HashMap<>() {
        {
            put(TWEAK_FAIL, "The selected range cannot refactor.");
            put(ERROR_AST, "The selected range exist syntax error.");
            put(NOT_CALL_EXPR, "The selected code is not an call expression. Select an call expression to inline.");
            put(FUNC_DEC_NOT_FOUND, "Failed to find the declaration of the selected call expression.");
            put(IS_GENERIC_FUNC, "The declaration of the selected call expression has generic types.");
            put(IS_EXTEND_FUNC, "The declaration of the selected call expression is extend method.");
            put(IS_LAMBDA_FUNC, "The declaration of the selected call expression is lambda method.");
            put(IS_RECURSIVE_FUNC, "The declaration of the selected call expression is recursive method.");
            put(HAS_PRIVATE_ACCESS, "The declaration of the selected call expression get private field.");
            put(CANNOT_EXTRACT, "Cannot extract the selected code.");
        }
    };

    public CangjieInLineMethodHandler() {
        setTweak("Inline function");
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
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock,
                                     int start, int end) throws ExecutionException {
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
        manager.executeCommands4Refactor(Collections.singletonList(command));
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
