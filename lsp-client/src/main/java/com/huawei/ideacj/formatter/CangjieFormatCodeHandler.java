/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.notification.NotificationUtil;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * CangjieFormatCodeManager
 *
 * @since 2025-07-11
 */
public class CangjieFormatCodeHandler {
    private static final Logger LOG = Logger.getInstance(CangjieFormatCodeHandler.class);

    private static final String CJFMT_WINDOWS = "cjfmt.exe";

    private static final String CJFMT_MAC = "cjfmt";

    private static final String REFORMAT_FILE = "-f";

    private static final String REFORMAT_LINE = "-l";

    private static final String COLON = ":";

    private static final String RUNTIMEPATH = "build-tools/runtime/lib/windows_x86_64_cjnative";

    private static final String MAC_X86_RUNTIME_PATH = "build-tools/runtime/lib/darwin_x86_64_cjnative";

    private static final String MAC_AARCH64_RUNTIME_PATH = "build-tools/runtime/lib/darwin_aarch64_cjnative";

    private static final String MAC_TOOLS_LIB_PATH = "build-tools/tools/lib";

    private static final String LINUX_RUNTIME_PATH = "build-tools/runtime/lib/linux_ohos_aarch64_cjnative";

    private static final String DYLD_LIBRARY_PATH = "DYLD_LIBRARY_PATH";

    private static final String DYLD_FALLBACK_LIBRARY_PATH = "DYLD_FALLBACK_LIBRARY_PATH";

    private static final String MAC_PATH = "PATH";

    private static final String WIN_PATH = "Path";

    private static String sdkPath = "";

