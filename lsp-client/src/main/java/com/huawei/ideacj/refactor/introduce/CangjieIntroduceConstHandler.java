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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * CangjieIntroduceConstHandler
 *
 * @since 2025-07-20
 */
public class CangjieIntroduceConstHandler extends RefactorBaseHandler {
    private static final int TWEAK_FAIL = 0;
    private static final int ERROR_AST = 1;
    private static final int INVALID_EXPR = 2;
    private static final int INVALID_CONST_EXPR = 3;
    private static final int PARTIAL_SELECTION = 4;
    private static final int CANNOT_EXTRACT = 5;

    private static final Map<Integer, String> errorCodeMap = new HashMap<>() {{
        put(TWEAK_FAIL, "The selected range cannot refactor.");
        put(ERROR_AST, "The selected range exist syntax error.");
        put(INVALID_EXPR, "The selected code contains an invalid expression.");
        put(INVALID_CONST_EXPR, "Selected expression cannot be a constant initializer.");
        put(PARTIAL_SELECTION, "Some nodes are partially selected.");
        put(CANNOT_EXTRACT, "Cannot extract the selected code.");
    }};

    /**
     * CangjieIntroduceConstHandler
     */
    public CangjieIntroduceConstHandler() {
        setTweak("Introduce expression to constant variable");
    }

    @Override
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock,
                                     int start, int end) {
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager cangjieEditorEventManager)) {
            reportError(codeBlock, "Manager is null");
            return;
        }
        List<Either<Command, CodeAction>> res = cangjieEditorEventManager.codeAction4Refactor(start, end);
        for (Either<Command, CodeAction> either : res) {
            Command cmd = either.isLeft() ? either.getLeft() : either.getRight().getCommand();
            if (cmd.getTitle().startsWith(tweak)) {
                if (!checkCommand(codeBlock, cmd)) {
                    return;
                }
                executeCommands(cangjieEditorEventManager, cmd);
                return;
            }
        }
        reportError(codeBlock, this.tweak + " failed. " + warningMessage);
    }

    @Override
    protected void executeCommands(CangjieEditorEventManager manager, Command command) {
        FileDocumentManager.getInstance().saveAllDocuments();
        JsonObject newName = new JsonObject();
        newName.addProperty("suggestName", getSuggestName());
        if (command.getArguments().isEmpty()) {
            return;
        }
        Object object = command.getArguments().get(0);
        if (!(object instanceof JsonObject newProperty)) {
            return;
        }
        newProperty.add("extraOptions", newName);
        command.setArguments(new ArrayList<>(List.of(newProperty)));
        manager.executeCommands4Refactor(Collections.singletonList(command));
    }

    private String getSuggestName() {
        return "constVar";
    }

    /**
     * check Command and report introduce constant error
     *
     * @param codeBlock CangjieCodeBlock
     * @param command Command
     * @return boolean
     */
    protected boolean checkCommand(CangjieCodeBlock codeBlock, Command command) {
        if (command.getArguments().isEmpty()) {
            return false;
        }
        JsonObject jsonObject = JsonParser.parseString(command.getArguments().get(0).toString()).getAsJsonObject();
        if (jsonObject.get("ErrorCode") != null) {
            Integer errCode = jsonObject.get("ErrorCode").getAsInt();
            reportError(codeBlock, errorCodeMap.get(errCode) == null
                    ? errorCodeMap.get(CANNOT_EXTRACT) : errorCodeMap.get(errCode));
            return false;
        }
        return true;
    }
}
