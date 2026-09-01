/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import com.huawei.ideacj.filetypes.TomlFile;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.wm.IdeFrame;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Toml application activation registrar.
 *
 * @since 2026-06-01
 */
public class TomlRegistrarListener implements ApplicationActivationListener {
    private final AtomicBoolean registered = new AtomicBoolean(false);

    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        if (registered.compareAndSet(false, true)) {
            registerToml();
        }
    }

    private void registerToml() {
        String tomlExtension = "toml";
        String tomlLockExtension = "lock";
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType existingType = fileTypeManager.getFileTypeByExtension(tomlExtension);
        if (!(existingType instanceof UnknownFileType)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                if (!(FileTypeManager.getInstance() instanceof FileTypeManagerImpl manager)) {
                    return;
                }
                if (!(manager.getFileTypeByExtension(tomlExtension) instanceof UnknownFileType)) {
                    return;
                }
                manager.associateExtension(TomlFile.INSTANCE, tomlExtension);
                manager.associateExtension(TomlFile.INSTANCE, tomlLockExtension);
            });
        });
    }
}