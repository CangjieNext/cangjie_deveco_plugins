/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.trace.TraceAdapterImpl;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.find.FindSettings;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.xml.breadcrumbs.BreadcrumbsForceShownSettings;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * after gradle sync , setup lsp config
 *
 * @author SonicNing
 * @since 2019-09-24
 */
public class PostSyncLspStartUp implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // 注册打点实例
        TraceUtils.registerTraceAdapter(new TraceAdapterImpl());

        EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
        if (settings == null) {
            return;
        }
        settings.setBreadcrumbsShownFor(CangJieLanguage.INSTANCE.getID(), true);
        for (Editor editor : LspConfigUtils.getAllOpenEditors(project)) {
            if (editor != null && BreadcrumbsForceShownSettings.setForcedShown(true, editor)) {
                UISettings.getInstance().fireUISettingsChanged();
            }
        }

        FindSettings findSettings = FindSettings.getInstance();
        String[] recentFileMasks = findSettings.getRecentFileMasks();
        for (String recentFileMask : recentFileMasks) {
            if (Objects.equals(recentFileMask, "*.cj")) {
                return;
            } else {
                findSettings.setFileMask("*.cj");
            }
        }
        findSettings.setFileMask(recentFileMasks[0]);
        findSettings.setFileMask(null);
    }
}
