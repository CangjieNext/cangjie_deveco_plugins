/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import com.intellij.openapi.util.SystemInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * File util class
 *
 * @since 2023-02-24
 */
public class FileUtil {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(FileUtil.class);

    private static final Map<String, String> DIR_PERMISSION_MAP = new HashMap<>();

    private static final String DEFAULT_PERMISSION = "rwxr-x---";

    /**
     * check whether the file path exists. if not, create new folder
     *
     * @param filePath file path
     */
    public static void createFilePath(String filePath) {
        if (Files.exists(Paths.get(filePath))) {
            return;
        }
        makeDirsWithPermission(filePath);
    }

    /**
     * make directory recursively
     *
     * @param path directory path
     */
    public static void makeDirsWithPermission(final String path) {
        try (FileSystem fileSystem = FileSystems.getDefault()) {
            Path directory = fileSystem.getPath(path);
            if (SystemInfo.isWindows) {
                Files.createDirectories(directory);
            } else {  // 非windows 指定创建目录的权限
                if (!DIR_PERMISSION_MAP.containsKey(path)) {
                    LOGGER.warn("makeDirsWithPermission path {} doesn't config permission", path);
                }
                String permission = DIR_PERMISSION_MAP.getOrDefault(path, DEFAULT_PERMISSION);
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permission);
                FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
                Files.createDirectories(directory, attr);
            }
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException | SecurityException e) {
            LOGGER.warn("Failed to create Dirs With Permission.");
        }
    }

    /**
     * write string to file
     *
     * @param filePath filePath
     * @param content content string
     */
    public static void writeStringToFile(String filePath, String content) {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            LOGGER.warn("writeStringToFile error, filepath is {}, content is {}", filePath, content);
        }
    }
}
