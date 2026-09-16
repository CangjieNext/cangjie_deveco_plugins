/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.sdkmanager.listener;

import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.openapi.components.Service;

/**
 * Application service that owns the plugin state listener registration.
 *
 * @since 2026-09-05
 */
@Service
public final class CangjiePluginStateService {
    /**
     * Instantiates a new Cangjie plugin state service.
     */
    public CangjiePluginStateService() {
        PluginInstaller.addStateListener(new CangjiePluginUnloadListener());
    }
}
