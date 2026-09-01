/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import com.intellij.AbstractBundle;
import com.intellij.DynamicBundle;
import com.intellij.reference.SoftReference;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * LanguageProperties
 *
 * @since 2022-10-17
 */
public final class LanguageProperties {
    private static Reference<ResourceBundle> ourBundle;

    /**
     * message
     *
     * @param key key
     * @return String
     */
    public static String message(String key) {
        return AbstractBundle.message(getBundle(), key);
    }

    private static ResourceBundle getBundle() {
        String bundleName = DynamicBundle.getLocale().getLanguage();
        if (bundleName == null || bundleName.isEmpty()) {
            bundleName = "language.en";
        }
        ResourceBundle bundle = SoftReference.dereference(ourBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("language." + bundleName);
            ourBundle = new java.lang.ref.SoftReference<>(bundle);
        }
        return bundle;
    }
}