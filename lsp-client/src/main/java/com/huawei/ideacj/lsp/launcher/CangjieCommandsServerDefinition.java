/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import static com.huawei.ideacj.lsp.utils.PathConstants.CANGJIE_CACHE_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DEPENDENCY_FILE;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_DEVECO_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_IDEA_DIR;

import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ProductManager;
import com.huawei.deveco.projectmodel.ohos.v2.impl.HvigorProductV2;
import com.huawei.ideacj.lsp.listener.CangjieStartServerListener;
import com.huawei.ideacj.lsp.project.LspProject;
import com.huawei.ideacj.lsp.project.LspProjectManager;
import com.huawei.ideacj.lsp.utils.Constants;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.application.PathManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.connection.ProcessStreamConnectionProvider;
import org.wso2.lsp4intellij.client.connection.StreamConnectionProvider;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProgressConfig;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cangjie commands server definition
 *
 * @since 2022-09-24
 */
public class CangjieCommandsServerDefinition extends RawCommandServerDefinition {
    private static final Logger LOG = Logger.getInstance(CangjieCommandsServerDefinition.class);

    private static final String FILE_PREFIX = "file:///";

    private String cangjieSdkPath = "";

    private String projectRootUri = "";

    /**
     * Instantiates a new Cangjie commands server definition.
     *
     * @param ext               the ext
     * @param languageIds       the language ids
     * @param command           the command
     * @param progressConfig    the progressConfig
     * @param listener          the startServerListener
     */
    public CangjieCommandsServerDefinition(@NotNull String ext, Map<String, String> languageIds, String[] command,
                                           ProgressConfig progressConfig, CangjieStartServerListener listener) {
        super(ext, languageIds, command, progressConfig, listener, false, true);
    }

    public String getSdkPath() {
        return this.cangjieSdkPath;
    }

    public void setSdkPath(String sdkPath) {
        this.cangjieSdkPath = sdkPath;
    }

    @Override
    public Object getInitializationOptions(URI uri) {
        Map<String, Object> options = new HashMap<>();
        Project project = ProjectUtil.guessProjectForFile(
                VirtualFileManager.getInstance().findFileByNioPath(Path.of(uri)));
        if (project == null) {
            return options;
        }
        LspProject lspProject = LspProjectManager.getModuleInfoByProject(project.getBasePath());
        if (lspProject != null) {
            String modulePath = FileUtils.sanitizeURI(FILE_PREFIX
                    + lspProject.getRootModule().toString().replace("\\", "/"));
            initOption(options, modulePath, lspProject);
        }
        return options;
    }

    @Override
    public StreamConnectionProvider createConnectionProvider(String workingDir) {
        try {
            return new ProcessStreamConnectionProvider(createProcessBuilder(Arrays.asList(this.command), workingDir));
        } catch (IOException e) {
            return new ProcessStreamConnectionProvider(Arrays.asList(this.command), workingDir);
        }
    }

    @Override
    public Triple<InputStream, OutputStream, Long> startAndGetAllInfo(String workingDir) throws IOException {
        VirtualFile fileByNioPath = VirtualFileManager.getInstance().findFileByNioPath(Path.of(projectRootUri));
        if (fileByNioPath == null) {
            throw new FileNotFoundException("Can't start server due to not found file: " + projectRootUri);
        }
        Project project = ProjectUtil.guessProjectForFile(
                fileByNioPath);
        setSdkPath(LspConfigUtils.getSdkPath(project));
        LspProjectManager.getMultiModuleOption(project);
        return super.startAndGetAllInfo(workingDir);
    }

    private ProcessBuilder createProcessBuilder(List<String> commands, String workingDir) throws IOException {
        commands.forEach((c) -> {
            c = c.replace("'", "");
        });
        ProcessBuilder tempBuilder = new ProcessBuilder(commands);
        tempBuilder.directory(new File(workingDir));
        tempBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        LspConfigUtils.collectLspNeedEnv(tempBuilder.environment(), projectRootUri);
        return tempBuilder;
    }

    private String getProjectDir() {
        return this.projectRootUri;
    }

    public void setProjectDir(String uri) {
        this.projectRootUri = uri;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.ext.hashCode() + 3 * Arrays.hashCode(this.command);
    }

    private void initOption(Map<String, Object> options, String modulePath, LspProject lspProject) {
        options.put("cangjieRootUri", modulePath);
        String basePath = lspProject.getMyProject().getBasePath();
        if (!StringUtils.isEmpty(basePath)) {
            Path depsPath = Paths.get(basePath, DOT_IDEA_DIR, DOT_DEVECO_DIR,
                    CANGJIE_CACHE_DIR, DEPENDENCY_FILE);
            try {
                String content = new String(Files.readAllBytes(depsPath));
                JSONObject multiModuleOption = JSON.parseObject(content);
                options.put(Constants.MULTI_MODULE_OPTION,
                        multiModuleOption.getJSONObject(Constants.MULTI_MODULE_OPTION));
            } catch (IOException e) {
                LOG.error(e);
            }
        }
        File cangjieDir = Paths.get(getProjectDir(), DOT_IDEA_DIR, DOT_DEVECO_DIR, CANGJIE_CACHE_DIR).toFile();
        if (cangjieDir.exists()) {
            options.put("cachePath", cangjieDir.getPath());
        }
        initStdLibOption(options, lspProject.getMyProject());
        initCjdPathOption(options);
        initConditionCompileOption(options, lspProject);
    }

