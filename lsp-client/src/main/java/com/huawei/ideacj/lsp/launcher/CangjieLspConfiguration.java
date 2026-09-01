/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import static com.huawei.ideacj.lsp.utils.PathConstants.CANGJIE_CACHE_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_DEVECO_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.DOT_IDEA_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.LOG_DIR;

import com.huawei.cjlsp.capabilities.documentlink.CangjieDocumentLink;
import com.huawei.ideacj.lsp.extend.CangjieExtendLspExtensionManagerImpl;
import com.huawei.ideacj.lsp.listener.CangjieCommandListener;
import com.huawei.ideacj.lsp.listener.CangjieFileListenerRegisterSingleton;
import com.huawei.ideacj.lsp.listener.CangjieLookupListener;
import com.huawei.ideacj.lsp.listener.CangjieStartServerListener;
import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.notification.NotificationUtil;

import com.google.gson.GsonBuilder;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManagerListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProgressConfig;
import org.wso2.lsp4intellij.editor.DocumentEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.requests.Timeouts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LSP configuration and start on project open
 *
 * @author SonicNing
 * @since 2019-06-01
 */
public class CangjieLspConfiguration {
    private static final Logger LOG = Logger.getInstance(CangjieLspConfiguration.class);

    private static final String MODULES_JSON_NAME = "modules.json";

    private static final String LOG_PATH_ARG = "--log-path=";

    private static final String LOG_ENABLE_ARG = "--enable-log=";

    private static final String CRASH_REPORT_ARG = "-V";

    private static final String CACHE_PATH_ARG = "--cache-path=";

    private CangjieLspConfiguration() {
        // add a private constructor to hide the implicit public one
    }

    /**
     * Get the main_dir from modules.json, modules.json is in the cangjie project path;
     * By default, a cangjie project path must contain a modules.json
     *
     * @author t30009182
     * @since 2021-10-21
     * @param path : a cangjie project path
     * @return return a main_dir from modules.json, if modules.json not exists, return "src"
     * @throws IOException  throw stream or buffer errors
     */
    private static String getMainDir(String path) throws IOException {
        String mainDir = "cangjie";
        String modulesJsonPath = path + File.separator + MODULES_JSON_NAME;
        File file = new File(modulesJsonPath);
        if (!file.exists()) {
            return mainDir;
        }
        String result = "";
        try (InputStream input = new FileInputStream(modulesJsonPath);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            result = buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (FileNotFoundException e) {
            LOG.warn("Get the main_dir from modules.json error: {}", e);
        }
        GsonBuilder gson = new GsonBuilder();
        ModulesSettings modulesSettings = gson.create().fromJson(result, ModulesSettings.class);
        if (modulesSettings != null && modulesSettings.getMainDir() != null) {
            mainDir = modulesSettings.getMainDir();
        }
        return mainDir;
    }

    /**
     * addServerDefinitionForClient
     *
     * @param project project
     * @throws IOException  throw getMainDir() errors
     */
    public static synchronized void addServerDefinitionForClient(Project project) throws IOException {
        Path sdkPath = Path.of(LspConfigUtils.getSdkPath(project));
        if (!sdkPath.toFile().exists()) {
            LspConfigUtils.sendNotification(project, "can not get sdk path");
            NotificationUtil.notifyInfo("Can't get Cangjie sdk path", project, NotificationType.WARNING);
            return;
        }
        String serverPath = "";
        boolean isInSdk = true;
        // Connect the lsp in the plugin to verify dts
        if (Strings.isEmpty(serverPath)) {
            String lsp = "LSPServer.exe";
            if (LspConfigUtils.isMac()) {
                lsp = "LSPServer";
            }
            serverPath = Paths.get(sdkPath.toString(), "build-tools", "tools", "bin", lsp).toString();
        }
        // make sure cangjie lsp exists and project is not null
        if (project.getBasePath() != null && !Strings.isEmpty(serverPath)) {
            VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null) {
                DocumentEventManager.removeUri(projectDir.getUrl(), true);
            }
            CrashLogPackager.configCrashLogPackager(PathManager.getLogPath(),
                    Paths.get(project.getBasePath(), DOT_IDEA_DIR, DOT_DEVECO_DIR,
                            CANGJIE_CACHE_DIR, LOG_DIR).toString());
            initServerDefinition(project, serverPath, isInSdk);
            CangjieCommandListener listener = new CangjieCommandListener(project);
            EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();
            eventMulticaster.addDocumentListener(listener, project);

            // 注册CommandListener
            project.getMessageBus().connect().subscribe(CommandListener.TOPIC, listener);
        } else {
            String message = CangjieBundle.message("deveco.lspserver.not.exist");
            LOG.info(String.format(Locale.ENGLISH, "%s, Bin Path should be %s",
                    message, serverPath));
            LspConfigUtils.sendNotification(project, message);
        }
    }

