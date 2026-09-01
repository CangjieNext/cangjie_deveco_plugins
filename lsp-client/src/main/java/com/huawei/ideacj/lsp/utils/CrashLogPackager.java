/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Package lsp log and deveco log when lsp crashed
 *
 * @since 2025-03-06
 */
public class CrashLogPackager {
    private static final Logger LOG = Logger.getInstance(CrashLogPackager.class);
    private static final String IDEA_LOG = "idea.log";

    private static final String LSP_LOG = "log.txt";

    private static final String CRASH_LOG_ZIP = "crash_log.zip";

    private static File ideaLogFile;

    private static File lspLogFile;

    private static Path storePath;

    /**
     * Configure the pathes info
     *
     * @param ideaLogDir the idea log dir
     * @param lspLogDir the lsp log dir
     */
    public static void configCrashLogPackager(@NotNull String ideaLogDir, @NotNull String lspLogDir) {
        CrashLogPackager.ideaLogFile = Path.of(ideaLogDir, IDEA_LOG).toFile();
        CrashLogPackager.lspLogFile = Path.of(lspLogDir, LSP_LOG).toFile();
        CrashLogPackager.storePath = Path.of(lspLogDir);
    }

    /**
     *  Package idea log and lsp log
     */
    public static void packageLogFiles() {
        if (!storePath.toFile().exists()) {
            return;
        }

        File[] logFiles = {ideaLogFile, lspLogFile};
        File storeFile = Path.of(storePath.toString(), CRASH_LOG_ZIP).toFile();
        storeFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(storeFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : logFiles) {
                if (!file.exists()) {
                    continue;
                }
                writeLog(file, zos);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void writeLog(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
    }
}
