/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.ohoslauncher;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.getBuildOption;
import static com.huawei.ideacj.lsp.utils.Constants.ARGUMENTS;
import static com.huawei.ideacj.lsp.utils.Constants.BIN_DEPENDENCIES;
import static com.huawei.ideacj.lsp.utils.Constants.BUILD;
import static com.huawei.ideacj.lsp.utils.Constants.CANGJIE_OPTIONS;
import static com.huawei.ideacj.lsp.utils.Constants.COMBINED;
import static com.huawei.ideacj.lsp.utils.Constants.COMPILE_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.CONDITION_TARGET;
import static com.huawei.ideacj.lsp.utils.Constants.CUSTOMIZED_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.DEPENDENCIES;
import static com.huawei.ideacj.lsp.utils.Constants.DYNAMIC;
import static com.huawei.ideacj.lsp.utils.Constants.GIT;
import static com.huawei.ideacj.lsp.utils.Constants.NAME;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE;
import static com.huawei.ideacj.lsp.utils.Constants.PACKAGE_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.PATH;
import static com.huawei.ideacj.lsp.utils.Constants.PATH_OPTION;
import static com.huawei.ideacj.lsp.utils.Constants.PROFILE;
import static com.huawei.ideacj.lsp.utils.Constants.SRC_DIR;
import static com.huawei.ideacj.lsp.utils.Constants.TARGET;
import static com.huawei.ideacj.lsp.utils.Constants.X86_64_TARGET;
import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_FILE;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.TargetManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosTarget;
import com.huawei.deveco.projectmodel.ohos.util.ModuleType;
import com.huawei.deveco.projectmodel.ohos.util.ProjectUtil;
import com.huawei.deveco.projectmodel.ohos.util.PsiJsonFileUtil;
import com.huawei.ideacj.lsp.project.LspProjectManager;
import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.lsp.utils.Constants;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;
import com.huawei.ideacj.notification.NotificationUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.Strings;

import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * CJModuleParser
 *
 * @since 2024/03/23
 */
public class CJModuleParser {
    private static final ProcessBuilder PROCESS_BUILDER = new ProcessBuilder();

    private static final Logger LOG = Logger.getInstance(CJModuleParser.class);

    private static final Pattern CFG_PATTERN = Pattern.compile("--cfg=\"([^\"]*)\"");

    private static final Pattern INTERPOLATED_STRING_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * module uri -> ModuleDetail
     */
    private final Map<String, ModuleDetail> multiModuleOption = new HashMap<>();

    /**
     * module name -> {"conditionKey": "conditionValue"}
     */
    private final Map<String, Map<String, String>> moduleConditionCompileOption = new HashMap<>();

    private final StringBuilder requiresEnvPath = new StringBuilder();

    private final Set<String> uriSet = new HashSet<>();

    private final Project project;

    public CJModuleParser(Project project) {
        this.project = project;
    }

    class ModuleDetail {
        /**
         * packageRequires
         */
        @JSONField(name = "package_requires")
        private PackageRequires packageRequires;

        /**
         * name
         */
        @JSONField(name = "name")
        private String name;

        /**
         * combined
         */
        @JSONField(name = "combined")
        private boolean combined;

        /**
         * requires
         */
        @JSONField(name = "requires")
        private Map<String, Require> requires;

        /**
         * src_path
         */
        @JSONField(name = "src_path")
        private String srcPath;

        public PackageRequires getPackageRequires() {
            return packageRequires;
        }

        public void setPackageRequires(PackageRequires packageRequires) {
            this.packageRequires = packageRequires;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public Map<String, Require> getRequires() {
            return requires;
        }

        public void setRequires(Map<String, Require> requires) {
            this.requires = requires;
        }

        public boolean getCombined() {
            return combined;
        }

        public void setCombined(boolean combined) {
            this.combined = combined;
        }
    }

    class PackageRequires {
        /**
         * pathOption
         */
        @JSONField(name = "path_option")
        private List<String> pathOption;

        /**
         * packageOption
         */
        @JSONField(name = "package_option")
        private Map<String, Object> packageOption;

