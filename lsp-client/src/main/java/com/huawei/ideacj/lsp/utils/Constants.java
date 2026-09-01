/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import java.util.Set;

/**
 * Constants
 *
 * @since 2024-03-11
 */
public class Constants {
    /**
     * PACKAGE
     */
    public static final String PACKAGE = "package";

    /**
     * Name
     */
    public static final String NAME = "name";

    /**
     * SRC_DIR
     */
    public static final String SRC_DIR = "src-dir";

    /**
     * SRC
     */
    public static final String SRC = "src";

    /**
     * PATH
     */
    public static final String PATH = "path";

    /**
     * GIT
     */
    public static final String GIT = "git";

    /**
     * COMMIT_ID
     */
    public static final String COMMIT_ID = "commitId";

    /**
     * DEPENDENCIES
     */
    public static final String DEPENDENCIES = "dependencies";

    /**
     * DEV_DEPENDENCIES
     */
    public static final String DEV_DEPENDENCIES = "dev-dependencies";

    /**
     * PACKAGE_REQUIRES
     */
    public static final String PACKAGE_REQUIRES = "package-requires";

    /**
     * PATH_OPTION
     */
    public static final String PATH_OPTION = "path-option";

    /**
     * PACKAGE_OPTION
     */
    public static final String PACKAGE_OPTION = "package-option";

    /**
     * TARGET
     */
    public static final String TARGET = "target";

    /**
     * BIN_DEPENDENCIES
     */
    public static final String BIN_DEPENDENCIES = "bin-dependencies";

    /**
     * FFI
     */
    public static final String FFI = "ffi";

    /**
     * C
     */
    public static final String C_REQUIRES = "c";

    /**
     * JAVA
     */
    public static final String JAVA = "java";

    /**
     * LSP_REQUIRES
     */
    public static final String LSP_REQUIRES = "requires";

    /**
     * LSP_PACKAGE_REQUIRES
     */
    public static final String LSP_PACKAGE_REQUIRES = "package_requires";

    /**
     * LSP_PATH_OPTION
     */
    public static final String LSP_PATH_OPTION = "path_option";

    /**
     * LSP_PACKAGE_OPTION
     */
    public static final String LSP_PACKAGE_OPTION = "package_option";

    /**
     * LSP_JAVA_REQUIRES
     */
    public static final String LSP_JAVA_REQUIRES = "java_requires";

    /**
     * LSP_JAVA_MODULES
     */
    public static final String LSP_JAVA_MODULES = "java_modules";

    /**
     * LSP_MODULE_NAME
     */
    public static final String LSP_MODULE_NAME = "module_name";

    /**
     * SPLIT_WINDOWS
     */
    public static final String SPLIT_WINDOWS = ";";

    /**
     * SPLIT_MAC
     */
    public static final String SPLIT_MAC = ":";

    /**
     * The Keyword withs space.
     */
    public static final Set<String> KEYWORD_WITH_SPACE = Set.of("class", "struct", "enum", "CFunc", "package",
            "import", "interface", "func", "macro", "let", "var", "const", "type", "if", "else", "case", "try",
            "catch", "finally", "for", "do", "while", "throw", "return", "in", "match", "from", "where", "extend",
            "prop", "static", "public", "private", "protected", "override", "redef", "abstract", "sealed", "open",
            "foreign", "inout", "mut", "unsafe", "spawn", "synchronized", "as", "is", "operator", "internal");

    /**
     * x86_64 target
     */
    public static final String X86_64_TARGET = "x86_64-unknown-windows-gnu";

    /**
     * COMPILE_OPTION
     */
    public static final String COMPILE_OPTION = "compile-option";

    /**
     * PROFILE
     */
    public static final String PROFILE = "profile";

    /**
     * BUILD
     */
    public static final String BUILD = "build";

    /**
     * COMBINED
     */
    public static final String COMBINED = "combined";

    /**
     * DYNAMIC
     */
    public static final String DYNAMIC = "dynamic";

    /**
     * CUSTOMIZED_OPTION
     */
    public static final String CUSTOMIZED_OPTION = "customized-option";

    /**
     * CONDITION_COMPILE_OPTION
     */
    public static final String CONDITION_COMPILE_OPTION = "conditionCompileOption";

    /**
     * CONDITION_COVERAGE
     */
    public static final String CONDITION_COVERAGE = "coverage";

    /**
     * CONDITION_PRODUCT
     */
    public static final String CONDITION_PRODUCT = "product";

    /**
     * CONDITION_TARGET
     */
    public static final String CONDITION_TARGET = "target";

    /**
     * BUILD_PROFILE_JSON
     */
    public static final String BUILD_PROFILE_JSON5 = "build-profile.json5";

    /**
     * CANGJIE_OPTIONS
     */
    public static final String CANGJIE_OPTIONS = "cangjieOptions";

    /**
     * ARGUMENTS
     */
    public static final String ARGUMENTS = "arguments";

    /**
     * MULTI_MODULE_OPTION
     */
    public static final String MULTI_MODULE_OPTION = "multiModuleOption";

    /**
     * REQUIRES_ENV_PATH
     */
    public static final String REQUIRES_ENV_PATH = "requiresEnvPath";

    /**
     * MODULE_CONDITION_COMPILE_OPTION
     */
    public static final String MODULE_CONDITION_COMPILE_OPTION = "moduleConditionCompileOption";

    /**
     * BUILD_OPTION
     */
    public static final String BUILD_OPTION = "buildOption";
}
