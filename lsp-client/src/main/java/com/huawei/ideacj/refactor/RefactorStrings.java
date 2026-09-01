/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor;

import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.util.LocaleSensitiveApplicationCacheService;

import org.jetbrains.annotations.NotNull;

/**
 * RefactorStrings
 *
 * @since 2025-06-19
 */
public final class RefactorStrings {
    /**
     * Extract Variable
     */
    @Label
    public final String extractVariable = "Extract expression to variable";

    /**
     * Inline Variable
     */
    public final @Label
    String inlineVariable = "Inline variable";

    /**
     * Inline Function
     */
    public final @Label
    String inlineFunction = "Inline function";

    /**
     * Extract Interface
     */
    public final @Label
    String extractInterface = "Extract interface";

    /**
     * Introduce Field
     */
    public final @Label
    String introduceField = "Introduce Field";

    /**
     * Introduce Parameter
     */
    public final @Label
    String introduceParameter = "Introduce Parameter";

    private RefactorStrings() {}

    /**
     * getInstance
     *
     * @return RefactorStrings
     */
    @NotNull
    public static RefactorStrings getInstance() {
        return LocaleSensitiveApplicationCacheService.getInstance().getData(RefactorStrings.class,
                RefactorStrings::new);
    }
}
