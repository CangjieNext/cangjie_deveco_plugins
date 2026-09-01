/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.isCangjieProject;
import static com.huawei.ideacj.lsp.utils.CommonUtils.renameLspBuildPath;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.removeEditorListeners;
import static com.huawei.ideacj.lsp.utils.PathConstants.CANGJIE_CACHE_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJD_CACHE_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DEPENDENCY_FILE;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_DEVECO_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_IDEA_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.LOG_DIR;
import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;
import static org.wso2.lsp4intellij.utils.FileUtils.getAllOpenedEditors;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.build.ohos.api.CompileBuildListener;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.listener.CangjieStartServerListener;
import com.huawei.ideacj.lsp.ohoslauncher.CJModuleParser;
import com.huawei.ideacj.lsp.ohoslauncher.EnvUtils;
import com.huawei.ideacj.lsp.utils.CommonUtils;
import com.huawei.ideacj.lsp.utils.LSPThreadPoolManager;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.lsp.utils.PathConstants;
import com.huawei.ideacj.notification.CangjieEditorNotificationProvider;
import com.huawei.ideacj.notification.NotificationUtil;
import com.huawei.ideacj.trace.TraceUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used to control server start or restart process
 *
 * @since 2025-03-14
 */
public class CangjieCompileBuildListener implements CompileBuildListener {
    private static final Logger LOG = Logger.getInstance(CangjieCompileBuildListener.class);

    private Project project;

    private Map<Path, ChangeDetectionContext> allFolderChangeDetectors;

    @Override
    public void buildStart(@NotNull Project project) {
        allFolderChangeDetectors = new HashMap<>();
        List<Path> folderPaths = getModuleBuildPaths(project);
        if (CollectionUtils.isEmpty(folderPaths)) {
            return;
        }
        for (Path targetFolder : folderPaths) {
            if (!Files.isDirectory(targetFolder)) {
                continue;
            }
            allFolderChangeDetectors.put(targetFolder, new ChangeDetectionContext(targetFolder));
        }
    }

    @Override
    public void buildEnd(@NotNull Project project, boolean buildResult) {
        this.project = project;
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (!isCangjieProject(projectModel)) {
            return;
        }
        LanguageServerWrapper curWrapper = getServerWrappersFor("cj", FileUtils.projectToUri(project));
        if (curWrapper != null) {
            // check server status and details.json to decide restart or not
            if (!ServerStatus.INITIALIZED.equals(curWrapper.getStatus())) {
                return;
            }
            if (!needRestart() && !needRestartWhenBuildFailed(buildResult, project)) {
                return;
            }
            restart(curWrapper);
        } else {
            // start without details.json check
            startFirst();
        }
        VirtualFile changedFile = CangjieEditorNotificationProvider.getChangedFile();
        if (changedFile != null && CangjieEditorNotificationProvider.getIsFileChange()) {
            changedFile.refresh(false, false);
            CangjieEditorNotificationProvider.setIsFileChange(false);
            CangjieEditorNotificationProvider.setChangedFile(null);
            EditorNotifications.getInstance(project).updateAllNotifications();
        }
    }

    /**
     * pre start lsp action
     *
     * @param project project
     */
    public static void preStartLsp(Project project) {
        try {
            initEnv(project);
            prepare(project);
        } catch (IOException e) {
            LOG.error("Pre start action lsp failed.");
        }
    }

