/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.testframework.utils;

import static com.huawei.cangjie.projectmgmt.utils.Constants.DEFAULT;
import static com.huawei.cangjie.projectmgmt.utils.Constants.SRC;
import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;
import static com.huawei.deveco.projectmodel.ohos.model.constants.RuntimeOS.HARMONY_OS;

import com.huawei.cangjie.projectmgmt.extend.toml.Toml;
import com.huawei.cangjie.projectmgmt.utils.CangjieEnvUtils;
import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieComponent;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieIdeaSdkInfoHandler;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;
import com.huawei.cangjie.testframework.configuration.CangjieTestRunConfiguration;
import com.huawei.cangjie.testframework.sync.CangjieTestConfigurationPersistentSetting;
import com.huawei.deveco.build.ohos.service.HvigorParamsBuilder;
import com.huawei.deveco.build.ohos.service.HvigorService;
import com.huawei.deveco.debugger.ohos.util.ProjectUtil;
import com.huawei.deveco.ohos.testframework.utils.CommonUtil;
import com.huawei.deveco.ohos.testframework.utils.HvigorTaskModel;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.TargetManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosTarget;
import com.huawei.deveco.projectmodel.ohos.util.ModuleType;
import com.huawei.deveco.sdkmanager.core.util.StringUtil;
import com.huawei.ideacj.language.CangjieFile;
import com.huawei.ideacj.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.ideacj.language.psi.othersnode.CjAtomicExpression;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.toplevel.CjTopLevelObject;
import com.huawei.ideacj.language.psi.toplevel.CjTypeParameters;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassBody;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.expressionnode.CjExpression;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjBlock;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclaration;
import com.huawei.ideacj.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclarations;
import com.huawei.ideacj.language.psi.toplevel.functionnode.blocknode.CjVarOrFuncDeclaration;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputExprWithoutParens;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TestUtil
 *
 * @since 2025/02/20
 */
public class TestUtil {
    private static final Logger LOG = Logger.getInstance(TestUtil.class);
    private static final String AT = "@";
    private static final String AT_TEST_CLASS = "Test";
    private static final String AT_TEST_FUNCTION = "TestCase";
    private static final List<String> TEST_MACRO = List.of(AT_TEST_CLASS, AT_TEST_FUNCTION);
    private static final Pattern REGISTER_GRAMMAR = Pattern.compile(
            "registerTestSuite\\s*\\{\\s*([^\\s,;()]+)\\s*\\(\\s*\\)\\s*}");

    /**
     * CANGJIE_OHOS_TEST_PROTOCOL_ID
     */
    private static final String CANGJIE_OHOS_TEST_PROTOCOL_ID = "CangjieOhosTest";

    /**
     * CANGJIE_LOCAL_TEST_PROTOCOL_ID
     */
    private static final String CANGJIE_LOCAL_TEST_PROTOCOL_ID = "CangjieLocalTest";

    /**
     * Cache: filePath -->  @Test
     */
    private static final Map<String, Set<PsiElement>> testClasses = new HashMap<>();

    /**
     * Cache: filePath  -->  @Test  -->  @TestCase
     */
    private static final Map<String, Map<PsiElement, Set<PsiElement>>> testCases = new HashMap<>();

    /**
     * Cache: LocationProtocol  -->  test element
     */
    private static final Map<String, PsiElement> testLocationMap = new HashMap<>();

    /**
     * check is support test run
     *
     * @param psiElement psiElement
     * @return is support test run
     */
    public static boolean isSupportTestRun(@NotNull PsiElement psiElement) {
        if (!TEST_MACRO.contains(psiElement.getText())) {
            return false;
        }
        PsiFile file = psiElement.getContainingFile();
        if (file == null || !file.getName().endsWith(Constant.CJ_TEST_FILE_SUFFIX)) {
            return false;
        }
        OhosModuleModel moduleModel = getSelectFileOhosModuleModel(psiElement.getProject(), file.getVirtualFile());
        if (!isSupportedModule(moduleModel)) {
            return false;
        }
        String ohosTestPath = Path.of(moduleModel.getModulePath(), Constant.SRC, Constant.OHOS_TEST,
                Constant.CANGJIE).toString();
        String localTestPath = Path.of(moduleModel.getModulePath(), Constant.SRC, Constant.TEST,
                Constant.CANGJIE).toString();
        if (!isSameOrSubPath(ohosTestPath, file.getVirtualFile().getCanonicalPath())
                && !isSameOrSubPath(localTestPath, file.getVirtualFile().getCanonicalPath())) {
            return false;
        }
        if (!(psiElement.getParent() instanceof CjIdentifier)) {
            return false;
        }
        PsiElement prevSibling = psiElement.getParent().getPrevSibling();
        if (prevSibling == null || !prevSibling.getText().equals("@")) {
            return false;
        }
        PsiElement macroExpression = psiElement.getParent().getParent();
        if (!(macroExpression instanceof CjMacroExpression)) {
            return false;
        }
        String filePath = file.getVirtualFile().getCanonicalPath();
        if (StringUtils.isNotEmpty(filePath)) {
            filePath = Path.of(filePath).toAbsolutePath().normalize().toString();
        }
        if (AT_TEST_FUNCTION.equals(psiElement.getText())) {
            Optional<PsiElement> testElement = isSupportTestCase(psiElement);
            if (testElement.isPresent()) {
                if (StringUtils.isEmpty(filePath)) {
                    // can not get filePath, but is supported test case
                    return true;
                }
                // add to cache
                if (!testCases.containsKey(filePath)) {
                    testCases.put(filePath, new HashMap<>());
                }
                Map<PsiElement, Set<PsiElement>> testToTestCase = testCases.get(filePath);
                if (!testToTestCase.containsKey(testElement.get())) {
                    testToTestCase.put(testElement.get(), new HashSet<>());
                }
                testToTestCase.get(testElement.get()).add(psiElement);
                initElementLocation(
                        psiElement,
                        getTestClassNameByTestClass(testElement.get()) + Constant.DOT + getTestCaseName(psiElement)
                );
                return true;
            }
            return false;
        }
        if (!isSupportTestClass(psiElement)) {
            return false;
        }
        if (StringUtils.isEmpty(filePath)) {
            // can not get filePath, but is supported test class
            return true;
        }
        // add to cache
        if (!testClasses.containsKey(filePath)) {
            testClasses.put(filePath, new HashSet<>());
        }
        testClasses.get(filePath).add(psiElement);
        initElementLocation(psiElement, getTestClassNameByTestClass(psiElement));
        return true;
    }

