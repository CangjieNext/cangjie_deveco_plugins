/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage;

import org.jetbrains.annotations.NotNull;

/**
 * CrossLanguageDefinitionParam
 *
 * @since 2025/06/23
 */
public class CrossLanguageDefinitionParam {
    @NotNull
    private String packageName;

    @NotNull
    private String name;

    private String outerName;

    private boolean isCombined = false;

    public CrossLanguageDefinitionParam() {}

    public CrossLanguageDefinitionParam(@NotNull String packageName, @NotNull String name, String outerName,
                                        boolean isCombined) {
        this.packageName = packageName;
        this.name = name;
        this.outerName = outerName;
        this.isCombined = isCombined;
    }

    @NotNull
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(@NotNull String packageName) {
        this.packageName = packageName;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getOuterName() {
        return outerName;
    }

    public void setOuterName(String outerName) {
        this.outerName = outerName;
    }

    public boolean isCombined() {
        return isCombined;
    }

    public void setCombined(boolean combined) {
        isCombined = combined;
    }
}