    /**
     * execute CodeFormat
     *
     * @param project Project
     * @param editor Editor
     * @param lineRanges LineRanges
     */
    public static void executeCodeFormat(Project project, Editor editor, Collection<LineRange> lineRanges) {
        if (editor == null || project == null) {
            return;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            return;
        }
        sdkPath = LspConfigUtils.getSdkPath(project);
        if (sdkPath.isEmpty()) {
            return;
        }

        Document document = editor.getDocument();
        String originalContent = document.getText();

        Optional<ExecuteResult> executeResult = formatSelectedCode(file.getVirtualFile(), project, editor,
            document, lineRanges);
        if (executeResult.isEmpty()) {
            LOG.warn("Reformat Selected Code error when lsp invoke cjformat.");
            NotificationUtil.notifyInfo(CangjieBundle.message("lsp.format.fail"),
                    project, NotificationType.INFORMATION);
            return;
        }
        if (executeResult.get().exitCode() != 0) {
            LOG.warn("reformat code failed, exit code is " + executeResult.get().exitCode());
            NotificationUtil.notifyInfo(executeResult.get().executeOut(), project, NotificationType.INFORMATION);
            return;
        }

        String formattedContent = executeResult.get().executeOut();
        if (!originalContent.equals(formattedContent)) {
            if (CommandProcessor.getInstance().getCurrentCommand() != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    document.setText(formattedContent);
                    UndoUtil.markPsiFileForUndo(file);
                });
            } else {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.setText(formattedContent);
                    UndoUtil.markPsiFileForUndo(file);
                });
            }
        }
    }

    private static String readFileContent(VirtualFile file) {
        try {
            return new String(Files.readAllBytes(file.toNioPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read formatted file content", e);
            return "";
        }
    }

    private static String readFileContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read formatted file content", e);
            return "";
        }
    }

    private static Optional<ExecuteResult> formatSelectedCode(VirtualFile file, Project project, Editor editor,
                                                              Document document, Collection<LineRange> lineRanges) {
        Optional<String> exePathOptional = getExePath();
        if (exePathOptional.isEmpty()) {
            LOG.warn("Reformat Selected Code error, get sdk path is fail.");
            return Optional.of(new ExecuteResult(1, "Please check Cangjie SDK path."));
        }

        Path tempFile = null;
        try {
            String fileContent = document.getText();
            if (fileContent.isEmpty()) {
                return Optional.of(new ExecuteResult(1, "Failed to read file content"));
            }

            tempFile = Files.createTempFile("cjfmt_", ".cj");
            Files.writeString(tempFile, fileContent, StandardCharsets.UTF_8);
            List<LineRange> sLineRanges = lineRanges.stream()
                    .sorted(Comparator.comparingInt(LineRange::startLine).reversed())
                    .toList();
            String currentContent = fileContent;
            for (LineRange lineRange: sLineRanges) {
                List<String> processArgs = getFormatToolPath(exePathOptional.get());
                processArgs.add(REFORMAT_FILE);
                processArgs.add(toSystemIndependentName(tempFile.toString()));
                processArgs.add(REFORMAT_LINE);
                processArgs.add(lineRange.startLine() + COLON + lineRange.endLine());
                addCjfmtConfigParam(processArgs, project);
                Optional<ExecuteResult> executeResult = executeCommand(processArgs, exePathOptional.get());
                if (executeResult.isEmpty() || executeResult.get().exitCode() != 0) {
                    LOG.warn("cjfmt format failed, skipping format");
                    return Optional.empty();
                }
                currentContent = readFileContent(tempFile.toFile());
            }
            return Optional.of(new ExecuteResult(0, currentContent));
        } catch (IOException e) {
            LOG.warn("Failed to create temp file for formatting", e);
            return Optional.empty();
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temp file", e);
                }
            }
        }
    }

    /**
     * get FormatTool Path
     *
     * @param executePath String
     * @return List
     */
    public static List<String> getFormatToolPath(String executePath) {
        List<String> processArgs = new ArrayList<>();
        if (SystemInfo.isWindows) {
            processArgs.add(executePath + File.separator + CJFMT_WINDOWS);
        } else {
            processArgs.add(executePath + File.separator + CJFMT_MAC);
        }
        return processArgs;
    }

    private static void addCjfmtConfigParam(List<String> processArgs, Project project) {
        if (project.getBasePath() == null) {
            return;
        }
        Path cjfmtConfigPath = Paths.get(project.getBasePath(), ".idea", ".deveco", "cangjie", "tools",
                "cjfmt", "cangjie-format.toml");
        if (cjfmtConfigPath.toFile().exists()) {
            processArgs.add("-c");
            processArgs.add(toSystemIndependentName(cjfmtConfigPath.toString()));
        }
    }

    private static Optional<ExecuteResult> executeCommand(List<String> command, String executePath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String path;
        if (SystemInfo.isWindows) {
            path = WIN_PATH;
        } else {
            path = MAC_PATH;
            String runtimePath = getRuntimePath();
            processBuilder.environment().put(DYLD_LIBRARY_PATH, runtimePath);
            processBuilder.environment().put(DYLD_FALLBACK_LIBRARY_PATH, runtimePath);
        }
        Map<String, String> env = processBuilder.environment();
        String pathEnv = env.get(path);
        processBuilder.environment().put(path, pathEnv);
        processBuilder.command(command);
        processBuilder.directory(new File(executePath));
        processBuilder.redirectErrorStream(true);
        ExecutorService threadPool = new ThreadPoolExecutor(0, 64, 60L,
                TimeUnit.SECONDS, new SynchronousQueue<>());
        try {
            Process process;
            process = processBuilder.start();
            Future<String> outputFuture = threadPool.submit(new StreamCallable(process.getInputStream()));
            String output = outputFuture.get();
            return Optional.of(new ExecuteResult(process.waitFor(), output));
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOG.warn("Failed to execute shell command.");
            return Optional.empty();
        } finally {
            threadPool.shutdown();
        }
    }

    private static Optional<String> getExePath() {
        String cjToolPath = sdkPath + File.separator + "build-tools"
                + File.separator + "tools" + File.separator + "bin";
        return Optional.of(toSystemIndependentName(cjToolPath));
    }

    private static String getRuntimePath() {
        if (SystemInfo.isWindows) {
            return String.valueOf(Path.of(sdkPath, RUNTIMEPATH));
        } else if (SystemInfo.isMac) {
            return Path.of(sdkPath, MAC_X86_RUNTIME_PATH) + ":" + Path.of(sdkPath, MAC_AARCH64_RUNTIME_PATH) + ":"
                    + Path.of(sdkPath, MAC_TOOLS_LIB_PATH);
        } else {
            return String.valueOf(Path.of(sdkPath, LINUX_RUNTIME_PATH));
        }
    }

    private record StreamCallable(InputStream stream) implements Callable<String> {
        @Override
        public String call() {
            StringBuilder retString = new StringBuilder();
            try (BufferedReader brInputStream =
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = brInputStream.readLine()) != null) {
                    retString.append(line);
                    retString.append(System.lineSeparator());
                }
                return retString.toString();
            } catch (IOException e) {
                LOG.warn("Error reading next line!!");
                return "";
            }
        }
    }
}