    private static void initElementLocation(PsiElement psiElement, String name) {
        PsiFile file = psiElement.getContainingFile();
        if (file == null) {
            return;
        }
        OhosModuleModel moduleModel = getSelectFileOhosModuleModel(psiElement.getProject(), file.getVirtualFile());
        String pkgeName = "";
        String protocolId = "";
        String filePath = psiElement.getContainingFile().getVirtualFile().getCanonicalPath();
        if (StringUtils.isEmpty(filePath)) {
            return;
        }
        if (filePath.contains("src/main/cangjie")) {
            pkgeName = TestUtil.getPackageName(moduleModel, psiElement.getContainingFile().getVirtualFile(), false);
            protocolId = CANGJIE_OHOS_TEST_PROTOCOL_ID;
        } else if (filePath.contains("src/ohosTest/cangjie")) {
            pkgeName = TestUtil.getPackageName(moduleModel, psiElement.getContainingFile().getVirtualFile(), true);
            protocolId = CANGJIE_OHOS_TEST_PROTOCOL_ID;
        } else if (filePath.contains("src/test/cangjie")) {
            pkgeName = LocalTestUtil.getLocalPackageName(moduleModel, psiElement.getContainingFile().getVirtualFile());
            protocolId = CANGJIE_LOCAL_TEST_PROTOCOL_ID;
        } else {
            return;
        }
        if (StringUtils.isEmpty(pkgeName) || StringUtils.isEmpty(protocolId)) {
            return;
        }
        String locationUrl = moduleModel.getModuleName() + Constant.URL_SPLITS + pkgeName + Constant.HASH_TAGS + name;
        testLocationMap.put(locationUrl, psiElement);
    }

    /**
     * check is support module
     *
     * @param moduleModel target module
     * @return is support module
     */
    public static boolean isSupportedModule(ModuleModel moduleModel) {
        if (!(moduleModel instanceof OhosModuleModel ohosModuleModel)) {
            return false;
        }
        if (!FileUtils.isCangjieModule(ohosModuleModel) || FileUtils.isContainEtsModule(ohosModuleModel)) {
            return false;
        }
        // just support pure hap or har
        return ModuleType.ENTRY.toString().equalsIgnoreCase(ohosModuleModel.getModuleType())
                || ModuleType.FEATURE.toString().equalsIgnoreCase(ohosModuleModel.getModuleType())
                || ohosModuleModel.isHarLibrary();
    }