        public List<String> getPathOption() {
            return pathOption;
        }

        public void setPathOption(List<String> pathOption) {
            this.pathOption = pathOption;
        }

        public Map<String, Object> getPackageOption() {
            return packageOption;
        }

        public void setPackageOption(Map<String, Object> packageOption) {
            this.packageOption = packageOption;
        }
    }

    class Require {
        /**
         * path
         */
        @JSONField(name = "path")
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * set need evn
     *
     * @param project current project
     */
    public void setNeedEnv(Project project) {
        Map<String, String> envs = CangjieEnvUtils.getProjectEnvs(project);
        if (envs != null) {
            PROCESS_BUILDER.environment().putAll(envs);
        }
    }

    /**
     * write json data to depFile
     *
     * @param depFile store dependency info
     */
    public void write(File depFile) {
        try (OutputStream writer = new FileOutputStream(depFile)) {
            JSONObject multiModuleJson = new JSONObject();
            multiModuleJson.put(Constants.MULTI_MODULE_OPTION, multiModuleOption);
            multiModuleJson.put(Constants.MODULE_CONDITION_COMPILE_OPTION, moduleConditionCompileOption);
            multiModuleJson.put(Constants.REQUIRES_ENV_PATH, requiresEnvPath.toString());

            JSON.writeTo(writer, multiModuleJson, JSONWriter.Feature.PrettyFormat);
        } catch (IOException e) {
            LOG.warn("Failed to write JSON data to the dependency file: " + depFile.getAbsolutePath(), e);
        }
    }

    /**
     * parse one module's source and binary dependency
     *
     * @param moduleToml a toml object of cjpm.toml
     * @param moduleDir dir of cjpm.toml
     */
    public void parse(Toml moduleToml, String moduleDir) {
        ModuleDetail moduleDetail = new ModuleDetail();
        if (this.uriSet.contains(moduleDir)) {
            return;
        }
        this.uriSet.add(moduleDir);
        if (moduleToml == null) {
            moduleDetail.setName(Path.of(moduleDir).getFileName().toString());
            multiModuleOption.put(toAbsDirectory(moduleDir, moduleDir).toString(), moduleDetail);
            return;
        }
        Optional<Toml> pkgConfig = moduleToml.getTable(PACKAGE);
        pkgConfig.ifPresent(toml -> parseToml(moduleDir, toml, moduleDetail));
        if (moduleDetail.getName() == null || moduleDetail.getName().isEmpty()) {
            moduleDetail.setName(Path.of(moduleDir).getFileName().toString());
        }

        // combined
        boolean combined = parseCombined(moduleToml, moduleDetail.getName());
        moduleDetail.setCombined(combined);

        // dependency
        Map<String, Require> requires = new HashMap<>();
        Optional<Toml> depsToml = moduleToml.getTable(DEPENDENCIES);
        depsToml.ifPresent(toml -> parseDeps(toml, moduleDir, requires));
        moduleDetail.setRequires(requires);

        // target.vendor.bin-dependencies.package-requires
        PackageRequires packageRequires = new PackageRequires();
        packageRequires.setPackageOption(new HashMap<>());
        packageRequires.setPathOption(new ArrayList<>());
        Optional<Toml> targetsToml = moduleToml.getTable(TARGET);
        if (targetsToml.isPresent()) {
            for (String vendor : targetsToml.get().toMap().keySet()) {
                // tmp solution for previewer
                if (X86_64_TARGET.equals(vendor)) {
                    continue;
                }
                parseVendor(moduleDir, vendor, targetsToml.get(), packageRequires);
            }
        }

        // buildEnvPath
        try {
            String splitChar = LspConfigUtils.getSplit();
            String buildEnvPath = LspConfigUtils.getBuildEnvPath(moduleDir, project);
            if (!StringUtil.isEmpty(buildEnvPath)) {
                String[] buildEnvs = buildEnvPath.split(LspConfigUtils.getSplit());
                for (String buildEnv : buildEnvs) {
                    requiresEnvPath.append(buildEnv);
                    requiresEnvPath.append(splitChar);
                    packageRequires.getPathOption().add(
                            FileUtils.sanitizeURI(toAbsDirectory(buildEnv, moduleDir).toString()));
                }
            }
        } catch (IOException exception) {
            LOG.info("get module build binary error!");
        }

        moduleDetail.setPackageRequires(packageRequires);

        multiModuleOption.put(toAbsDirectory(moduleDir, moduleDir).toString(), moduleDetail);
    }

