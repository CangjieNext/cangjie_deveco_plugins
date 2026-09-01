/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;
import static com.huawei.deveco.projectmodel.ohos.model.constants.RuntimeOS.HARMONY_OS;
import static com.huawei.ideacj.lsp.utils.CangjieCommandExecutor.quoteForZsh;
import static com.huawei.ideacj.lsp.utils.Constants.BUILD_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.BUILD_PROFILE_JSON5;
import static com.huawei.ideacj.lsp.utils.Constants.CANGJIE_OPTIONS;
import static com.huawei.ideacj.lsp.utils.Constants.COMMIT_ID;
import static com.huawei.ideacj.lsp.utils.Constants.GIT;
import static com.huawei.ideacj.lsp.utils.Constants.LSP_REQUIRES;
import static com.huawei.ideacj.lsp.utils.Constants.NAME;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE;
import static com.huawei.ideacj.lsp.utils.Constants.SPLIT_MAC;
import static com.huawei.ideacj.lsp.utils.Constants.SPLIT_WINDOWS;
import static com.huawei.ideacj.lsp.utils.Constants.SRC;
import static com.huawei.ideacj.lsp.utils.Constants.SRC_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.CANGJIE_CACHE_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_FILE;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_LOCK_FILE;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJ_LSP_INTEROP_PATH;
import static com.huawei.ideacj.lsp.utils.PathConstants.DEPENDENCY_FILE;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_DEVECO_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_IDEA_DIR;
import static org.wso2.lsp4intellij.utils.FileUtils.editorToURIString;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieModulePathType;
import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieComponent;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieIdeaSdkInfoHandler;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProductManager;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.TargetManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosTarget;
import com.huawei.deveco.projectmodel.ohos.util.ModuleType;
import com.huawei.deveco.projectmodel.ohos.util.PsiJsonFileUtil;
import com.huawei.deveco.projectmodel.ohos.v2.impl.HvigorProductV2;
import com.huawei.deveco.sdkmanager.core.util.StringUtil;
import com.huawei.ideacj.lsp.ohoslauncher.EnvUtils;
import com.huawei.ideacj.notification.NotificationUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.intellij.json.psi.JsonObject;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * LSP related utilities
 *
 * @author rice
 * @since 2019-11-21
 */
public class LspConfigUtils {
    /**
     * definition separator constant
     */
    public static final String SEPARATOR = "\n";

    /**
     * WIN_BAT
     */
    public static final String WIN_BAT = "cmd.exe";

    /**
     * WIN_BAT_OPTION
     */
    public static final String WIN_BAT_OPTION = "/c";

    /**
     * MAC_BASH
     */
    public static final String MAC_BASH = "/bin/zsh";

    /**
     * MAC_BASH_OPTION
     */
    public static final String MAC_BASH_OPTION = "-c";

    /**
     * definition print log info instance
     */
    private static final Logger LOG = Logger.getInstance(LspConfigUtils.class);

    private static final Set<String> ignoreBuildEnvDir = new HashSet<>() {
        {
        add(".build-logs");
        add(".cached");
        }
    };

    private LspConfigUtils() {
        // add a private constructor to hide the implicit public one
    }

