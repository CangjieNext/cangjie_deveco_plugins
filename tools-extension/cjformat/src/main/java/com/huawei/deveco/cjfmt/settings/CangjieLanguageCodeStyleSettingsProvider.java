/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.deveco.cjfmt.core.ReformatCodeService;
import com.huawei.deveco.cjfmt.utils.FormatConstants;
import com.huawei.deveco.cjfmt.utils.FormatUtils;
import com.huawei.ideacj.utils.LogPrinter;
import com.huawei.ideacj.filetypes.CangjieCodeFile;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.util.LocalTimeCounter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * class CangjieLanguageCodeStyleSettingsProvider
 *
 * @since 2020 -4-20
 */
public class CangjieLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(ReformatCodeService.class);

    private static String previewPreVersion = "pre";

    @Nullable
    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return CodeStyleAbstractPanel.readFromFile(this.getClass(), FormatConstants.PREVIEW_FILE);
    }

    @Override
    @NotNull
    public Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }

    @Override
    protected void customizeDefaults(@NotNull CommonCodeStyleSettings commonSettings,
        @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
        // Default both commenter options to unchecked for Cangjie: keep the
        // comment indented with the code (not at the first column) and without
        // an injected space after "//". Applied only when defaults are built,
        // never overriding values the user has explicitly chosen.
        commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false;
        commonSettings.LINE_COMMENT_ADD_SPACE = false;
    }

    @Override
    @Nullable
    public PsiFile createFileFromText(@NotNull Project project, @NotNull String text) {
        Path projectToolsPath = FormatUtils.getProjectToolsPath(project);
        if ("pre".equals(previewPreVersion)) {
            previewPreVersion = "";
        } else {
            previewPreVersion = "pre";
        }
        Path tempPreviewPath = projectToolsPath.resolve("cjfmt").resolve("tempPreview" + previewPreVersion + ".cj");
        File tempFile = tempPreviewPath.toFile();
        FileWriter writer = null;
        try {
            if (tempPreviewPath.getParent() != null) {
                Files.createDirectories(tempPreviewPath.getParent());
            }
            writer = new FileWriter(tempFile);
            writer.write(text);
        } catch (IOException e) {
            LOGGER.error("Cangjie cjfmt write demo failed.", e);
            return PsiFileFactory.getInstance(project)
                .createFileFromText("tempPreview.cj", CangjieCodeFile.INSTANCE, text, LocalTimeCounter.currentTime(),
                    false);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.error("Cangjie cjfmt write demo close failed.", e);
                }
            }
        }
        Optional<VirtualFile> virtualFileOptional = convertFileToVirtualFile(tempFile);
        if (virtualFileOptional.isEmpty()) {
            return PsiFileFactory.getInstance(project)
                .createFileFromText("tempPreview.cj", CangjieCodeFile.INSTANCE, text, LocalTimeCounter.currentTime(),
                    false);
        }
        return PsiManager.getInstance(project).findFile(virtualFileOptional.get());
    }

    private Optional<VirtualFile> convertFileToVirtualFile(File file) {
        // Ensure the file exists
        if (!file.exists()) {
            return Optional.empty();
        }
        // Refresh the virtual file system to recognize the new file
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        // Find and return the VirtualFile
        VirtualFile localFile = localFileSystem.findFileByIoFile(file);
        return localFile == null ? Optional.empty() : Optional.of(localFile);
    }
}
