/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Change detection context.
 *
 * @since 2026-3-14
 */
public class ChangeDetectionContext {
    private static final Logger LOG = Logger.getInstance(ChangeDetectionContext.class);

    private static final String EXCLUDE_FOLDER_NAME = ".build-logs";

    private final Path targetFolder;

    private final Map<String, FileInfo> initialFolderState;

    private final Map<String, FileInfo> currentFolderState;

    private final AtomicBoolean changeDetected;

    /**
     * 记录目标文件夹的初始状态。
     *
     * @param targetFolder 需要监控变更的目标文件夹。
     */
    public ChangeDetectionContext(@NotNull Path targetFolder) {
        this.targetFolder = targetFolder;
        this.initialFolderState = new HashMap<>();
        this.currentFolderState = new HashMap<>();
        this.changeDetected = new AtomicBoolean(false);
        recordFolderState();
    }

    /**
     * 遍历文件树并检测新增、修改和删除。
     *
     * @return 如果检测到任何变化（新增、修改、删除），则返回 {@code true}，否则返回 {@code false}。
     */
    public boolean detectChanges() {
        // 在每次检测前重置 currentFolderState 和 changeDetected 标志
        currentFolderState.clear();
        changeDetected.set(false);

        try {
            Files.walkFileTree(targetFolder, new SimpleFileVisitor<>() {
                @Override
                @NotNull
                public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                    if (changeDetected.get()) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (dir.getFileName() != null && EXCLUDE_FOLDER_NAME.equals(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NotNull
                public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
                    if (changeDetected.get()) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (handleFileChangeDetection(file, attrs)) {
                        // 如果当前文件有变化，则可以终止遍历
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NotNull
                public FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) throws IOException {
                    LOG.warn(
                        "Failed to visit file during change detection for folder: " + targetFolder + ", file: " + file);
                    throw exc;
                }
            });
        } catch (IOException e) {
            if (changeDetected.get()) {
                return true;
            }
            LOG.warn("Failed to walk folder tree during change detection for folder: " + targetFolder);
            changeDetected.set(true);
            return true;
        }

        // 遍历完成后，检查文件删除
        if (checkForDeletedFiles()) {
            changeDetected.set(true);
            return true;
        }

        // 处理初始状态为空，当前状态不为空的情况 (新增了大量文件，或者文件夹是空的但现在有了文件)
        if (initialFolderState.isEmpty() && !currentFolderState.isEmpty()) {
            LOG.warn("Change detected in " + targetFolder + ": Initial state was empty, but current has files.");
            changeDetected.set(true);
            return true;
        }

        return changeDetected.get();
    }

    /**
     * 记录单个文件夹的初始状态
     */
    private void recordFolderState() {
        try {
            Files.walkFileTree(targetFolder, new SimpleFileVisitor<>() {
                @Override
                @NotNull
                public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                    // 排除 .build-logs 文件夹
                    if (dir.getFileName() != null && EXCLUDE_FOLDER_NAME.equals(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NotNull
                public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
                    String relativePath = targetFolder.relativize(file).toString();
                    initialFolderState.put(relativePath,
                        new FileInfo(attrs.size(), attrs.lastModifiedTime().toMillis()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NotNull
                public FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) throws IOException {
                    LOG.warn(
                        "Failed to visit file during initial state recording for folder: " + targetFolder + ", file: "
                            + file);
                    throw exc;
                }
            });
        } catch (IOException e) {
            LOG.warn("Failed to record complete initial state for folder: " + targetFolder
                + ". Its changes might not be tracked accurately.");
        }
    }

    /**
     * 处理文件变更检测的逻辑。
     *
     * @param file 文件路径。
     * @param attrs 文件的基本属性。
     * @return 如果检测到文件有变更（新增或修改），则返回 {@code true}，否则返回 {@code false}。
     */
    private boolean handleFileChangeDetection(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
        String relativePath = targetFolder.relativize(file).toString();
        FileInfo currentInfo = new FileInfo(attrs.size(), attrs.lastModifiedTime().toMillis());
        currentFolderState.put(relativePath, currentInfo); // 记录当前状态

        FileInfo initialInfo = initialFolderState.get(relativePath);
        if (initialInfo == null) {
            LOG.warn("Change detected in " + targetFolder + ": New file added: " + relativePath);
            changeDetected.set(true);
            return true;
        }
        if (!initialInfo.equals(currentInfo)) {
            LOG.warn("Change detected in " + targetFolder + ": File modified: " + relativePath);
            changeDetected.set(true);
            return true;
        }
        return false;
    }

    /**
     * 检查是否有文件被删除。
     *
     * @return 如果有文件被删除，则返回 {@code true}，否则返回 {@code false}。
     */
    private boolean checkForDeletedFiles() {
        for (String relativePath : initialFolderState.keySet()) {
            if (!currentFolderState.containsKey(relativePath)) {
                LOG.warn("Change detected in " + targetFolder + ": File deleted: " + relativePath);
                changeDetected.set(true);
                return true;
            }
        }
        return false;
    }
}