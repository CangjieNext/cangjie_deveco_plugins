/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * CangjieBundle
 *
 * @author rice
 * @since 2019 -09-02
 */
public class CangjieBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "i18n.CharBundle";

    private static CangjieBundle instance = null;

    private CangjieBundle() {
        super(BUNDLE);
    }

    /**
     * get message bundle singleton instance
     *
     * @return message bundle singleton instance
     */
    public static synchronized CangjieBundle getSingletonInstance() {
        if (instance == null) {
            instance = new CangjieBundle();
        }
        return instance;
    }

    /**
     * Message string.
     *
     * @param key message key
     * @return the string
     */
    public static String message(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key) {
        return getSingletonInstance().getMessage(key);
    }

    /**
     * Message string.
     *
     * @param key message key
     * @param params message params
     * @return the string
     */
    public static String message(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull String... params) {
        return getSingletonInstance().getMessage(key, params);
    }
}