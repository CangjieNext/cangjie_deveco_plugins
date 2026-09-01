/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.documentlink;

import com.huawei.ideacj.edit.CangjieEditorEventManager;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

/**
 * implents a file listener about file selection change,
 * when changed, send a documentlink to get a diagnostic information
 *
 * @author taoye
 * @since 2021-04-27
 */
public class CangjieDocumentLink implements FileEditorManagerListener {
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        Editor editor = event.getManager().getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        CangjieEditorEventManager manager = null;
        if (EditorEventManagerBase.forEditor(editor) instanceof CangjieEditorEventManager) {
            manager = (CangjieEditorEventManager) EditorEventManagerBase.forEditor(editor);
        }
        if (manager == null) {
            return;
        }
        manager.documentLink();
    }
}
