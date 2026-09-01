/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import static com.huawei.ideacj.lsp.utils.LspConfigUtils.getSdkPath;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.isMac;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.MAC_BASH;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.MAC_BASH_OPTION;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.WIN_BAT;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.WIN_BAT_OPTION;

import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.ideacj.notification.NotificationUtil;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Cangjie command executor
 *
 * @since 2025-05-06
 */
public class CangjieCommandExecutor {
    /**
     * Exec cangjie command
     *
     * @param project project
     * @param command cangjie command
     * @param showProgress show progress
     * @param progressMsg progress message
     * @param callback callback
     */
    public static void executeCommand(@Nullable Project project,
                                      @NotNull String command,
                                      boolean showProgress,
                                      String progressMsg,
                                      @Nullable Consumer<CommandResult> callback) {
        List<String> commands = confEnv(project, command);
        Optional<File> workingDirOpt = getWorkingDir(project);
        File file = null;
        if (workingDirOpt.isPresent()) {
            file = workingDirOpt.get();
        }
        if (showProgress) {
            executeWithProgress(project, commands, file, progressMsg, callback);
        } else {
            executeWithoutProgress(project, commands, file);
        }
    }

    /**
     * Configure the command execution environment
     *
     * @param project project
     * @param command cangjie command
     * @return commands
     */
    public static List<String> confEnv(@Nullable Project project,
                                  @NotNull String command) {
        List<String> commands = new ArrayList<>();
        String sdkPath = getSdkPath(project);
        if (Strings.isEmpty(sdkPath)) {
            return commands;
        }
        boolean isMac = LspConfigUtils.isMac();
        String envSetupPath = Paths.get(
            sdkPath,
            "build-tools",
            isMac ? "envsetup.sh" : "envsetup.bat"
        ).toString();
        if (isMac) {
            commands.add(MAC_BASH);
            commands.add(MAC_BASH_OPTION);
            commands.add("source " + quoteForZsh(project, envSetupPath) + " && " + command);
        } else {
            commands.add(WIN_BAT);
            commands.add(WIN_BAT_OPTION);
            commands.add("\"" + quoteForCmdPath(project, envSetupPath) + " && " + command + "\"");
        }
        return commands;
    }