    private static void initServerDefinition(Project project, String serverPath, boolean isInSdk) throws IOException {
        IntellijLanguageClient.setTimeout(Timeouts.INIT, 50000);
        IntellijLanguageClient.setTimeout(Timeouts.DEFINITION, 200);
        // setup clangd extensions
        String fileTypes = "cj";
        Map<String, String> languageIds = new HashMap<>();
        languageIds.put("cj", "Cangjie");
        String mainDir = getMainDir(project.getBasePath());
        String[] cmd = getServerCmd(project, serverPath, mainDir);
        ProgressConfig.Config startingServerConfig = new ProgressConfig.Config(
                true, "Cangjie indexing...", "Initialize Cangjie language server", 100, Integer.MAX_VALUE);
        ProgressConfig progressConfig = new ProgressConfig(startingServerConfig,
                new ProgressConfig.Config(false));
        CangjieCommandsServerDefinition cangjieCommandsServerDefinition =
            new CangjieCommandsServerDefinition(fileTypes, languageIds, cmd, progressConfig,
                    new CangjieStartServerListener(project));
        cangjieCommandsServerDefinition.setProjectDir(project.getBasePath());
        // using extended lspExtensionManager to solve repeat characters in completion
        Optional<LSPExtensionManager> extensionManagerForDefinition =
                IntellijLanguageClient.getExtensionManagerForDefinition(cangjieCommandsServerDefinition);
        LSPExtensionManager manager =
                extensionManagerForDefinition.orElseGet(CangjieExtendLspExtensionManagerImpl::new);

        String[] allExtension = fileTypes.split(",");
        for (String extension : allExtension) {
            IntellijLanguageClient.addExtensionManager(extension, manager);
        }
        IntellijLanguageClient.addServerDefinition(cangjieCommandsServerDefinition, project);

        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new CangjieDocumentLink());
        project.getMessageBus().connect().subscribe(LookupManagerListener.TOPIC, new LookupManagerListener() {
            @Override
            public void activeLookupChanged(Lookup oldLookup, Lookup newLookup) {
                if (newLookup == null) {
                    return;
                }
                newLookup.addLookupListener(new CangjieLookupListener(project));
            }
        });

        IntellijLanguageClient.initProjectConnections(project);

        // this is for default opened editors along with the project open
        // get all editors with the right extension and call editorOpened
        CangjieFileListenerRegisterSingleton.registerVFSListener();
    }

    @NotNull
    private static String[] getServerCmd(Project project, String serverPath, String mainDir) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return new String[0];
        }
        Path logFilePath = Paths.get(basePath, DOT_IDEA_DIR, DOT_DEVECO_DIR, CANGJIE_CACHE_DIR, LOG_DIR);
        Path cachePath = Paths.get(basePath, DOT_IDEA_DIR, DOT_DEVECO_DIR, CANGJIE_CACHE_DIR);
        String enableCangjieLog = String.valueOf(System.getProperties().get("deveco.is.enableCangjieLog"));
        if (enableCangjieLog.equalsIgnoreCase("true")) {
            return new String[]{serverPath, mainDir, LOG_PATH_ARG + logFilePath,
                    LOG_ENABLE_ARG + "true", CRASH_REPORT_ARG, CACHE_PATH_ARG + cachePath};
        }
        return new String[]{serverPath, mainDir, LOG_ENABLE_ARG + "false", CACHE_PATH_ARG + cachePath};
    }
}