    /**
     * get testing type
     *
     * @param psiElement psiElement
     * @return testing type
     */
    public static CangjieTestRunConfiguration.TestingType getTestingType(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiDirectory) {
            return CangjieTestRunConfiguration.TestingType.TEST_ALL_IN_PACKAGE;
        }
        if (psiElement instanceof CangjieFile) {
            return CangjieTestRunConfiguration.TestingType.TEST_FILE;
        }
        if (psiElement.getParent() == null) {
            return CangjieTestRunConfiguration.TestingType.UNDEFINED;
        }
        PsiElement macroExpression = psiElement.getParent().getParent();
        if (!(macroExpression instanceof CjMacroExpression) || macroExpression.getLastChild() == null) {
            return CangjieTestRunConfiguration.TestingType.UNDEFINED;
        }
        PsiElement testBody = macroExpression.getLastChild().getLastChild();
        if (testBody == null) {
            return CangjieTestRunConfiguration.TestingType.UNDEFINED;
        }
        if (testBody instanceof CjClassDefinition && AT_TEST_CLASS.equals(psiElement.getText())) {
            return CangjieTestRunConfiguration.TestingType.TEST_CLASS;
        }
        if (testBody instanceof CjFunctionDefinition && AT_TEST_FUNCTION.equals(psiElement.getText())) {
            return CangjieTestRunConfiguration.TestingType.TEST_METHOD;
        }
        return CangjieTestRunConfiguration.TestingType.UNDEFINED;
    }

    /**
     * get test class name by test class
     *
     * @param psiElement psiElement
     * @return test class name
     */
    public static String getTestClassNameByTestClass(@NotNull PsiElement psiElement) {
        if (!isSupportTestClass(psiElement)) {
            return StringUtil.EMPTY;
        }
        PsiElement classDefinition = getTestBodyElementByTestClassOrCase(psiElement).get();
        if (!(classDefinition instanceof CjClassDefinition cjClassDefinition)) {
            return StringUtil.EMPTY;
        }
        return cjClassDefinition.getName();
    }

    /**
     * get test class name by test case
     *
     * @param psiElement psiElement
     * @return test class name
     */
    public static String getTestClassNameByTestCase(@NotNull PsiElement psiElement) {
        if (isSupportTestCase(psiElement).isEmpty()) {
            return StringUtil.EMPTY;
        }
        PsiElement classDefinition = getTestClassElementByTestCase(psiElement);
        if (!(classDefinition instanceof CjClassDefinition cjClassDefinition)) {
            return StringUtil.EMPTY;
        }
        return cjClassDefinition.getName();
    }

    /**
     * get test case name
     *
     * @param psiElement psiElement
     * @return test case name
     */
    public static String getTestCaseName(@NotNull PsiElement psiElement) {
        if (isSupportTestCase(psiElement).isEmpty()) {
            return StringUtil.EMPTY;
        }
        PsiElement functionDefinition = getTestBodyElementByTestClassOrCase(psiElement).get();
        if (!(functionDefinition instanceof CjFunctionDefinition cjFunctionDefinition)) {
            return StringUtil.EMPTY;
        }
        String name = cjFunctionDefinition.getName();
        CjIdentifier identifier = PsiTreeUtil.getChildOfType(cjFunctionDefinition, CjIdentifier.class);
        if (identifier != null && identifier.getNextSibling() instanceof CjTypeParameters typeParameters) {
            name = name + typeParameters.getText();
        }
        return name;
    }

    /**
     * check is support @Test
     *
     * @param psiElement psiElement
     * @return is support @Test
     */
    public static boolean isSupportTestClass(@NotNull PsiElement psiElement) {
        if (!AT_TEST_CLASS.equals(psiElement.getText())) {
            return false;
        }
        if (!(psiElement.getParent() instanceof CjIdentifier)) {
            return false;
        }
        PsiElement prevSibling = psiElement.getParent().getPrevSibling();
        if (prevSibling == null || !prevSibling.getText().equals("@")) {
            return false;
        }
        PsiElement macroExpression = psiElement.getParent().getParent();
        if (!(macroExpression instanceof CjMacroExpression) || macroExpression.getLastChild() == null) {
            return false;
        }
        return macroExpression.getLastChild().getLastChild() instanceof CjClassDefinition;
    }

    /**
     * check is support @TestCase
     *
     * @param psiElement psiElement
     * @return is support testCase
     */
    public static Optional<PsiElement> isSupportTestCase(@NotNull PsiElement psiElement) {
        if (!AT_TEST_FUNCTION.equals(psiElement.getText())) {
            return Optional.empty();
        }
        if (!(psiElement.getParent() instanceof CjIdentifier)) {
            return Optional.empty();
        }
        PsiElement prevSibling = psiElement.getParent().getPrevSibling();
        if (prevSibling == null || !prevSibling.getText().equals("@")) {
            return Optional.empty();
        }
        PsiElement classDefinition = getTestClassElementByTestCase(psiElement);
        if (!(classDefinition instanceof CjClassDefinition) || classDefinition.getParent() == null) {
            return Optional.empty();
        }
        PsiElement macroExpression = classDefinition.getParent().getParent();
        if (!(macroExpression instanceof CjMacroExpression)) {
            return Optional.empty();
        }
        PsiElement atElement = macroExpression.getFirstChild();
        if (!AT.equals(atElement.getText())) {
            return Optional.empty();
        }
        CjIdentifier testElement = PsiTreeUtil.getChildOfType(macroExpression, CjIdentifier.class);
        if (testElement == null || !AT_TEST_CLASS.equals(testElement.getText())) {
            return Optional.empty();
        }
        return Optional.of(testElement.getFirstChild());
    }

    /**
     * get test body by @Test or @TestCase
     *
     * @param psiElement @Test or @TestCase
     * @return test body(class or function)
     */
    public static Optional<PsiElement> getTestBodyElementByTestClassOrCase(@NotNull PsiElement psiElement) {
        PsiElement macroExpression = psiElement.getParent().getParent();
        if (!(macroExpression instanceof CjMacroExpression) || macroExpression.getLastChild() == null) {
            return Optional.empty();
        }
        return Optional.of(macroExpression.getLastChild().getLastChild());
    }

    /**
     * get @Test class element by @TestCase
     *
     * @param psiElement psiElement
     * @return @Test class
     */
    public static PsiElement getTestClassElementByTestCase(@NotNull PsiElement psiElement) {
        int toTestClassDeep = 12;
        PsiElement classDefinition = psiElement;
        for (int i = 0; i < toTestClassDeep; i++) {
            classDefinition = classDefinition.getParent();
            if (classDefinition == null) {
                break;
            }
        }
        return classDefinition;
    }

    /**
     * save module info
     *
     * @param configuration configuration
     */
    public static void saveModuleInfo(CangjieTestRunConfiguration configuration) {
        String name = configuration.getName();
        if (configuration.getModule() != null) {
            String moduleName = configuration.getModule().getModuleName();
            configuration.setModuleName(moduleName);
            if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(moduleName)) {
                CangjieTestConfigurationPersistentSetting.getSetting(configuration.getProject())
                        .getModuleModelConfigurationState().put(name, moduleName);
            }
        }
    }

    /**
     * get all valid modules for the project
     *
     * @param projectModel ProjectModel
     * @return the accepted modules
     */
    public static List<OhosModuleModel> getAcceptedModules(ProjectModel projectModel) {
        final List<OhosModuleModel> list = new ArrayList<>();
        if (projectModel == null) {
            return list;
        }
        List<ModuleModel> moduleModelList = projectModel.getModuleModelList();
        for (final ModuleModel module : moduleModelList) {
            if (!(module instanceof OhosModuleModel ohosModuleModel) || !FileUtils.isCangjieModule(module)
                    || FileUtils.isContainEtsModule(ohosModuleModel)) {
                continue;
            }
            list.add(ohosModuleModel);
        }
        return list;
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
     * get cangjie sdk path
     *
     * @param projectModel the target projectModel
     * @return cangjie sdk path
     */
    public static String getSdkPath(ProjectModel projectModel) {
        String apiVersion = projectModel.getFullCompileSdkVersion().getValue();
        boolean isHarmony = projectModel.getActiveRuntimeOS() == HARMONY_OS;
        Map<String, CangjieComponent> localSdks = new CangjieIdeaSdkInfoHandler().getLocalSdks(isHarmony, apiVersion);
        CangjieComponent cangjieComponent = localSdks.get(CangjieComponentPath.CANGJIE.value());
        if (cangjieComponent == null) {
            return StringUtil.EMPTY;
        }
        Path cangjieSdkPath = cangjieComponent.getLocation();
        if (cangjieSdkPath == null) {
            return StringUtil.EMPTY;
        }
        return cangjieSdkPath.toString();
    }

    /**
     * get class names by file
     *
     * @param project project
     * @param filePath filePath
     * @param className className
     * @return class names
     */
    public static Set<String> getClassNamesByFile(Project project, String filePath, String className) {
        return ApplicationManager.getApplication().runReadAction((Computable<Set<String>>) () -> {
            String realPath = Path.of(filePath).toAbsolutePath().normalize().toString();
            Set<String> classOrMethodNames = getClassOrMethodNamesByCache(realPath, className);
            if (!classOrMethodNames.isEmpty()) {
                return classOrMethodNames;
            }
            // cache can not find, result is empty, walk target file
            List<CjTopLevelObject> topLevelObjects = isContainValidPsiElement(realPath, project);
            if (topLevelObjects.isEmpty()) {
                return classOrMethodNames;
            }
            for (CjTopLevelObject topLevelObject : topLevelObjects) {
                if (!(topLevelObject.getFirstChild() instanceof CjExpression)) {
                    continue;
                }
                topLevelObject.accept(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(@NotNull PsiElement element) {
                        if (!(element instanceof CjMacroExpression macroExpression)) {
                            super.visitElement(element);
                            return;
                        }
                        PsiElement atElement = macroExpression.getFirstChild();
                        if (!AT.equals(atElement.getText())) {
                            super.visitElement(element);
                            return;
                        }
                        CjIdentifier testElement = PsiTreeUtil.getChildOfType(macroExpression, CjIdentifier.class);
                        if (testElement == null || !AT_TEST_CLASS.equals(testElement.getText())) {
                            super.visitElement(element);
                            return;
                        }
                        if (!(macroExpression.getLastChild() instanceof CjMacroInputExprWithoutParens macroInputExpr)) {
                            super.visitElement(element);
                            return;
                        }
                        if (!(macroInputExpr.getFirstChild() instanceof CjClassDefinition classDefinition)) {
                            super.visitElement(element);
                            return;
                        }
                        CjIdentifier cjIdentifier = PsiTreeUtil.getChildOfType(classDefinition, CjIdentifier.class);
                        if (cjIdentifier == null) {
                            super.visitElement(element);
                            return;
                        }
                        // add to set
                        if (StringUtils.isEmpty(className)) {
                            classOrMethodNames.add(cjIdentifier.getText());
                        }
                        // add to cache
                        if (!testClasses.containsKey(realPath)) {
                            testClasses.put(realPath, new HashSet<>());
                        }
                        testClasses.get(realPath).add(testElement.getFirstChild());
                        initElementLocation(
                                testElement.getFirstChild(), getTestClassNameByTestClass(testElement.getFirstChild()));
                        // refresh test case cache
                        classOrMethodNames.addAll(refreshTestCaseCacheByAccept(realPath, classDefinition));
                    }
                });
            }
            return classOrMethodNames;
        });
    }

    /**
     * refresh TestCase cache by psi accept
     *
     * @param filePath filePath
     * @param classDefinition classDefinition
     * @return TestCase method name
     */
    public static Set<String> refreshTestCaseCacheByAccept(String filePath, CjClassDefinition classDefinition) {
        Set<String> methodNames = new HashSet<>();
        CjClassBody cjClassBody = PsiTreeUtil.getChildOfType(classDefinition, CjClassBody.class);
        if (cjClassBody == null) {
            return methodNames;
        }
        List<CjClassMemberDeclaration> classMemberDeclarations = PsiTreeUtil.getChildrenOfTypeAsList(
                cjClassBody, CjClassMemberDeclaration.class);
        if (classMemberDeclarations.isEmpty()) {
            return methodNames;
        }
        for (CjClassMemberDeclaration classMemberDeclaration : classMemberDeclarations) {
            PsiElement expression = classMemberDeclaration.getFirstChild();
            if (!(expression instanceof CjExpression)) {
                continue;
            }
            PsiElement postfixExpression = expression.getFirstChild();
            if (!(postfixExpression instanceof CjPostfixExpression)) {
                continue;
            }
            PsiElement atomExpression = postfixExpression.getFirstChild();
            if (!(atomExpression instanceof CjAtomicExpression)) {
                continue;
            }
            if (!(atomExpression.getFirstChild() instanceof CjMacroExpression macroExpression)) {
                continue;
            }
            PsiElement atElement = macroExpression.getFirstChild();
            if (!AT.equals(atElement.getText())) {
                continue;
            }
            CjIdentifier testCaseElement = PsiTreeUtil.getChildOfType(macroExpression, CjIdentifier.class);
            if (testCaseElement == null || !AT_TEST_FUNCTION.equals(testCaseElement.getText())) {
                continue;
            }
            if (!(macroExpression.getLastChild() instanceof CjMacroInputExprWithoutParens macroInputExpr)) {
                continue;
            }
            if (!(macroInputExpr.getFirstChild() instanceof CjFunctionDefinition functionDefinition)) {
                continue;
            }
            CjIdentifier cjIdentifier = PsiTreeUtil.getChildOfType(functionDefinition, CjIdentifier.class);
            if (cjIdentifier == null) {
                continue;
            }
            // add to set
            methodNames.add(cjIdentifier.getText());
            // add to cache
            PsiElement testElement = getTestClassElementByTestCase(testCaseElement);
            if (testElement == null) {
                continue;
            }
            if (!testCases.containsKey(filePath)) {
                testCases.put(filePath, new HashMap<>());
            }
            Map<PsiElement, Set<PsiElement>> testToTestCase = testCases.get(filePath);
            if (!testToTestCase.containsKey(testElement)) {
                testToTestCase.put(testElement, new HashSet<>());
            }
            testToTestCase.get(testElement).add(testCaseElement.getFirstChild());
            initElementLocation(
                    testCaseElement.getFirstChild(),
                    getTestClassNameByTestClass(testElement)
                            + Constant.DOT + getTestCaseName(testCaseElement.getFirstChild())
            );
        }
        return methodNames;
    }

    /**
     * get class or method names by cache
     *
     * @param filePath filePath
     * @param targetClassName targetClassName
     * @return class or method names
     */
    @NotNull
    public static Set<String> getClassOrMethodNamesByCache(String filePath, String targetClassName) {
        Set<String> classNames = new HashSet<>();
        String realPath = Path.of(filePath).toAbsolutePath().normalize().toString();
        PsiElement targetTestElement = null;
        // research @Test cache
        targetTestElement = researchTestCache(targetClassName, realPath, classNames);
        if (StringUtils.isEmpty(targetClassName)) {
            return classNames;
        }
        Set<String> methodNames = new HashSet<>();
        // research @TestCase cache
        if (!testCases.containsKey(realPath)) {
            return methodNames;
        }
        Map<PsiElement, Set<PsiElement>> testToTestCase = testCases.get(realPath);
        if (testToTestCase == null || testToTestCase.isEmpty()) {
            return methodNames;
        }
        Set<PsiElement> testCaseSet = null;
        // research parent @Test
        Iterator<Map.Entry<PsiElement, Set<PsiElement>>> testIterator = testToTestCase.entrySet().iterator();
        while (testIterator.hasNext()) {
            Map.Entry<PsiElement, Set<PsiElement>> entry = testIterator.next();
            PsiElement testElement = entry.getKey();
            if (!testElement.isValid()) {
                // remove old @Test
                testIterator.remove();
            }
            if (!testElement.equals(targetTestElement)) {
                continue;
            }
            testCaseSet = entry.getValue();
        }
        if (testCaseSet == null || testCaseSet.isEmpty()) {
            return methodNames;
        }
        // research method name
        Iterator<PsiElement> testCaseIterator = testCaseSet.iterator();
        while (testCaseIterator.hasNext()) {
            PsiElement psiElement = testCaseIterator.next();
            if (!psiElement.isValid()) {
                // remove old test case element
                testCaseIterator.remove();
                continue;
            }
            methodNames.add(getTestCaseName(psiElement));
        }
        return methodNames;
    }

    private static PsiElement researchTestCache(String targetClassName, String realPath,
                                                Set<String> classNames) {
        PsiElement targetTestElement = null;
        if (testClasses.containsKey(realPath) && !testClasses.get(realPath).isEmpty()) {
            Iterator<PsiElement> iterator = testClasses.get(realPath).iterator();
            while (iterator.hasNext()) {
                PsiElement psiElement = iterator.next();
                if (!psiElement.isValid()) {
                    // remove old test class element
                    iterator.remove();
                    // remove method under invalid class element
                    Map<PsiElement, Set<PsiElement>> testToTestCase = testCases.get(realPath);
                    if (testToTestCase == null || testToTestCase.isEmpty()) {
                        continue;
                    }
                    if (testToTestCase.get(psiElement) != null) {
                        testToTestCase.remove(psiElement);
                    }
                    continue;
                }
                String className = getTestClassNameByTestClass(psiElement);
                classNames.add(className);
                if (!StringUtils.isEmpty(targetClassName) && targetClassName.equals(className)) {
                    targetTestElement = psiElement;
                }
            }
        }
        return targetTestElement;
    }

    /**
     * check is contain valid psi element
     *
     * @param filePath filePath
     * @param project project
     * @return CjTopLevelObject List
     */
    public static List<CjTopLevelObject> isContainValidPsiElement(String filePath, Project project) {
        List<CjTopLevelObject> topLevelObjects = new ArrayList<>();
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile == null) {
            return topLevelObjects;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        CjTranslationUnit cjTranslationUnit = PsiTreeUtil.getChildOfType(psiFile, CjTranslationUnit.class);
        if (cjTranslationUnit == null) {
            return topLevelObjects;
        }
        topLevelObjects = PsiTreeUtil.getChildrenOfTypeAsList(
                cjTranslationUnit, CjTopLevelObject.class);
        return topLevelObjects;
    }

    /**
     * get register class names in package
     *
     * @param project project
     * @param filePath filePath
     * @return register class names
     */
    public static List<String> getRegisterClassNamesInPackage(Project project, String filePath) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<String>>) () -> {
            List<String> registerClassNames = new ArrayList<>();
            Path path = Path.of(filePath).toAbsolutePath().normalize();
            String pkgPath = path.toString();
            if (!path.toFile().isDirectory()) {
                pkgPath = Path.of(path.toFile().getParent()).toAbsolutePath().normalize().toString();
            }
            Path testListPath = Path.of(pkgPath, Constant.CJ_TEST_LIST_FILE_NAME).toAbsolutePath().normalize();
            if (!testListPath.toFile().exists()) {
                return registerClassNames;
            }

            List<CjTopLevelObject> topLevelObjects = isContainValidPsiElement(testListPath.toString(), project);
            if (topLevelObjects.isEmpty()) {
                return registerClassNames;
            }
            List<CjExpressionOrDeclaration> registerElementList = new ArrayList<>();
            // collect register element
            for (CjTopLevelObject topLevelObject : topLevelObjects) {
                PsiElement varOrFuncDeclaration = topLevelObject.getFirstChild();
                if (!(varOrFuncDeclaration instanceof CjVarOrFuncDeclaration)) {
                    continue;
                }
                PsiElement funcDeclaration = varOrFuncDeclaration.getFirstChild();
                if (!(funcDeclaration instanceof CjFunctionDefinition)) {
                    continue;
                }
                CjBlock block = PsiTreeUtil.getChildOfType(funcDeclaration, CjBlock.class);
                if (block == null) {
                    continue;
                }
                CjExpressionOrDeclarations expressionOrDeclarations =
                        PsiTreeUtil.getChildOfType(block, CjExpressionOrDeclarations.class);
                if (expressionOrDeclarations == null) {
                    continue;
                }
                List<CjExpressionOrDeclaration> expressionList =
                        PsiTreeUtil.getChildrenOfTypeAsList(expressionOrDeclarations, CjExpressionOrDeclaration.class);
                if (expressionList.isEmpty()) {
                    continue;
                }
                registerElementList.addAll(expressionList);
            }

            // find real register
            for (CjExpressionOrDeclaration expression : registerElementList) {
                if (expression == null) {
                    continue;
                }
                String registerText = expression.getText();
                Matcher matcher = REGISTER_GRAMMAR.matcher(registerText);
                if (!matcher.find()) {
                    continue;
                }
                // invalid identifier could be ignored, because it will build failed.
                registerClassNames.add(matcher.group(1));
            }
            return registerClassNames;
        });
    }

    /**
     * check has cj files in target dir
     *
     * @param dir dir
     * @return has cj files
     */
    public static boolean hasCjFiles(VirtualFile dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        VirtualFile[] files = dir.getChildren();
        for (VirtualFile file : files) {
            if (file.getName().endsWith(Constant.CJ_FILE_SUFFIX)) {
                return true;
            }
        }
        return false;
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
        if (moduleModel == null) {
            return DEFAULT;
        }
        String cangjieModuleName = FileUtils.getCangjieModuleName(moduleModel, isOhosTest);
        String defaultPkgName = StringUtil.isEmpty(cangjieModuleName) ? DEFAULT : cangjieModuleName;
        if (virtualFile == null || (!FileUtils.isInCangjieCodeDir(moduleModel, virtualFile, isOhosTest)
                && !isOhosTest)) {
            return defaultPkgName;
        }
        if (isOhosTest) {
            defaultPkgName = defaultPkgName + "_test";
        }
        String srcDir = FileUtils.getCangjieModuleSrcDir(moduleModel, isOhosTest);
        String cangjieModuleRootPath = "";
        try {
            String cjpmDirPath = moduleModel.getModulePath();
            if (!Path.of(cjpmDirPath, "cjpm.toml").toFile().exists()) {
                cjpmDirPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie").toString();
            }
            cangjieModuleRootPath = Path.of(cjpmDirPath, srcDir)
                    .normalize().toString().replaceAll("\\\\", "/");
            if (isOhosTest) {
                cangjieModuleRootPath = Path.of(moduleModel.getModulePath(), "src", "ohosTest", "cangjie", srcDir)
                        .normalize().toString().replaceAll("\\\\", "/");
            }
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
        for (String name : foldersName) {
            if (StringUtil.isEmpty(name)) {
                continue;
            }
            packageName.append(".");
            packageName.append(name);
        }
        return packageName.toString();
    }

    /**
     * execute cj cmd
     *
     * @param executePath executePath
     * @param targetCmd targetCmd
     * @param model model
     */
    public static void executeCjCmd(Path executePath, String targetCmd, ModuleModel model) {
        if (model == null) {
            return;
        }
        ProjectModel projectModel = model.getProjectModel();
        if (projectModel == null) {
            LOG.warn("projectModel is null");
            return;
        }
        String sdkPath = TestUtil.getSdkPath(projectModel);
        ProcessBuilder processBuilder;
        if (SystemInfo.isWindows) {
            String batPath = Path.of(sdkPath, Constant.BUILD_TOOLS, Constant.ENV_SCRIPT_WIN).toString();
            processBuilder = new ProcessBuilder(
                    Constant.WIN_BAT, Constant.WIN_BAT_OPTION, "\"" + batPath + "\"" + targetCmd);
        } else {
            String batPath = Path.of(sdkPath, Constant.BUILD_TOOLS, Constant.ENV_SCRIPT_MAC).toString();
            processBuilder = new ProcessBuilder(Constant.MAC_BASH, Constant.MAC_BASH_OPTION,
                    "source " + "\"" + batPath + "\"" + targetCmd);
        }

        processBuilder.directory(executePath.toFile());
        processBuilder.environment().putAll(CangjieEnvUtils.getProjectEnvs(projectModel.getProject()));
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            LOG.info("cjcov execute failed.");
            return;
        }

        // check is execute success
        try (InputStream inputStream = process.getErrorStream(); BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            if (!stringBuffer.isEmpty()) {
                LOG.warn("cjcov execute failed: " + stringBuffer);
            }
            process.destroy();
        } catch (IOException e) {
            LOG.warn("cjcov execute failed.");
        }
    }

    /**
     * deleteOldResults
     *
     * @param module module
     */
    public static void deleteOldResults(ModuleModel module) {
        if (module == null) {
            LOG.warn("deleteOldResults failed! module is null!");
            return;
        }
        String modulePath = module.getModulePath();
        Path outputReportPath = Path.of(modulePath, Constant.TEST_OUTPUT_DIR, Constant.TEST_REPORTS_OUTPUT_DIR);
        if (!outputReportPath.toFile().exists()) {
            return;
        }
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(outputReportPath.toFile());
        } catch (IOException exception) {
            LOG.warn("Failed to delete old results directory.");
        }
    }

    /**
     * run ohosTest build hap
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is run success
     */
    public static boolean assembleHapForOhosTest(@NotNull CangjieTestRunConfiguration runConfiguration,
                                                 @NotNull HvigorTaskModel hvigorTaskModel) {
        boolean hasHspModule = hvigorTaskModel
                .getModule().getProjectModel().getModuleModelList().stream().anyMatch(CommonUtil::isHspModule);
        HvigorParamsBuilder builderForTest =
                getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                        .withTaskName("assembleHap").withProduct(hvigorTaskModel.getProduct());
        if (hasHspModule) {
            builderForTest =
                    getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                            .withTaskName("assembleHap assembleHsp").withProduct(hvigorTaskModel.getProduct());
        }

        return HvigorService.executeInternalHvigorTask(
                hvigorTaskModel.getProject(),
                builderForTest.build(), builderForTest.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * run ohosTest build hap
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is run success
     */
    public static boolean assembleHapCoverage(@NotNull CangjieTestRunConfiguration runConfiguration,
                                              @NotNull HvigorTaskModel hvigorTaskModel) {
        boolean hasHspModule =
                hvigorTaskModel
                        .getModule().getProjectModel().getModuleModelList().stream().anyMatch(CommonUtil::isHspModule);
        HvigorParamsBuilder builderForCoverage =
                getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                        .withTaskName("assembleHap").withProduct(hvigorTaskModel.getProduct()).withCoverage();
        if (hasHspModule) {
            builderForCoverage =
                    getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                            .withTaskName("assembleHap assembleHsp")
                            .withProduct(hvigorTaskModel.getProduct()).withCoverage();
        }

        return HvigorService
                .executeInternalHvigorTask(
                        hvigorTaskModel.getProject(),
                        builderForCoverage.build(), builderForCoverage.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * run ohosTest build har
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is run success
     */
    public static boolean assembleOhosTestCoverageForHar(@NotNull CangjieTestRunConfiguration runConfiguration,
                                                         @NotNull HvigorTaskModel hvigorTaskModel) {
        hvigorTaskModel.setTargets(new String[]{"ohosTest"});
        HvigorParamsBuilder builder =
                getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                        .withTaskName("genOnDeviceTestHap").withCoverage();
        return HvigorService
                .executeInternalHvigorTask(
                        hvigorTaskModel.getProject(),
                        builder.build(), builder.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * run ohosTest build har
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return is run success
     */
    public static boolean assembleOhosTestForHar(@NotNull CangjieTestRunConfiguration runConfiguration,
                                                 @NotNull HvigorTaskModel hvigorTaskModel) {
        hvigorTaskModel.setTargets(new String[]{"ohosTest"});
        HvigorParamsBuilder builder =
                getHvigorParamsBuilder(runConfiguration, hvigorTaskModel)
                        .withTaskName("genOnDeviceTestHap");
        return HvigorService
                .executeInternalHvigorTask(
                        hvigorTaskModel.getProject(),
                        builder.build(), builder.getTaskName(), hvigorTaskModel.getEnv());
    }

    /**
     * getHvigorParamsBuilder
     *
     * @param runConfiguration runConfiguration
     * @param hvigorTaskModel hvigorTaskModel
     * @return hvigorParamsBuilder
     */
    public static HvigorParamsBuilder getHvigorParamsBuilder(@NotNull CangjieTestRunConfiguration runConfiguration,
                                                              @NotNull HvigorTaskModel hvigorTaskModel) {
        HvigorParamsBuilder hvigorParamsBuilder = HvigorParamsBuilder.newBuilder();
        OhosModuleModel module = hvigorTaskModel.getModule();
        setModuleNameAndTargets(hvigorTaskModel, module, hvigorParamsBuilder, runConfiguration.getTestPathType());
        if (runConfiguration.getTestPathType() == CangjieTestRunConfiguration.TestPathType.LOCAL_TEST_PATH) {
            return hvigorParamsBuilder.withModuleMode().withBuildMode("test").withAsan(hvigorTaskModel.isAsan());
        }
        return hvigorParamsBuilder.withModuleMode()
                .withBuildMode("test").withParam("isOhosTest", "true").withParam("isCangjie", "true")
                .withProduct(hvigorTaskModel.getProduct()).withAsan(hvigorTaskModel.isAsan());
    }

    private static void setModuleNameAndTargets(@NotNull HvigorTaskModel hvigorTaskModel,
                                                OhosModuleModel module,
                                                HvigorParamsBuilder hvigorParamsBuilder,
                                                CangjieTestRunConfiguration.TestPathType testPathType) {
        String[] targets = hvigorTaskModel.getTargets();
        boolean isOhosTestTarget = Arrays.equals(targets, new String[]{"ohosTest"});
        String moduleName = module.getModuleName();
        if (isOhosTestTarget || testPathType == CangjieTestRunConfiguration.TestPathType.LOCAL_TEST_PATH) {
            hvigorParamsBuilder.withModuleNameAndTargets(moduleName, targets);
        } else {
            if (hvigorTaskModel.isAutoDependencies()) {
                List<OhosModuleModel> moduleList = getSelectMultiModules(module);

                for (OhosModuleModel moduleModel : moduleList) {
                    OhosTarget currentTarget = TargetManager.getInstance().getCurrentTarget(moduleModel);
                    hvigorParamsBuilder.withModuleNameAndTargets(moduleModel.getModuleName(),
                            currentTarget == null ? "default" : currentTarget.getName());
                }
            }

            if (!CommonUtil.isHarModule(module)) {
                hvigorParamsBuilder.withModuleNameAndTargets(moduleName, targets);
            }
        }
    }

    private static List<OhosModuleModel> getSelectMultiModules(OhosModuleModel moduleModel) {
        return ProjectUtil.collectNonHarDependentModuleList(moduleModel, false);
    }

    /**
     * getModuleCjpmToml
     *
     * @param tomlFile tomlFile
     * @return ModuleCjpmToml
     */
    public static Optional<Toml> getModuleCjpmToml(File tomlFile) {
        if (!tomlFile.exists()) {
            return Optional.empty();
        }
        Optional<Toml> optCjpmObj;
        try {
            optCjpmObj = new Toml().read(tomlFile);
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
        return optCjpmObj;
    }

    /**
     * copyFile
     *
     * @param sourceDir sourceDir
     * @param targetDir targetDir
     * @param suffix suffix
     * @param isRemove isRemove
     * @throws IOException IOException
     */
    public static void copyFile(Path sourceDir, Path targetDir, String suffix, boolean isRemove) throws IOException {
        if (!targetDir.toFile().exists()) {
            Files.createDirectories(targetDir);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry) || !entry.toString().endsWith(suffix)) {
                    continue;
                }
                Path targetFile = targetDir.resolve(entry.getFileName());
                // copy
                Files.copy(entry, targetFile, StandardCopyOption.REPLACE_EXISTING);
                // delete
                if (isRemove) {
                    Files.delete(entry);
                }
            }
        }
    }

    /**
     * getTestLocation
     *
     * @param protocol protocol
     * @return element
     */
    public static PsiElement getTestLocation(String protocol) {
        return testLocationMap.get(protocol);
    }

    /**
     * check is contain dangerous character
     *
     * @param input input
     * @return is contain dangerous character
     */
    public static boolean containsDangerous(@NotNull String input) {
        return input.contains("&&")
                || input.contains("&")
                || input.contains(",")
                || input.contains(";")
                || input.contains("|")
                || input.contains("$")
                || input.contains("$(")
                || input.contains(")")
                || input.contains("`")
                || input.contains(">")
                || input.contains("<")
                || input.contains("'")
                || input.contains("\"")
                || input.contains("{")
                || input.contains("}");
    }
}
