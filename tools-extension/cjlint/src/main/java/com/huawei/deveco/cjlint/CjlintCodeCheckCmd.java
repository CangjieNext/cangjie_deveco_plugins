/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjlint;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.getCangjieModuleSrcDir;
import static com.huawei.deveco.cjlint.CjlintEngine.LIB_PATH;
import static com.huawei.deveco.cjlint.utils.CodeCheckConstantUtil.ENGINE_NAME;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.AARCH64_LINUX_OHOS;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.BIN_DEPENDENCIES;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.CANGJIE;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.CJPM_TOML;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.HUMP_CANGJIE;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.MAIN;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.PACKAGE_OPTION;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.PATH_OPTION;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.SRC;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.TARGET;
import static com.huawei.deveco.cjquality.constants.CommonConstants.CONNECT;
import static com.huawei.deveco.cjquality.constants.CommonConstants.ENVSETUP;
import static com.huawei.deveco.cjquality.constants.CommonConstants.IMPORT_PATH;
import static com.huawei.deveco.cjquality.constants.CommonConstants.SPACE;
import static com.huawei.deveco.cjquality.utils.ModuleUtils.getModuleFromFile;
import static com.huawei.deveco.cjquality.utils.ModuleUtils.isCangjieModuleModel;
import static com.huawei.deveco.cjquality.utils.PathUtils.getProjectCanonicalPath;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.cjquality.constants.CommonConstants;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.util.PsiJsonFileUtil;
import com.huawei.deveco.cjquality.utils.CangjieCompileArg;
import com.huawei.deveco.cjquality.utils.ExecuteResult;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.deveco.cjquality.utils.NotificationUtil;
import com.huawei.deveco.cjquality.utils.PathUtils;
import com.huawei.deveco.cjquality.utils.ShellCommand;
import com.huawei.deveco.cjquality.utils.trace.TraceUtils;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.tools.idea.codecheck.core.cmd.CodeCheckCmd;
import com.huawei.tools.idea.codecheck.extensions.service.CodeCheckManager;
import com.huawei.tools.idea.codecheck.extensions.window.panel.ResultContentPanel;
import com.huawei.tools.idea.codecheck.extensions.window.problem.DefectProblemDescriptor;
import com.huawei.tools.idea.codecheck.support.exceptions.CheckException;
import com.huawei.tools.idea.codecheck.support.i18n.CodeCheckBundle;
import com.huawei.tools.idea.codecheck.support.model.CheckCodeParams;
import com.huawei.tools.idea.codecheck.support.model.CodeMarsResult;
import com.huawei.tools.idea.codecheck.utils.EditorUtil;
import com.huawei.tools.idea.codecheck.utils.FileUtil;
import com.huawei.tools.idea.codecheck.utils.PsiUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cjlint check code class
 *
 * @since 2024-04-13
 */
