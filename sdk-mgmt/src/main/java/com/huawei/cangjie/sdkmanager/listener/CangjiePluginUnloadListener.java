/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.sdkmanager.listener;

import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.CANGJIE_PLUGIN_ID;
import static com.huawei.cangjie.sdkmanager.constants.SdkConstants.UNKNOWN_VERSION;

import com.huawei.cangjie.sdkmanager.util.CangjieSdkUtil;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Listens for unloading of the Cangjie plugin.
 *
 * @since 2026-09-05
 */
public final class CangjiePluginUnloadListener implements PluginStateListener {
    private static final Logger LOG = Logger.getInstance(CangjiePluginUnloadListener.class);
    private static final String INSTALLED_VERSION_FILE = "cangjie-plugin-version.txt";
    private static final String PENDING_DELETE_VERSION_FILE = "cangjie-plugin-pending-delete-version.txt";

    public CangjiePluginUnloadListener() {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(CANGJIE_PLUGIN_ID));
        if (plugin != null && StringUtils.isBlank(readInstalledPluginVersion())) {
            writeInstalledPluginVersion(plugin.getVersion());
        }
    }

    @Override
    public void install(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (!CANGJIE_PLUGIN_ID.equals(pluginDescriptor.getPluginId().getIdString())) {
            return;
        }
        String previousPluginVersion = readInstalledPluginVersion();
        String newPluginVersion = pluginDescriptor.getVersion();
        String pendingDeleteVersion = readPendingDeletePluginVersion();
        if (isSameMajorMinorVersion(previousPluginVersion, newPluginVersion)) {
            updatePendingDeleteVersion(previousPluginVersion, deleteSdkDirectory(previousPluginVersion));
        } else if (StringUtils.isNotBlank(previousPluginVersion)) {
            LOG.info("Skip deleting SDK because plugin major/minor versions differ: "
                    + previousPluginVersion + " -> " + newPluginVersion);
        }
        if (isSameMajorMinorVersion(pendingDeleteVersion, newPluginVersion)
                && !pendingDeleteVersion.equals(previousPluginVersion)) {
            updatePendingDeleteVersion(pendingDeleteVersion, deleteSdkDirectory(pendingDeleteVersion));
        }
        writeInstalledPluginVersion(newPluginVersion);
    }

    @Override
    public void uninstall(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (!CANGJIE_PLUGIN_ID.equals(pluginDescriptor.getPluginId().getIdString())) {
            return;
        }
        if (onCangjiePluginUnloaded(pluginDescriptor.getVersion())) {
            deleteInstalledPluginVersion();
            deletePendingDeletePluginVersion();
        }
    }

    /**
     * Executes cleanup required after the Cangjie plugin is unloaded.
     *
     * <p>Keep this hook local to the SDK management module so unload-specific
     * cleanup can be added without changing plugin descriptor wiring.</p>
     */
    private boolean onCangjiePluginUnloaded(String pluginVersion) {
        return deleteSdkDirectory(pluginVersion);
    }

    private boolean deleteSdkDirectory(String pluginVersion) {
        String sdkVersion = StringUtils.isBlank(pluginVersion) ? UNKNOWN_VERSION : pluginVersion;
        Path sdkRoot = CangjieSdkUtil.getSdkDefaultPath().toAbsolutePath().normalize();
        Path sdkDirectory = CangjieSdkUtil.getSdkExpectPath(sdkVersion).toAbsolutePath().normalize();
        if (!sdkRoot.equals(sdkDirectory.getParent())) {
            LOG.warn("Refusing to delete SDK path outside SDK root: " + sdkDirectory);
            return false;
        }
        File directory = sdkDirectory.toFile();
        if (!directory.exists()) {
            LOG.info("Cangjie SDK directory does not exist: " + directory);
            return true;
        }
        try {
            CangjieSdkUtil.stopLspServers();
            FileUtils.deleteDirectory(directory);
            LOG.info("Deleted Cangjie SDK directory: " + directory);
            return true;
        } catch (IOException exception) {
            LOG.warn("Failed to delete Cangjie SDK directory: " + directory + " cause: " + exception.getMessage());
            return false;
        }
    }

    private boolean isSameMajorMinorVersion(String previousVersion, String newVersion) {
        if (StringUtils.isBlank(previousVersion) || StringUtils.isBlank(newVersion)) {
            return false;
        }
        String[] previousParts = previousVersion.trim().split("\\.");
        String[] newParts = newVersion.trim().split("\\.");
        return previousParts.length >= 2 && newParts.length >= 2
                && previousParts[0].equals(newParts[0])
                && previousParts[1].equals(newParts[1]);
    }

    private String readInstalledPluginVersion() {
        Path versionFile = Path.of(PathManager.getConfigPath(), INSTALLED_VERSION_FILE);
        if (!Files.isRegularFile(versionFile)) {
            return StringUtils.EMPTY;
        }
        try {
            return Files.readString(versionFile, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            LOG.warn("Failed to read installed Cangjie plugin version: " + exception.getMessage());
            return StringUtils.EMPTY;
        }
    }

    private void writeInstalledPluginVersion(String pluginVersion) {
        String version = StringUtils.isBlank(pluginVersion) ? UNKNOWN_VERSION : pluginVersion;
        Path versionFile = Path.of(PathManager.getConfigPath(), INSTALLED_VERSION_FILE);
        try {
            Files.writeString(versionFile, version, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOG.warn("Failed to save installed Cangjie plugin version: " + exception.getMessage());
        }
    }

    private String readPendingDeletePluginVersion() {
        Path versionFile = Path.of(PathManager.getConfigPath(), PENDING_DELETE_VERSION_FILE);
        if (!Files.isRegularFile(versionFile)) {
            return StringUtils.EMPTY;
        }
        try {
            return Files.readString(versionFile, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            LOG.warn("Failed to read pending Cangjie SDK version: " + exception.getMessage());
            return StringUtils.EMPTY;
        }
    }

    private void updatePendingDeleteVersion(String pluginVersion, boolean deleteSuccess) {
        if (deleteSuccess) {
            deletePendingDeletePluginVersion();
            return;
        }
        Path versionFile = Path.of(PathManager.getConfigPath(), PENDING_DELETE_VERSION_FILE);
        try {
            Files.writeString(versionFile, pluginVersion, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOG.warn("Failed to save pending Cangjie SDK version: " + exception.getMessage());
        }
    }

    private void deleteInstalledPluginVersion() {
        try {
            Files.deleteIfExists(Path.of(PathManager.getConfigPath(), INSTALLED_VERSION_FILE));
        } catch (IOException exception) {
            LOG.warn("Failed to clear installed Cangjie plugin version: " + exception.getMessage());
        }
    }

    private void deletePendingDeletePluginVersion() {
        try {
            Files.deleteIfExists(Path.of(PathManager.getConfigPath(), PENDING_DELETE_VERSION_FILE));
        } catch (IOException exception) {
            LOG.warn("Failed to clear pending Cangjie SDK version: " + exception.getMessage());
        }
    }
}
