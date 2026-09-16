/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.sdkmanager.sync;

import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_PLUGIN_ID;
import static com.huawei.cangjie.sdkmanager.idea.bundle.CangjieIdeaMessageBundle.message;
import static com.huawei.cangjie.sdkmanager.idea.constants.CangjieIdeConstants.SDK_FINISH_FILE_NAME;

import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.cangjie.sdkmanager.util.CangjieSdkManagerPropertiesUtil;
import com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil;
import com.huawei.cangjie.sdkmanager.util.NotificationUtil;
import com.huawei.cangjie.sdkmanager.util.UnzipUtil;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.sync.OhosSyncInvoker;
import com.huawei.deveco.projectmodel.ohos.sync.SyncRequest;
import com.huawei.deveco.projectmodel.ohos.sync.syncinterface.ProjectInit;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Cangjie sdk sync.
 *
 * @since 2024-7-6
 */
public class CangjieSdkSync implements ProjectInit {
    private static final Logger LOG = Logger.getInstance(CangjieSdkSync.class);

    @Override
    public void projectInit(@NotNull ProjectModel projectModel, @NotNull SyncRequest syncRequest) {
        AnAction anAction = retryInstallSdkAction(projectModel, syncRequest);
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(CANGJIE_PLUGIN_ID));
        if (plugin == null) {
            NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.failed"),
                message("cangjie.plugin.not.exist"), projectModel.getProject(), NotificationType.ERROR, anAction);
            return;
        }
        Path pluginPath = plugin.getPluginPath();
        String zipFileName = getSdkZipFileName();
        File zipFile = pluginPath.resolve(zipFileName).toFile();
        if (!zipFile.exists()) {
            return;
        }
        doInstallCangjieSdkSync(pluginPath, zipFile, projectModel, anAction, plugin.getVersion());
    }

    private String getSdkZipFileName() {
        String zipFileName = "";
        if (SystemInfo.isWindows) {
            zipFileName = CangjieSdkManagerPropertiesUtil.INSTANCE.getValue("cangjie.sdk.window.zip");
            return zipFileName;
        }
        if (SystemInfo.isMac) {
            if (CpuArch.isArm64()) {
                zipFileName = CangjieSdkManagerPropertiesUtil.INSTANCE.getValue("cangjie.sdk.mac.aarch64.zip");
            } else {
                zipFileName = CangjieSdkManagerPropertiesUtil.INSTANCE.getValue("cangjie.sdk.mac.x64.zip");
            }
        }
        return zipFileName;
    }

    /**
     * Do install cangjie sdk sync.
     *
     * @param pluginPath the plugin path
     * @param zipFile the zip file
     * @param projectModel the project model
     * @param anAction anAction
     */
    public static void doInstallCangjieSdkSync(Path pluginPath, File zipFile, ProjectModel projectModel,
        AnAction anAction) {
        doInstallCangjieSdkSync(pluginPath, zipFile, projectModel, anAction,
            CangjieSdkUtil.getCangjiePluginShortVersion());
    }

    private static void doInstallCangjieSdkSync(Path pluginPath, File zipFile, ProjectModel projectModel,
        AnAction anAction, String pluginVersion) {
        File finishFile = pluginPath.resolve(SDK_FINISH_FILE_NAME).toFile();
        String cangjieSdkPath = CangjieSdkUtil.obtainInstalledSdkPath(CangjieComponentPath.CANGJIE, true,
            projectModel.getFullCompileSdkVersion().getMajor());
        File cangjieSdkFolder = new File(cangjieSdkPath);
        boolean isSdkFolderExists = StringUtils.isNotEmpty(cangjieSdkPath) && cangjieSdkFolder.exists();
        if (finishFile.exists() && isSdkFolderExists) {
            return;
        }
        Path parentSdkPath = Paths.get(cangjieSdkPath).getParent();
        NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.start"), projectModel.getProject(),
            NotificationType.INFORMATION);
        if (!isSdkFolderExists) {
            File destCangjieSdkFile = CangjieSdkUtil.getSdkExpectPath(pluginVersion).toFile();
            deleteTempFolder(destCangjieSdkFile);
            boolean isUnzipSuccess = doUnzipCangjieSdk(zipFile, projectModel, destCangjieSdkFile, anAction);
            if (isUnzipSuccess) {
                generateFinishFile(finishFile, zipFile);
                NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.finish"), projectModel.getProject(),
                    NotificationType.INFORMATION);
            } else {
                deleteFinishFile(finishFile);
            }
        } else {
            deleteTempFolder(cangjieSdkFolder.getParentFile());
            File newFolder = parentSdkPath.resolve(".cangjie-" + new Date().getTime()).toFile();
            boolean isRenameSuccess = tryRenameCangjieFolder(cangjieSdkFolder, newFolder);
            if (!isRenameSuccess) {
                LOG.warn("cangjie folder rename error.");
                deleteFinishFile(finishFile);
                NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.failed"),
                    message("cangjie.folder.opened.message", cangjieSdkPath), projectModel.getProject(),
                    NotificationType.ERROR, anAction);
                return;
            }
            boolean isUnzipSuccess =
                doUnzipCangjieSdk(zipFile, projectModel, cangjieSdkFolder.getParentFile(), anAction);
            try {
                if (isUnzipSuccess) {
                    FileUtils.deleteDirectory(newFolder);
                    generateFinishFile(finishFile, zipFile);
                    NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.finish"), projectModel.getProject(),
                        NotificationType.INFORMATION);
                } else {
                    deleteFinishFile(finishFile);
                }
            } catch (IOException e) {
                NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.finish"),
                    message("cangjie.plugin.delete.tempFile.failed"), projectModel.getProject(),
                    NotificationType.WARNING);
                LOG.warn(e);
            }
        }
    }

    private static boolean tryRenameCangjieFolder(File cangjieSdkFolder, File newFolder) {
        boolean isRenameSuccess = false;
        int tryCount = 1;
        while (tryCount <= 4 && !isRenameSuccess) {
            isRenameSuccess = cangjieSdkFolder.renameTo(newFolder);
            if (!isRenameSuccess && tryCount < 4) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            tryCount++;
        }
        return isRenameSuccess;
    }

    private static void generateFinishFile(File finishFile, File zipFile) {
        try (FileWriter writer = new FileWriter(finishFile)) {
            writer.write(String.valueOf(new Date().getTime()));
            FileUtils.delete(zipFile);
        } catch (IOException e) {
            LOG.info("Generate cangjie sdk install finished file failed.");
        }
    }

    private static void deleteFinishFile(File finishFile) {
        if (finishFile.exists()) {
            try {
                FileUtils.delete(finishFile);
            } catch (IOException e) {
                LOG.info("Delete cangjie sdk install finished file failed.");
            }
        }
    }

    private static boolean doUnzipCangjieSdk(File zipFile, ProjectModel projectModel, File cangjieFoler,
        AnAction anAction) {
        try {
            UnzipUtil.unzip(zipFile, cangjieFoler);
        } catch (IOException e) {
            NotificationUtil.notifyInfo(message("cangjie.plugin.initialize.failed"),
                message("cangjie.decompress.file.failed"), projectModel.getProject(), NotificationType.ERROR, anAction);
            LOG.warn(e);
            return false;
        }
        return removeSdkAttribute(cangjieFoler);
    }

    private static boolean removeSdkAttribute(File cangjieFoler) {
        ExecutorService threadPool = null;
        try {
            Path sdkPath = Paths.get(cangjieFoler.getCanonicalPath()).resolve("cangjie");
            if (!SystemInfo.isMac || !sdkPath.toFile().exists()) {
                return true;
            }
            threadPool = new ThreadPoolExecutor(0, 64, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
            String command = "xattr -dr com.apple.quarantine " + sdkPath;
            Process process = Runtime.getRuntime().exec(command);
            StreamConsumer errConsumer = new StreamConsumer(process.getErrorStream(), "dealErrorStream");
            StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), "dealInputStream");
            errConsumer.start();
            outputConsumer.start();
            errConsumer.join();
            outputConsumer.join();
            Path runtimeSoPath =
                sdkPath.resolve("build-tools").resolve("tools").resolve("lib").resolve("libcangjie-lsp.dylib");
            if (SystemInfo.isMac && runtimeSoPath.toFile().exists()) {
                String destFolder = CpuArch.isArm64() ? "darwin_aarch64_cjnative" : "darwin_x86_64_cjnative";
                File destFile = sdkPath.resolve("build-tools").resolve("runtime").resolve("lib").resolve(destFolder)
                    .resolve("libcangjie-lsp.dylib").toFile();
                if (!destFile.exists()) {
                    FileUtils.createParentDirectories(destFile);
                }
                FileUtils.copyFile(runtimeSoPath.toFile(), destFile);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOG.info("Succeeded in granting permissions to Cangjie SDK.");
                return true;
            } else {
                LOG.info("Failed to grant permissions to the Cangjie SDK.");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOG.info("Failed to grant permissions to the Cangjie SDK.");
            return false;
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    private static final class StreamConsumer extends Thread {
        private final InputStream stream;

        /**
         * Instantiates a new Stream consumer.
         *
         * @param stream the stream
         * @param name the name
         */
        public StreamConsumer(InputStream stream, String name) {
            super.setName(name);
            this.stream = stream;
        }

        @Override
        public void run() {
            StringBuilder retString = new StringBuilder();
            try (BufferedReader brInputStream = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = brInputStream.readLine()) != null) {
                    retString.append(line);
                    retString.append(System.lineSeparator());
                }
                LOG.info(retString.toString());
            } catch (IOException e) {
                LOG.warn("Error reading next line!!");
            }
        }
    }

    /**
     * retry install sdk
     *
     * @param projectModel projectModel
     * @param syncRequest syncRequest
     * @return AnAction
     */
    public AnAction retryInstallSdkAction(ProjectModel projectModel, SyncRequest syncRequest) {
        return new AnAction(message("cangjie.plugin.initialize.again")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ProgressManager.getInstance().run(
                    new Task.Backgroundable(projectModel.getProject(), message("cangjie.plugin.initialize.start")) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            doRetryInstall(projectModel, syncRequest);
                        }
                    });
            }
        };
    }

    /**
     * Do retry install.
     *
     * @param projectModel the project model
     * @param syncRequest the sync request
     */
    public void doRetryInstall(ProjectModel projectModel, SyncRequest syncRequest) {
        CangjieSdkUtil.stopLspServer(projectModel);
        projectInit(projectModel, syncRequest);
        OhosSyncInvoker.getInstance().doSync(projectModel.getProject(), SyncRequest.RELOAD_PROJECT_MODEL);
    }

    private static void deleteTempFolder(File folder) {
        String pattern = "^\\.cangjie-\\d+$";
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }
        File[] subFiles = folder.listFiles();
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (!subFile.isDirectory() || !subFile.getName().matches(pattern)) {
                continue;
            }
            try {
                FileUtils.deleteDirectory(subFile);
            } catch (IOException e) {
                LOG.info("Failed to delete temporary files before installing the SDK");
            }
        }
    }
}