    private void parseToml(String moduleDir, Toml toml, ModuleDetail moduleDetail) {
        moduleDetail.setName(toml.getString(NAME).isEmpty()
                ? Path.of(moduleDir).getFileName().toString() : toml.getString(NAME));
        // src_dir
        String srcDir = toml.getString(SRC_DIR);
        if (StringUtil.isEmpty(srcDir)) {
            return;
        }
        if (!LspConfigUtils.isAbsolutePath(srcDir)) {
            try {
                srcDir = Path.of(moduleDir, srcDir).normalize().toString().replaceAll("\\\\", "/");
            } catch (InvalidPathException e) {
                Path cjpmFilePath = Path.of(moduleDir, CJPM_FILE);
                NotificationUtil.notifyInfo(
                        CangjieBundle.message("lsp.src.dir.invalid", cjpmFilePath.toString()),
                        project, NotificationType.WARNING,
                        NotificationUtil.getOpenFileAction(cjpmFilePath.toString()));
                return;
            }
        }
        try {
            moduleDetail.setSrcPath(FileUtils.sanitizeURI(toAbsDirectory(srcDir, moduleDir).toString()));
        } catch (IllegalArgumentException e) {
            Path cjpmFilePath = Path.of(moduleDir, CJPM_FILE);
            NotificationUtil.notifyInfo(
                    CangjieBundle.message("lsp.src.dir.invalid", cjpmFilePath.toString()),
                    project, NotificationType.WARNING,
                    NotificationUtil.getOpenFileAction(cjpmFilePath.toString()));
        }
    }

    private void parseVendor(String moduleDir, String vendor, Toml targetsToml, PackageRequires packageRequires) {
        Optional<Toml> vendorToml = targetsToml.getTable(vendor);
        if (vendorToml.isPresent()) {
            Optional<Toml> binDepsToml = vendorToml.get().getTable(BIN_DEPENDENCIES);
            binDepsToml.ifPresent(toml -> parsePkgReqs(packageRequires, toml, moduleDir));
        }
    }

    private void parseDeps(Toml depsToml, String moduleDir, Map<String, Require> requires) {
        for (String key : depsToml.toMap().keySet()) {
            Optional<Toml> optionalToml = depsToml.getTable(key);
            if (optionalToml.isEmpty()) {
                return;
            }
            Toml reqItem = optionalToml.get();
            Require req = new Require();
            String path = reqItem.getString(PATH);
            if (!Strings.isEmpty(path)) {
                // path src code
                String pathDir = getRealPath(path, moduleDir);
                req.setPath(FileUtils.sanitizeURI(toAbsDirectory(pathDir, moduleDir).toString()));
                requires.put(key, req);
                if (this.uriSet.contains(pathDir)) {
                    continue;
                }
                Toml moduleToml = null;
                Optional<Toml> optModuleToml = LspConfigUtils.getModuleCjpmToml(
                        project, new File(pathDir, CJPM_FILE), true);
                if (optModuleToml.isPresent()) {
                    moduleToml = optModuleToml.get();
                }
                parse(moduleToml, pathDir);
            } else {
                // git code
                parseGit(moduleDir, requires, key, reqItem, req);
            }
        }
    }