    /**
     * Quote a string for safe use in zsh/bash single-quoted context
     *
     * @param project Project
     * @param value the string to quote
     * @return the quoted string
     */
    public static String quoteForZsh(@Nullable Project project, @NotNull String value) {
        checkNoNul(project, value);
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String quoteForCmdPath(@Nullable Project project, @NotNull String value) {
        checkNoNul(project, value);

        if (value.indexOf('"') >= 0
                || value.indexOf('%') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            String exception = "Unsafe SDK path: " + value;
            NotificationUtil.notifyInfo(exception, project, NotificationType.WARNING);
            throw new IllegalArgumentException(exception);
        }
        return "\"" + value + "\"";
    }

    private static void checkNoNul(@Nullable Project project, @NotNull String value) {
        if (value.indexOf('\0') >= 0) {
            String exception = "Unsafe SDK path contains NUL";
            NotificationUtil.notifyInfo(exception, project, NotificationType.WARNING);
            throw new IllegalArgumentException(exception);
        }
    }

    /**
     * Execution mode with progress
     *
     * @param project project
     * @param command command
     * @param workingDir workingDir
     * @param progressMsg progress message
     * @param callback callback
     */
    public static void executeWithProgress(Project project,
                                           List<String> command,
                                           File workingDir,
                                           String progressMsg,
                                           @Nullable Consumer<CommandResult> callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Executing Cangjie Command", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(
                        (progressMsg == null || progressMsg.isEmpty())
                                ? "Cangjie command: " + String.join(" ", command)
                                : progressMsg
                );
                CommandResult result = executeProcess(project, command, workingDir, indicator);
                if (callback != null) {
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(result));
                }
            }
        });
    }

    /**
     * Execution without progress
     *
     * @param project project
     * @param command command
     * @param workingDir workingDir
     * @return command result
     */
    private static CommandResult executeWithoutProgress(@Nullable Project project, List<String> command,
        File workingDir) {
        return executeProcess(project, command, workingDir, null);
    }

    /**
     * Get the root directory where the command is executed
     *
     * @param project project
     * @return root directory
     */
    private static Optional<File> getWorkingDir(Project project) {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        if (selectedFiles.length == 0) {
            return Optional.empty();
        }
        VirtualFile file = selectedFiles[0];
        File workingDir = file.isDirectory()
                ? new File(file.getPath())
                : new File(file.getParent().getPath());
        return Optional.of(workingDir);
    }

    /**
     * Get the system default encoding
     *
     * @return system default encoding
     */
    private static String getSystemEncoding() {
        if (isMac()) {
            return "UTF-8";
        }
        return "GBK";
    }

    /**
     * Execute process
     *
     * @param project project
     * @param command command
     * @param workingDir workingDir
     * @param indicator progress indicator
     * @return command result
     */
    private static CommandResult executeProcess(@Nullable Project project, List<String> command,
                                                File workingDir,
                                                @Nullable ProgressIndicator indicator) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (project != null) {
            ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
            Map<String, String> projectEnvs = CangjieEnvUtils.getProjectEnvs(projectModel.getProject());
            if (projectEnvs != null) {
                processBuilder.environment().putAll(projectEnvs);
            }
        }
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        try {
            Process process = processBuilder.start();
            List<String> outputLines = new ArrayList<>();
            List<String> errorLines = new ArrayList<>();
            Optional<CommandResult> result;
            try (InputStream inputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, getSystemEncoding()))) {
                result = getSuccessResult(indicator, reader, process, outputLines);
            }
            int exitCode = process.waitFor();
            if (result.isPresent()) {
                return result.get();
            }
            try (InputStream errorStream = process.getErrorStream();
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(errorStream, getSystemEncoding()))) {
                return getErrorResult(errorReader, errorLines, exitCode, outputLines);
            }
        } catch (IOException | InterruptedException e) {
            return new CommandResult(-1, List.of(), List.of("Error executing command: " + e.getMessage()));
        }
    }

    /**
     * get success result
     *
     * @param indicator indicator
     * @param reader reader
     * @param process process
     * @param outputLines outputLines
     * @return success result
     * @throws IOException e
     */
    private static Optional<CommandResult> getSuccessResult(@Nullable ProgressIndicator indicator,
                                                  BufferedReader reader,
                                                  Process process,
                                                  List<String> outputLines) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (indicator != null) {
                if (indicator.isCanceled()) {
                    process.destroy();
                    return Optional.of(new CommandResult(-1, List.of("Command canceled"), List.of()));
                }
                indicator.setText2(line);
            }
            outputLines.add(line);
        }
        return Optional.empty();
    }

    /**
     * Get error result
     *
     * @param errorReader errorReader
     * @param errorLines errorLines
     * @param exitCode exitCode
     * @param outputLines outputLines
     * @return error result
     * @throws IOException e
     */
    private static CommandResult getErrorResult(BufferedReader errorReader,
                                                List<String> errorLines,
                                                int exitCode,
                                                List<String> outputLines) throws IOException {
        String line;
        while ((line = errorReader.readLine()) != null) {
            errorLines.add(line);
        }
        return new CommandResult(exitCode, outputLines, errorLines);
    }

    /**
     * Execute command result
     */
    public static class CommandResult {
        private final int exitCode;
        private final List<String> output;
        private final List<String> error;

        /**
         * command result
         *
         * @param exitCode exit code
         * @param output output
         * @param error error
         */
        public CommandResult(int exitCode, List<String> output, List<String> error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        /**
         * get exit code
         *
         * @return exit code
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * get output
         *
         * @return output
         */
        public List<String> getOutput() {
            return output;
        }

        /**
         * get error msg
         *
         * @return error msg
         */
        public List<String> getError() {
            return error;
        }

        /**
         * is success
         *
         * @return is success
         */
        public boolean isSuccess() {
            return exitCode == 0;
        }

        /**
         * to string
         *
         * @return str
         */
        @Override
        public String toString() {
            return "CommandResult{"
                    + "exitCode=" + exitCode
                    + ", output=" + output
                    + ", error=" + error
                    + '}';
        }
    }
}