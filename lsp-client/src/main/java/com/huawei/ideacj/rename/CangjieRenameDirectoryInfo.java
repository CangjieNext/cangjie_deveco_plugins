/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;

import java.nio.file.Path;

/**
 * The type Cangjie rename directory info.
 *
 * @since 2025-08-21
 */
public class CangjieRenameDirectoryInfo {
    private OhosModuleModel belongModuleModel;

    private Path rootPath;

    private String rootPackageName;

    /**
     * Gets belong module model.
     *
     * @return the belong module model
     */
    public OhosModuleModel getBelongModuleModel() {
        return belongModuleModel;
    }

    /**
     * Sets belong module model.
     *
     * @param belongModuleModel the belong module model
     */
    public void setBelongModuleModel(OhosModuleModel belongModuleModel) {
        this.belongModuleModel = belongModuleModel;
    }

    /**
     * Gets root path.
     *
     * @return the root path
     */
    public Path getRootPath() {
        return rootPath;
    }

    /**
     * Sets root path.
     *
     * @param rootPath the root path
     */
    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Gets root package name.
     *
     * @return the root package name
     */
    public String getRootPackageName() {
        return rootPackageName;
    }

    /**
     * Sets root package name.
     *
     * @param rootPackageName the root package name
     */
    public void setRootPackageName(String rootPackageName) {
        this.rootPackageName = rootPackageName;
    }
}
