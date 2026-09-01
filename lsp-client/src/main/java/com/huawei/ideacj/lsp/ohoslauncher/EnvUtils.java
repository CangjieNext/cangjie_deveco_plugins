/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.ohoslauncher;

import java.util.Map;

/**
 * EnvUtils
 *
 * @since 2024/03/23
 */
public class EnvUtils {
    /**
     * process builder
     */
    public static final ProcessBuilder BUILDER = new ProcessBuilder();

    /**
     * deveco cangjie home
     */
    public static final String DEVECO_CANGJIE_HOME = "DEVECO_CANGJIE_HOME";

    /**
     * add tmp env
     *
     * @param key env key
     * @param value env value
     */
    public static void addEnv(String key, String value) {
        BUILDER.environment().put(key, value);
    }

    /**
     * add tmp env
     *
     * @param envMap envs
     */
    public static void addAllEnv(Map<String, String> envMap) {
        if (envMap != null) {
            BUILDER.environment().putAll(envMap);
        }
    }
}