public class CjlintCodeCheckCmd extends CodeCheckCmd {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjlintCodeCheckCmd.class);

    private static final int COMMAND_PARAM_SIZE = 2;

    private static final int ZERO = 0;

    private static final String POINT = ".";

    private static final String COLON = ":";

    private static final String UNDER_LINE = "_";

    private static final String SUGGESTIONS_SEVERITY = "1";

    private static final String MANDATORY_SEVERITY = "2";

    private static final String SUGGESTIONS = "suggestions";

    private static final String MANDATORY = "mandatory";

    private static final String DOCS = "docs";

    private static final String MD_TYPE = ".md";

    private static final String CONFIG_TERMINAL = "-c";

    private static final String CONFIG = "config";

    private static final ExecutorService EXECUTOR_SERVICE =
        AppExecutorUtil.createBoundedScheduledExecutorService("GIT-DIFF", 2);

    private static final int TIMEOUT_SECONDS = 5; // 设置超时时间为5秒

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private final Project project;

    private final ResultContentPanel panel;

    private final List<CodeMarsResult.DefectFile> resultList = new ArrayList<>();

    private final Map<PsiFile, Set<DefectProblemDescriptor>> problemDescriptorsMap = new HashMap<>();

    private final Map<String, CodeMarsResult.DefectFile> defectsMap;

    private final List<String> command = new ArrayList<>();

    private final CheckCodeParams checkCodeParams;

    private String cjOutPath = "";

    // 本项目临时存放CJLint配置文件的绝对路径
    private String cacheCJLintConfigPathStr = "";

    private boolean returnEmptyResult = false;

    /**
     * cjlint code check cmd
     *
     * @param project project
     * @param indicator indicator
     * @param panel panel
     * @param commandLine command line
     * @param checkCodeParams checkCodeParams
     * @throws ExecutionException exception
     */
    public CjlintCodeCheckCmd(Project project, @NotNull ProgressIndicator indicator, ResultContentPanel panel,
        GeneralCommandLine commandLine, @NotNull CheckCodeParams checkCodeParams) throws ExecutionException {
        super(project, indicator, panel, commandLine);
        this.project = project;
        this.panel = panel;
        this.defectsMap = CodeCheckManager.getInstance(panel.getProject()).getDefectsCache();
        this.checkCodeParams = checkCodeParams;
    }

    @Override
    public void checkCode() throws ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        Map<String, String> checkMap = getCheckPath(checkCodeParams, project);
        if (checkMap.isEmpty()) {
            LOGGER.warn("Cjlint: No files can be checked.");
            CodeCheckManager.getInstance(project).setUnCheckFile(ENGINE_NAME);
            return;
        }
        Optional<String> compilerPathOptional = PathUtils.getCompilerPath(project);
        if ("".equals(CangjieCompileArg.getCjSdkPath())) {
            CangjieCompileArg.initCjSdkPath(project);
        }
        resultList.clear();
        try {
            // 仓颉规则配置通过可以通过工程根目录下自定义cjlint_rule_list.json和exclude_lists.json来替换默认规则
            boolean hasCustomizedConfig = processCustomizedCJLintConfigs();
            if (returnEmptyResult) {
                resultList.clear();
                editProblemMap(project, panel);
                return;
            }

            for (String checkModulePath : checkMap.keySet()) {
                command.clear();
                if (compilerPathOptional.isEmpty()
                    || commandLine.getParametersList().getParameters().size() < COMMAND_PARAM_SIZE) {
                    LOGGER.warn("Can't get Cangjie SDK path or command is empty");
                    return;
                }
                if (SystemInfo.isWindows) {
                    command.add(compilerPathOptional.get() + File.separator + ENVSETUP);
                    command.add(CONNECT);
                }
                cjOutPath = commandLine.getParametersList().getParameters().get(1);
                command.add(commandLine.getExePath());
                command.add(CommonConstants.FILE_PATH_TERMINAL);
                command.add(checkModulePath);
                command.add(CommonConstants.OUTPUT_TERMINAL);
                command.add(cjOutPath);
                if (StringUtils.isNotEmpty(checkMap.get(checkModulePath))) {
                    command.add(IMPORT_PATH);
                    command.add(checkMap.get(checkModulePath));
                }

                if (hasCustomizedConfig) {
                    // 设置命令行-e参数，将配置文件夹路径改为本项目缓存路径
                    String cacheCJLintConfigFatherPath =
                            cacheCJLintConfigPathStr.substring(
                                    0, cacheCJLintConfigPathStr.length() - (CONFIG.length() + 1));
                    command.add(CONFIG_TERMINAL);
                    command.add(cacheCJLintConfigFatherPath);
                }

                LOGGER.info("Cjlint command: %s".formatted(String.join(SPACE, command)));
                Optional<ExecuteResult> executeResult =
                    ShellCommand.executeCommand(command, compilerPathOptional.get(), project);
                if (executeResult.isEmpty()) {
                    LOGGER.warn("ExecuteResult is empty.");
                    return;
                }
                ProcessOutput result = new ProcessOutput(executeResult.get().exitCode());
                if (result.getExitCode() != 0) {
                    final String message =
                        String.format(Locale.ENGLISH, "Error occurred while executing code check. Exit code: %d.",
                            result.getExitCode());
                    LOGGER.warn(message);
                    throw new ExecutionException(message);
                }
                editCheckResult();
                if (StringUtils.isNotEmpty(cjOutPath)) {
                    FileUtils.delete(new File(cjOutPath));
                }
            }
            // 清空本项目临时存放的自定义CJLint配置文件
            if (StringUtils.isNotEmpty(cacheCJLintConfigPathStr)) {
                FileUtils.deleteDirectory(new File(cacheCJLintConfigPathStr));
            }
            // 根据code-linter.json5中的files和ignore配置，筛选检查结果resultList
            filterResultListByCodeLinterJson5();
            if (returnEmptyResult) {
                resultList.clear();
                editProblemMap(project, panel);
                TraceUtils.trace(TraceUtils.Action.CODE_LINTER, TraceUtils.Cause.DEFAULT, -1,
                        System.currentTimeMillis() - startTime);
                return;
            }
        } catch (IOException e) {
            TraceUtils.trace(TraceUtils.Action.CODE_LINTER, TraceUtils.Cause.CRASH);
            LOGGER.warn("Error occurred when executing code check.");
            return;
        }
        editProblemMap(project, panel);
        TraceUtils.trace(TraceUtils.Action.CODE_LINTER, TraceUtils.Cause.DEFAULT, -1,
            System.currentTimeMillis() - startTime);
    }

    void editCheckResult() throws IOException {
        final File outputFile = Paths.get(cjOutPath).toFile();
        List<DefectInfo> defectFiles =
            JSON.parseObject(FileUtil.readToString(outputFile.getCanonicalPath()), new TypeReference<>() {});
        if (defectFiles == null) {
            return;
        }
        Map<String, List<CodeMarsResult.DefectInfo>> defectMap = new HashMap<>();
        for (DefectInfo defectFile : defectFiles) {
            String filePath = new File(defectFile.getFile()).getCanonicalPath();
            CodeMarsResult.DefectInfo defectInfo = getInfo(defectFile);
            if (defectMap.containsKey(filePath)) {
                defectMap.get(filePath).add(defectInfo);
            } else {
                List<CodeMarsResult.DefectInfo> defectInfos = new ArrayList<>();
                defectInfos.add(defectInfo);
                defectMap.put(filePath, defectInfos);
            }
        }
        for (Map.Entry<String, List<CodeMarsResult.DefectInfo>> entry : defectMap.entrySet()) {
            CodeMarsResult.DefectFile defectEntry = new CodeMarsResult.DefectFile();
            defectEntry.setDefects(entry.getValue());
            defectEntry.setFilePath(entry.getKey());
            resultList.add(defectEntry);
        }
    }

    @NotNull
    private CodeMarsResult.DefectInfo getInfo(DefectInfo originDefectInfo) {
        CodeMarsResult.DefectInfo defectInfo = new CodeMarsResult.DefectInfo();
        defectInfo.setDescription(originDefectInfo.getDescription());
        defectInfo.setReportLine(originDefectInfo.getLine());
        defectInfo.setReportColumn(originDefectInfo.getColumn());
        defectInfo.setRuleId(originDefectInfo.getDefectType());
        defectInfo.setCategory("");
        if (originDefectInfo.getDefectLevel().equalsIgnoreCase(SUGGESTIONS)) {
            defectInfo.setSeverity(SUGGESTIONS_SEVERITY);
        } else if (originDefectInfo.getDefectLevel().equalsIgnoreCase(MANDATORY)) {
            defectInfo.setSeverity(MANDATORY_SEVERITY);
        } else {
            LOGGER.info("defectFile level is {}", originDefectInfo.getDefectLevel());
        }
        defectInfo.setMergeKey(originDefectInfo.hashCode() + "");
        defectInfo.setFileName(originDefectInfo.getLanguage());
        defectInfo.setRuleDocPath(getRuleDocPath(originDefectInfo));
        defectInfo.setEngineName(ENGINE_NAME);
        defectInfo.setShowIgnoreIcon(true);
        return defectInfo;
    }

    private String getRuleDocPath(DefectInfo defectInfo) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.huawei.cangjie-support-plugin"));
        if (plugin == null) {
            LOGGER.warn("Install cangjie sdk failed.");
            return "";
        }
        String description = defectInfo.getDescription();
        StringBuilder docPath = new StringBuilder();
        docPath.append(plugin.getPluginPath()).append(File.separator).append(LIB_PATH).append(File.separator)
                .append(ENGINE_NAME).append(File.separator);
        String result = description.substring(ZERO, description.indexOf(COLON)).replace(POINT, UNDER_LINE);
        docPath.append(DOCS).append(File.separator).append(result).append(MD_TYPE);
        return docPath.toString();
    }

    private void editProblemMap(@NotNull Project project, ResultContentPanel panel) {
        problemDescriptorsMap.clear();
        for (CodeMarsResult.DefectFile defectFile : resultList) {
            String defectFilePath = defectFile.getFilePath();
            final PsiFile psiFile = EditorUtil.getPsiFile(defectFilePath, project);
            if (psiFile == null) {
                continue;
            }
            List<CodeMarsResult.DefectInfo> defects = defectFile.getDefects();
            // 修改文件以后，重新扫描文件无报错，这里也要返回
            if (defects == null || defects.isEmpty()) {
                this.problemDescriptorsMap.remove(psiFile);
                continue;
            }
            defects.forEach(defect -> defect.setReportColumn(getReportColumn(psiFile, defect.getReportLine() - 1,
                defect.getReportColumn())));
            panel.incrementUpdate(defectFile);
            updateDefectCache(defectFile);
            Set<DefectProblemDescriptor> descriptors = new HashSet<>();
            defects.forEach(defect -> {
                // 过滤行号小于1的异常
                if (defect.getReportLine() <= 0) {
                    return;
                }
                DefectProblemDescriptor descriptor = new DefectProblemDescriptor(psiFile, defect);
                PsiElement psiElement = PsiUtil.formatPsiElementInDescriptor(psiFile, defect);
                if (psiElement != null) {
                    descriptor.setPsiElement(psiElement);
                }
                descriptors.add(descriptor);
            });
            if (CollectionUtils.isNotEmpty(descriptors)) {
                this.problemDescriptorsMap.put(psiFile, descriptors);
            }
        }
    }

    @Override
    public List<CodeMarsResult.DefectFile> checkResult() {
        return resultList;
    }

    @Override
    public Map<PsiFile, Set<DefectProblemDescriptor>> getProblemsMap() {
        return this.problemDescriptorsMap;
    }

    /**
     * 将单个文件的检查结果更新到缓存中
     *
     * @param defectFile 单个文件的检查结果
     */
    private void updateDefectCache(CodeMarsResult.DefectFile defectFile) {
        String filePath = defectFile.getFilePath();
        if (defectsMap.containsKey(filePath)) {
            CodeMarsResult.DefectFile result = defectsMap.get(filePath);
            if (StringUtil.isEmpty(result.getOutput())) {
                result.setOutput(defectFile.getOutput());
            }
            result.getDefects().addAll(defectFile.getDefects());
        } else {
            defectsMap.put(filePath, defectFile);
        }
    }

    private static Map<String, String> getCheckPath(@NotNull CheckCodeParams checkCodeParams,
        @NotNull Project project) {
        Map<String, String> checkMap = getFullCheckFiles(checkCodeParams, project);
        if (checkCodeParams.isIncremental()) {
            return getIncrementalFiles(checkMap, project);
        } else {
            return checkMap;
        }
    }

    private static Map<String, String> getIncrementalFiles(Map<String, String> checkMap, Project project) {
        try {
            Set<String> modifiedPaths = getModifiedPaths(project);
            Map<String, String> incrementalPaths = new HashMap<>();
            boolean isEmpty = updateDetectFileMap(checkMap, modifiedPaths, incrementalPaths);
            if (isEmpty) {
                throw new CheckException(CodeCheckBundle.message("incremental.not.Change"));
            }
            return incrementalPaths;
        } catch (IOException | TimeoutException e) {
            if (e.getMessage().startsWith("Cannot run program \"git\"")) {
                throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
            }
            LOGGER.warn("Cannot run program \"git\"", e);
        }
        return new HashMap<>();
    }

    private static boolean updateDetectFileMap(Map<String, String> checkMap, Set<String> modifiedPaths,
        Map<String, String> incrementalPaths) {
        boolean isEmpty = true;
        for (String dir : checkMap.keySet()) {
            for (String file : modifiedPaths) {
                if (file.startsWith(dir)) {
                    incrementalPaths.put(dir, checkMap.get(dir));
                }
            }
        }
        if (!incrementalPaths.isEmpty()) {
            isEmpty = false;
        }
        return isEmpty;
    }

    @Nullable
    private static Set<String> getModifiedPaths(Project project) throws IOException, TimeoutException {
        Optional<String> projectOptional = getProjectCanonicalPath(project);
        if (projectOptional.isEmpty()) {
            LOGGER.warn("Can't find project canonical path");
            return new HashSet<>();
        }
        String projectPath = projectOptional.get();
        return getModifiedPath(projectPath);
    }

    private static Set<String> getModifiedPath(String projectPath) throws TimeoutException {
        String gitPath = getGitPath(projectPath);
        String[] gitCommand = {"git", "diff", "HEAD", "--name-status"};
        final Process finalProcess = getGitCommandProcess(projectPath, gitCommand);
        // 提交读取标准输出的任务
        Future<Set<String>> stdoutFuture = getStdoutFuture(gitPath, finalProcess);
        // 提交读取错误输出的任务
        Future<String> stderrFuture = getStderrFuture(finalProcess);
        // 等待两个Future都完成，或超时
        try {
            Set<String> modifiedPaths = stdoutFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String errorMessage = stderrFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (StringUtil.isNotEmpty(errorMessage) && errorMessage.contains("warning: Not a git repository.")) {
                throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
            }
            return modifiedPaths;
        } catch (TimeoutException e) {
            // 终止进程，并可能需要处理其他资源清理
            finalProcess.destroyForcibly();
            throw e;
        } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
            LOGGER.warn("Problem executing git." + e.getMessage());
            throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
        }
    }

    private static String getGitPath(String projectPath) throws TimeoutException {
        String[] gitCommand = {"git", "rev-parse", "--show-toplevel"};
        final Process finalProcess = getGitCommandProcess(projectPath, gitCommand);
        // 提交读取标准输出的任务
        Future<String> stdoutFuture = getGitPathFuture(finalProcess);
        // 提交读取错误输出的任务
        Future<String> stderrFuture = getStderrFuture(finalProcess);
        try {
            String gitPath = stdoutFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String errorMessage = stderrFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (StringUtil.isNotEmpty(errorMessage) && errorMessage.contains("warning: Not a git repository.")) {
                throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
            }
            return gitPath;
        } catch (TimeoutException e) {
            // 终止进程，并可能需要处理其他资源清理
            finalProcess.destroyForcibly();
            throw e;
        } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
            LOGGER.warn("Problem executing git." + e.getMessage());
            throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
        }
    }

    @NotNull
    private static Future<String> getGitPathFuture(Process finalProcess) {
        return EXECUTOR_SERVICE.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String filePath = new File(line).getCanonicalPath();
                    if (StringUtils.isNotEmpty(filePath) && new File(line).exists()) {
                        return filePath;
                    }
                }
                return "";
            }
        });
    }

    private static Process getGitCommandProcess(String projectPath, String[] gitCommand) {
        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand);
        processBuilder.directory(new File(projectPath));
        Process process = null;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            if (e.getMessage().startsWith("Cannot run program \"git\"")) {
                throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
            }
            LOGGER.warn("Cannot run program \"git\"", e);
        }
        if (process == null) {
            throw new CheckException(CodeCheckBundle.message("incremental.not.git"));
        }
        return process;
    }

    @NotNull
    private static Future<String> getStderrFuture(Process finalProcess) {
        return EXECUTOR_SERVICE.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(finalProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        });
    }

    @NotNull
    private static Future<Set<String>> getStdoutFuture(String gitPath, Process finalProcess) {
        return EXECUTOR_SERVICE.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                Set<String> modifiedPaths = new HashSet<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String filePath = getFilePath(gitPath, line);
                    if (StringUtils.isNotEmpty(filePath)) {
                        modifiedPaths.add(filePath);
                    }
                }
                return modifiedPaths;
            }
        });
    }

    private static String getFilePath(String projectPath, String line) {
        if (line.startsWith("A") || line.startsWith("M") || line.startsWith("U")) {
            return Path.of(projectPath, line.substring(line.indexOf("\t") + 1).trim()).toString();
        }
        if (line.startsWith("R")) {
            String[] split = line.split("\t");
            if (split.length == 3) {
                return Path.of(projectPath, split[2].trim()).toString();
            }
        }
        return StringUtils.EMPTY;
    }

    private static Map<String, String> getFullCheckFiles(CheckCodeParams checkCodeParams, Project project) {
        List<String> checkedFilePaths = new Gson().fromJson(checkCodeParams.getCustomCheckPathJsonStr(),
            new TypeToken<List<String>>() {}.getType());
        Optional<String> projectPathOptional = getProjectCanonicalPath(project);
        Map<String, String> checkFileMap = new HashMap<>();
        String filePath;
        for (String checkedFilePath : checkedFilePaths) {
            try {
                filePath = new File(checkedFilePath).getCanonicalPath();
            } catch (IOException e) {
                String message = String.format("File %s was skipped because exception occurred.", checkedFilePath);
                LOGGER.warn(message);
                NotificationUtil.showWithProject(message, HUMP_CANGJIE, NotificationUtil.Type.WARN, project);
                continue;
            }
            ModuleModel moduleModel = getModuleFromFile(filePath, project);
            if (moduleModel != null) {
                addModulePath(moduleModel, checkFileMap, project);
                continue;
            }
            if (projectPathOptional.isPresent() && filePath.equals(projectPathOptional.get())) {
                List<ModuleModel> cangjieModules = getCangjieModules(project);
                for (ModuleModel cangjieModule : cangjieModules) {
                    addModulePath(cangjieModule, checkFileMap, project);
                }
            } else {
                LOGGER.warn(String.format("Path %s may be not cangjie module.", filePath));
            }
        }
        return checkFileMap;
    }

    private static void addModulePath(ModuleModel moduleModel, Map<String, String> checkFileMap, Project project) {
        String modulePath;
        try {
            modulePath = new File(moduleModel.getModulePath()).getCanonicalPath();
        } catch (IOException e) {
            LOGGER.warn("Get modulePath error,  module path is {}", moduleModel.getModulePath());
            return;
        }
        if (isCangjieModuleModel(moduleModel) && moduleModel instanceof OhosModuleModel) {
            String cangjieModuleSrcDir = getCangjieModuleSrcDir((OhosModuleModel) moduleModel, false);
            if (StringUtil.isEmpty(cangjieModuleSrcDir)) {
                cangjieModuleSrcDir = SRC;
            }
            try {
                doAddModulePath(modulePath, cangjieModuleSrcDir, checkFileMap, project);
            } catch (InvalidPathException e) {
                LOGGER.warn("Invalid custom combination src-dir file path.");
                doAddModulePath(modulePath, SRC, checkFileMap, project);
            }
        } else {
            String message = String.format("Module %s was skipped because is not cangjieModule.", modulePath);
            LOGGER.warn(message);
        }
    }

    private static void doAddModulePath(String modulePath, String srcDir, Map<String, String> checkFileMap,
        Project project) {
        String cjpmDirPath = Path.of(modulePath).toString();
        if (!Path.of(cjpmDirPath, CJPM_TOML).toFile().exists()) {
            cjpmDirPath = Path.of(modulePath, SRC, MAIN, CANGJIE).toString();
        }
        String cangjieSrcPath = Path.of(cjpmDirPath, srcDir).normalize().toString();
        if (checkFileMap.containsKey(cangjieSrcPath)) {
            return;
        }
        checkFileMap.put(cangjieSrcPath, Strings.EMPTY);
        Path tomlPath = Path.of(cjpmDirPath, CJPM_TOML).normalize();
        if (!Files.exists(tomlPath)) {
            return;
        }
        Optional<Toml> tomlObjOption = LspConfigUtils.getModuleCjpmToml(project, tomlPath.toFile(), true);
        if (tomlObjOption.isEmpty()) {
            return;
        }
        Toml tomlObj = tomlObjOption.get();
        Optional<Toml> targets = tomlObj.getTable(TARGET);
        if (targets.isEmpty()) {
            return;
        }
        Toml targetObj = targets.get();
        Map<String, Object> targetItems = targetObj.toMap();
        if (targetItems.isEmpty()) {
            return;
        }
        Set<String> cjoPathSet = new HashSet<>();
        getCjoDepends(modulePath, project, targetObj, targetItems, cjoPathSet);
        if (CollectionUtils.isNotEmpty(cjoPathSet)) {
            checkFileMap.put(cangjieSrcPath, String.join(SPACE, cjoPathSet));
        }
    }

    private static void getCjoDepends(String modulePath, Project project, Toml targetObj,
        Map<String, Object> targetItems, Set<String> cjoPathSet) {
        Map<String, String> projectEnvs = CangjieEnvUtils.getProjectEnvs(project);
        Path tomlDirPath = Path.of(modulePath);
        if (!Path.of(tomlDirPath.toString(), CJPM_TOML).toFile().exists()) {
            tomlDirPath = Path.of(modulePath, SRC, MAIN, CANGJIE);
        }
        Optional<Toml> armTargetOption = targetObj.getTable(AARCH64_LINUX_OHOS);
        if (armTargetOption.isEmpty() || armTargetOption.get().getTable(BIN_DEPENDENCIES).isEmpty()) {
            return;
        }
        Toml binDependsObj = armTargetOption.get().getTable(BIN_DEPENDENCIES).get();
        List<String> pathOptions = binDependsObj.getList(PATH_OPTION);
        if (CollectionUtils.isNotEmpty(pathOptions)) {
            for (String path : pathOptions) {
                String realPath = getRealPath(path, tomlDirPath, projectEnvs);
                if (StringUtils.isEmpty(realPath)) {
                    continue;
                }
                cjoPathSet.add(realPath);
            }
        }
        Optional<Toml> packageOption = binDependsObj.getTable(PACKAGE_OPTION);
        if (packageOption.isPresent()) {
            Map<String, Object> packageMap = packageOption.get().toMap();
            for (String packageItem : packageMap.keySet()) {
                if (!(packageMap.get(packageItem) instanceof String)) {
                    continue;
                }
                String realPath = getRealPath((String) packageMap.get(packageItem), tomlDirPath, projectEnvs);
                if (StringUtils.isEmpty(realPath)) {
                    continue;
                }
                cjoPathSet.add(realPath);
            }
        }
    }

    private static String getRealPath(String path, Path tomlDirPath, Map<String, String> projectEnvs) {
        String realPath = path;
        if (StringUtils.isEmpty(realPath)) {
            return Strings.EMPTY;
        }
        Matcher matcher = ENV_PATTERN.matcher(realPath);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String math = matcher.group(1);
            String env = projectEnvs.get(math);
            if (env == null) {
                env = "";
            }
            matcher.appendReplacement(sb, env.replace("\\", "/"));
        }
        matcher.appendTail(sb);
        realPath = sb.toString();
        if (!LspConfigUtils.isAbsolutePath(realPath)) {
            Path normalizePath = tomlDirPath.resolve(realPath).normalize();
            if (!Files.exists(normalizePath)) {
                return Strings.EMPTY;
            }
            if (!Files.isDirectory(normalizePath)) {
                normalizePath = normalizePath.getParent();
            }
            return normalizePath.toString().replaceAll("\\\\", "/");
        } else {
            return Files.exists(Path.of(realPath)) ? realPath : Strings.EMPTY;
        }
    }

    /**
     * get cangjie module from project
     *
     * @param project project
     * @return cangjie modules
     */
    private static List<ModuleModel> getCangjieModules(Project project) {
        List<ModuleModel> cangjieModules = new ArrayList<>();
        ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
        if (projectModel == null) {
            LOGGER.warn("ProjectModel is null when get cangjie modules");
            return cangjieModules;
        }
        List<ModuleModel> modules = projectModel.getModuleModelList();
        for (ModuleModel moduleModel : modules) {
            if (isCangjieModuleModel(moduleModel)) {
                cangjieModules.add(moduleModel);
            }
        }
        return cangjieModules;
    }

    private static int getReportColumn(PsiFile psiFile, int lineNumber, int reportColumn) {
        Document document = FileDocumentManager.getInstance().getDocument(psiFile.getVirtualFile());
        if (document == null) {
            return reportColumn;
        }
        if (lineNumber < 0 || lineNumber >= document.getLineCount()) {
            return reportColumn;
        }
        String lineContent = document.getText(
            new TextRange(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)));
        if (reportColumn > lineContent.length()) {
            int leadingSpacesCount = 0;
            for (char c : lineContent.toCharArray()) {
                if (c == ' ' || c == '\t') {
                    leadingSpacesCount++;
                } else {
                    break;
                }
            }
            return leadingSpacesCount + 1;
        }
        return reportColumn;
    }

    /**
     * 仓颉规则配置通过可以通过工程根目录下自定义cjlint_rule_list.json和exclude_lists.json来替换默认规则
     * 处理所有的配置文件
     *
     * @return 是否处理完成，如果是，则需要替换CJLint -e的参数为缓存配置文件夹，如果否则不需要传-e参数
     * @throws IOException 删除或复制文件中可能引发的异常
     */
    private boolean processCustomizedCJLintConfigs() throws IOException {
        Optional<String> compilerOptional = PathUtils.getCompilerPath(project);
        if (compilerOptional.isEmpty()) {
            LOGGER.warn("Can't get Cangjie SDK path");
            return false;
        }
        // 仓颉编译器绝对路径字符串
        String compilerPath = compilerOptional.get();

        Optional<String> projectOptional = getProjectCanonicalPath(project);
        if (projectOptional.isEmpty()) {
            LOGGER.warn("Can't find project canonical path");
            return false;
        }
        // 本项目工程根目录的绝对路径字符串
        String projectPath = projectOptional.get();

        // 本项目临时存放CJLint配置文件的绝对路径
        Path cacheCJLintConfigPath = Path.of(projectPath, ".idea", ".deveco", "cangjie", CONFIG);
        cacheCJLintConfigPathStr = cacheCJLintConfigPath.toString();

        Path defaultCJLintConfigPath = Path.of(compilerPath, "tools", CONFIG);

        // 获取本项目缓存的CJLint配置文件夹，将项目根目录可能存在的两个文件（cjlint_rule_list.json和exclude_lists.json）复制过去
        File cacheCJLintConfigFolder = cacheCJLintConfigPath.toFile();
        if (cacheCJLintConfigFolder.exists()) {
            // 如果缓存配置文件夹已存在，清空
            FileUtils.deleteDirectory(cacheCJLintConfigFolder);
        }
        // 新建缓存配置文件夹
        boolean createCacheFolderSuccessfully = cacheCJLintConfigFolder.mkdirs();
        if (!createCacheFolderSuccessfully) {
            LOGGER.warn("create cacheCJLintConfigPath failed");
            return false;
        }

        Path cjlintRuleListPath = Path.of(projectPath, "cjlint_rule_list.json");
        File cjlintRuleListFile = cjlintRuleListPath.toFile();
        if (cjlintRuleListFile.exists()) {
            // 检查能否正常解析并且要能拿到RuleList，可以则继续，否则直接返回空结果
            JsonObject ruleListjsonObject = PsiJsonFileUtil.findPsiFileJsonObject(project, cjlintRuleListPath);
            if (cjlintRuleListFile.length() == 0 || ruleListjsonObject == null) {
                returnEmptyResult = true;
                return false;
            }
            JsonArray ruleListJsonArray = PsiJsonFileUtil.getPsiJsonArray(ruleListjsonObject, "RuleList");
            if (ruleListJsonArray == null) {
                returnEmptyResult = true;
                return false;
            }
            CjlintRuleListConfig cjLintRuleListConfig = new CjlintRuleListConfig();
            cjLintRuleListConfig.setRuleList(PsiJsonFileUtil.getStrListFromJsonArray(ruleListJsonArray));
            if (cjLintRuleListConfig.getRuleList() == null) {
                returnEmptyResult = true;
                return false;
            }
            Files.copy(cjlintRuleListPath, Path.of(cacheCJLintConfigPathStr, cjlintRuleListFile.getName()));
        }

        Path excludeListsPath = Path.of(projectPath, "exclude_lists.json");
        File excludeListsFile = excludeListsPath.toFile();
        if (excludeListsFile.exists() && excludeListsFile.length() > 0) {
            JsonObject excludejsonObject = PsiJsonFileUtil.findPsiFileJsonObject(project, excludeListsPath);
            if (excludejsonObject == null) {
                returnEmptyResult = true;
                return false;
            }
            Files.copy(excludeListsPath, Path.of(cacheCJLintConfigPathStr, excludeListsFile.getName()));
        }

        // 此时在缓存文件夹里，理论上仅有上述两个自定义的json文件（可能不全）
        List<String> customizedCJLintConfigFileNames = new ArrayList<>();
        File[] customizedCJLintConfigFiles = cacheCJLintConfigFolder.listFiles();
        if (customizedCJLintConfigFiles != null) {
            Arrays.asList(customizedCJLintConfigFiles)
                    .forEach(file -> customizedCJLintConfigFileNames.add(file.getName()));
        }

        // 获取所有CJLint的默认config文件，查看本项目是否存在，不存在则复制
        File defaultCJLintConfigFolder = defaultCJLintConfigPath.toFile();
        if (!defaultCJLintConfigFolder.exists() || !defaultCJLintConfigFolder.isDirectory()) {
            // 默认配置目录不存在
            return false;
        }
        File[] defaultCJLintConfigFiles = defaultCJLintConfigFolder.listFiles();
        if (defaultCJLintConfigFiles != null) {
            for (File file : defaultCJLintConfigFiles) {
                if (file.isFile() && !customizedCJLintConfigFileNames.contains(file.getName())) {
                    // 如果该配置文件在本项目没有，则复制到本项目
                    Files.copy(file.toPath(), Path.of(cacheCJLintConfigPathStr, file.getName()));
                }
            }
        }
        // 处理完毕，返回
        return true;
    }

    /**
     * 根据code-linter.json5中的files和ignore配置，筛选检查结果resultList
     *
     * @throws IOException 读取文件中可能引发的异常
     */
    private void filterResultListByCodeLinterJson5() throws IOException {
        // 读取code-linter.json5配置文件中的files和ignore配置
        Optional<String> projectOptional = getProjectCanonicalPath(project);
        if (projectOptional.isEmpty()) {
            LOGGER.warn("Can't find project canonical path");
            return;
        }
        Path codeLinterJson5Path = Path.of(projectOptional.get(), "code-linter.json5");
        if (!codeLinterJson5Path.toFile().exists()) {
            returnEmptyResult = true;
            return;
        }
        JsonObject jsonObject = PsiJsonFileUtil.findPsiFileJsonObject(project, codeLinterJson5Path);
        if (jsonObject == null) {
            returnEmptyResult = true;
            return;
        }
        CodeLinterJson5Config codeLinterJson5Config = new CodeLinterJson5Config();
        JsonArray filesJsonArray = PsiJsonFileUtil.getPsiJsonArray(jsonObject, "files");
        JsonArray ignoreJsonArray = PsiJsonFileUtil.getPsiJsonArray(jsonObject, "ignore");
        if (filesJsonArray == null || ignoreJsonArray == null) {
            returnEmptyResult = true;
            return;
        }
        codeLinterJson5Config.setFiles(PsiJsonFileUtil.getStrListFromJsonArray(filesJsonArray));
        codeLinterJson5Config.setIgnore(PsiJsonFileUtil.getStrListFromJsonArray(ignoreJsonArray));
        filterResultListByFiles(codeLinterJson5Config);
        filterResultListByIgnore(codeLinterJson5Config, projectOptional.get().length());
    }

    /**
     * 根据files筛选检查结果，如果不能匹配files规则，则剔除
     *
     * @param codeLinterJson5Config code-linter.json5配置文件中的配置
     */
    private void filterResultListByFiles(CodeLinterJson5Config codeLinterJson5Config) {
        // 根据files筛选检查结果，如果不能匹配files规则，则剔除
        // files:  用于表示配置适用的文件范围的 glob 模式数组。
        List<String> filesList = codeLinterJson5Config.getFiles();
        if (filesList == null || filesList.isEmpty()) {
            return;
        }
        Iterator<CodeMarsResult.DefectFile> iterator = resultList.iterator();
        while (iterator.hasNext()) {
            boolean hasMatched = false;
            CodeMarsResult.DefectFile defectFile = iterator.next();
            for (String filesConfig : filesList) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filesConfig);
                if (matcher.matches(Paths.get(defectFile.getFilePath()))) {
                    hasMatched = true;
                    break;
                }
            }
            if (!hasMatched) {
                // 如果一条规则都没匹配上，则不在检查范围内
                iterator.remove();
            }
        }
    }

    /**
     * 根据files筛选检查结果，如果不能匹配files规则，则剔除
     *
     * @param codeLinterJson5Config code-linter.json5配置文件中的配置
     * @param projectRootPathLength 工程根目录绝对路径字符串的长度
     */
    private void filterResultListByIgnore(CodeLinterJson5Config codeLinterJson5Config, int projectRootPathLength) {
        // 根据ignore筛选检查结果，如果能匹配ignore规则，则剔除
        // ignore：配置无需检查的文件目录，其指定的目录或文件需使用相对路径格式，相对于code-linter.json5所在工程根目录
        List<String> ignoreList = codeLinterJson5Config.getIgnore();
        if (ignoreList == null || ignoreList.isEmpty()) {
            return;
        }
        Iterator<CodeMarsResult.DefectFile> iterator = resultList.iterator();
        while (iterator.hasNext()) {
            CodeMarsResult.DefectFile defectFile = iterator.next();
            String defectFilePath = defectFile.getFilePath();

            if (defectFilePath.length() <= projectRootPathLength + 1) {
                continue;
            }
            // 将检查结果文件路径处理成相对工程根目录的相对路径
            String relativePath = defectFilePath.substring(projectRootPathLength + 1);

            for (String ignoreConfig : ignoreList) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + ignoreConfig);
                if (matcher.matches(Paths.get(relativePath))) {
                    iterator.remove();
                    break;
                }
            }
        }
    }
}