    private void parseGit(String moduleDir, Map<String, Require> requires, String key, Toml reqItem, Require req) {
        String gitOption = reqItem.getString(GIT);
        if (!Strings.isEmpty(gitOption)) {
            String gitPath = LspConfigUtils.getPathByLockFile(key, moduleDir, project);
            if (!Strings.isEmpty(gitPath)) {
                req.setPath(FileUtils.sanitizeURI(toAbsDirectory(gitPath, moduleDir).toString()));
                requires.put(key, req);
                if (this.uriSet.contains(gitPath)) {
                    return;
                }
                Toml moduleToml = null;
                Optional<Toml> optModuleToml = LspConfigUtils.getModuleCjpmToml(
                        project, new File(gitPath, CJPM_FILE), true);
                if (optModuleToml.isPresent()) {
                    moduleToml = optModuleToml.get();
                }
                parse(moduleToml, gitPath);
            }
        }
    }

    private void parsePkgReqs(PackageRequires packageRequires, Toml pkgReqsToml, String moduleDir) {
        List<String> pathOption = pkgReqsToml.getList(PATH_OPTION);
        String splitChar = LspConfigUtils.getSplit();
        if (pathOption != null) {
            pathOption.forEach(path -> {
                String relPath = getRealPath(path, moduleDir);
                requiresEnvPath.append(relPath);
                requiresEnvPath.append(splitChar);
                packageRequires.getPathOption()
                    .add(FileUtils.sanitizeURI(toAbsDirectory(relPath, moduleDir).toString()));
            });
        }

        Optional<Toml> packageOption = pkgReqsToml.getTable(PACKAGE_OPTION);
        packageOption.ifPresent(toml -> toml.toMap().forEach((key, path) -> {
            String relPath = getRealPath(path.toString(), moduleDir);
            requiresEnvPath.append(relPath);
            requiresEnvPath.append(splitChar);
            packageRequires.getPackageOption()
                .put(key, FileUtils.sanitizeURI(toAbsDirectory(relPath, moduleDir).toString()));
        }));
    }

    private boolean parseCombined(Toml toml, String packageName) {
        Optional<Toml> profiler = toml.getTable(PROFILE);
        if (profiler.isEmpty()) {
            return false;
        }
        Optional<Toml> build = profiler.get().getTable(BUILD);
        if (build.isEmpty()) {
            return false;
        }
        Optional<Toml> combined = build.get().getTable(COMBINED);
        if (combined.isEmpty()) {
            return false;
        }
        Map<String, Object> combinedMap = combined.get().toMap();
        return !combinedMap.isEmpty() && combinedMap.containsKey(packageName)
                && DYNAMIC.equals(combinedMap.get(packageName));
    }

    /**
     * init condition option to lsp
     *
     * @param toml target work module toml obj
     * @param tomlPath target work module toml path
     * @param project target project
     */
    public void initConditionCompileOption(Toml toml, Path tomlPath, Project project) {
        if (toml == null) {
            return;
        }
        Optional<Toml> pkgConfig = toml.getTable(PACKAGE);
        if (pkgConfig.isEmpty()) {
            return;
        }
        String cjModuleName = pkgConfig.get().getString(NAME);
        if (Strings.isEmpty(cjModuleName) || moduleConditionCompileOption.containsKey(cjModuleName)) {
            return;
        }
        Map<String, String> conditionMap = new HashMap<>();
        moduleConditionCompileOption.put(cjModuleName, conditionMap);
        // init condition by cjpm.toml (package.compile-option)
        String compileOption = pkgConfig.get().getString(COMPILE_OPTION);
        conditionMap.putAll(parseToCfg(compileOption));
        // init condition by ohos module
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return;
        }
        ModuleModel model = ProjectUtil.getModuleModelByCanonicalPath(projectModel, tomlPath.toString());
        if (!(model instanceof OhosModuleModel ohosModuleModel)) {
            return;
        }
        // ohos module target condition
        if (ModuleType.HAR.toString().equalsIgnoreCase(model.getModuleType())) {
            conditionMap.put(CONDITION_TARGET, "default");
        } else {
            String targetVal = "default";
            OhosTarget currentTarget = TargetManager.getInstance().getCurrentTarget(ohosModuleModel);
            if (currentTarget != null) {
                targetVal = currentTarget.getName();
            }
            conditionMap.put(CONDITION_TARGET, targetVal);
        }
        // ohos module build-profile.json5 (<build-profile.json5>arguments --> <cjpm.toml>profile.customized-option)
        Optional<JsonObject> buildOptionJson = getBuildOption(project, model);
        if (buildOptionJson.isEmpty()) {
            return;
        }
        JsonObject cangjieOptionsJson = PsiJsonFileUtil.getPsiJsonObject(buildOptionJson.get(), CANGJIE_OPTIONS);
        if (cangjieOptionsJson == null || cangjieOptionsJson.getPropertyList().isEmpty()) {
            return;
        }
        conditionMap.putAll(collectCustomCondition(toml, cangjieOptionsJson));
    }

