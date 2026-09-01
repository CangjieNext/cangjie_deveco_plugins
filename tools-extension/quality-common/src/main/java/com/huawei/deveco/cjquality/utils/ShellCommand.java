/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils;

import static com.huawei.deveco.cjquality.constants.CangjieConstants.CANGJIE_HOME;
import static com.huawei.deveco.cjquality.constants.CmdConstants.MAC_PATH;
import static com.huawei.deveco.cjquality.constants.CmdConstants.WIN_PATH;
import static com.huawei.deveco.cjquality.utils.CangjieCompileArg.getRuntimePath;
import static com.huawei.deveco.cjquality.utils.PathUtils.getCangjieSdkPath;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * ShellCommand
 *
 * @since 2022-10-20
 */
public class ShellCommand {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(ShellCommand.class);

    private static final String DEVECO_CANGJIE_HOME = "DEVECO_CANGJIE_HOME";

    private static final String AARCH64_LIBS = "AARCH64_LIBS";

    private static final String AARCH64_MACRO_LIBS = "AARCH64_MACRO_LIBS";

    private static final String X86_64_LIBS = "X86_64_LIBS";

    private static final String X86_64_MACRO_LIBS = "X86_64_MACRO_LIBS";

    private static final String DYLD_LIBRARY_PATH = "DYLD_LIBRARY_PATH";

    private static final String DYLD_FALLBACK_LIBRARY_PATH = "DYLD_FALLBACK_LIBRARY_PATH";

    /**
     * execute command
     *
     * @param command command
     * @param executePath executePath
     * @param project project
     * @return execute result
     */
    public static Optional<ExecuteResult> executeCommand(List<String> command, String executePath, Project project) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process;
        Optional<String> cangjieSdkPathOptional = getCangjieSdkPath(project);
        if (cangjieSdkPathOptional.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> compilerPathOptional = PathUtils.getCompilerPath(project);
        if (compilerPathOptional.isEmpty()) {
            return Optional.empty();
        }
        processBuilder.environment().put(CANGJIE_HOME, compilerPathOptional.get());
        processBuilder.environment().put(DEVECO_CANGJIE_HOME, cangjieSdkPathOptional.get());
        String path;
        if (SystemInfo.isWindows) {
            path = WIN_PATH;
            processBuilder.environment().put(AARCH64_LIBS, PathUtils.getAarchLibPath(cangjieSdkPathOptional.get()));
            processBuilder.environment()
                .put(AARCH64_MACRO_LIBS, PathUtils.getAarchMacroLibsPath(cangjieSdkPathOptional.get()));
            processBuilder.environment().put(X86_64_LIBS, PathUtils.getX86LibsPath(cangjieSdkPathOptional.get()));
            processBuilder.environment()
                .put(X86_64_MACRO_LIBS, PathUtils.getX86MacroLibsPath(cangjieSdkPathOptional.get()));
        } else {
            path = MAC_PATH;
            String runtimePath = getRuntimePath();
            processBuilder.environment().put(DYLD_LIBRARY_PATH, runtimePath);
            processBuilder.environment().put(DYLD_FALLBACK_LIBRARY_PATH, runtimePath);
            processBuilder.environment().put(AARCH64_LIBS, PathUtils.getAarchLibPath(cangjieSdkPathOptional.get()));
            processBuilder.environment()
                .put(AARCH64_MACRO_LIBS, PathUtils.getMacAarchMacroLibPath(cangjieSdkPathOptional.get()));
        }
        Map<String, String> env = processBuilder.environment();
        String pathEnv = env.get(path);
        String cjcEnvs = CangjieCompileArg.getCjcEnvs();
        String totalEnv = cjcEnvs.concat(pathEnv);
        processBuilder.environment().put(path, totalEnv);
        processBuilder.command(command);
        processBuilder.directory(new File(executePath));
        processBuilder.redirectErrorStream(true);
        ExecutorService threadPool = new ThreadPoolExecutor(0, 64, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        try {
            LOGGER.info("Starting shell command...");
            process = processBuilder.start();
            Future<String> outputFuture = threadPool.submit(new StreamCallable(process.getInputStream()));
            String output = outputFuture.get();
            LOGGER.info("Exe command end, output is {}", output);
            return Optional.of(new ExecuteResult(process.waitFor(), output));
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.warn("Failed to execute shell command.");
            return Optional.empty();
        } finally {
            threadPool.shutdown();
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
                LOGGER.warn("Error reading next line!!");
                return "";
            }
        }
    }
}
