/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * compile command singleton
 *
 * @author rice
 * @since 2020-06-08
 */
public class CompileCommandJsonSingleton {
    private static final Logger LOG = Logger.getInstance(CompileCommandJsonSingleton.class);

    private final String projectBasePath;

    private CompileCommandJsonSingleton(Project project) {
        this.projectBasePath = Optional.ofNullable(project.getBasePath()).orElse("");
    }

    /**
     * using project service to ask IntelliJ Platform to ensure that only one instance of a service is loaded
     * even though the service is called several times
     *
     * @param project the project
     * @return the Singleton related to the project
     */
    public static CompileCommandJsonSingleton getInstance(@NotNull Project project) {
        return project.getService(CompileCommandJsonSingleton.class);
    }

    /**
     * create command_compile.json for module
     *
     * @param project a project
     * @param moduleModel a moduleModel
     */
    public synchronized void createCompileDatabase(Project project, ModuleModel moduleModel) {
        DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            try {
                if (projectBasePath == null || project.isDisposed()) {
                    LOG.info("project base path is null or project is disposed, can't createCompileDatabase");
                    return;
                }
                List<File> allCMakeFiles = getAllCMakeFile(projectBasePath);
                if (allCMakeFiles.isEmpty()) {
                    LspConfigUtils.sendNotification(project, CangjieBundle.message("deveco.cmake.list.not.exist"));
                }
                // if there is no json file, also add lsp config
                CangjieLspConfiguration.addServerDefinitionForClient(project);
            } catch (IOException ex) {
                LOG.error(ex);
            }
        });
    }

    private List<File> getAllCMakeFile(@NotNull String projectBasePath) throws IOException {
        List<File> cmakeFiles = new ArrayList<>();
        File root = new File(projectBasePath);
        File[] listOfFiles = root.listFiles();
        if (listOfFiles == null) {
            return new ArrayList<>();
        }
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isDirectory()) {
                cmakeFiles.addAll(getAllCMakeFile(listOfFile.getCanonicalPath()));
            }
            if (listOfFile.isFile() && "CMakeLists.txt".equals(listOfFile.getName())) {
                cmakeFiles.add(listOfFile);
            }
        }
        return cmakeFiles;
    }
}