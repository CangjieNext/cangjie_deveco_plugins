/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionAndWorkDoneProgressAndPartialResultParams;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * OverrideMethods request param
 *
 * @since 2025-07-01
 */
public class OverrideMethodsParams extends TextDocumentPositionAndWorkDoneProgressAndPartialResultParams {
    @NonNull
    private boolean isExtend;

    public OverrideMethodsParams(@NonNull final TextDocumentIdentifier textDocument, @NonNull final Position position,
                                 @NonNull final boolean isExtend) {
        super(textDocument, position);
        this.isExtend = isExtend;
    }

    public boolean getIsExtend() {
        return isExtend;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
