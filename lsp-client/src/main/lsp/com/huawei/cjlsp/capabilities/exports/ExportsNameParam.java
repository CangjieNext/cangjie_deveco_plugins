/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.exports;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * ExportsNameParam
 *
 * @since 2025/09/03
 */
public class ExportsNameParam {
    @NonNull
    private TextDocumentIdentifier textDocument;

    @NonNull
    private Position position;
    @NotNull
    private String packageName;

    public ExportsNameParam(@NonNull final TextDocumentIdentifier textDocument, @NonNull final Position position,
                            @NonNull String packageName) {
        this.textDocument = textDocument;
        this.position = position;
        this.packageName = packageName;
    }

    @NotNull
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(@NotNull String packageName) {
        this.packageName = packageName;
    }
}