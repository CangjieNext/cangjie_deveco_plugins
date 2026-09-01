/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;

/**
 * Register the cangjie file change listener.
 *
 * @since 2024-01-14
 */
public class CangjieFileListenerRegisterSingleton {
    private CangjieFileListenerRegisterSingleton() {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES,
                new BulkVirtualFileListenerAdapter(new CangjieFileListener()));
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static CangjieFileListenerRegisterSingleton getInstance() {
        return ApplicationManager.getApplication().getService(CangjieFileListenerRegisterSingleton.class);
    }

    /**
     * Register vfs listener.
     */
    public static void registerVFSListener() {
        getInstance();
    }
}
