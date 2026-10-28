/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.projectmgmt.utils;

import static com.huawei.cangjie.projectmgmt.utils.Constants.AARCH64_APPLE_DARWIN;
import static com.huawei.cangjie.projectmgmt.utils.Constants.AARCH64_LINUX_OHOS;
import static com.huawei.cangjie.projectmgmt.utils.Constants.API_VERSION_20;
import static com.huawei.cangjie.projectmgmt.utils.Constants.API_VERSION_22;
import static com.huawei.cangjie.projectmgmt.utils.Constants.BIN_DEPENDENCIES;
import static com.huawei.cangjie.projectmgmt.utils.Constants.COMPILE_OPTION;
import static com.huawei.cangjie.projectmgmt.utils.Constants.CUSTOMIZED_OPTION;
import static com.huawei.cangjie.projectmgmt.utils.Constants.DEFAULT;
import static com.huawei.cangjie.projectmgmt.utils.Constants.KEY_PROFILE;
import static com.huawei.cangjie.projectmgmt.utils.Constants.NAME;
import static com.huawei.cangjie.projectmgmt.utils.Constants.OVERRIDE_COMPILE_OPTION;
import static com.huawei.cangjie.projectmgmt.utils.Constants.PACKAGE;
import static com.huawei.cangjie.projectmgmt.utils.Constants.PATH_OPTION;
import static com.huawei.cangjie.projectmgmt.utils.Constants.PROJECT_PATH;
import static com.huawei.cangjie.projectmgmt.utils.Constants.SRC;
import static com.huawei.cangjie.projectmgmt.utils.Constants.SRC_DIR;
import static com.huawei.cangjie.projectmgmt.utils.Constants.TARGET;
import static com.huawei.cangjie.projectmgmt.utils.Constants.X86_64_APPLE_DARWIN;
import static com.huawei.cangjie.projectmgmt.utils.Constants.X86_64_LINUX_OHOS;
import static com.huawei.deveco.projectmgmt.ohos.template.freemarker.FreeMarkerParam.ATOMIC_SERVICE;
import static com.huawei.deveco.projectmgmt.ohos.template.utils.TemplateConstant.DTO_STAGE_MODE;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.settings.ProjectOptimizationSettingsService;
import com.huawei.cangjie.projectmgmt.sync.upgrade.vo.TomlUpdateTypeData;
import com.huawei.deveco.projectmgmt.ohos.template.render.RenderHashMap;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.hos.v2.impl.HosProjectModelV2;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.util.ModuleType;
import com.huawei.deveco.projectmodel.ohos.util.PsiJsonFileUtil;
import com.huawei.deveco.sdkmanager.core.util.StringUtil;

import com.intellij.json.psi.JsonObject;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * FileUtils
 *
 * @since 2022-10-31
 */
public class FileUtils {
    /**
     * definition print log info instance
     */
    private static final Logger LOG = Logger.getInstance(FileUtils.class);

    private static final Map<TomlKind, BiFunction<String, Toml, ?>> TOML_BI_FUNCTION = Map.of(
            TomlKind.STRING, (BiFunction<String, Toml, String>) (key, toml) -> toml.getString(key),
            TomlKind.LONG, (BiFunction<String, Toml, Long>) (key, toml) -> toml.getLong(key),
            TomlKind.LIST, (BiFunction<String, Toml, List>) (key, toml) -> toml.getList(key),
            TomlKind.BOOLEAN, (BiFunction<String, Toml, Boolean>) (key, toml) -> toml.getBoolean(key),
            TomlKind.TABLE, (BiFunction<String, Toml, Optional<Toml>>) (key, toml) -> toml.getTable(key),
            TomlKind.TABLES, (BiFunction<String, Toml, List<Toml>>) (key, toml) -> toml.getTables(key)
    );

    /**
     * check is Cangjie Project
     *
     * @param projectModel projectModel
     * @return isCangjieProject
     */
    public static boolean isCangjieProject(ProjectModel projectModel) {
        if (projectModel == null) {
            return false;
        }
        List<ModuleModel> modelsList = projectModel.getModuleModelList();
        for (ModuleModel model : modelsList) {
            if ((model instanceof OhosModuleModel)
                    && FileUtils.isCangjieModule((OhosModuleModel) model)) {
                return true;
            }
        }
        return false;
    }

