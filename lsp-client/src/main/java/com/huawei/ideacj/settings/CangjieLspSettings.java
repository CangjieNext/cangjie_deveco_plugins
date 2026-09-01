/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * cangjie 持久化配置
 *
 * @since 2025-11-17
 */
@State(name = "CangjieLspSettings",
        storages = @Storage(value = "cangjie_lsp_settings.xml",
                roamingType = RoamingType.PER_OS)
)
public class CangjieLspSettings implements PersistentStateComponent<CangjieLspSettings.State> {
    private State state = new State();

    /**
     * 持久化State
     */
    public static class State {
        private boolean removeImportAsk = true;

        public boolean isRemoveImportAsk() {
            return removeImportAsk;
        }

        public void setRemoveImportAsk(boolean removeImportAsk) {
            this.removeImportAsk = removeImportAsk;
        }
    }

    /**
     * 获取配置文件实例
     *
     * @return 配置文件实例
     */
    public static CangjieLspSettings getInstance() {
        return ApplicationManager.getApplication().getService(CangjieLspSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isRemoveImportAsk() {
        return state.removeImportAsk;
    }
}