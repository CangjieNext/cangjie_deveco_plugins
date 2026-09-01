/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.isCangjieModule;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * module utils
 *
 * @since 2024-7-30
 */
public class ModuleUtils {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(ModuleUtils.class);

    /**
     * has cangjie module
     *
     * @param project project
     * @return has cangjie module
     */
    public static boolean hasCangjieModule(Project project) {
        ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
        if (projectModel == null) {
            LOGGER.warn("ProjectModel is null.");
            return false;
        }
        List<ModuleModel> modules = projectModel.getModuleModelList();
        boolean hasCangjieModule = false;
        for (ModuleModel moduleModel : modules) {
            if (isCangjieModuleModel(moduleModel)) {
                hasCangjieModule = true;
            }
        }
        return hasCangjieModule;
    }

    /**
     * is cangjie module
     *
     * @param moduleModel module model
     * @return true or false
     */
    public static boolean isCangjieModuleModel(ModuleModel moduleModel) {
        return moduleModel instanceof OhosModuleModel && isCangjieModule(moduleModel);
    }

    /**
     * get module from filepath
     *
     * @param filePath file path
     * @param project project
     * @return module
     */
    public static ModuleModel getModuleFromFile(String filePath, @NotNull Project project) {
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(filePath))
            .map(vf -> com.huawei.deveco.res.ohos.utils.ModuleUtils.findModuleModelByVirtualFile(project, vf))
            .orElse(null);
    }
}
