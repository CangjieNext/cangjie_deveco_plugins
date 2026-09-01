/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.project;

import static com.huawei.ideacj.lsp.utils.Constants.BIN_DEPENDENCIES;
import static com.huawei.ideacj.lsp.utils.Constants.C_REQUIRES;
import static com.huawei.ideacj.lsp.utils.Constants.DEPENDENCIES;
import static com.huawei.ideacj.lsp.utils.Constants.DEV_DEPENDENCIES;
import static com.huawei.ideacj.lsp.utils.Constants.FFI;
import static com.huawei.ideacj.lsp.utils.Constants.GIT;
import static com.huawei.ideacj.lsp.utils.Constants.LSP_PACKAGE_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.LSP_PACKAGE_REQUIRES;
import static com.huawei.ideacj.lsp.utils.Constants.LSP_PATH_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.LSP_REQUIRES;
import static com.huawei.ideacj.lsp.utils.Constants.NAME;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE_REQUIRES;
import static com.huawei.ideacj.lsp.utils.Constants.PATH;
import static com.huawei.ideacj.lsp.utils.Constants.PATH_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.TARGET;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_FILE;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.ideacj.lsp.ohoslauncher.EnvUtils;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;

import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * client parse cjpm.toml
 *
 * @since 2022-11-5
 */
public class LspProjectManager {
    private static final Logger LOG = Logger.getInstance(LspProjectManager.class);

    private static final String URI_PREFIX = "file:///";

    private static final Map<String, LspProject> modulesMap = new HashMap<>();

    private static final ProcessBuilder PROCESS_BUILDER = new ProcessBuilder();

    private static final Pattern PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * get multi module option
     *
     * @param project target project
     */
    public static void getMultiModuleOption(Project project) {
        if (project == null) {
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        Map<String, String> envs = CangjieEnvUtils.getProjectEnvs(project);
        if (envs != null) {
            PROCESS_BUILDER.environment().putAll(envs);
        }
        LspProject lspProject = new LspProject(project);
        modulesMap.put(basePath, lspProject);

        Module openedModule = LspConfigUtils.getOpenModule(project);
        Path rootModule = null;
        if (openedModule != null) {
            rootModule = Path.of(basePath, openedModule.getName(), "src", "main", "cangjie");
        }
        // no open cangjie module, check all module in project
        if (openedModule == null) {
            Module[] modules = com.intellij.openapi.module.ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                if (module instanceof OhosModuleModel && LspConfigUtils.isCangjieModule((OhosModuleModel) module)) {
                    rootModule = Path.of(basePath, module.getName(), "src", "main", "cangjie");
                    break;
                }
            }
        }
        // default module
        if (rootModule == null) {
            rootModule = Path.of(basePath, "entry", "src", "main", "cangjie");
        }
        lspProject.setRootModule(rootModule);
        generateProtocol("", rootModule, lspProject);
    }

    /**
     * Abs path to URI
     *
     * @param dir To-be-converted path
     * @param workspace Current dir path
     * @return URI
     */
    public static String toAbsDirectory(String dir, String workspace) {
        String urlStr = "";
        try {
            if (dir.contains(":") | dir.startsWith("/")) {
                urlStr =
                        FileUtils.sanitizeURI(URI_PREFIX + dir.replaceAll("\\\\", "/").replaceAll(":/", "%3A/"));
                return URI.create(urlStr).toString();
            }
            urlStr = URI_PREFIX + Path.of(workspace, dir).normalize().toString().replaceAll("\\\\", "/");
            return URI.create(FileUtils.sanitizeURI(urlStr)).toString();
        } catch (IllegalArgumentException exception) {
            LOG.warn("Failed to sanitize directory: " + urlStr);
            return urlStr;
        }
    }