    private Map<String, String> collectCustomCondition(Toml toml, JsonObject cangjieOptionsJson) {
        Map<String, String> conditions = new HashMap<>();
        if (toml == null || cangjieOptionsJson == null || cangjieOptionsJson.getPropertyList().isEmpty()) {
            return conditions;
        }
        // get custom arguments in build-profile.json5
        List<String> argumentsList = new ArrayList<>();
        JsonValue argsJsonValue = PsiJsonFileUtil.getPsiJsonValue(cangjieOptionsJson, ARGUMENTS);
        if (argsJsonValue instanceof JsonStringLiteral stringValue) {
            String arguments = ReadAction.compute(stringValue::getValue);
            argumentsList.addAll(
                    Stream.of(arguments.split("\\s+")).filter(element -> element.startsWith("--")).toList()
            );
        } else if (argsJsonValue instanceof JsonArray arrayValue) {
            List<String> satisfyArgs = ReadAction.compute(() ->
                arrayValue.getValueList().stream()
                        .filter(element -> element instanceof JsonStringLiteral)
                        .map(element -> ((JsonStringLiteral) element).getValue())
                        .filter(element -> element.startsWith("--"))
                        .toList());
            argumentsList.addAll(satisfyArgs);
        } else {
            return conditions;
        }
        if (argumentsList.isEmpty()) {
            return conditions;
        }
        // collect custom option in cjpm.toml
        Optional<Toml> profileConfig = toml.getTable(PROFILE);
        if (profileConfig.isEmpty()) {
            return conditions;
        }
        Optional<Toml> customOptions = profileConfig.get().getTable(CUSTOMIZED_OPTION);
        if (customOptions.isEmpty()) {
            return conditions;
        }
        Map<String, Object> customOptionsMap = customOptions.get().toMap();
        // collect real cfg condition
        for (String argument : argumentsList) {
            String customArgument = argument.substring(2);
            if (!(customOptionsMap.get(customArgument) instanceof String customOptionValue)) {
                continue;
            }
            conditions.putAll(parseToCfg(customOptionValue));
        }
        return conditions;
    }

    private Map<String, String> parseToCfg(String conditionString) {
        String tmpCondStr = conditionString;
        Matcher conditionStrmatcher = INTERPOLATED_STRING_PATTERN.matcher(tmpCondStr);
        StringBuffer sb = new StringBuffer();
        while (conditionStrmatcher.find()) {
            String math = conditionStrmatcher.group(1);
            String env = EnvUtils.BUILDER.environment().get(math);
            if (env == null) {
                env = "";
            }
            conditionStrmatcher.appendReplacement(sb, env.replace("\\", "/"));
        }
        conditionStrmatcher.appendTail(sb);
        tmpCondStr = sb.toString();
        Map<String, String> conditions = new HashMap<>();
        Matcher conditionMatcher = CFG_PATTERN.matcher(tmpCondStr);
        while (conditionMatcher.find()) {
            String content = conditionMatcher.group(1);
            String[] conditionDecls = content.split(",");
            for (String conditionDecl : conditionDecls) {
                String[] condition = conditionDecl.trim().split("=");
                if (condition.length < 2) {
                    continue;
                }
                conditions.put(condition[0].trim(), condition[1].trim());
            }
        }
        return conditions;
    }

    private static String toAbsDirectory(String dir, String workspace) {
        return LspProjectManager.toAbsDirectory(dir, workspace);
    }

    private static String getRealPath(String path, String workspace) {
        return LspProjectManager.getRealPath(path, workspace);
    }
}
