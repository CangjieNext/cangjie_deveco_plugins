/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.testframework.utils;

import static com.huawei.cangjie.testframework.runner.CangjieLocalTestRunner.EXECUTOR_SERVICE;
import static com.huawei.ideacj.lsp.utils.Constants.NAME;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE;
import static com.huawei.ideacj.lsp.utils.Constants.SRC_DIR;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_FILE;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.cangjie.testframework.configuration.CangjieTestRunConfiguration;
import com.huawei.deveco.build.ohos.service.HvigorParamsBuilder;
import com.huawei.deveco.build.ohos.service.HvigorService;
import com.huawei.deveco.ohos.testframework.utils.CommonUtil;
import com.huawei.deveco.ohos.testframework.utils.HvigorTaskModel;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProductManager;
import com.huawei.deveco.projectmodel.ohos.model.TargetManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosTarget;
import com.huawei.deveco.projectmodel.ohos.util.ModuleType;
import com.huawei.deveco.projectmodel.ohos.v2.impl.HvigorProductV2;
import com.huawei.ideacj.lsp.utils.Constants;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LocalTestUtil
 *
 * @since 2025/08/31
 */
public class LocalTestUtil {
    private static final Logger LOG = Logger.getInstance(LocalTestUtil.class);

    /**
     * assembleLocalTest
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is assembled success
     */
    public static boolean assembleLocalTest(@NotNull CangjieTestRunConfiguration runConfiguration,
                                            @NotNull HvigorTaskModel hvigorTaskModel) {
        boolean hasHspModule =
                hvigorTaskModel.getModule()
                        .getProjectModel().getModuleModelList().stream().anyMatch(CommonUtil::isHspModule);
        HvigorParamsBuilder builderForTest =
                TestUtil.getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                        .withTaskName("UnitTestBuild").withProduct(hvigorTaskModel.getProduct())
                        .withParam("isLocalTest", "true")
                        .withParam("unitTestMode", "true")
                        .withParam("isCangjie", "true");
        if (hasHspModule) {
            builderForTest = TestUtil.getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                    .withTaskName("assemble assemble")
                    .withProduct(hvigorTaskModel.getProduct())
                    .withParam("isLocalTest", "true")
                    .withParam("unitTestMode", "true")
                    .withParam("isCangjie", "true");
        }
        return HvigorService.executeInternalHvigorTask(
                hvigorTaskModel.getProject(),
                builderForTest.build(), builderForTest.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * assembleLocalTestWithCoverage
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is assembled success
     */
    public static boolean assembleLocalTestWithCoverage(@NotNull CangjieTestRunConfiguration runConfiguration,
                                                        @NotNull HvigorTaskModel hvigorTaskModel) {
        boolean hasHspModule =
                hvigorTaskModel.getModule()
                        .getProjectModel().getModuleModelList().stream().anyMatch(CommonUtil::isHspModule);
        HvigorParamsBuilder builderForCoverage =
                TestUtil.getHvigorParamsBuilder(runConfiguration, hvigorTaskModel).withTaskName("UnitTestBuild")
                        .withParam("isLocalTest", "true")
                        .withParam("unitTestMode", "true")
                        .withParam("ohos-test-coverage", "true")
                        .withParam("isCangjie", "true");
        if (hasHspModule) {
            builderForCoverage =
                    TestUtil.getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                            .withTaskName("assembleHap assembleHsp")
                            .withParam("isLocalTest", "true")
                            .withParam("unitTestMode", "true")
                            .withParam("ohos-test-coverage", "true")
                            .withParam("isCangjie", "true");
        }
        return HvigorService.executeInternalHvigorTask(
                hvigorTaskModel.getProject(),
                builderForCoverage.build(), builderForCoverage.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * closeQuietly
     *
     * @param closeable closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * killProcessTree
     *
     * @param process process
     */
    public static void killProcessTree(Process process) {
        ProcessHandle handle = process.toHandle();

        handle.descendants().forEach(child -> {
            child.destroy();
            if (child.isAlive()) {
                child.destroyForcibly();
            }
        });

        handle.destroy();
        if (handle.isAlive()) {
            handle.destroyForcibly();
        }
    }

    /**
     * getLocalPackageName
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @return LocalPackageName
     */
    public static String getLocalPackageName(ModuleModel moduleModel, VirtualFile virtualFile) {
        if (moduleModel == null) {
            return StringUtils.EMPTY;
        }
        String cangjieLocalTestPath = Path.of(moduleModel.getModulePath(), "src", "test", "cangjie")
                .toAbsolutePath().normalize().toString().replaceAll("\\\\", "/");
        String targetRootPath;
        String targetPath = virtualFile.getCanonicalPath();
        if (!virtualFile.isDirectory() && virtualFile.getParent() != null) {
            targetPath = virtualFile.getParent().getCanonicalPath();
        }
        if (StringUtils.isEmpty(targetPath)) {
            return StringUtils.EMPTY;
        }
        String cangjieModuleName = getLocalTestModuleName(moduleModel);
        if (StringUtils.isEmpty(cangjieModuleName)) {
            return StringUtils.EMPTY;
        }
        String srcDir = getLocalTestModuleSrcDir(moduleModel);
        targetPath = targetPath.replaceAll("\\\\", "/");
        if (targetPath.contains(cangjieLocalTestPath)) {
            targetRootPath = Path.of(moduleModel.getModulePath(), "src", "test", "cangjie", srcDir)
                    .normalize().toString().replaceAll("\\\\", "/");
        } else {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isEmpty(targetRootPath) || !targetPath.contains(targetRootPath)) {
            return StringUtils.EMPTY;
        }
        String[] folders = targetPath.substring(targetRootPath.length()).split("/");
        StringBuilder packageName = new StringBuilder(cangjieModuleName);
        for (String folderName : folders) {
            if (StringUtils.isEmpty(folderName)) {
                continue;
            }
            packageName.append(".");
            packageName.append(folderName);
        }
        if (packageName.length() <= 0 || TestUtil.containsDangerous(packageName.toString())) {
            return StringUtils.EMPTY;
        }
        return packageName.toString();
    }

    /**
     * getLocalTestModuleSrcDir
     *
     * @param module module
     * @return LocalTestModuleSrcDir
     */
    @NotNull
    public static String getLocalTestModuleSrcDir(ModuleModel module) {
        if (!FileUtils.isCangjieModule(module)) {
            return StringUtils.EMPTY;
        }
        Path cjpmFilePath = Path.of(module.getModulePath(), "src", "test", "cangjie", CJPM_FILE);
        Optional<Toml> optCjpmObj = TestUtil.getModuleCjpmToml(cjpmFilePath.toFile());
        if (optCjpmObj.isEmpty()) {
            return StringUtils.EMPTY;
        }
        Optional<Toml> packageInfo = optCjpmObj.get().getTable(PACKAGE);
        if (packageInfo.isEmpty()) {
            return StringUtils.EMPTY;
        }
        String srcDir = packageInfo.get().getString(SRC_DIR);
        if (Strings.isEmpty(srcDir)) {
            srcDir = Constants.SRC;
        }
        return srcDir;
    }

    /**
     * getLocalTestModuleName
     *
     * @param module module
     * @return LocalTestModuleName
     */
    @NotNull
    public static String getLocalTestModuleName(ModuleModel module) {
        String cangjieModuleName = "";
        if (!FileUtils.isCangjieModule(module)) {
            return cangjieModuleName;
        }
        Path cjpmFilePath = Path.of(module.getModulePath(), "src", "test", "cangjie", CJPM_FILE);
        Optional<Toml> optCjpmObj = TestUtil.getModuleCjpmToml(cjpmFilePath.toFile());
        if (optCjpmObj.isEmpty()) {
            return cangjieModuleName;
        }
        Optional<Toml> packageInfo = optCjpmObj.get().getTable(PACKAGE);
        if (packageInfo.isPresent()) {
            cangjieModuleName = packageInfo.get().getString(NAME);
        }
        return cangjieModuleName;
    }

    /**
     * checkIsMock
     *
     * @param project project
     * @return is mock
     */
    public static boolean checkIsMock(Project project) {
        if (project == null || StringUtils.isEmpty(project.getBasePath())) {
            return false;
        }
        Path buildLogPath = Path.of(project.getBasePath(),
                Constant.CACHE_IDEA_DIR,
                Constant.CACHE_DEVECO_DIR,
                Constant.CACHE_CANGJIE_DIR,
                Constant.CACHE_BUILD_LOGS_DIR,
                Constant.CACHE_TEST_DIR,
                Constant.CACHE_TEST_BUILD_LOG);
        if (!buildLogPath.toFile().exists()) {
            return false;
        }
        String content;
        try {
            content = new String(Files.readAllBytes(buildLogPath));
        } catch (IOException exception) {
            LOG.warn("Failed to read build logs.");
            return false;
        }
        if (StringUtils.isEmpty(content)) {
            return false;
        }
        JSONObject json = JSON.parseObject(content);
        String testBuildPath = json.getString(Constant.TEST_BUILD_PATH);
        return !StringUtils.isEmpty(testBuildPath) && Constant.MOCK.equals(testBuildPath);
    }

    /**
     * getLocalTestEnv
     *
     * @param module module
     * @param isDebug isDebug
     * @return LocalTestEnv
     */
    public static Map<String, String> getLocalTestNeedEnv(ModuleModel module, boolean isDebug) {
        Map<String, String> env = new HashMap<>();
        if (module == null || module.getProjectModel() == null) {
            return env;
        }
        Project project = module.getProjectModel().getProject();
        if (project == null) {
            return env;
        }
        String binDir = "release";
        String mockDir = "";
        if (checkIsMock(project)) {
            mockDir = "mock";
        }
        // find product name
        String productName = "default";
        HvigorProductV2 product =
                ProductManager.getInstance().getCurrentProduct(CommonProjectUtil.getProjectModel(project));
        if (product != null) {
            productName = product.getName();
        }
        // find target name
        String targetName = "default";
        if (!ModuleType.HAR.toString().equalsIgnoreCase(module.getModuleType())) {
            OhosTarget currentTarget = TargetManager.getInstance().getCurrentTarget(module);
            if (currentTarget != null) {
                targetName = currentTarget.getName();
            }
        }
        Path buildPath = Path.of(module.getModulePath(),
                "build", productName, "intermediates", "cj", "build", targetName, mockDir, binDir);
        String localModuleName = getLocalTestModuleName(module);
        String dynamicPath = Path.of(buildPath.toString(), localModuleName).toString();
        String executablePath = Path.of(buildPath.toString(), "unittest_bin").toString();
        String envPath = "Path";
        if (SystemInfo.isMac) {
            envPath = "PATH";
        }
        env.put(envPath, dynamicPath + File.pathSeparator + executablePath);
        return env;
    }

    /**
     * getAllTestClassInOnePkg
     *
     * @param model model
     * @param pkgName pkgName
     * @param isDebug isDebug
     * @return get all test class in one pkg
     */
    public static List<String> getAllTestClassInOnePkg(ModuleModel model, String pkgName, boolean isDebug) {
        List<String> allTestClassInOnePkg = new ArrayList<>();
        if (model == null || StringUtils.isEmpty(pkgName)) {
            return allTestClassInOnePkg;
        }
        if (!generateList(model, pkgName, isDebug)) {
            return allTestClassInOnePkg;
        }
        Path xmlReportsPath = Path.of(model.getModulePath(),
                Constant.TEST_OUTPUT_DIR,
                Constant.TEST_REPORTS_OUTPUT_DIR,
                Constant.TEST_REPORTS_TESTS_OUTPUT_DIR);
        if (!xmlReportsPath.toFile().exists() || xmlReportsPath.toFile().listFiles() == null) {
            return allTestClassInOnePkg;
        }
        List<File> reportFiles = List.of(xmlReportsPath.toFile().listFiles());
        if (reportFiles.isEmpty()) {
            return allTestClassInOnePkg;
        }
        String regex = "^test-" + Pattern.quote(pkgName) + "\\.([^.]+)\\.xml$";
        Pattern pattern = Pattern.compile(regex);
        for (File reportFile : reportFiles) {
            String reportFileName = reportFile.getName();
            Matcher matcher = pattern.matcher(reportFileName);
            if (!matcher.matches()) {
                continue;
            }
            String testClassName = matcher.group(1);
            if (StringUtils.isEmpty(testClassName) || allTestClassInOnePkg.contains(testClassName)) {
                continue;
            }
            allTestClassInOnePkg.add(testClassName);
        }
        TestUtil.deleteOldResults(model);
        return allTestClassInOnePkg;
    }

    private static boolean generateList(ModuleModel model, String pkgName, boolean isDebug) {
        String command = getTestListCommand(model, pkgName);
        if (StringUtils.isEmpty(command)) {
            return false;
        }
        String sdkPath = TestUtil.getSdkPath(model.getProjectModel());
        String batPath = Path.of(sdkPath, Constant.BUILD_TOOLS, Constant.ENV_SCRIPT_WIN).toString();
        if (SystemInfo.isMac) {
            batPath = Path.of(sdkPath, Constant.BUILD_TOOLS, Constant.ENV_SCRIPT_MAC).toString();
        }
        ProcessBuilder processBuilder;
        if (SystemInfo.isWindows) {
            processBuilder = new ProcessBuilder(
                    Constant.WIN_BAT, Constant.WIN_BAT_OPTION, "\"" + batPath + "\"" + "&&" + command);
        } else {
            processBuilder = new ProcessBuilder(Constant.MAC_BASH, Constant.MAC_BASH_OPTION,
                    "source " + "\"" + batPath + "\"" + "&&" + command);
        }

        // set need env
        Map<String, String> localTestNeedEnv = getLocalTestNeedEnv(model, isDebug);
        Map<String, String> processEnv = processBuilder.environment();
        for (Map.Entry<String, String> env : localTestNeedEnv.entrySet()) {
            String key = env.getKey();
            String value = env.getValue();
            String oldPath = processEnv.get(key);
            processBuilder.environment().put(key, value + File.pathSeparator + oldPath);
        }

        Process process;
        try {
            process = processBuilder.inheritIO().start();
        } catch (IOException e) {
            LOG.warn("local test execute get list command failed.");
            return false;
        }

        Future<?> stdoutFuture = EXECUTOR_SERVICE.submit(() -> {
            try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(System.out::println);
            } catch (IOException exception) {
                LOG.warn("local test execute get list command failed.");
            }
        });
        Future<?> stderrFuture = EXECUTOR_SERVICE.submit(() -> {
            try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(System.err::println);
            } catch (IOException exception) {
                LOG.warn("local test execute get list command failed.");
            }
        });
        try {
            stdoutFuture.get();
            stderrFuture.get();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.warn("process exitCode: " + exitCode);
                return false;
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("local test execute get list command failed.");
            return false;
        } finally {
            closeQuietly(process.getInputStream());
            closeQuietly(process.getOutputStream());
            closeQuietly(process.getErrorStream());
            killProcessTree(process);
        }
        return true;
    }

    private static String getTestListCommand(ModuleModel model, String pkgName) {
        if (StringUtils.isEmpty(pkgName)) {
            return StringUtils.EMPTY;
        }
        String testReportOutputPath = Path.of(
                model.getModulePath(),
                Constant.TEST_OUTPUT_DIR,
                Constant.TEST_REPORTS_OUTPUT_DIR).toAbsolutePath().normalize().toString();
        String binaryName = pkgName;
        if (SystemInfo.isWindows) {
            binaryName += ".exe";
        }
        List<String> hdcArgs = new ArrayList<>();
        hdcArgs.add(binaryName);
        hdcArgs.add("--dry-run");
        hdcArgs.add("--report-path=" + testReportOutputPath);
        return String.join(" ", hdcArgs);
    }
}
