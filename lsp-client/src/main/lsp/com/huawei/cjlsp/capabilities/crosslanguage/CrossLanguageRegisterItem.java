/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.crosslanguage;

import org.eclipse.lsp4j.Location;

/**
 * CrossLanguageRegisterItem
 *
 * @since 2025/09/03
 */
public class CrossLanguageRegisterItem {
    private Location definition;

    private Location declaration;

    private String registerName;

    private int registerType;

    public CrossLanguageRegisterItem() {
    }

    public CrossLanguageRegisterItem(Location definition, Location declaration, String registerName, int registerType) {
        this.definition = definition;
        this.declaration = declaration;
        this.registerName = registerName;
        this.registerType = registerType;
    }

    /**
     * register function definition position
     *
     * @return register function target position
     */
    public Location getDefinition() {
        return this.definition;
    }

    /**
     * register function declaration position
     *
     * @return register function declaration position
     */
    public Location getDeclaration() {
        return this.declaration;
    }

    /**
     * register function Name
     *
     * @return register function name
     */
    public String getRegisterName() {
        return this.registerName;
    }

    /**
     * function registerType
     *
     * @return register type
     */
    public int getRegisterType() {
        return this.registerType;
    }
}
