/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.xtext.xbase.lib.Pure;

import java.util.List;

/**
 * Clangd Diagnostics notification are sent from the server to the client to signal results of validation runs.
 *
 * @since 2024-12-18
 */
public class ExtendDiagnostic extends Diagnostic {
    private List<CodeAction> codeActions;

    @Pure
    public List<CodeAction> getCodeActions() {
        return codeActions;
    }

    public void setCodeActions(List<CodeAction> codeActions) {
        this.codeActions = codeActions;
    }
}
