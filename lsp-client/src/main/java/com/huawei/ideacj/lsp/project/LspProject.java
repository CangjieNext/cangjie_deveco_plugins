/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.project;

import com.intellij.openapi.project.Project;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * LspProject
 *
 * @since 2024/03/13
 */
public class LspProject {
    private final Project myProject;

    private final Map<String, Map<String, Object>> modulesInfos;

    private final StringBuilder requiresEnvPath;

    private final Set<String> uriSet;

    private Path rootModule;

    public LspProject(Project myProject) {
        this.modulesInfos = new HashMap<>();
        this.myProject = myProject;
        this.requiresEnvPath = new StringBuilder();
        this.uriSet = new HashSet<>();
    }

    public Project getMyProject() {
        return myProject;
    }

    public Map<String, Map<String, Object>> getModulesInfos() {
        return modulesInfos;
    }

    public StringBuilder getRequiresEnvPath() {
        return requiresEnvPath;
    }

    public Set<String> getUriSet() {
        return uriSet;
    }

    public Path getRootModule() {
        return rootModule;
    }

    public void setRootModule(Path rootModule) {
        this.rootModule = rootModule;
    }
}