    private boolean needRestart() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return false;
        }
        // 检查details.json是否存在
        String targetBuildFileUrl =
            Path.of(basePath, ".hvigor", "outputs", "logs", "details", "details.json").toFile()
                .getAbsolutePath().replaceAll("\\\\", "/");
        File file = new File(targetBuildFileUrl);
        if (!file.exists()) {
            return false;
        }
        // 检查字段
        try (FileReader reader = new FileReader(targetBuildFileUrl)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("HVIGOR")) {
                return false;
            }
            JsonObject hvigor = jsonObject.getAsJsonObject("HVIGOR");
            if (!hvigor.has("TASK_TIME") || hvigor.has("ERROR_MESSAGE")) {
                return false;
            }
            JsonObject taskTime = hvigor.getAsJsonObject("TASK_TIME");
            for (String key : taskTime.keySet()) {
                if ("APP".equals(key)) {
                    continue;
                }
                JsonObject module = taskTime.getAsJsonObject(key);
                boolean restart = module.has("GenerateCangjieResource") || module.has("CompileCangjie") || module.has(
                    "GenerateApiDependencies") || module.has("GenerateTomlDependencies");
                if (restart) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOG.error("Can't read details.json", e);
        }
        return false;
    }

    private void startFirst() {
        CangjieStartServerListener.initProjectCrashCount(project);
        preStartLsp(project);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Sync for cangjie project") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    CangjieLspConfiguration.addServerDefinitionForClient(project);
                    TraceUtils.trace(TraceUtils.Action.LSP_START);
                } catch (IOException e) {
                    LOG.error("Add ServerDefinition failed when sync project.");
                }
            }
        });
    }

    private void restart(LanguageServerWrapper wrapper) {
        if (!(wrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            return;
        }
        requestManager.setRestarting(true);
        removeEditorListeners(wrapper);
        List<Editor> allOpenedEditors = getAllOpenedEditors(project);
        allOpenedEditors.forEach(IntellijLanguageClient::editorClosed);
        wrapper.stop(true);

        Path lspBuildTempPath = renameLspBuildPath(project);
        preStartLsp(project);
        ExtendRequestManager finalRequestManager = requestManager;
        LSPThreadPoolManager.pool(() -> {
            IntellijLanguageClient.initProjectConnections(project);
            finalRequestManager.setRestarting(false);
        });
        CommonUtils.tryDeleteDirectory(lspBuildTempPath);
    }

    private static void initEnv(@NotNull Project project) {
        EnvUtils.addAllEnv(CangjieEnvUtils.getProjectEnvs(project));
    }

    private static void prepare(@NotNull Project project) throws IOException {
        File ideaDir = new File(project.getBasePath(), DOT_IDEA_DIR);
        boolean isIdeaCreated = ideaDir.mkdir();
        if (!isIdeaCreated) {
            LOG.info("Failed to create the idea folder.");
        }
        File devecoDir = new File(ideaDir, DOT_DEVECO_DIR);
        File cangjieDir = new File(devecoDir, CANGJIE_CACHE_DIR);
        File logDir = new File(cangjieDir, LOG_DIR);
        File cjdCacheDir = new File(PathManager.getSystemPath(), CJD_CACHE_DIR);
        boolean isCreated = devecoDir.mkdir();
        if (!isCreated) {
            LOG.info("Failed to create the deveco folder.");
        }
        isCreated = cangjieDir.mkdir();
        if (!isCreated) {
            LOG.info("Failed to create the cangjie folder.");
        }
        String enableCangjieLog = String.valueOf(System.getProperties().get("deveco.is.enableCangjieLog"));
        if (enableCangjieLog.equals("true")) {
            isCreated = logDir.mkdir();
            if (!isCreated) {
                LOG.info("Failed to create the log folder.");
            }
        }
        isCreated = cjdCacheDir.mkdir();
        if (!isCreated) {
            LOG.info("Failed to create the cjd cache folder.");
        }
        File depsFile = new File(cangjieDir, DEPENDENCY_FILE);
        if (!depsFile.exists()) {
            isCreated = depsFile.createNewFile();
            if (!isCreated) {
                LOG.info("Failed to create the dependency file.");
            }
        }
        NotificationUtil.clearTargetProjectNotification(project);
        findDeps(project, depsFile);
    }

    /**
     * find dependency
     *
     * @param project target project
     * @param depFile target file
     */
    public static void findDeps(Project project, File depFile) {
        CJModuleParser parser = new CJModuleParser(project);
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return;
        }
        List<ModuleModel> modules = projectModel.getModuleModelList();
        parser.setNeedEnv(project);
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        Path lspBuildPath = Paths.get(basePath, ".idea", ".deveco", "cangjie", ".cache", "lsp");
        for (int i = 0; lspBuildPath.toFile().exists() && i < 3; i++) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(lspBuildPath.toFile());
            } catch (IOException e) {
                LOG.info("remove lsp cache binary failed.");
            }
        }

        List<Path> targetCjPaths = new ArrayList<>();
        collectCangjieModule(modules, targetCjPaths);

        for (Path cjpmTomlPath : targetCjPaths) {
            Toml moduleToml = null;
            Optional<Toml> optModuleToml = LspConfigUtils.getModuleCjpmToml(project, cjpmTomlPath.toFile(), true);
            if (optModuleToml.isPresent()) {
                moduleToml = optModuleToml.get();
            }
            parser.parse(moduleToml,
                    cjpmTomlPath.getParent().toAbsolutePath().normalize().toString().replaceAll("\\\\", "/"));
            optModuleToml.ifPresent(toml -> parser.initConditionCompileOption(toml, cjpmTomlPath, project));
        }
        parser.write(depFile);
    }

    private static void collectCangjieModule(List<ModuleModel> modules, List<Path> targetCjPaths) {
        for (ModuleModel module : modules) {
            if (!(module instanceof OhosModuleModel ohModuleModel) || !LspConfigUtils.isCangjieModule(ohModuleModel)) {
                continue;
            }
            Path cjpmTomlPath = Paths.get(module.getModulePath(), PathConstants.CJPM_FILE);
            if (!cjpmTomlPath.toFile().exists()) {
                cjpmTomlPath = Paths.get(module.getModulePath(), "src", "main", "cangjie", PathConstants.CJPM_FILE);
            }
            if (cjpmTomlPath.toFile().exists()) {
                targetCjPaths.add(cjpmTomlPath);
            }
            if (com.huawei.cangjie.projectmgmt.utils.FileUtils.isContainEtsModule(ohModuleModel)) {
                continue;
            }
            Path ohosTestTomlPath = Paths.get(
                    module.getModulePath(), "src", "ohosTest", "cangjie", PathConstants.CJPM_FILE);
            if (ohosTestTomlPath.toFile().exists()) {
                targetCjPaths.add(ohosTestTomlPath);
            }
            Path localTestTomlPath = Paths.get(
                    module.getModulePath(), "src", "test", "cangjie", PathConstants.CJPM_FILE);
            if (localTestTomlPath.toFile().exists()) {
                targetCjPaths.add(localTestTomlPath);
            }
        }
    }

    /**
     * Need restart when build failed boolean.
     *
     * @param buildResult the build result
     * @param project the project
     * @return the boolean
     */
    public boolean needRestartWhenBuildFailed(boolean buildResult, Project project) {
        if (buildResult) {
            return false;
        }
        List<Path> moduleBuildPaths = getModuleBuildPaths(project);
        if (CollectionUtils.isEmpty(moduleBuildPaths)) {
            return false;
        }
        for (Path targetFolder : moduleBuildPaths) {
            ChangeDetectionContext detector = allFolderChangeDetectors.get(targetFolder);
            if (detector == null) {
                LOG.warn("No initial state recorded for folder: " + targetFolder
                    + ". Assuming changes occurred and restart is needed.");
                return true;
            }
            if (detector.detectChanges()) {
                return true;
            }
        }
        return false;
    }

    private List<Path> getModuleBuildPaths(@NotNull Project project) {
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return Collections.emptyList();
        }
        List<ModuleModel> moduleModelList = projectModel.getModuleModelList();
        if (CollectionUtils.isEmpty(moduleModelList)) {
            return Collections.emptyList();
        }
        return moduleModelList.stream().map(LspConfigUtils::getCjBuildPath).flatMap(Optional::stream)
            .filter(path -> path.toFile().exists()).toList();
    }
}