    /**
     * check is cangjie module
     *
     * @param module module
     * @return is cangjie module
     */
    public static boolean isCangjieModule(ModuleModel module) {
        if (module == null) {
            return false;
        }
        ProjectModel projectModel = module.getProjectModel();
        if (projectModel == null) {
            return false;
        }
        Optional<JsonObject> buildOption = getBuildOption(projectModel.getProject(), module);
        return buildOption.filter(jsonObject ->
                PsiJsonFileUtil.getPsiJsonObject(jsonObject, Constants.CANGJIE_OPTIONS) != null).isPresent();
    }

    /**
     * get BuildOption
     *
     * @param project Project
     * @param module ModuleModel
     * @return Optional<JsonObject>
     */
    public static Optional<JsonObject> getBuildOption(Project project, ModuleModel module) {
        if (project == null || module == null) {
            return Optional.empty();
        }
        Path buildOptionPath = Paths.get(module.getModulePath(), Constants.BUILD_PROFILE_JSON5);
        if (!buildOptionPath.toFile().exists()) {
            return Optional.empty();
        }
        JsonObject buildProfileJsonObject = PsiJsonFileUtil.findPsiFileJsonObject(project, buildOptionPath);
        if (buildProfileJsonObject == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(PsiJsonFileUtil.getPsiJsonObject(buildProfileJsonObject, Constants.BUILD_OPTION));
    }

    /**
     * read file content
     *
     * @param file file
     * @return file content
     */
    public static String readToString(File file) {
        Long fileLength = file.length();
        byte[] fileContent = new byte[fileLength.intValue()];
        String result = "";
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in.read(fileContent) > 0) {
                result = new String(fileContent, Charset.defaultCharset());
            }
        } catch (IOException e) {
            LOG.error(e);
        }
        return result;
    }

    /**
     * check the os is mac
     *
     * @return is mac
     */
    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    /**
     * check is absolute path
     *
     * @param path target path
     * @return is absolute path
     */
    public static boolean isAbsolutePath(String path) {
        if (path.startsWith("/") || path.indexOf(":") > 0) {
            return true;
        }
        return false;
    }

    /**
     * check is cangjie support module
     *
     * @param project project
     * @param moduleModel moduleModel
     * @return is cangjie support module
     */
    public static boolean isSupportModule(Project project, OhosModuleModel moduleModel) {
        if (project == null || moduleModel == null) {
            return false;
        }
        // check project bundle type
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel instanceof HosProjectModelV2) {
            if (ATOMIC_SERVICE.equals(((HosProjectModelV2) projectModel).getBundleType())) {
                return false;
            }
        }
        // check module api type and project api version
        if (!DTO_STAGE_MODE.equals(moduleModel.getApiType())
                || !isSupportApiVersion(moduleModel.getProjectModel().getFullCompatibleSdkVersion().getMajor())) {
            return false;
        }
        return true;
    }

    /**
     * check is support api version
     *
     * @param version target version
     * @return is support api version
     */
    public static boolean isSupportApiVersion(int version) {
        if (!SdkUtils.isConfigCompatibleSdk()) {
            return version >= API_VERSION_22;
        }
        return version >= API_VERSION_20;
    }

    /**
     * check is contain cangjie ability
     *
     * @param moduleModel target module
     * @return is contain cangjie ability
     */
    public static boolean isContainCangjieAbility(OhosModuleModel moduleModel) {
        if (moduleModel == null) {
            return false;
        }
        Path cangjieAbilityPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie");
        Path cangjieCjpmPath = Path.of(getRealCjpmTomlDir(moduleModel, CangjieModulePathType.MAIN), "cjpm.toml");
        return cangjieAbilityPath.toFile().exists() && cangjieCjpmPath.toFile().exists();
    }

    /**
     * isContainCangjieHap
     *
     * @param renderParameterMap renderParameterMap
     * @return isContainCangjieHap
     */
    public static boolean isContainCangjieHap(RenderHashMap renderParameterMap) {
        Path projectPath = Path.of(renderParameterMap.getString(PROJECT_PATH));
        if (!projectPath.toFile().exists()) {
            return false;
        }
        Project project = ProjectUtil.guessProjectForFile(
                VirtualFileManager.getInstance().findFileByNioPath(projectPath));
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return false;
        }
        for (ModuleModel model : projectModel.getModuleModelList()) {
            if (!(model instanceof OhosModuleModel)
                    || !FileUtils.isCangjieModule(model)) {
                continue;
            }
            if (ModuleType.ENTRY.toString().equalsIgnoreCase(model.getModuleType())
                    || ModuleType.FEATURE.toString().equalsIgnoreCase(model.getModuleType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * check is contain ets module
     *
     * @param moduleModel moduleModel
     * @return is contain ets module
     */
    public static boolean isContainEtsModule(OhosModuleModel moduleModel) {
        if (moduleModel == null) {
            return false;
        }
        Path etsModulePath = Path.of(moduleModel.getModulePath(), "src", "main", "ets");
        return etsModulePath.toFile().exists();
    }

    /**
     * check is in cangjie dir
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @param isOhosTest isOhosTest
     * @return is in cangjie dir
     */
    public static boolean isInCangjieCodeDir(OhosModuleModel moduleModel, VirtualFile virtualFile,
                                             boolean isOhosTest) {
        if (isOhosTest) {
            return isInCangjieCodeDir(moduleModel, virtualFile, CangjieModulePathType.OHOS_TEST);
        }
        return isInCangjieCodeDir(moduleModel, virtualFile, CangjieModulePathType.MAIN);
    }

    /**
     * check is in cangjie dir
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @param pathType pathType
     * @return is in cangjie dir
     */
    public static boolean isInCangjieCodeDir(OhosModuleModel moduleModel, VirtualFile virtualFile,
                                             CangjieModulePathType pathType) {
        if (moduleModel == null || virtualFile == null) {
            return false;
        }
        if (!isCangjieModule(moduleModel) || !isContainCangjieAbility(moduleModel)) {
            return false;
        }
        String cjpmDirPath = getRealCjpmTomlDir(moduleModel, pathType);
        try {
            String srcDir = getCangjieModuleSrcDir(moduleModel, pathType);
            String cangjieCodeDirPath = "";
            try {
                cangjieCodeDirPath = Path.of(cjpmDirPath, srcDir)
                        .toFile().getCanonicalPath().replaceAll("\\\\", "/");
            } catch (InvalidPathException e) {
                LOG.warn("Invalid custom combination src-dir file path.");
                cangjieCodeDirPath = Path.of(moduleModel.getModulePath())
                        .toFile().getCanonicalPath().replaceAll("\\\\", "/");
            }
            String targetFilePath = virtualFile.getCanonicalPath();
            if (StringUtil.isEmpty(targetFilePath)) {
                return false;
            }
            targetFilePath = targetFilePath.replaceAll("\\\\", "/");
            return targetFilePath.startsWith(cangjieCodeDirPath);
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * check is in hybrid module loader
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @return is in hybrid module loader
     */
    public static boolean isInEtsCjLoader(OhosModuleModel moduleModel, VirtualFile virtualFile) {
        if (moduleModel == null || virtualFile == null) {
            return false;
        }
        Path etsCodePath = Path.of(moduleModel.getModulePath(), "src", "main", "ets");
        if (!etsCodePath.toFile().exists()) {
            return false;
        }
        String etsCjLoaderPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "loader")
                .normalize().toString().replaceAll("\\\\", "/");
        String etsCjTypesPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "types")
                .normalize().toString().replaceAll("\\\\", "/");
        String etsCjInteropPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", "ark_interop_api")
                .normalize().toString().replaceAll("\\\\", "/");
        String targetFilePath = virtualFile.getCanonicalPath();
        if (StringUtil.isEmpty(targetFilePath)) {
            return false;
        }
        targetFilePath = targetFilePath.replaceAll("\\\\", "/");
        return targetFilePath.startsWith(etsCjLoaderPath) || targetFilePath.startsWith(etsCjTypesPath)
                || targetFilePath.startsWith(etsCjInteropPath);
    }

    /**
     * get package name
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @param isOhosTest isOhosTest
     * @return package name
     */
    public static String getPackageName(OhosModuleModel moduleModel, VirtualFile virtualFile, boolean isOhosTest) {
        if (isOhosTest) {
            return getPackageName(moduleModel, virtualFile, CangjieModulePathType.OHOS_TEST);
        }
        return getPackageName(moduleModel, virtualFile, CangjieModulePathType.MAIN);
    }

    /**
     * get package name
     *
     * @param moduleModel moduleModel
     * @param virtualFile virtualFile
     * @param pathType pathType
     * @return package name
     */
    public static String getPackageName(OhosModuleModel moduleModel, VirtualFile virtualFile,
                                        CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return DEFAULT;
        }
        String cangjieModuleName = FileUtils.getCangjieModuleName(moduleModel, pathType);
        String defaultPkgName = StringUtil.isEmpty(cangjieModuleName) ? DEFAULT : cangjieModuleName;
        if (virtualFile == null || !FileUtils.isInCangjieCodeDir(moduleModel, virtualFile, pathType)) {
            return defaultPkgName;
        }
        if (pathType == CangjieModulePathType.OHOS_TEST) {
            defaultPkgName = defaultPkgName + "_test";
        }
        if (pathType == CangjieModulePathType.LOCAL_TEST) {
            defaultPkgName = defaultPkgName + "_local_test";
        }
        String srcDir = FileUtils.getCangjieModuleSrcDir(moduleModel, pathType);
        String cangjieModuleRootPath = "";
        try {
            String cjpmDirPath = getRealCjpmTomlDir(moduleModel, pathType);
            cangjieModuleRootPath = Path.of(cjpmDirPath, srcDir)
                    .normalize().toString().replaceAll("\\\\", "/");
        } catch (InvalidPathException e) {
            LOG.warn("Invalid custom combination src-dir file path.");
            cangjieModuleRootPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie", SRC)
                    .normalize().toString().replaceAll("\\\\", "/");
        }
        String targetPath = virtualFile.getCanonicalPath();
        if (!virtualFile.isDirectory() && virtualFile.getParent() != null) {
            targetPath = virtualFile.getParent().getCanonicalPath();
        }
        if (StringUtil.isEmpty(targetPath)) {
            return defaultPkgName;
        }
        targetPath = targetPath.replaceAll("\\\\", "/");
        if (!targetPath.contains(cangjieModuleRootPath)) {
            return defaultPkgName;
        }
        String[] foldersName = targetPath.substring(cangjieModuleRootPath.length()).split("/");
        StringBuilder packageName = new StringBuilder(cangjieModuleName);
        for (int i = 0; i < foldersName.length; i++) {
            if (StringUtil.isEmpty(foldersName[i])) {
                continue;
            }
            packageName.append(".");
            packageName.append(foldersName[i]);
        }
        return packageName.toString();
    }

    /**
     * getCangjieModuleName
     *
     * @param moduleModel moduleModel
     * @param isOhosTest isOhosTest
     * @return Cangjie module name (in toml)
     */
    public static String getCangjieModuleName(OhosModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getCangjieModuleName(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getCangjieModuleName(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * getCangjieModuleName
     *
     * @param moduleModel moduleModel
     * @param pathType pathType
     * @return Cangjie module name (in toml)
     */
    public static String getCangjieModuleName(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        Queue<String> keys = new LinkedList<>(List.of(PACKAGE, NAME));
        return getCjpmTomlContent(moduleModel, keys, TomlKind.STRING, StringUtil.EMPTY, pathType);
    }

    /**
     * getCangjieModuleSrcDir
     *
     * @param moduleModel moduleModel
     * @param isOhosTest isOhosTest
     * @return Cangjie module src-dir (in toml)
     */
    public static String getCangjieModuleSrcDir(OhosModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getCangjieModuleSrcDir(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getCangjieModuleSrcDir(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * getCangjieModuleSrcDir
     *
     * @param moduleModel moduleModel
     * @param pathType pathType
     * @return Cangjie module src-dir (in toml)
     */
    public static String getCangjieModuleSrcDir(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        Queue<String> keys = new LinkedList<>(List.of(PACKAGE, SRC_DIR));
        String srcDir = getCjpmTomlContent(moduleModel, keys, TomlKind.STRING, SRC, pathType);
        if (StringUtil.isEmpty(srcDir)) {
            srcDir = SRC;
        }
        return srcDir;
    }

    /**
     * getCangjieTargetCompileOption
     *
     * @param moduleModel moduleModel
     * @param isOhosTest isOhosTest
     * @return Cangjie module target.aarch64-linux-ohos.compile-option
     */
    public static String getCangjieTargetCompileOption(OhosModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getCangjieTargetCompileOption(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getCangjieTargetCompileOption(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * getCangjieTargetCompileOption
     *
     * @param moduleModel moduleModel
     * @param pathType pathType
     * @return Cangjie module target.aarch64-linux-ohos.compile-option
     */
    public static String getCangjieTargetCompileOption(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        Queue<String> keys = new LinkedList<>(List.of(TARGET, AARCH64_LINUX_OHOS, COMPILE_OPTION));
        return getCjpmTomlContent(moduleModel, keys, TomlKind.STRING, StringUtils.EMPTY, pathType);
    }

    /**
     * getCangieStdxDir
     *
     * @param moduleModel moduleModel
     * @param pathType pathType
     * @return stdx path
     */
    public static Set<String> getMacCangieStdxDir(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (!FileUtils.isMac()) {
            return Collections.emptySet();
        }

        if (moduleModel == null) {
            return Collections.emptySet();
        }

        Set<String> stdxSet = new HashSet<>();
        final List<String> targets = List.of(
                AARCH64_APPLE_DARWIN,
                AARCH64_LINUX_OHOS,
                X86_64_APPLE_DARWIN,
                X86_64_LINUX_OHOS
        );
        targets.forEach(target -> {
            Queue<String> keys = createKeysQueue(target);
            List<String> paths = getCjpmTomlContent(
                    moduleModel,
                    keys,
                    TomlKind.LIST,
                    new ArrayList<String>(),
                    pathType
            );
            paths.stream()
                    .filter(path -> path.contains("stdx"))
                    .forEach(stdxSet::add);
        });
        return stdxSet;
    }

    /**
     * getCangjiePackageCompileOption
     *
     * @param moduleModel OhosModuleModel
     * @param isOhosTest boolean
     * @return String
     */
    public static String getCangjiePackageCompileOption(OhosModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getCangjiePackageCompileOption(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getCangjiePackageCompileOption(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * getCangjiePackageCompileOption
     *
     * @param moduleModel OhosModuleModel
     * @param pathType pathType
     * @return String
     */
    public static String getCangjiePackageCompileOption(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        Queue<String> keys = new LinkedList<>(List.of(PACKAGE, COMPILE_OPTION));
        return getCjpmTomlContent(moduleModel, keys, TomlKind.STRING, StringUtils.EMPTY, pathType);
    }

    /**
     * getCangjieOverrideCompileOption
     *
     * @param moduleModel OhosModuleModel
     * @param pathType pathType
     * @return String
     */
    public static String getCangjieOverrideCompileOption(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return StringUtil.EMPTY;
        }
        Queue<String> keys = new LinkedList<>(List.of(PACKAGE, OVERRIDE_COMPILE_OPTION));
        return getCjpmTomlContent(moduleModel, keys, TomlKind.STRING, StringUtils.EMPTY, pathType);
    }

    /**
     * getTomlUnitMap
     *
     * @param tomlMap tomlMap
     * @param tomlKeys tomlKeys
     * @param operation operation
     * @return map
     */
    public static Map<String, Object> getTomlUnitMap(Map<String, Object> tomlMap, List<String> tomlKeys,
                                           TomlUpdateTypeData.OperationEnum operation) {
        Map<String, Object> currentMap = tomlMap;
        for (int i = 0; i < tomlKeys.size() - 1; i++) {
            String key = tomlKeys.get(i);
            Object nextLevel = currentMap.get(key);
            if (nextLevel instanceof Map) {
                currentMap = (Map<String, Object>) nextLevel;
                continue;
            }
            if (nextLevel == null && operation == TomlUpdateTypeData.OperationEnum.FORCED_REPLACE) {
                Map<String, Object> nextLevelMap = new HashMap<>();
                currentMap.put(key, nextLevelMap);
                currentMap = nextLevelMap;
            } else {
                currentMap = new HashMap<>();
                break;
            }
        }
        return currentMap;
    }

    /**
     * getCangjiePackageCompileOption
     *
     * @param moduleModel OhosModuleModel
     * @param pathType the path type
     * @return String customized option
     */
    public static Optional<Toml> getCustomizedOption(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        if (moduleModel == null) {
            return Optional.empty();
        }
        Queue<String> keys = new LinkedList<>(List.of(KEY_PROFILE, CUSTOMIZED_OPTION));
        return getCjpmTomlContent(moduleModel, keys, TomlKind.TABLE, Optional.empty(), pathType);
    }

    private static Queue<String> createKeysQueue(String target) {
        return new LinkedList<>(List.of(TARGET, target, BIN_DEPENDENCIES, PATH_OPTION));
    }

    private static <T> T getCjpmTomlContent(OhosModuleModel moduleModel,
                                            Queue<String> keys,
                                            TomlKind kind,
                                            T defaultValue,
                                            CangjieModulePathType pathType) {
        if (moduleModel == null || keys == null || keys.isEmpty()) {
            return defaultValue;
        }
        Path tomlPath = getRealCjpmFilePath(moduleModel, pathType);
        if (!tomlPath.toFile().exists()) {
            return defaultValue;
        }
        Optional<Toml> optModuleToml;
        try {
            optModuleToml = new Toml().read(tomlPath.toFile());
        } catch (IllegalStateException e) {
            optModuleToml = Optional.empty();
        }
        if (optModuleToml.isEmpty()) {
            return defaultValue;
        }
        return getCjpmTomlContent(optModuleToml.get(), keys, kind, defaultValue);
    }

    private static <T> T getCjpmTomlContent(Toml toml, Queue<String> keys, TomlKind kind, T defaultValue) {
        if (toml == null || keys == null || keys.isEmpty()) {
            return defaultValue;
        }
        String key = keys.poll();
        if (keys.isEmpty()) {
            BiFunction<String, Toml, T> function = (BiFunction<String, Toml, T>) TOML_BI_FUNCTION.get(kind);
            if (function == null) {
                return defaultValue;
            }
            return function.apply(key, toml);
        }
        Optional<Toml> tomlTable = toml.getTable(key);
        if (tomlTable.isEmpty()) {
            return defaultValue;
        }
        return getCjpmTomlContent(tomlTable.get(), keys, kind, defaultValue);
    }

    /**
     * Check if target dir is exists
     *
     * @param dirPath target dir path
     * @return boolean if dir is exists
     */
    public static boolean isDirExist(String dirPath) {
        return org.apache.commons.io.FileUtils.getFile(dirPath).exists();
    }

    /**
     * Check if it is a file
     *
     * @param dirPath dir path
     * @return boolean if it is a file
     */
    public static boolean isFile(String dirPath) {
        return org.apache.commons.io.FileUtils.getFile(dirPath).isFile();
    }

    /**
     * Check if the dir is not exists and empty
     *
     * @param dirPath dir path
     * @return boolean if the dir is exists and empty
     */
    public static boolean isDirEmptyOrNotExist(String dirPath) {
        File dir = org.apache.commons.io.FileUtils.getFile(dirPath);
        if (!dir.exists()) {
            return true;
        }
        if (dir.list() == null) {
            return true;
        }
        return dir.list().length == 0;
    }

    /**
     * Check if the file has wirte permission
     *
     * @param path the target path
     * @return boolean has write permission
     */
    public static boolean hasPermission(String path) {
        File parent = org.apache.commons.io.FileUtils.getFile(path);
        do {
            parent = parent.getParentFile();
            if (parent == null) {
                return false;
            }
            if (parent.exists()) {
                return Files.isWritable(parent.toPath());
            }
        } while (true);
    }

    /**
     * change file name style to snake case
     *
     * @param camelCaseString camelCaseString
     * @return snake case file name
     */
    public static String convertFileNameToSnakeCase(String camelCaseString) {
        // Add underline between uppercase and lowercase letters
        return camelCaseString
                .replaceAll("([a-zA-Z0-9])([A-Z])", "$1_$2")
                .toLowerCase();
    }

    /**
     * get cangjie files name under folder
     *
     * @param folder folder
     * @return cangjie files name
     */
    public static List<String> getCjFilesName(VirtualFile folder) {
        List<String> fileNames = new ArrayList<>();
        if (folder == null || !folder.isDirectory()) {
            return fileNames;
        }
        for (VirtualFile file : folder.getChildren()) {
            if (file == null || file.isDirectory() || StringUtil.isEmpty(file.getName())
                    || !"cj".equals(file.getExtension())) {
                continue;
            }
            String name = file.getNameWithoutExtension();
            fileNames.add(name.toLowerCase(Locale.ROOT));
            if (name.contains("_")) {
                fileNames.add(replaceUnderlineFileName(name).toLowerCase(Locale.ROOT));
            }
        }
        return fileNames;
    }

    /**
     * replace Underline FileName
     *
     * @param fileName fileName
     * @return replace string
     */
    public static String replaceUnderlineFileName(String fileName) {
        if (!fileName.startsWith("_")) {
            return fileName.replaceAll("_", "");
        }
        return "_" + fileName.replaceAll("_", "");
    }

    /**
     * check is dynamic combined
     *
     * @param moduleModel moduleModel
     * @return is dynamic combined
     */
    public static boolean isDynamicCombined(ModuleModel moduleModel) {
        if (moduleModel == null) {
            return false;
        }
        String cjpmDirPath = getRealCjpmTomlDir(moduleModel, CangjieModulePathType.MAIN);
        Path tomlPath = Path.of(cjpmDirPath, "cjpm.toml");
        if (!tomlPath.toFile().exists()) {
            return false;
        }
        Optional<Toml> optModuleToml;
        try {
            optModuleToml = new Toml().read(tomlPath.toFile());
        } catch (IllegalStateException e) {
            optModuleToml = Optional.empty();
        }
        if (optModuleToml.isEmpty()) {
            return false;
        }
        Optional<Toml> profile = optModuleToml.get().getTable(Constants.KEY_PROFILE);
        if (profile.isEmpty()) {
            return false;
        }
        Optional<Toml> build = profile.get().getTable(Constants.KEY_BUILD);
        if (build.isEmpty()) {
            return false;
        }
        Optional<Toml> combined = build.get().getTable(Constants.KEY_COMBINED);
        return combined.isPresent();
    }

    /**
     * get real cjpm.toml Dir
     *
     * @param moduleModel moduleModel
     * @param isOhosTest isOhosTest
     * @return cjpm.toml Dir
     */
    public static String getRealCjpmTomlDir(ModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getRealCjpmTomlDir(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getRealCjpmTomlDir(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * get real cjpm.toml Dir
     *
     * @param moduleModel moduleModel
     * @param pathType pathType
     * @return cjpm.toml Dir
     */
    public static String getRealCjpmTomlDir(ModuleModel moduleModel, CangjieModulePathType pathType) {
        if (pathType == CangjieModulePathType.OHOS_TEST) {
            return Path.of(moduleModel.getModulePath(), SRC, Constants.OHOS_TEST, Constants.CANGJIE).toString();
        }
        if (pathType == CangjieModulePathType.LOCAL_TEST) {
            return Path.of(moduleModel.getModulePath(), Constants.SRC, Constants.TEST, Constants.CANGJIE).toString();
        }
        String cjpmDirPath = moduleModel.getModulePath();
        if (!Path.of(cjpmDirPath, Constants.CJPM_FILE).toFile().exists()) {
            cjpmDirPath = Path.of(
                    moduleModel.getModulePath(), Constants.SRC, Constants.MAIN, Constants.CANGJIE).toString();
        }
        return cjpmDirPath;
    }

    /**
     * get real cjpm file path
     *
     * @param module module
     * @param pathType pathType
     * @return real cjpm file path
     */
    public static Path getRealCjpmFilePath(@NotNull ModuleModel module, CangjieModulePathType pathType) {
        Path cjpmFilePath = Path.of(module.getModulePath(), Constants.CJPM_FILE);
        if (!cjpmFilePath.toFile().exists()) {
            cjpmFilePath = Path.of(
                    module.getModulePath(), Constants.SRC, Constants.MAIN, Constants.CANGJIE, Constants.CJPM_FILE);
        }
        if (pathType == CangjieModulePathType.OHOS_TEST) {
            cjpmFilePath = Path.of(
                    module.getModulePath(),
                    Constants.SRC, Constants.OHOS_TEST, Constants.CANGJIE, Constants.CJPM_FILE);
        }
        if (pathType == CangjieModulePathType.LOCAL_TEST) {
            cjpmFilePath = Path.of(
                    module.getModulePath(),
                    Constants.SRC, Constants.TEST, Constants.CANGJIE, Constants.CJPM_FILE);
        }
        return cjpmFilePath;
    }

    /**
     * getCangjieSrcRootPath
     *
     * @param moduleModel moduleModel
     * @param isOhosTest isOhosTest
     * @return the cangjie src root path
     */
    public static String getCangjieSrcRootPath(OhosModuleModel moduleModel, boolean isOhosTest) {
        if (isOhosTest) {
            return getCangjieSrcRootPath(moduleModel, CangjieModulePathType.OHOS_TEST);
        }
        return getCangjieSrcRootPath(moduleModel, CangjieModulePathType.MAIN);
    }

    /**
     * Gets cangjie src root path.
     *
     * @param moduleModel the module model
     * @param pathType pathType
     * @return the cangjie src root path
     */
    public static String getCangjieSrcRootPath(OhosModuleModel moduleModel, CangjieModulePathType pathType) {
        String srcDir = FileUtils.getCangjieModuleSrcDir(moduleModel, pathType);
        String cangjieSrcRootPath;
        try {
            String cjpmDirPath = getRealCjpmTomlDir(moduleModel, pathType);
            cangjieSrcRootPath = Path.of(cjpmDirPath, srcDir)
                .normalize().toString().replaceAll("\\\\", "/");
        } catch (InvalidPathException e) {
            LOG.warn("Invalid custom combination src-dir file path.");
            cangjieSrcRootPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie")
                .normalize().toString().replaceAll("\\\\", "/");
        }
        return cangjieSrcRootPath;
    }

    private enum TomlKind {
        STRING,
        LONG,
        LIST,
        BOOLEAN,
        TABLE,
        TABLES;

        private TomlKind() {
        }
    }

    /**
     * remove stdx mac attribute
     *
     * @param projectModel projectModel
     */
    public static void removeStdxAttribute(@NotNull ProjectModel projectModel) {
        Set<String> stdxSet = new HashSet<>();
        List<ModuleModel> moduleModels = projectModel.getModuleModelList();
        for (ModuleModel moduleModel : moduleModels) {
            if (!(moduleModel instanceof OhosModuleModel)
                    || !FileUtils.isCangjieModule((OhosModuleModel) moduleModel)) {
                continue;
            }
            Set<String> stdxDirs = FileUtils.getMacCangieStdxDir((OhosModuleModel) moduleModel,
                    CangjieModulePathType.MAIN);
            stdxSet.addAll(stdxDirs);
        }
        stdxSet.forEach(FileUtils::removeFileAttribute);
    }

    /**
     * check cangjie path type
     *
     * @param model model
     * @param pathStr path
     * @return cangjie path type
     */
    public static CangjieModulePathType checkCangjiePathType(ModuleModel model, String pathStr) {
        if (model == null) {
            return CangjieModulePathType.MAIN;
        }
        if (StringUtil.isEmpty(pathStr)) {
            return CangjieModulePathType.MAIN;
        }
        Path path = Path.of(pathStr);
        if (!path.toFile().exists()) {
            return CangjieModulePathType.MAIN;
        }
        String parentPath = Path.of(model.getModulePath(), Constants.SRC, Constants.MAIN, Constants.CANGJIE)
                .toAbsolutePath().normalize().toString();
        if (isSameOrSubPath(parentPath, pathStr)) {
            return CangjieModulePathType.MAIN;
        }
        parentPath = Path.of(model.getModulePath(), Constants.SRC, Constants.OHOS_TEST, Constants.CANGJIE)
                .toAbsolutePath().normalize().toString();
        if (isSameOrSubPath(parentPath, pathStr)) {
            return CangjieModulePathType.OHOS_TEST;
        }
        parentPath = Path.of(model.getModulePath(), Constants.SRC, Constants.TEST, Constants.CANGJIE)
                .toAbsolutePath().normalize().toString();
        if (isSameOrSubPath(parentPath, pathStr)) {
            return CangjieModulePathType.LOCAL_TEST;
        }
        return CangjieModulePathType.MAIN;
    }

    /**
     * check is same or sub path
     *
     * @param parentPath parentPath
     * @param childPath childPath
     * @return is same or sub path
     */
    public static boolean isSameOrSubPath(String parentPath, String childPath) {
        Path parent = Path.of(parentPath).toAbsolutePath().normalize();
        Path child = Path.of(childPath).toAbsolutePath().normalize();
        return child.startsWith(parent);
    }

    /**
     * removeFileAttribute
     *
     * @param file file
     */
    public static void removeFileAttribute(String file) {
        if (!FileUtils.isMac() || StringUtils.isEmpty(file)) {
            return;
        }
        Path path = Paths.get(file);
        if (!Files.exists(path)) {
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("xattr", "-dr", "com.apple.quarantine", path.toString());
            Process process = pb.start();
            StreamConsumer errConsumer = new StreamConsumer(process.getErrorStream(), "dealErrorStream");
            StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), "dealInputStream");
            errConsumer.start();
            outputConsumer.start();
            int exitCode = process.waitFor();
            errConsumer.join();
            outputConsumer.join();
            if (exitCode == 0) {
                LOG.info("Succeeded to remove file mac attribute.");
            } else {
                LOG.info("Failed to remove file mac attribute.");
            }
        } catch (IOException | InterruptedException e) {
            LOG.info("Failed to remove file mac attribute.");
        }
    }

    /**
     * reset profile.
     *
     * @param project the project
     */
    public static void resetProfileFlag(Project project) {
        Application application = ApplicationManager.getApplication();
        if (application.isDisposed() || project.isDisposed()) {
            return;
        }
        application.invokeLater(() -> {
            if (application.isDisposed() || project.isDisposed()) {
                return;
            }
            ProjectOptimizationSettingsService.State state =
                    ProjectOptimizationSettingsService.getInstance(project).getState();
            if (state == null || !state.isGenerateOptimizationProfile()) {
                return;
            }
            state.setGenerateOptimizationProfile(false);
            application.executeOnPooledThread(() -> {
                if (application.isDisposed() || project.isDisposed()) {
                    return;
                }
                project.save();
            });
        }, ModalityState.nonModal());
    }

    private static final class StreamConsumer extends Thread {
        private final InputStream stream;

        /**
         * custom thread
         *
         * @param stream stream
         * @param name name
         */
        public StreamConsumer(InputStream stream, String name) {
            super.setName(name);
            this.stream = stream;
        }

        @Override
        public void run() {
            StringBuilder retString = new StringBuilder();
            try (BufferedReader brInputStream =
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = brInputStream.readLine()) != null) {
                    retString.append(line);
                    retString.append(System.lineSeparator());
                }
                LOG.info(retString.toString());
            } catch (IOException e) {
                LOG.warn("Error reading next line!!");
            }
        }
    }
}
