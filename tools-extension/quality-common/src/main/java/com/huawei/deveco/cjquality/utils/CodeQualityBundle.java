/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import com.intellij.DynamicBundle;
import com.intellij.ide.IdeDeprecatedMessagesBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * CodeQuality Bundle class
 *
 * @since 2024-04-25
 */
public class CodeQualityBundle extends DynamicBundle {
    private static final String BUNDLE = "messages.CodeQuality";

    private static final CodeQualityBundle INSTANCE = new CodeQualityBundle();

    private CodeQualityBundle() {
        super(BUNDLE);
    }

    /**
     * get CodeQuality bundle message
     *
     * @param key key
     * @param params param
     * @return string
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
        String @NotNull... params) {
        if (!INSTANCE.containsKey(key)) {
            return IdeDeprecatedMessagesBundle.message(key, params);
        }
        return INSTANCE.getMessage(key, params);
    }
}