    /**
     * Replacing Relative Paths and Environment Variables
     *
     * @param path To-be-converted path
     * @param workspace Current dir path
     * @return Real Abs Path
     */
    public static String getRealPath(String path, String workspace) {
        String realPath = path;
        if (Strings.isEmpty(realPath)) {
            return realPath;
        }
        Matcher matcher = PATTERN.matcher(realPath);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String math = matcher.group(1);
            String env = EnvUtils.BUILDER.environment().get(math);
            if (env == null) {
                env = "";
            }
            matcher.appendReplacement(sb, env.replace("\\", "/"));
        }
        matcher.appendTail(sb);
        realPath = sb.toString();
        if (!LspConfigUtils.isAbsolutePath(realPath)) {
            realPath = Path.of(workspace, realPath).normalize().toString().replaceAll("\\\\", "/");
        }
        return realPath;
    }

    /**
     * getModuleInfo
     *
     * @param uri target project uri
     * @return module info
     */
    public static LspProject getModuleInfoByProject(String uri) {
        return modulesMap.get(uri);
    }


    /**
     * Generate protocol object.
     *
     * @param name module name
     * @param workspace the workspace pass from lsp4intellij,
     *                  The URI is obtained from the project and is not an external input.
     * @param lspProject project
     */
    @Nullable
    private static void generateProtocol(String name, Path workspace, LspProject lspProject) {
        if (workspace == null) {
            return;
        }
        String workspacePath = workspace.toString();
        if (lspProject.getUriSet().contains(workspacePath)) {
            return;
        }
        lspProject.getUriSet().add(workspacePath);
        Path cjpmFilePath = Path.of(workspacePath, CJPM_FILE);
        JSONObject curModuleData = new JSONObject();
        if (!cjpmFilePath.toFile().exists()) {
            curModuleData.put(NAME, workspace.getFileName().toString());
            lspProject.getModulesInfos().put(toAbsDirectory(workspace.toString(), workspacePath), curModuleData);
            return;
        }
        Optional<Toml> optCjpmObj = LspConfigUtils.getModuleCjpmToml(
                lspProject.getMyProject(), cjpmFilePath.toFile(), false);
        if (optCjpmObj.isEmpty()) {
            curModuleData.put(NAME, workspace.getFileName().toString());
            lspProject.getModulesInfos().put(toAbsDirectory(workspace.toString(), workspacePath), curModuleData);
            return;
        }
        Toml cjpmObj = optCjpmObj.get();
        Optional<Toml> packageInfo = cjpmObj.getTable(PACKAGE);
        String moduleName = "";
        if (packageInfo.isPresent()) {
            moduleName = packageInfo.get().getString(NAME);
        }
        if (moduleName.isEmpty()) {
            moduleName = workspace.getFileName().toString();
        }
        curModuleData.put(NAME, moduleName);

        parseCJPM(workspace, lspProject, cjpmObj, curModuleData, workspacePath);
    }

    private static void parseCJPM(Path workspace, LspProject lspProject, Toml cjpmObj, JSONObject curModuleData,
        String workspacePath) {
        // package-requires
        Optional<Toml> packageRequires = cjpmObj.getTable(PACKAGE_REQUIRES);
        packageRequires.ifPresent(
            toml -> curModuleData.put(LSP_PACKAGE_REQUIRES, getPackageRequires(toml, workspacePath, lspProject)));
        // target.bin-dependencies
        Optional<Toml> targets = cjpmObj.getTable(TARGET);
        if (targets.isPresent()) {
            JSONObject packageRequiresObject = curModuleData.getJSONObject(PACKAGE_REQUIRES);
            if (packageRequiresObject == null) {
                packageRequiresObject = new JSONObject();
            }
            packageRequiresObject.putAll(getTargetPackageRequires(targets.get(), workspacePath, lspProject));
            curModuleData.put(LSP_PACKAGE_REQUIRES, packageRequiresObject);
        }
        // ffi
        Optional<Toml> ffi = cjpmObj.getTable(FFI);
        if (ffi.isPresent()) {
            Optional<Toml> ffiC = ffi.get().getTable(C_REQUIRES);
            ffiC.ifPresent(toml -> getCModules(toml, workspacePath, lspProject));
        }
        // dependencies
        Optional<Toml> dependencies = cjpmObj.getTable(DEPENDENCIES);
        dependencies.ifPresent(toml -> curModuleData.put(LSP_REQUIRES, getRequires(toml, workspacePath, lspProject)));
        // dev-dependencies
        Optional<Toml> devDependencies = cjpmObj.getTable(DEV_DEPENDENCIES);
        if (devDependencies.isPresent()) {
            JSONObject requiresObject = curModuleData.getJSONObject(LSP_REQUIRES);
            if (requiresObject == null) {
                requiresObject = new JSONObject();
            }
            requiresObject.putAll(getRequires(devDependencies.get(), workspacePath, lspProject));
            curModuleData.put(LSP_REQUIRES, requiresObject);
        }
        // target.dependencies && target.dev-dependencies
        if (targets.isPresent()) {
            JSONObject requiresObject = curModuleData.getJSONObject(LSP_REQUIRES);
            if (requiresObject == null) {
                requiresObject = new JSONObject();
            }
            requiresObject.putAll(getTargetRequires(targets.get(), workspacePath, lspProject));
            curModuleData.put(LSP_REQUIRES, requiresObject);
        }
        lspProject.getModulesInfos().put(toAbsDirectory(workspace.toString(), workspacePath), curModuleData);
    }

    private static JSONObject getPackageRequires(Toml packageRequires, String workspace, LspProject lspProject) {
        JSONObject relPackageRequires = new JSONObject();
        JSONArray relPathOption = new JSONArray();
        JSONObject relPackageOption = new JSONObject();
        relPackageRequires.put(LSP_PATH_OPTION, relPathOption);
        relPackageRequires.put(LSP_PACKAGE_OPTION, relPackageOption);
        String splitChar = LspConfigUtils.getSplit();
        // package-requires.path-option
        List<String> pathOption = packageRequires.getList(PATH_OPTION);
        if (pathOption != null) {
            int index = 0;
            for (String path : pathOption) {
                String relPath = getRealPath(path, workspace);
                lspProject.getRequiresEnvPath().append(relPath);
                lspProject.getRequiresEnvPath().append(splitChar);
                relPathOption.set(index++, FileUtils.sanitizeURI(toAbsDirectory(relPath, workspace)));
            }
        }
        // package-requires.package-option
        Optional<Toml> packageOption = packageRequires.getTable(PACKAGE_OPTION);
        if (packageOption.isPresent()) {
            Map<String, Object> packageItem = packageOption.get().toMap();
            for (String key : packageItem.keySet()) {
                if (!(packageItem.get(key) instanceof String)) {
                    continue;
                }
                String path = getRealPath((String) packageItem.get(key), workspace);
                lspProject.getRequiresEnvPath().append(path);
                lspProject.getRequiresEnvPath().append(splitChar);
                relPackageOption.put(key, FileUtils.sanitizeURI(toAbsDirectory(path, workspace)));
            }
        }
        return relPackageRequires;
    }

    private static JSONObject getTargetPackageRequires(Toml targets, String workspace, LspProject lspProject) {
        JSONObject relPackageRequires = new JSONObject();
        Map<String, Object> targetItem = targets.toMap();
        for (String key : targetItem.keySet()) {
            Optional<Toml> target = targets.getTable(key);
            if (target.isEmpty() || target.get().getTable(BIN_DEPENDENCIES).isEmpty()) {
                continue;
            }
            relPackageRequires
                .putAll(getPackageRequires(target.get().getTable(BIN_DEPENDENCIES).get(), workspace, lspProject));
        }
        return relPackageRequires;
    }

    private static void getCModules(Toml ffiC, String workspace, LspProject lspProject) {
        String splitChar = LspConfigUtils.getSplit();
        Map<String, Object> cItemMap = ffiC.toMap();
        for (String key : cItemMap.keySet()) {
            Optional<Toml> cItem = ffiC.getTable(key);
            if (cItem.isEmpty()) {
                continue;
            }
            String path = cItem.get().getString(PATH);
            if (Strings.isEmpty(path)) {
                continue;
            }
            String relPath = getRealPath(path, workspace);
            lspProject.getRequiresEnvPath().append(relPath);
            lspProject.getRequiresEnvPath().append(splitChar);
        }
    }

    private static JSONObject getRequires(Toml requires, String workspace, LspProject lspProject) {
        JSONObject relRequires = new JSONObject();
        Map<String, Object> requireItemMap = requires.toMap();
        for (String key : requireItemMap.keySet()) {
            JSONObject relRequireItem = new JSONObject();
            relRequires.put(key, relRequireItem);

            Optional<Toml> requireItem = requires.getTable(key);
            if (requireItem.isEmpty()) {
                continue;
            }
            String path = requireItem.get().getString(PATH);
            if (!Strings.isEmpty(path)) {
                // path src code
                String relPath = getRealPath(path, workspace);
                relRequireItem.put(PATH, FileUtils.sanitizeURI(toAbsDirectory(relPath, workspace)));
                generateProtocol(key, Path.of(relPath), lspProject);
            } else {
                // git code
                String gitOption = requireItem.get().getString(GIT);
                if (Strings.isEmpty(gitOption)) {
                    continue;
                }
                String gitPath = LspConfigUtils.getPathByLockFile(key, workspace, lspProject.getMyProject());
                if (!Strings.isEmpty(gitPath)) {
                    relRequireItem.put(PATH, FileUtils.sanitizeURI(toAbsDirectory(gitPath, workspace)));
                    generateProtocol(key, Path.of(gitPath), lspProject);
                }
            }
        }
        return relRequires;
    }

    private static JSONObject getTargetRequires(Toml targets, String workspace, LspProject lspProject) {
        JSONObject relRequires = new JSONObject();
        Map<String, Object> targetItem = targets.toMap();
        for (String key : targetItem.keySet()) {
            Optional<Toml> target = targets.getTable(key);
            if (target.isEmpty()) {
                continue;
            }
            if (target.get().getTable(DEPENDENCIES).isPresent()) {
                relRequires.putAll(getRequires(target.get().getTable(DEPENDENCIES).get(), workspace, lspProject));
            }
            if (target.get().getTable(DEV_DEPENDENCIES).isPresent()) {
                relRequires.putAll(getRequires(target.get().getTable(DEV_DEPENDENCIES).get(), workspace, lspProject));
            }
        }
        return relRequires;
    }
}
