/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

import org.eclipse.lsp4j.jsonrpc.util.Preconditions;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Overridable Method Info
 *
 * @since 2025-07-01
 */
public class OverridableMethodInfo {
    @NonNull
    private final boolean deprecated;

    @NonNull
    private final boolean isProp;

    @NonNull
    private final String signatureWithRet;

    @NonNull
    private final String insertText;

    public OverridableMethodInfo(@NonNull final boolean deprecated, @NonNull final boolean isProp,
                                 @NonNull final String signature, @NonNull final String insertText) {
        this.deprecated = Preconditions.checkNotNull(deprecated, "deprecated");
        this.isProp = Preconditions.checkNotNull(isProp, "isProp");
        this.signatureWithRet = Preconditions.checkNotNull(signature, "signatureWithRet");
        this.insertText = Preconditions.checkNotNull(insertText, "insertText");
    }

    @NonNull
    public boolean getDeprecated() {
        return deprecated;
    }

    @NonNull
    public boolean getIsProp() {
        return isProp;
    }

    @NonNull
    public String getInsertText() {
        return insertText;
    }

    @NonNull
    public String getSignatureWithRet() {
        return signatureWithRet;
    }
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
