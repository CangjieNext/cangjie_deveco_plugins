/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjiePackageUtils
 *
 * @since 2025/11/01
 */
public class CangjiePackageUtils {
    /**
     * cangjie file package name pattern
     */
    public static final Pattern PACKAGE_DECLARATION_PATTERN = Pattern.compile(
        "^\\s*package\\s+([a-zA-Z_][a-zA-Z0-9_.]*)\\s*;?\\s*$", Pattern.MULTILINE);

    /**
     * extract package name
     *
     * @param sourceCode code content
     * @return package name
     */
    public static Optional<String> extractPackageName(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = PACKAGE_DECLARATION_PATTERN.matcher(sourceCode);
        if (matcher.find()) {
            String packageName = matcher.group(1).trim();
            return Optional.of(packageName);
        }
        return Optional.empty();
    }
}
