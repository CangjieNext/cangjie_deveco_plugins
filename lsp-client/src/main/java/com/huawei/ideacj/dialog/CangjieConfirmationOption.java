/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.dialog;

import com.huawei.ideacj.settings.CangjieLspSettings;

import com.intellij.openapi.vcs.VcsShowConfirmationOption;

/**
 * 持久化ConfirmDialog的Don't ask again
 *
 * @since 2025-12-10
 */
public class CangjieConfirmationOption implements VcsShowConfirmationOption {
    public CangjieConfirmationOption() {
    }

    @Override
    public Value getValue() {
        boolean ask = CangjieLspSettings.getInstance().isRemoveImportAsk();
        if (ask) {
            return Value.SHOW_CONFIRMATION;
        } else {
            return Value.DO_ACTION_SILENTLY;
        }
    }

    @Override
    public void setValue(Value value) {
        if (value == Value.DO_ACTION_SILENTLY) {
            CangjieLspSettings settings = CangjieLspSettings.getInstance();
            if (settings.getState() != null) {
                settings.getState().setRemoveImportAsk(false);
            }
        }
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