    private void initStdLibOption(Map<String, Object> options, Project project) {
        String modulesPath = "";
        String stdLibPath = "";
        if (StringUtils.isEmpty(this.cangjieSdkPath)) {
            setSdkPath(LspConfigUtils.getSdkPath(project));
        }
        if (!StringUtils.isEmpty(this.cangjieSdkPath)) {
            modulesPath = Path.of(this.cangjieSdkPath, "build-tools").toString();
            stdLibPath = Path.of(this.cangjieSdkPath, "build-tools", "lib", "src").toString();
        }

        if (!new File(stdLibPath).exists()) {
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(
                    "com.huawei.cangjie-support-plugin"));
            if (plugin != null) {
                String pluginPath = plugin.getPluginPath().toString();
                stdLibPath = Paths.get(pluginPath, "lib", "src").toString();
            }
        }
        options.put("stdLibPathOption", stdLibPath);
        options.put("modulesHomeOption", modulesPath);
    }

    private void initCjdPathOption(Map<String, Object> options) {
        String stdCjdPathOption = "";
        String ohosCjdPathOption = "";
        String cjdCachePathOption = Path.of(PathManager.getSystemPath(), "cj-declaration-cache").toString();
        if (options.containsKey("modulesHomeOption")) {
            String modulesPath = options.get("modulesHomeOption").toString();
            stdCjdPathOption = Paths.get(modulesPath, "modules", "linux_ohos_aarch64_cjnative", "std").toString();
        }
        if (!StringUtils.isEmpty(this.cangjieSdkPath)) {
            ohosCjdPathOption = Path.of(this.cangjieSdkPath,
                    "api", "modules", "linux_ohos_aarch64_cjnative", "ohos").toString();
        }
        options.put("stdCjdPathOption", stdCjdPathOption);
        options.put("ohosCjdPathOption", ohosCjdPathOption);
        options.put("cjdCachePathOption", cjdCachePathOption);
    }

    private void initConditionCompileOption(Map<String, Object> options, LspProject lspProject) {
        initGlobalConditionCompileOption(options, lspProject);
        initModuleConditionCompileOption(options, lspProject);
    }

    private void initGlobalConditionCompileOption(Map<String, Object> options, LspProject lspProject) {
        initDefaultConditionCompileOption(options, lspProject);
        if (!options.containsKey(Constants.CONDITION_COMPILE_OPTION)) {
            options.put(Constants.CONDITION_COMPILE_OPTION, new JSONObject());
        }
        Object field = options.get(Constants.CONDITION_COMPILE_OPTION);
        if (!(field instanceof JSONObject compileOptField)) {
            return;
        }
        if (lspProject == null || lspProject.getMyProject() == null) {
            return;
        }
        Project project = lspProject.getMyProject();
        HvigorProductV2 product =
                ProductManager.getInstance().getCurrentProduct(CommonProjectUtil.getProjectModel(project));
        if (product == null) {
            return;
        }
        compileOptField.put(Constants.CONDITION_PRODUCT, product.getName());
    }

    private void initDefaultConditionCompileOption(Map<String, Object> options, LspProject lspProject) {
        if (!options.containsKey(Constants.CONDITION_COMPILE_OPTION)) {
            options.put(Constants.CONDITION_COMPILE_OPTION, new JSONObject());
        }
        Object field = options.get(Constants.CONDITION_COMPILE_OPTION);
        if (!(field instanceof JSONObject compileOptField)) {
            return;
        }
        compileOptField.put(Constants.CONDITION_COVERAGE, "true");
        Project myProject = lspProject.getMyProject();
        Map<String, String> projectEnvs = CangjieEnvUtils.getProjectEnvs(myProject);
        if (projectEnvs == null) {
            return;
        }
        String envCondition = projectEnvs.getOrDefault("COMPILE_CONDITION", "");
        String[] items = envCondition.split(",");
        for (String item : items) {
            int eqIndex = item.indexOf('=');
            if (eqIndex < 0) {
                continue;
            }
            String key = item.substring(0, eqIndex).trim();
            String value = item.substring(eqIndex + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                compileOptField.put(key, value);
            }
        }
    }

    private void initModuleConditionCompileOption(Map<String, Object> options, LspProject lspProject) {
        if (lspProject == null || lspProject.getMyProject() == null) {
            return;
        }
        String basePath = lspProject.getMyProject().getBasePath();
        if (StringUtils.isEmpty(basePath)) {
            return;
        }
        Path depsPath = Paths.get(basePath, DOT_IDEA_DIR, DOT_DEVECO_DIR,
                CANGJIE_CACHE_DIR, DEPENDENCY_FILE);
        try {
            String content = new String(Files.readAllBytes(depsPath));
            JSONObject multiModuleOption = JSON.parseObject(content);
            options.put(
                    Constants.MODULE_CONDITION_COMPILE_OPTION,
                    multiModuleOption.getJSONObject(Constants.MODULE_CONDITION_COMPILE_OPTION));
        } catch (IOException e) {
            LOG.warn("Read module condition compile info failed.");
        }
    }
}
