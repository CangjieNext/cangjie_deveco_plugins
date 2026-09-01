/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cangjie persistent component
 *
 * @author rice
 * @since 2020-06-08
 */
@State(name = "CHAR_LSP_SETTINGS", storages = {
        @Storage(value = "CHAR_LSP_SETTINGS.xml", roamingType = RoamingType.DISABLED)
})
public class CangjieLspPersistentComponent implements PersistentStateComponent<CangjieLspPersistentComponent> {
    private String path = "";

    /**
     * singleton
     *
     * @param project the project
     * @return CangjieLspPersistentComponent
     */
    public static CangjieLspPersistentComponent getInstance(Project project) {
        return project.getService(CangjieLspPersistentComponent.class);
    }

    @Override
    @Nullable
    public CangjieLspPersistentComponent getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CangjieLspPersistentComponent state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}