    /**
     * check the os is mac
     *
     * @return is mac
     */
    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    /**
     * collect windows need env
     *
     * @param envMap env map
     * @param projectUri project uri
     * @throws IOException IO Exception
     */
    public static void collectLspNeedEnv(Map<String, String> envMap, String projectUri) throws IOException {
        VirtualFile fileByNioPath = VirtualFileManager.getInstance().findFileByNioPath(Path.of(projectUri));
        if (fileByNioPath == null) {
            return;
        }
        Project project = ProjectUtil.guessProjectForFile(fileByNioPath);
        if (project == null) {
            return;
        }
        String split = getSplit();
        Map<String, String> sdkEnvMap = getSdkEnvPath(project);
        Path depsPath = Paths.get(project.getBasePath(), DOT_IDEA_DIR, DOT_DEVECO_DIR,
                CANGJIE_CACHE_DIR, DEPENDENCY_FILE);
        String content = new String(Files.readAllBytes(depsPath));
        JSONObject multiModuleOption = JSON.parseObject(content);
        String requiresEnvPath = "";
        if (!multiModuleOption.isEmpty()) {
            requiresEnvPath = multiModuleOption.getString(Constants.REQUIRES_ENV_PATH);
        }
        String macroServerPath = Path.of(getSdkPath(project), "build-tools", "tools", "bin").toString();
        if (LspConfigUtils.isMac()) {
            String originalDyldLibraryPath = sdkEnvMap.getOrDefault("DYLD_LIBRARY_PATH", "");
            String originalDyldFallbackLibraryPath = sdkEnvMap.getOrDefault("DYLD_FALLBACK_LIBRARY_PATH", "");
            String dyldLibraryPath = appendEnvPath(originalDyldLibraryPath, requiresEnvPath, split);
            String finalDyldLibraryPath = appendEnvPath(originalDyldFallbackLibraryPath, dyldLibraryPath, split);
            envMap.put("DYLD_LIBRARY_PATH", finalDyldLibraryPath);
            String finalDyldFallbackLibraryPath =
                appendEnvPath(originalDyldFallbackLibraryPath, requiresEnvPath, split);
            envMap.put("DYLD_FALLBACK_LIBRARY_PATH", finalDyldFallbackLibraryPath);
            envMap.put("PATH", macroServerPath);
        } else {
            String originalPath = sdkEnvMap.getOrDefault("PATH", "");
            String finalPath = appendEnvPath(originalPath, requiresEnvPath, split);
            envMap.put("PATH", finalPath);
        }
        // for interop macro
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null || projectModel.getModuleModelList() == null) {
            return;
        }
        List<ModuleModel> moduleModelList = projectModel.getModuleModelList();
        StringBuilder modulesPath = new StringBuilder();
        boolean addModulePath = false;
        for (ModuleModel model : moduleModelList) {
            if (!(model instanceof OhosModuleModel) || !isCangjieModule((OhosModuleModel) model)) {
                continue;
            }
            if (addModulePath) {
                modulesPath.append(";");
            }
            modulesPath.append(Path.of(model.getModulePath(), "src", "main", "cangjie"));
            addModulePath = true;
        }
        envMap.put(CJ_LSP_INTEROP_PATH, modulesPath.toString());
    }

    /**
     * get sdk env path
     *
     * @param project cur project
     * @return the sdk env path
     * @throws IOException IO Exception
     */
    public static Map<String, String> getSdkEnvPath(Project project) throws IOException {
        String sdkPath = getSdkPath(project);
        if (Strings.isEmpty(sdkPath)) {
            return Collections.emptyMap();
        }
        Process process = null;
        Map<String, String> envResult = new HashMap<>();

        try {
            if (LspConfigUtils.isMac()) {
                String batPath = Paths.get(sdkPath, "build-tools", "envsetup.sh").toString();
                String quotedPath = quoteForZsh(project, batPath);
                String command = "source " + quotedPath + " > /dev/null 2>&1 "
                    + "&& echo \"DYLD_LIBRARY_PATH_START==\"$DYLD_LIBRARY_PATH\"==DYLD_LIBRARY_PATH_END\" "
                    + "&& echo \"DYLD_FALLBACK_LIBRARY_PATH_START==\"$DYLD_FALLBACK_LIBRARY_PATH"
                    + "\"==DYLD_FALLBACK_LIBRARY_PATH_END\"";
                process = new ProcessBuilder(MAC_BASH, MAC_BASH_OPTION, command).start();

                try (InputStream inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder stringBuffer = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line).append("\n");
                    }
                    String output = stringBuffer.toString();
                    String dyldLibraryPath =
                        extractValue(output, "DYLD_LIBRARY_PATH_START==", "==DYLD_LIBRARY_PATH_END");
                    String dyldFallbackLibraryPath =
                        extractValue(output, "DYLD_FALLBACK_LIBRARY_PATH_START==", "==DYLD_FALLBACK_LIBRARY_PATH_END");
                    envResult.put("DYLD_LIBRARY_PATH", dyldLibraryPath);
                    envResult.put("DYLD_FALLBACK_LIBRARY_PATH", dyldFallbackLibraryPath);
                }
            } else {
                String batPath = Paths.get(sdkPath, "build-tools", "envsetup.bat").toString();
                process =
                    new ProcessBuilder(WIN_BAT, WIN_BAT_OPTION, "\"" + "\"" + batPath + "\"" + "&&PATH" + "\"").start();

                try (InputStream inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder stringBuffer = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line);
                    }
                    envResult.put("PATH", stringBuffer.toString().replace("PATH=", "").trim());
                }
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return envResult;
    }

    /**
     * get cangjie sdk path
     *
     * @param project the target project
     * @return cangjie sdk path
     */
    public static String getSdkPath(Project project) {
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return StringUtil.EMPTY;
        }
        String apiVersion = projectModel.getFullCompileSdkVersion().getValue();
        boolean isHarmony = projectModel.getActiveRuntimeOS() == HARMONY_OS;
        Map<String, CangjieComponent> localSdks = new CangjieIdeaSdkInfoHandler().getLocalSdks(isHarmony, apiVersion);
        if (localSdks == null) {
            return StringUtil.EMPTY;
        }
        CangjieComponent cangjieComponent = localSdks.get(CangjieComponentPath.CANGJIE.value());
        if (cangjieComponent == null) {
            return StringUtil.EMPTY;
        }
        Path cangjieSdkPath = cangjieComponent.getLocation();
        if (cangjieSdkPath == null) {
            return StringUtil.EMPTY;
        }
        return cangjieSdkPath.toString();
    }

    /**
     * get cangjie build path
     *
     * @param cangjieModuleDir target dir
     * @param project target project
     * @return Build Env Path
     * @throws IOException IOException
     */
    public static String getBuildEnvPath(String cangjieModuleDir, Project project) throws IOException {
        if (project == null || project.getBasePath() == null) {
            return StringUtil.EMPTY;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(cangjieModuleDir);
        if (virtualFile == null) {
            return StringUtil.EMPTY;
        }
        OhosModuleModel module = getSelectFileOhosModuleModel(project, virtualFile);
        Optional<Path> cjBuildPathOpt = getCjBuildPath(module);
        if (cjBuildPathOpt.isEmpty()) {
            return StringUtil.EMPTY;
        }
        Path cjBuildPath = cjBuildPathOpt.get();
        Path projectPath = Path.of(project.getBasePath());
        Path lspBuildPath = Paths.get(projectPath.toString(), ".idea", ".deveco", "cangjie", ".cache", "lsp");

        if (isMac()) {
            lspBuildPath = cjBuildPath;
        } else {
            if (cjBuildPath.toFile().exists()) {
                org.apache.commons.io.FileUtils.copyDirectory(cjBuildPath.toFile(), lspBuildPath.toFile());
            }
        }
        if (lspBuildPath.toFile().exists()) {
            StringBuilder stringBuilder = new StringBuilder();
            LspConfigUtils.addBuildPath(lspBuildPath, stringBuilder);
            return stringBuilder.toString();
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets cj build path.
     *
     * @param module the module
     * @return the cj build path
     */
    public static Optional<Path> getCjBuildPath(ModuleModel module) {
        if (module == null) {
            return Optional.empty();
        }
        Path ohosModulePath = Path.of(module.getModulePath());
        if (!ohosModulePath.toFile().exists()) {
            return Optional.empty();
        }
        // find product name
        String productName = "default";
        HvigorProductV2 product =
            ProductManager.getInstance().getCurrentProduct(CommonProjectUtil.getProjectModel(module.getProjectModel()
                .getProject()));
        if (product != null) {
            productName = product.getName();
        }
        // find target name
        String targetName = "default";
        if (!ModuleType.HAR.toString().equalsIgnoreCase(module.getModuleType())) {
            OhosTarget currentTarget = TargetManager.getInstance().getCurrentTarget(module);
            if (currentTarget != null) {
                targetName = currentTarget.getName();
            }
        }
        Path cjBuildPath = Paths.get(ohosModulePath.toString(), "build", productName,
            "intermediates", "cj", "build", targetName, "release");
        if (!cjBuildPath.toFile().exists()) {
            cjBuildPath = Paths.get(ohosModulePath.toString(), "build", productName, "intermediates",
                "cj", "build", targetName, "debug");
        }
        return Optional.of(cjBuildPath);
    }

    /**
     * splicing path
     *
     * @param path build path
     * @param stringBuilder target path container
     * @throws IOException IO Exception
     */
    private static void addBuildPath(Path path, StringBuilder stringBuilder) throws IOException {
        if (!path.toFile().exists()) {
            return;
        }

        File[] files = path.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isDirectory() || LspConfigUtils.ignoreBuildEnvDir.contains(file.getName())) {
                continue;
            }
            stringBuilder.append(file.getCanonicalPath()).append(getSplit());
            addBuildPath(file.toPath(), stringBuilder);
        }
    }

    /**
     * get Open Module
     *
     * @param project current project
     * @return Open Module
     */
    public static Module getOpenModule(Project project) {
        return ReadAction.compute(() -> {
            Module module = null;
            if (project == null) {
                return module;
            }
            VirtualFile cangjieFile = null;
            FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            if (selectedEditor != null && selectedEditor.getFile().getName().endsWith(".cj")) {
                cangjieFile = selectedEditor.getFile();
                module = ModuleUtil.findModuleForFile(cangjieFile, project);
                return module;
            }
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
            if (openFiles.length == 0) {
                return module;
            }
            for (VirtualFile file : openFiles) {
                if (!file.getName().endsWith(".cj")) {
                    continue;
                }
                cangjieFile = file;
                break;
            }
            if (cangjieFile == null) {
                return module;
            }
            module = ModuleUtil.findModuleForFile(cangjieFile, project);
            return module;
        });
    }

    /**
     * check is cangjie module
     *
     * @param module module
     * @return is cangjie module
     */
    public static boolean isCangjieModule(OhosModuleModel module) {
        if (module == null) {
            return false;
        }
        ProjectModel projectModel = module.getProjectModel();
        if (projectModel == null) {
            return false;
        }
        Project project = projectModel.getProject();
        if (project == null) {
            return false;
        }
        Path buildOptionPath = Paths.get(module.getModulePath(), BUILD_PROFILE_JSON5);
        if (!buildOptionPath.toFile().exists()) {
            return false;
        }
        JsonObject buildProfileJsonObject = PsiJsonFileUtil.findPsiFileJsonObject(project, buildOptionPath);
        if (buildProfileJsonObject == null) {
            return false;
        }
        JsonObject buildOption = PsiJsonFileUtil.getPsiJsonObject(buildProfileJsonObject, BUILD_OPTION);
        if (buildOption == null) {
            return false;
        }
        return PsiJsonFileUtil.getPsiJsonObject(buildOption, CANGJIE_OPTIONS) != null;
    }

    /**
     * this is calling editorOpened for all default opened editor
     *
     * @param project   current project
     * @param fileTypes supported file types
     */
    public static void callEditorOpenedForAllPotentialEditors(Project project, String fileTypes) {
        List<Editor> allOpenEditors = getAllOpenEditors(project);

        for (Editor editor : allOpenEditors) {
            final VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            final String extensionFromEditor = (file != null) ? file.getExtension() : null;
            if (extensionFromEditor != null && fileTypes.contains(extensionFromEditor)) {
                IntellijLanguageClient.editorOpened(editor);
            }
        }
    }

    /**
     * this is for notification in IDE
     *
     * @param project current project
     * @param msg     the message
     */
    public static void sendNotification(Project project, String msg) {
        Notification lspNotification = new Notification("User Notification",
                CangjieBundle.message("lsp.client.error"), msg,
                NotificationType.WARNING);
        lspNotification.notify(project);
    }

    /**
     * check is absolute path
     *
     * @param path target path
     * @return is absolute path
     */
    public static boolean isAbsolutePath(String path) {
        if (path.startsWith("/") || path.indexOf(":") > 0) {
            return true;
        }
        return false;
    }

    /**
     * get split by platform
     *
     * @return split
     */
    public static String getSplit() {
        if (LspConfigUtils.isMac()) {
            return SPLIT_MAC;
        }
        return SPLIT_WINDOWS;
    }

    /**
     * get all open text editors (not disposed) for a project
     *
     * @param project project
     * @return List<Editor>
     */
    public static List<Editor> getAllOpenEditors(Project project) {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<List<Editor>>) () -> {
                    List<Editor> openEditors = new ArrayList<>();
                    for (FileEditor fEditor : FileEditorManager.getInstance(project).getAllEditors()) {
                        if (!(fEditor instanceof TextEditor)) {
                            continue;
                        }
                        Editor editor = ((TextEditor) fEditor).getEditor();
                        if (!editor.isDisposed()) {
                            openEditors.add(editor);
                        }
                    }
                    return openEditors;
                }
        );
    }

    /**
     * getPathByLockFile
     *
     * @param moduleName module name
     * @param workspace workspace path
     * @param project project
     * @return path in cjpm.lock
     */
    public static String getPathByLockFile(String moduleName, String workspace, Project project) {
        // gitCodePath: CjpmConfigPath + moduleName + commitId
        Path tomlLockPath = Path.of(workspace, CJPM_LOCK_FILE);
        if (!tomlLockPath.toFile().exists()) {
            return StringUtil.EMPTY;
        }
        Optional<Toml> optLockData = LspConfigUtils.getModuleCjpmToml(project, tomlLockPath.toFile(), true);
        if (optLockData.isEmpty()) {
            return StringUtil.EMPTY;
        }
        Toml lockData = optLockData.get();
        Optional<Toml> gitRequires = lockData.getTable(LSP_REQUIRES);
        if (gitRequires.isEmpty()) {
            return StringUtil.EMPTY;
        }
        Optional<Toml> targetRequire = gitRequires.get().getTable(moduleName);
        if (targetRequire.isEmpty()) {
            return StringUtil.EMPTY;
        }
        String commitId = targetRequire.get().getString(COMMIT_ID);
        if (Strings.isEmpty(commitId)) {
            return StringUtil.EMPTY;
        }
        String cjpmConfigPath = getCjpmConfigPath();
        return getCustomCombinationFilePath(cjpmConfigPath, moduleName, commitId);
    }

    /**
     * getCjpmConfigPath
     *
     * @return cjpm config path
     */
    public static String getCjpmConfigPath() {
        String cjpmConfigEnv = EnvUtils.BUILDER.environment().get("CJPM_CONFIG");
        if (!Strings.isEmpty(cjpmConfigEnv)) {
            return getCustomCombinationFilePath(cjpmConfigEnv, GIT);
        }
        if (LspConfigUtils.isMac()) {
            cjpmConfigEnv = EnvUtils.BUILDER.environment().get("HOME");
            return getCustomCombinationFilePath(cjpmConfigEnv, ".cjpm", GIT);
        } else {
            cjpmConfigEnv = EnvUtils.BUILDER.environment().get("USERPROFILE");
            return getCustomCombinationFilePath(cjpmConfigEnv, ".cjpm", GIT);
        }
    }

    /**
     * get module cjpm toml
     *
     * @param project project
     * @param tomlFile toml file
     * @param shouldShowInfo shouldShowInfo
     * @return cjpm toml
     */
    public static Optional<Toml> getModuleCjpmToml(Project project, File tomlFile, boolean shouldShowInfo) {
        if (!tomlFile.exists()) {
            return Optional.empty();
        }
        Optional<Toml> optCjpmObj;
        try {
            optCjpmObj = new Toml().read(tomlFile);
        } catch (IllegalStateException e) {
            if (shouldShowInfo) {
                NotificationUtil.addNotification(project,
                        NotificationUtil.notifyInfo(CangjieBundle.message("lsp.toml.invalid",
                                tomlFile.getPath(), e.getMessage()),
                                project, NotificationType.ERROR,
                                NotificationUtil.getOpenFileAction(tomlFile.getPath())));
            }
            return Optional.empty();
        }
        if (optCjpmObj.isEmpty()) {
            if (shouldShowInfo) {
                NotificationUtil.notifyInfo("The cjpm.toml in Cangjie module " + tomlFile.getPath() + " is illegal.",
                        project, NotificationType.WARNING,
                        NotificationUtil.getOpenFileAction(tomlFile.getPath()));
            }
            return Optional.empty();
        }
        return optCjpmObj;
    }

    /**
     * get cangjie module name of selected module
     *
     * @param project project
     * @param module module
     * @param pathType pathType
     * @return String
     */
    @NotNull
    public static String getCangjieModuleName(Project project, OhosModuleModel module, CangjieModulePathType pathType) {
        String cangjieModuleName = "";
        if (!LspConfigUtils.isCangjieModule(module)) {
            return cangjieModuleName;
        }
        Path cjpmFilePath = getRealCjpmFilePath(module, pathType);
        Optional<Toml> optCjpmObj = getModuleCjpmToml(project, cjpmFilePath.toFile(), true);
        if (optCjpmObj.isEmpty()) {
            return cangjieModuleName;
        }
        Optional<Toml> packageInfo = optCjpmObj.get().getTable(PACKAGE);
        if (packageInfo.isPresent()) {
            cangjieModuleName = packageInfo.get().getString(NAME);
        }
        return cangjieModuleName;
    }

    /**
     * get src dir of cangjie module
     *
     * @param project project
     * @param module module
     * @param pathType pathType
     * @return String
     */
    @NotNull
    public static String getCangjieModuleSrcDir(Project project, OhosModuleModel module,
                                                CangjieModulePathType pathType) {
        if (!LspConfigUtils.isCangjieModule(module)) {
            return StringUtil.EMPTY;
        }
        Path cjpmFilePath = getRealCjpmFilePath(module, pathType);
        Optional<Toml> optCjpmObj = getModuleCjpmToml(project, cjpmFilePath.toFile(), true);
        if (optCjpmObj.isEmpty()) {
            return StringUtil.EMPTY;
        }
        Optional<Toml> packageInfo = optCjpmObj.get().getTable(PACKAGE);
        if (packageInfo.isEmpty()) {
            return StringUtil.EMPTY;
        }
        String srcDir = packageInfo.get().getString(SRC_DIR);
        if (Strings.isEmpty(srcDir)) {
            srcDir = SRC;
        }
        return srcDir;
    }

    /**
     * get real cjpm file path
     *
     * @param module module
     * @param pathType pathType
     * @return real cjpm file path
     */
    public static Path getRealCjpmFilePath(@NotNull ModuleModel module, CangjieModulePathType pathType) {
        Path cjpmFilePath = Path.of(module.getModulePath(), CJPM_FILE);
        if (!cjpmFilePath.toFile().exists()) {
            cjpmFilePath = Path.of(module.getModulePath(), "src", "main", "cangjie", CJPM_FILE);
        }
        if (pathType == CangjieModulePathType.OHOS_TEST) {
            cjpmFilePath = Path.of(module.getModulePath(), "src", "ohosTest", "cangjie", CJPM_FILE);
        }
        if (pathType == CangjieModulePathType.LOCAL_TEST) {
            cjpmFilePath = Path.of(module.getModulePath(), "src", "test", "cangjie", CJPM_FILE);
        }
        return cjpmFilePath;
    }

    /**
     * get package name
     *
     * @param project project
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @return package name
     */
    public static String getPackageName(Project project, OhosModuleModel moduleModel, VirtualFile virtualFile) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        String targetPath = virtualFile.getCanonicalPath();
        if (!virtualFile.isDirectory() && virtualFile.getParent() != null) {
            targetPath = virtualFile.getParent().getCanonicalPath();
        }
        if (StringUtil.isEmpty(targetPath)) {
            return StringUtil.EMPTY;
        }
        CangjieModulePathType pathType = FileUtils.checkCangjiePathType(moduleModel, targetPath);
        return FileUtils.getPackageName(moduleModel, virtualFile, pathType);
    }

    /**
     * remove editor listeners
     *
     * @param lspWrapper LanguageServerWrapper
     */
    public static void removeEditorListeners(LanguageServerWrapper lspWrapper) {
        var connectedEditors = lspWrapper.getConnectedEditors();
        var connectedFiles = lspWrapper.getConnectedFiles();
        for (Editor editor : connectedEditors.keySet()) {
            var manager = connectedEditors.get(editor);
            if (manager != null) {
                manager.removeListeners();
                connectedFiles.remove(editorToURIString(editor));
                manager.documentEventManager.removeListeners();
            }
        }
        for (Editor editor : connectedEditors.keySet()) {
            var manager = connectedEditors.get(editor);
            if (manager != null) {
                manager.documentClosed();
            }
        }
    }

    /**
     * notifyDidOpenFile
     *
     * @param wrapper lsp wrapper
     * @param textDocumentItem textDocumentItem
     */
    public static void notifyDidOpenFile(LanguageServerWrapper wrapper, TextDocumentItem textDocumentItem) {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams(textDocumentItem);
        wrapper.getRequestManager().didOpen(didOpenTextDocumentParams);
    }

    /**
     * notifyDidCloseFile
     *
     * @param wrapper lsp wrapper
     * @param uri uri
     */
    public static void notifyDidCloseFile(LanguageServerWrapper wrapper, String uri) {
        DidCloseTextDocumentParams didCloseTextDocumentParams = new DidCloseTextDocumentParams(
                new TextDocumentIdentifier(uri));
        wrapper.getRequestManager().didClose(didCloseTextDocumentParams);
    }

    /**
     * add content changes
     *
     * @param textDocumentItem textDocumentItem
     * @param textEdit textEdit
     * @param contentChanges contentChanges
     * @param newText newText
     */
    public static void addContentChanges(TextDocumentItem textDocumentItem,
                                          TextEdit textEdit,
                                          List<TextDocumentContentChangeEvent> contentChanges,
                                          String newText) {
        textDocumentItem.setVersion(textDocumentItem.getVersion() + 1);
        contentChanges.add(new TextDocumentContentChangeEvent(textEdit.getRange(), newText));
    }

    /**
     * notifyDidChange
     *
     * @param wrapper lsp wrapper
     * @param identifier VersionedTextDocumentIdentifier
     * @param contentChanges contentChanges
     */
    public static void notifyDidChange(LanguageServerWrapper wrapper,
                                 VersionedTextDocumentIdentifier identifier,
                                 List<TextDocumentContentChangeEvent> contentChanges) {
        DidChangeTextDocumentParams didOpenTextDocumentParams = new DidChangeTextDocumentParams(identifier,
                contentChanges);
        RequestManager requestManager = wrapper.getRequestManager();
        if (requestManager == null) {
            LOG.warn("requestManager is null.");
            return;
        }
        requestManager.didChange(didOpenTextDocumentParams);
    }

    /**
     * notifyDidChangeWatchFile
     *
     * @param wrapper lsp wrapper
     * @param uri uri
     * @param type file change type
     */
    public static void notifyDidChangeWatchFile(LanguageServerWrapper wrapper,
                                          String uri, FileChangeType type) {
        if (wrapper == null) {
            return;
        }
        List<FileEvent> fileEvents = new ArrayList<>();
        fileEvents.add(new FileEvent(uri, type));
        DidChangeWatchedFilesParams didOpenTextDocumentParams = new DidChangeWatchedFilesParams(fileEvents);
        RequestManager requestManager = wrapper.getRequestManager();
        if (requestManager == null) {
            LOG.warn("requestManager is null.");
            return;
        }
        requestManager.didChangeWatchedFiles(didOpenTextDocumentParams);
    }

    /**
     * check is in hybrid module loader
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @return is in hybrid module loader
     */
    public static boolean isInEtsCjLoader(OhosModuleModel moduleModel, VirtualFile virtualFile) {
        if (moduleModel == null || virtualFile == null) {
            return false;
        }
        Path etsCodePath = Path.of(moduleModel.getModulePath(), "src", "main", "ets");
        if (!etsCodePath.toFile().exists()) {
            return false;
        }
        String etsCjLoaderPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "loader")
                .normalize().toString().replaceAll("\\\\", "/");
        String etsCjTypesPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "types")
                .normalize().toString().replaceAll("\\\\", "/");
        String etsCjInteropPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "ark_interop_api")
                .normalize().toString().replaceAll("\\\\", "/");
        String targetFilePath = virtualFile.getCanonicalPath();
        if (StringUtil.isEmpty(targetFilePath)) {
            return false;
        }
        targetFilePath = targetFilePath.replaceAll("\\\\", "/");
        return targetFilePath.startsWith(etsCjLoaderPath) || targetFilePath.startsWith(etsCjTypesPath)
                || targetFilePath.startsWith(etsCjInteropPath);
    }

    /**
     * get custom combination file path
     *
     * @param first first
     * @param more more
     * @return file path
     */
    public static String getCustomCombinationFilePath(String first, String... more) {
        try {
            return Path.of(first, more).toString();
        } catch (InvalidPathException e) {
            LOG.warn("Invalid custom combination file path.");
            return StringUtil.EMPTY;
        }
    }

    /**
     * Gets source absolute path.
     *
     * @param project the project
     * @param ohosModuleModel the ohos module model
     * @param pathType pathType
     * @return the source absolute path
     */
    @NotNull
    public static Optional<Path> getSourceAbsolutePath(Project project, OhosModuleModel ohosModuleModel,
        CangjieModulePathType pathType) {
        if (!LspConfigUtils.isCangjieModule(ohosModuleModel)) {
            return Optional.empty();
        }
        Path cjpmFilePath = getRealCjpmFilePath(ohosModuleModel, pathType);
        Optional<Toml> optCjpmObj = getModuleCjpmToml(project, cjpmFilePath.toFile(), true);
        if (optCjpmObj.isEmpty()) {
            return Optional.empty();
        }
        Optional<Toml> packageInfo = optCjpmObj.get().getTable(PACKAGE);
        if (packageInfo.isEmpty()) {
            return Optional.empty();
        }
        String srcDir = packageInfo.get().getString(SRC_DIR);
        if (Strings.isEmpty(srcDir)) {
            srcDir = SRC;
        }
        Path sourceAbsPath = cjpmFilePath.getParent().resolve(srcDir);
        return sourceAbsPath.toFile().exists() ? Optional.of(sourceAbsPath) : Optional.empty();
    }

    /**
     * Is child file boolean.
     *
     * @param parentDirPath the parent dir path
     * @param childFile the child file
     * @return the boolean
     */
    public static boolean isChildFile(Path parentDirPath, VirtualFile childFile) {
        if (childFile == null) {
            return false;
        }
        // 先获取srcDir对应的VirtualFile
        VirtualFile parentDir = LocalFileSystem.getInstance().findFileByPath(parentDirPath.toString());
        if (parentDir == null || !parentDir.isDirectory()) {
            return false;
        }
        // 第三个参数为true表示严格的祖先关系（不包括自身）
        return VfsUtilCore.isAncestor(parentDir, childFile, true);
    }

    /**
     * project contain Cangjie Module
     *
     * @param project project
     * @return boolean
     */
    public static boolean containCangjieModule(Project project) {
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null || projectModel.getModuleModelList() == null) {
            return false;
        }
        List<ModuleModel> moduleModelList = projectModel.getModuleModelList();
        for (ModuleModel model : moduleModelList) {
            if (!(model instanceof OhosModuleModel)) {
                continue;
            }
            if (isCangjieModule((OhosModuleModel) model)) {
                return true;
            }
        }
        return false;
    }

    private static String extractValue(String text, String startDelimiter, String endDelimiter) {
        int startIndex = text.indexOf(startDelimiter);
        if (startIndex != -1) {
            startIndex += startDelimiter.length();
            int endIndex = text.indexOf(endDelimiter, startIndex);
            if (endIndex != -1) {
                return text.substring(startIndex, endIndex);
            }
        }
        return StringUtil.EMPTY;
    }

    private static String appendEnvPath(String originalLibraryPath, String requiresEnvPath,
        String split) {
        StringBuilder finalLibraryPathBuilder = new StringBuilder(originalLibraryPath);
        if (!Strings.isEmpty(requiresEnvPath)) {
            if (!finalLibraryPathBuilder.isEmpty()) {
                finalLibraryPathBuilder.append(split);
            }
            finalLibraryPathBuilder.append(requiresEnvPath);
        }
        return finalLibraryPathBuilder.toString();
    }
}
