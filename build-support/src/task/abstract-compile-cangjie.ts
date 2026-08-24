/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import path from 'path';
import type {TargetTaskService} from '../../types/hvigor-imports';
import {
  BuildDirConst,
  buildOptionManager,
  DefaultTargetConst,
  Dependency,
  FileUtilOhos as FileUtil,
  hvigorCore,
  iconv,
  InjectUtil,
  isMac,
  isWindows,
  OhosLogger,
  type TaskDetails,
  TaskNames
} from '../../types/hvigor-imports';
import {spawnSync, type SpawnSyncOptions} from 'child_process';
import {BaseCangjieTask} from './base-cangjie-task';
import {checkIsValid, getDemandSrcPackages, getSeamlessPath, normalizeCjpmTomlPath, retry} from '../utils/common-utils';
import {
  areChildPath,
  cangjieSrcRelatePath,
  clearDir,
  copyLibsCommon,
  copySpecifiedFiles,
  createDir,
  hasArktsCangjieModule,
  hasEtsModule,
  linkFile,
  readAsanEnabled,
  readModuleConfigTomlName,
  readPackageName,
  readPackageNameDirect,
  readTomlContent,
  readTomlSrcDir,
  syscapApiConfigPath
} from '../utils/cangjie-file-util';
import {CangjieTaskNames} from './cangjie-task-names';
import {CangjieDependency} from '../module/cangjie-dependency';
import {ProcessUtil} from '../utils/process-util';
import fs from 'fs';
import os from 'os';
import {DependencyManager} from '../module/dependency-manager';
import {CangjiePathImpl} from '../common/cangjie-path-impl';
import {AbiEnum, getTargetByAbi} from '../enums/cangjie-cpu-abi-enum';
import {getModuleType} from '../enums/module-types-enum';
import {CjBuildDirConst} from '../constants/cangjie-build-dir-const';
import {CangjieLogger} from '../log/cangjie-logger';
import {
  ASAN_RUNTIME_DEPEND_LIBS,
  BIN_DEPEND_LIBS_MAP,
  CJPM_TEST_EXECUTABLE_FILES_PREFIX,
  CJPM_TEST_SUCCESS_FLAG,
  COMPATIBILITY_RUNTIME_LIBS_50,
  DEFAULT_COPY_RUNTIME_LIBS_50,
  DEMAND_BIN_PACKAGES,
  LOADER_SO_LIBS,
  STD_DEMAND_ALL_LIBS,
  STD_DEPEND_LIBS_MAP_50,
  STD_REQUIRES,
  TPC_DEPEND_LIBS
} from '../constants/constants';
import {CangjieHapDependency} from '../module/cangjie-hap-dependency';
import {CompileAppTypeEnum} from '../enums/compile-app-type-enum';
import {CjpmCommandBuilder} from '../utils/cjpm-command-builder';
import {configOhModulesEnv} from '../utils/env-util';
import {getCangjieOptimizationConfig, getDefaultProfdataFile, getProjectOption} from '../utils/read-config';
import {getHomedir} from '../utils/homedir';

/**
 * compile cangjie task
 *
 * @since 2024/1/6
 */
export class AbstractCompileCangjie extends BaseCangjieTask {
  private accLogger: OhosLogger = CangjieLogger.getLogger(AbstractCompileCangjie.name);
  private readonly srcFilterHarPaths: string[] = [];

  private readonly binFilterHarPaths: string[] = [];

  private compileAppType = CompileAppTypeEnum.NORMAL_APP;

  private asanEnabled = false;

  private diffSoMap: Map<string, string[]> = new Map();
  private mockSoMap: Map<string, string[]> = new Map();

  constructor(taskService: TargetTaskService, taskDetails: TaskDetails) {
    super(taskService, taskDetails);
  }

  initTaskDepends(): void {
    this.declareDepends(CangjieTaskNames.GENERATE_CJ_RESOURCE.name, TaskNames.Task.COMPILE_RESOURCE.name);
  }

  protected async doTaskAction(): Promise<void> {
    this.checkDependencies();
    this.initCangjieSdk();
    await this.executeCangjieCompile(false);
  }

  protected checkDependencies(): void {
    const strictCheckDependencies = this.cangjieOption?.strictCheckDependencies;
    if (strictCheckDependencies === undefined || strictCheckDependencies === true) {
      new DependencyManager(false, this.moduleModel, this.projectModel).checkModelDependencyInfo();
    }
  }

  protected getCjpmProcessEnv(): NodeJS.ProcessEnv {
    if (this.sdkCangjieComponent === undefined) {
      return {};
    }
    const gitPath = this.getGitPath();
    const cmdPath = isWindows() ? 'C:\\Windows;C:\\Windows\\system32' : '/bin/:/usr/bin/';
    const pathArr = [this.sdkCangjieComponent.runtimeLibPath, this.sdkCangjieComponent.cjcPath,
      this.sdkCangjieComponent.toolsBin, this.sdkCangjieComponent.toolsLib, cmdPath, gitPath];
    const env: NodeJS.ProcessEnv = {};
    if (isWindows()) {
      env.Path = pathArr.join(path.delimiter);
    } else {
      env.PATH = pathArr.join(path.delimiter);
      const libraryPaths = [this.sdkCangjieComponent.runtimeLibPath, this.sdkCangjieComponent.toolsLib].join(
        path.delimiter);
      env.DYLD_LIBRARY_PATH = libraryPaths;
      env.DYLD_FALLBACK_LIBRARY_PATH = libraryPaths;
      env.SDKROOT = this.getMacSdkRoot();
    }
    env.bundleName = this.projectModel.getDefaultBundleName();
    env.moduleName = this.moduleModel.getName();
    env.moduleType = getModuleType(this.moduleModel);
    env.idDefined = path.resolve(this.pathInfo.getIntermediatesRes(), BuildDirConst.IDS_MAP, 'id_defined.json');
    env.ohosSysIdDefined = path.resolve(this.sdkInfo.getSdkToolchainsDir(), 'id_defined.json');
    if (this.sdkInfo.isOhos) {
      env.hosSysIdDefined = path.resolve(this.sdkInfo.getSdkToolchainsDir(), 'id_defined.json');
    } else {
      env.hosSysIdDefined = path.resolve(this.sdkInfo.getHosToolchainsDir(), 'id_defined.json');
    }
    env.rawfileRes =
      path.resolve(this.pathInfo.getIntermediatesRes(), BuildDirConst.RESTOOL_BUILD_RESOURCES, BuildDirConst.RAW_FILE);
    env.DEVECO_OH_NATIVE_HOME = this.sdkInfo.getSdkNativeDir();
    const tomlContent = readTomlContent(this.cjpmTomlPath, this.accLogger);
    env.CJ_BUILD_INTEROP_PATH = `${path.join(this.cjSource, readTomlSrcDir(tomlContent))}/`;
    env.CJ_LSP_INTEROP_PATH = this.getAllModulesSrcPath().join(';');
    env.CURRENT_PACKAGE_NAME = readPackageNameDirect(tomlContent);
    const projectDir = this.projectModel.getProjectDir();
    const apiJsonPath = path.join(projectDir, syscapApiConfigPath());
    const apiLevel = this.targetData.getApiMeta().compatibleSdkVersion.fullVersion;
    let compileConditions = `APILevel_level=${apiLevel},product=${this.targetData.getProduct().name}`;
    if (fs.existsSync(apiJsonPath)) {
      compileConditions = `${compileConditions},APILevel_syscap=${apiJsonPath}`;
    }
    if (this.moduleModel.isHarModule()) {
      env.COMPILE_CONDITION = `${compileConditions},target=${this.targetName}`;
    } else {
      env[`COMPILE_CONDITION_${this.moduleName.toUpperCase()}`] = `${compileConditions},target=${this.targetName}`;
      env.COMPILE_CONDITION = `${compileConditions},target=${DefaultTargetConst.DEFAULT_TARGET}`;
    }
    this.initOptimizationSettings(projectDir, env);
    return env;
  }

  protected getCjpmBuildOptions(cmdEnv: NodeJS.ProcessEnv): SpawnSyncOptions {
    return {
      cwd: this.cjSource,
      env: cmdEnv,
      windowsHide: true,
    };
  }

  /**
   * Execute Cangjie compilation.
   * @param isNeedCompile isNeedCompile
   * @returns {Promise<void>}
   * @protected
   */
  protected async executeCangjieCompile(isNeedCompile: boolean): Promise<void> {
    if (this.sdkCangjieComponent === undefined) {
      return;
    }
    this.asanEnabled = hvigorCore.getExtraConfig().get('ohos-debug-asan') === 'true' ||
      readAsanEnabled(this.getAppJson5Path(), this.accLogger);
    this.compileAppType = this.getCompileAppType();
    const isNeedExecCompile = !this.moduleModel.isHarModule() || isNeedCompile || this.needCallCjpmBuild();
    const cjpm = path.resolve(this.sdkCangjieComponent.toolsBin, isWindows() ? 'cjpm.exe' : 'cjpm');
    const cpuNums = this.getCpus();
    const commonCmdEnv: NodeJS.ProcessEnv = this.getCjpmProcessEnv();
    const filterMsgs: RegExp[] = this.getFilterMsgs();
    const tomlName = readPackageName(this.cjpmTomlPath, this.accLogger);
    const moduleConfigTomlName = InjectUtil.isOhosTest() ?
      readModuleConfigTomlName(this.targetService, this.accLogger) : '';
    if (isNeedExecCompile) {
      this.initDiffMockDependMap(this.sdkCangjieComponent.diffDependsJsonPath);
    }
    await this.doCangjieCompile({
      isNeedExecCompile: isNeedExecCompile,
      commonCmdEnv: commonCmdEnv,
      cjpm: cjpm,
      cpuNums: cpuNums,
      filterMsgs: filterMsgs,
      tomlName: tomlName,
      moduleConfigTomlName,
    });
  }

  /**
   * Copying Cangjie Compiled Products to the Target Directory
   * @protected
   */
  protected async copyResultFile(cangjiePathImpl: CangjiePathImpl, dependsEnv: NodeJS.ProcessEnv,
    tomlName: string): Promise<void> {
    if (this.sdkCangjieComponent === undefined) {
      return;
    }
    const isHar = this.moduleModel.isHarModule() && !InjectUtil.isOhosTest();
    const isByteCodeHar = isHar && this.targetService.isByteCodeHar();
    const extendLibPath = cangjiePathImpl.getExtendLibPath(isHar);
    FileUtil.checkDirWithoutDelete(extendLibPath);
    const excludeFiles = [this.sdkCangjieComponent.apiPath];
    if (isByteCodeHar) {
      const cjDependenciesUtil = new CangjieDependency(cangjiePathImpl.abi, dependsEnv, this.targetService);
      cjDependenciesUtil.srcFilterHarPaths = this.srcFilterHarPaths;
      cjDependenciesUtil.binFilterHarPaths = this.binFilterHarPaths;
      cjDependenciesUtil.packageName = tomlName;
      cjDependenciesUtil.doFindAllDependencies(this.cjSource, excludeFiles);
      const cjBuildOutputPath = cangjiePathImpl.getIntermediatesCjBuildRelease();
      await cjDependenciesUtil.doCopyCompileLibs(cjBuildOutputPath, '', true, extendLibPath,
        isHar && !this.cjModuleOptions.flattenLibs);
      await cjDependenciesUtil.doCopyDependencies(extendLibPath, isHar && !this.cjModuleOptions.flattenLibs);
      if (!this.cjModuleOptions.flattenLibs) {
        // Binary har with macro cjo
        await cjDependenciesUtil.doCopyMacroCjoForBinHar(cangjiePathImpl.getIntermediatesCjBuildMacroRelease(),
          cangjiePathImpl.getIntermediatesCjBinLibs());
      }
    } else {
      const cjDependenciesUtil = new CangjieHapDependency(cangjiePathImpl.abi, dependsEnv);
      cjDependenciesUtil.doFindAllDependencies(this.cjSource, excludeFiles);
      const cjBuildOutputPath = cangjiePathImpl.getIntermediatesCjBuildRelease();
      const onlyCangjieHar = false;
      await cjDependenciesUtil.doCopyCompileLibs(cjBuildOutputPath, '', true, extendLibPath, onlyCangjieHar);
      await cjDependenciesUtil.doCopyDependencies(extendLibPath, onlyCangjieHar);
    }
    await retry(() => this.copyUnitTests(cangjiePathImpl.getIntermediatesUnitTestBin(),
      cangjiePathImpl.getIntermediatesCjUnitLibs(), tomlName));
    this.accLogger.debug(`Succeeded in copying cangjie libs to ${extendLibPath}.`);
  }

  protected getGitPath(): string {
    let gitPath = '';
    try {
      const args = ['git'];
      const result = spawnSync(isWindows() ? 'where' : 'which', args, {windowsHide: true});
      const stdout = iconv.decode(<Buffer>result.stdout, isWindows() ? 'GBK' : 'utf-8');
      if (checkIsValid(stdout)) {
        gitPath = path.dirname(stdout.split(/\r?\n|\r/g)[0]);
      }
    } catch (error) {
      // do nothing
    }
    return gitPath;
  }

  protected getMacSdkRoot(): string {
    if (!isMac()) {
      return '';
    }
    let sdkRoot = '';
    try {
      const args = ['--sdk', 'macosx', '--show-sdk-path'];
      const result = spawnSync('xcrun', args, {windowsHide: true});
      sdkRoot = iconv.decode(<Buffer>result.stdout, 'utf-8');
      if (!checkIsValid(sdkRoot)) {
        sdkRoot = '';
      }
    } catch (error) {
      // do nothing
    }
    return sdkRoot.trimEnd();
  }

  protected configDependsEnv(cangjiePathImpl: CangjiePathImpl): NodeJS.ProcessEnv {
    const seamlessPath = getSeamlessPath(this.targetService.getTargetData().getApiMeta().compileSdkVersion.version,
      this.projectModel.getProjectDir());
    const env = cangjiePathImpl.getBuildEnvByAbi(seamlessPath);
    env.ABI = cangjiePathImpl.abi;
    configOhModulesEnv(this.projectModel, env);
    return env;
  }

  protected async copyDemandLibsCommon(cangjiePathImpl: CangjiePathImpl, tomlName: string,
    moduleConfigTomlName: string): Promise<void> {
    // rootPackage:fullName ohos_entry.aa:ohos_entry
    const sourceDependFiles = new Map<string, string[]>();

    // libName:libDirPath  stdx.net:/aa/stdx
    const binDependFiles = new Map<string, string>();

    // std libs
    const stdDependFiles = new Set<string>();
    const rootTomlNames = new Set<string>([tomlName]);
    if (checkIsValid(moduleConfigTomlName)) {
      rootTomlNames.add(moduleConfigTomlName);
    }
    this.generateDepList(cangjiePathImpl, sourceDependFiles, binDependFiles, stdDependFiles, rootTomlNames);
    const demandPackages = getDemandSrcPackages(rootTomlNames);
    const extendLibPath = cangjiePathImpl.getExtendLibPath(this.moduleModel.isHarModule());
    const cjBuildRelease = cangjiePathImpl.getIntermediatesCjBuildRelease();
    for (const demandPkg of demandPackages) {
      const demandPkgLibs = sourceDependFiles.get(demandPkg);
      if (demandPkgLibs === undefined) {
        continue;
      }
      await copySpecifiedFiles(demandPkgLibs, path.join(cjBuildRelease, demandPkg),
        extendLibPath);
    }
    const demandRegExps = DEMAND_BIN_PACKAGES.map((pkg: string) => new RegExp(`^${pkg}(?<name>\\..*)*$`));
    this.addDependCppLibs(binDependFiles);
    await this.doCopyLibsCompatible(cangjiePathImpl, binDependFiles, stdDependFiles, demandRegExps);
  }

  private async doCopyLibsCompatible(cangjiePathImpl: CangjiePathImpl, binDependFiles: Map<string, string>,
    stdDependFiles: Set<string>, demandRegExps: RegExp[]): Promise<void> {
    let loaderSoSrcPath = cangjiePathImpl.getOhosLibsPathByAbi();
    switch (this.compileAppType) {
      case CompileAppTypeEnum.COMPILE_SDK_VER_LTE_21: {
        let collectSDKLibs = this.cjModuleOptions.collectSDKLibs;
        if (collectSDKLibs === undefined) {
          collectSDKLibs = true;
        }
        await this.doPackageWithDefaultSdk(cangjiePathImpl, binDependFiles, stdDependFiles, collectSDKLibs);
        loaderSoSrcPath = cangjiePathImpl.getApiCompatiblePathByAbi();
        break;
      }
      case CompileAppTypeEnum.COMPATIBLE_SDK_VER_EQ_22: {
        const collectSDKLibs = this.initCollectDependLibsFlag();
        await this.doPackageWithExtendSdk(cangjiePathImpl, binDependFiles, stdDependFiles, collectSDKLibs);
        break;
      }
      case CompileAppTypeEnum.COMPATIBLE_SDK_VER_LT_23: {
        const collectSDKLibs = this.initCollectDependLibsFlag();
        await this.doPackageWithExtendSdk(cangjiePathImpl, binDependFiles, stdDependFiles, collectSDKLibs);
        break;
      }
      default:
        break;
    }
    const rootDestPath = cangjiePathImpl.getExtendLibPath(this.moduleModel.isHarModule() && !InjectUtil.isOhosTest());

    // demand copy unsinked std libs to root dir
    await this.copyDemandStdLibs(stdDependFiles, cangjiePathImpl.getRuntimeOhosPathByAbi(), rootDestPath);

    // demand copy stdx libs to root dir
    await this.copyDemandBinLibs(demandRegExps, binDependFiles, rootDestPath);
    if (DEMAND_BIN_PACKAGES.includes('tpc')) {
      await this.copyDemandDependLibs(binDependFiles, rootDestPath);
    }
    if (hasEtsModule(this.moduleModel.getProjectDir())) {
      await retry(() => copySpecifiedFiles(LOADER_SO_LIBS, loaderSoSrcPath, rootDestPath));
    }
  }

  private initCollectDependLibsFlag(): boolean {
    const collectSDKLibs = this.cjModuleOptions.collectSDKLibs;
    if (this.sdkCangjieComponent?.isEscapeSdk === false) {
      return collectSDKLibs === undefined ? false : collectSDKLibs;
    } else {
      return collectSDKLibs === undefined ? true : collectSDKLibs;
    }
  }

  private async doPackageWithDefaultSdk(cangjiePathImpl: CangjiePathImpl, binDependFiles: Map<string, string>,
    stdDependFiles: Set<string>, collectSDKLibs = true): Promise<void> {
    const ohosDestPath = cangjiePathImpl.getIntermediatesCjOhosLibs();
    const runtimeDestPath = cangjiePathImpl.getIntermediatesCjRuntimeLibs();
    const isCompatibilityExists = fs.existsSync(cangjiePathImpl.getApiCompatibilityByAbi());
    if (isCompatibilityExists && collectSDKLibs) {
      // demand copy ohos/kit libs to ohos dir
      await this.copyDemandBinLibsLegacy([/^(?<type>ohos|kit)(?<name>\..*)*$/], binDependFiles,
        cangjiePathImpl.getApiCompatiblePathByAbi(), runtimeDestPath);

      // copy cwrapper mock libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getWrapperMockPathByAbi(), ohosDestPath);

      // copy cwrapper diff libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getWrapperDiffPathByAbi(), ohosDestPath);

      // copy api mock/diff libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getApiMockPathByAbi(), ohosDestPath);

      // copy runtime/std mock/diff libs to runtime dir
      await copyLibsCommon(cangjiePathImpl.getRuntimeMockPathByAbi(), runtimeDestPath);

      // demand copy sinked std libs to runtime dir
      await this.copyDemandStdLibsLegacy(stdDependFiles, cangjiePathImpl.getRuntimeOhosPathByAbi(),
        runtimeDestPath);
    }
    if ((isCompatibilityExists && collectSDKLibs) || this.asanEnabled) {
      // copy runtime.so file to runtime dir
      let runtimeLibSrcPath = cangjiePathImpl.getRuntimeCompatibilityPathByAbi();
      let runtimeLibDestPath = runtimeDestPath;
      if (this.asanEnabled) {
        runtimeLibSrcPath = cangjiePathImpl.getAsanRuntimeLibsPathByAbi();
        runtimeLibDestPath = cangjiePathImpl.getIntermediatesCjAsanLibs();
        await copySpecifiedFiles(ASAN_RUNTIME_DEPEND_LIBS, cangjiePathImpl.getRuntimeOhosPathByAbi(),
          runtimeLibDestPath);
      }
      await copySpecifiedFiles(COMPATIBILITY_RUNTIME_LIBS_50, runtimeLibSrcPath, runtimeLibDestPath);
    }
  }

  private async doPackageWithExtendSdk(cangjiePathImpl: CangjiePathImpl, binDependFiles: Map<string, string>,
    stdDependFiles: Set<string>, collectSDKLibs = false): Promise<void> {
    const runtimeDestPath = cangjiePathImpl.getIntermediatesCjRuntimeLibs();
    const ohosDestPath = cangjiePathImpl.getIntermediatesCjOhosLibs();
    const asanDestPath = cangjiePathImpl.getIntermediatesCjAsanLibs();
    if (collectSDKLibs) {
      // demand copy ohos/kit libs to ohos dir
      await this.copyDemandBinLibsLegacy([/^(?<type>ohos)(?<name>\..*)*$/], binDependFiles,
        cangjiePathImpl.getOhosLibsPathByAbi(), runtimeDestPath);
      await this.copyDemandBinLibsLegacy([/^(?<type>kit)(?<name>\..*)*$/], binDependFiles,
        cangjiePathImpl.getKitLibsPathByAbi(), runtimeDestPath);

      // copy mock diff so
      await this.copyDiffMockLibs(binDependFiles, cangjiePathImpl, ohosDestPath);

      // copy api mock/diff libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getEscapeApiMockPathByAbi(), ohosDestPath);

      // copy runtime/std mock/diff libs to runtime dir
      await copyLibsCommon(cangjiePathImpl.getEscapeRuntimeMockPathByAbi(), runtimeDestPath);

      // demand copy sinked std libs to runtime dir
      await this.copyDemandStdLibsLegacy(stdDependFiles, cangjiePathImpl.getRuntimeOhosPathByAbi(),
        runtimeDestPath);
    }
    if (collectSDKLibs || this.asanEnabled) {
      // copy runtime.so file to asan or runtime dir
      let runtimeLibSrcPath = cangjiePathImpl.getRuntimeOhosPathByAbi();
      let runtimeLibDestPath = runtimeDestPath;
      if (this.asanEnabled) {
        runtimeLibSrcPath = cangjiePathImpl.getAsanRuntimeLibsPathByAbi();
        runtimeLibDestPath = asanDestPath;
        await copySpecifiedFiles(ASAN_RUNTIME_DEPEND_LIBS, cangjiePathImpl.getRuntimeOhosPathByAbi(),
          runtimeLibDestPath);
      }
      await copySpecifiedFiles(COMPATIBILITY_RUNTIME_LIBS_50, runtimeLibSrcPath, runtimeLibDestPath);
    }
  }

  private getFilterMsgs(): RegExp[] {
    const loaderPath = path.join(this.moduleModel.getProjectDir(), cangjieSrcRelatePath(), 'loader');
    const arkInteropApiPath = path.join(this.moduleModel.getProjectDir(), cangjieSrcRelatePath(), 'ark_interop_api');
    const typesPath = path.join(this.moduleModel.getProjectDir(), cangjieSrcRelatePath(), 'types');
    const replaceMsgs: RegExp[] = [];
    replaceMsgs.push(new RegExp(`${this.generateWarnMsg(loaderPath).replace(/\\/g, '\\\\')}\\r?\\n?`, 'g'));
    replaceMsgs.push(new RegExp(`${this.generateWarnMsg(arkInteropApiPath).replace(/\\/g, '\\\\')}\\r?\\n?`, 'g'));
    replaceMsgs.push(new RegExp(`${this.generateWarnMsg(typesPath).replace(/\\/g, '\\\\')}\\r?\\n?`, 'g'));
    const ESC = '\x1b';
    replaceMsgs.push(
      new RegExp(`${ESC}\\[33mwarning${ESC}\\[0m: Link option '--dy-libs' is enabled by default.\\r?\\n?`, 'g'));
    return replaceMsgs;
  }

  private generateWarnMsg(directory: string): string {
    return `Warning: there is no '.cj' file in directory '${directory}', ` +
      'and its subdirectories will not be scanned as source code';
  }

  private initFilterDependPaths(har: Dependency, isNeedCollectSrcDepends: boolean): void {
    if (har.isLocal()) {
      this.binFilterHarPaths.push(path.normalize(path.join(har.getDependencyRootPath(), cangjieSrcRelatePath())));
      if (!isNeedCollectSrcDepends) {
        this.srcFilterHarPaths.push(
          path.normalize(path.join(har.getDependencyRootPath(), cangjieSrcRelatePath())));
        return;
      }
      const moduleName = har.getModuleJsonObj()?.module?.name;
      if (moduleName === undefined) {
        return;
      }
      const buildOption = buildOptionManager.getTargetBuildOption(moduleName, this.targetName);
      const isByteCodeHar = this.targetService.getByteCodeHar(buildOption?.strictMode?.useNormalizedOHMUrl,
        buildOption?.arkOptions?.byteCodeHar);
      if (isByteCodeHar) {
        this.srcFilterHarPaths.push(
          path.normalize(path.join(har.getDependencyRootPath(), cangjieSrcRelatePath())));
      }
    } else if (har.isByteCodeHarDependency()) {
      this.scanByteCodeHar(har);
    } else {
      const remoteSrcPath = path.join(har.getDependencyRootPath(), cangjieSrcRelatePath());
      if (fs.existsSync(remoteSrcPath)) {
        this.binFilterHarPaths.push(path.normalize(remoteSrcPath));
      }
    }
  }

  private scanByteCodeHar(har: Dependency): void {
    for (const key in AbiEnum) {
      if (!Object.prototype.hasOwnProperty.call(AbiEnum, key)) {
        continue;
      }
      const abi = AbiEnum[key as keyof typeof AbiEnum];
      const binDependPath = path.join(har.getDependencyRootPath(), 'libs', abi, CjBuildDirConst.BIN_LIBS);
      if (!fs.existsSync(binDependPath)) {
        continue;
      }
      const files = fs.readdirSync(binDependPath);
      files.forEach((file) => {
        const filePath = path.join(binDependPath, file);
        const isDirectory = fs.statSync(filePath).isDirectory();
        if (isDirectory) {
          this.srcFilterHarPaths.push(path.normalize(filePath));
          this.binFilterHarPaths.push(path.normalize(filePath));
        }
      });
    }
  }

  private getCpus(): number {
    const cpuNums = os.cpus().length - 2;
    return cpuNums > 0 ? cpuNums : 1;
  }

  private async doCangjieCompile({
    isNeedExecCompile,
    commonCmdEnv,
    cjpm,
    cpuNums,
    filterMsgs,
    tomlName,
    moduleConfigTomlName,
  }: BuildParams): Promise<void> {
    if (this.sdkCangjieComponent === undefined) {
      return;
    }
    const harDependencies = this.service.getHarDependencies();
    const isNeedCollectSrcDepends = !this.moduleModel.isHarModule() || this.targetService.isByteCodeHar();
    for (const har of harDependencies) {
      this.initFilterDependPaths(har, isNeedCollectSrcDepends);
    }
    const isOnlyCjTest = hvigorCore.getExtraConfig().get('isCangjie') === 'true';
    const isCjOhosTest = InjectUtil.isOhosTest() && isOnlyCjTest;
    const isCjLocalTest = this.isCjLocalTest();
    const isRealExecute = !InjectUtil.isLocalTest() &&
      !(hasArktsCangjieModule(this.moduleModel.getProjectDir()) && this.targetName ===
        DefaultTargetConst.OHOS_TEST_TARGET && this.moduleModel.isHapModule());
    await retry(() => clearDir(this.pathInfo.getIntermediatesCangjieOutPut()));
    for (const abi of this.abiFilters) {
      const cangjiePathImpl = new CangjiePathImpl(this.targetName, this.pathInfo, abi, this.sdkCangjieComponent,
        this.customCjpmArguments);

      // Configure environment variables when Cangjie dependency exists.
      const dependsEnv = this.configDependsEnv(cangjiePathImpl);
      let isNeedCopyDepend = false;
      if (isNeedExecCompile) {
        const cmdEnv = {...commonCmdEnv, ...dependsEnv};
        const options: SpawnSyncOptions = this.getCjpmBuildOptions(cmdEnv);
        this.accLogger._printDebugCommand('NodeEnv', cmdEnv);
        if (isRealExecute) {
          isNeedCopyDepend = true;
          const cjpmBuildCommands = new CjpmCommandBuilder(cjpm, this.accLogger).buildCmd().
            withOhosTestIfNeeded(isCjOhosTest).
            withTarget(getTargetByAbi(abi)).
            withParallelJobs(cpuNums).
            withTargetDir(path.relative(this.cjSource, cangjiePathImpl.getIntermediatesCjBuild())).
            withBuildMode(this.targetService.isDebug()).
            withAsan(abi, this.asanEnabled).
            withCustomArgs(this.customCjpmArguments).
            build();
          await new ProcessUtil(this.moduleName, this.name).execute({command: cjpmBuildCommands, options, filterMsgs});
        }
        if (isCjOhosTest || isCjLocalTest) {
          isNeedCopyDepend = true;
          const cjpmTestCommands = new CjpmCommandBuilder(cjpm, this.accLogger).testCmd(true).withCoverage().
            withTarget(getTargetByAbi(abi)).
            withParallelJobs(cpuNums).
            withTargetDir(path.relative(this.cjSource, cangjiePathImpl.getIntermediatesCjBuild())).
            withBuildMode(this.targetService.isDebug()).
            withAsan(abi, this.asanEnabled).
            withCustomArgs(this.customCjpmArguments).
            build();
          const stdOut = await new ProcessUtil(this.moduleName, this.name).execute(
            {command: cjpmTestCommands, options, filterMsgs});
          this.identifyTestResultPath(stdOut, cangjiePathImpl, isCjLocalTest);
        }
      }
      await retry(() => this.copyResultFile(cangjiePathImpl, dependsEnv, tomlName));
      if (isNeedCopyDepend) {
        await retry(() => this.copyDemandLibsCommon(cangjiePathImpl, tomlName, moduleConfigTomlName));
      }
    }
  }

  private generateDepList(cangjiePathImpl: CangjiePathImpl, sourceDependFiles: Map<string, string[]>,
    binDependFiles: Map<string, string>, stdDependFiles: Set<string>, rootTomlNames: Set<string>): void {
    const cjpmHistoryPath = cangjiePathImpl.getIntermediatesCjpmHistory();
    if (!fs.existsSync(cjpmHistoryPath)) {
      this.accLogger.printErrorExit('NOT_FOUND_CJPM_HISTORY_FILE', [cjpmHistoryPath]);
      return;
    }
    const historyJsonData = JSON.parse(fs.readFileSync(cjpmHistoryPath).toString());
    const incorrect = !checkIsValid(historyJsonData) ||
      !Object.prototype.hasOwnProperty.call(historyJsonData, 'resolves') ||
      !Object.prototype.hasOwnProperty.call(historyJsonData, 'crossBinDeps');
    if (incorrect) {
      this.accLogger.printErrorExit('CJPM_HISTORY_CONTENT_INCORRECT', [cjpmHistoryPath]);
      return;
    }
    const resolves = historyJsonData.resolves;
    const allPackageMap = new Map<string, any>();
    const rootPackages: any[] = [];
    resolves.forEach((srcObj: any) => {
      allPackageMap.set(srcObj.fullName, srcObj);
      if (rootTomlNames.has(srcObj.rootPackageName)) {
        rootPackages.push(srcObj);
      }
    });
    const collectedSrcDepends = new Map<string, any>();
    rootPackages.forEach(item => this.collectSrcDependencies(allPackageMap, collectedSrcDepends,
      stdDependFiles));
    const crossBinDeps = historyJsonData.crossBinDeps;
    collectedSrcDepends.forEach((rootNode: any, fullName: string) => {
      const hasRootPkgName = Object.prototype.hasOwnProperty.call(rootNode, 'rootPackageName');
      const rootPackageName = rootNode.rootPackageName;
      this.addSourceDepend(hasRootPkgName, sourceDependFiles, rootPackageName, fullName);
      const unNeedCollect = !Object.prototype.hasOwnProperty.call(rootNode, 'requires') ||
        !hasRootPkgName ||
        !Object.prototype.hasOwnProperty.call(crossBinDeps, rootPackageName) ||
        !Object.prototype.hasOwnProperty.call(crossBinDeps[rootPackageName], 'package-requires');
      if (unNeedCollect) {
        return;
      }
      this.collectDepList(binDependFiles, stdDependFiles, rootNode.requires as Array<string>,
        crossBinDeps[rootPackageName]['package-requires']);
    });
  }

  private collectDepList(binDepends: Map<string, string>, stdDepends: Set<string>, requireList: Array<string>,
    requiresData: any): void {
    const originalLength = requireList.length;
    if (originalLength === 0) {
      return;
    }
    for (let i = 0; i < originalLength; i++) {
      const item = requireList[i];
      if (BIN_DEPEND_LIBS_MAP.has(item)) {
        const dependencies = BIN_DEPEND_LIBS_MAP.get(item) ?? [];
        requireList.push(...dependencies);
      }
    }
    requireList.forEach((item) => {
      if (!checkIsValid(item) || !Object.prototype.hasOwnProperty.call(requiresData, item) || binDepends.has(item)) {
        return;
      }
      binDepends.set(item, '');
      const itemData = requiresData[item];
      const isItemDataValid = checkIsValid(itemData);
      if (isItemDataValid && Object.prototype.hasOwnProperty.call(itemData, 'libPath')) {
        binDepends.set(item, itemData.libPath);
      }

      // do not collect macro packages.
      if (!isItemDataValid ||
        (Object.prototype.hasOwnProperty.call(itemData, 'isMacroPackage') && itemData.isMacroPackage === true)) {
        return;
      }
      this.addStdDepends(itemData, stdDepends);
      if (!Object.prototype.hasOwnProperty.call(itemData, 'requires')) {
        return;
      }
      this.collectDepList(binDepends, stdDepends, itemData.requires, requiresData);
    });
  }

  private collectSrcDependencies(allPackageMap: Map<string, any>, collectedSrcDepends: Map<string, any>,
    stdDepends: Set<string>): void {
    for (const [, curr] of allPackageMap) {
      if (collectedSrcDepends.has(curr.fullName) || curr.isMacroPackage === true) {
        continue;
      }
      collectedSrcDepends.set(curr.fullName, curr);
      this.addStdDepends(curr, stdDepends);
    }
  }

  private async copyDemandBinLibs(demandRegExps: RegExp[], files: Map<string, string>,
    destPath: string): Promise<void> {
    for (const [file, srcPath] of files) {
      await this.doCopyOneFile(demandRegExps, file, srcPath, destPath);
    }
  }

  private async doCopyOneFile(demandRegExps: RegExp[], file: string, srcPath: string,
    destPath: string): Promise<void> {
    if (!demandRegExps.some(pattern => pattern.test(file)) || !checkIsValid(srcPath)) {
      return;
    }
    const soName = `lib${file}.so`;
    const srcFilePath = path.resolve(srcPath, soName);
    if (!checkIsValid(srcFilePath) || !fs.existsSync(srcFilePath)) {
      return;
    }
    await linkFile(srcFilePath, path.resolve(destPath, soName));
  }

  private async copyDemandBinLibsLegacy(demandRegExps: RegExp[], files: Map<string, string>, srcPath: string,
    destPath: string): Promise<void> {
    if (!checkIsValid(srcPath) || !fs.existsSync(srcPath)) {
      return;
    }
    files.set('ohos.labels', srcPath);
    for (const [file] of files) {
      await this.doCopyOneFile(demandRegExps, file, srcPath, destPath);
    }
  }

  private async copyDemandStdLibsLegacy(packages: Set<string>, srcPath: string,
    destPath: string): Promise<void> {
    const directSoNames = [...packages].map(pkg => `libcangjie-${pkg.replace('.', '-')}.so`);
    directSoNames.push(...DEFAULT_COPY_RUNTIME_LIBS_50);
    const soNames = this.collectDeps(directSoNames, STD_DEPEND_LIBS_MAP_50);
    STD_DEMAND_ALL_LIBS.forEach(soName => soNames.delete(soName));

    // Do not copy the libcangjie-runtime.so file under 5.1
    COMPATIBILITY_RUNTIME_LIBS_50.forEach(item => soNames.delete(item));
    await copySpecifiedFiles([...soNames], srcPath, destPath);
  }

  private collectDeps(stackSoNames: string[], depMap: Map<string, string[]>): Set<string> {
    const colSoNames = new Set<string>();
    while (stackSoNames.length) {
      const cur = stackSoNames.pop();
      if (cur === undefined) {
        continue;
      }
      if (colSoNames.has(cur)) {
        continue;
      }
      colSoNames.add(cur);
      const next = depMap.get(cur);
      if (next === undefined || next.length === 0) {
        continue;
      }
      for (const lib of next) {
        if (!colSoNames.has(lib)) {
          stackSoNames.push(lib);
        }
      }
    }
    return colSoNames;
  }

  private async copyDemandStdLibs(packages: Set<string>, runtimeLibPath: string, destPath: string): Promise<void> {
    const soNames = [...packages].map(pkg => `libcangjie-${pkg.replace('.', '-')}.so`).
      filter(soName => STD_DEMAND_ALL_LIBS.includes(soName));
    await copySpecifiedFiles(soNames, runtimeLibPath, destPath);
  }

  private addSourceDepend(hasRootPkgName: any, sourceDependFiles: Map<string, string[]>, rootPackageName: any,
    fullName: string): void {
    if (!hasRootPkgName) {
      return;
    }
    const packageBuildLibs = sourceDependFiles.get(rootPackageName);
    const soName = `lib${fullName}.so`;
    if (packageBuildLibs === undefined) {
      sourceDependFiles.set(rootPackageName, [soName]);
    } else {
      packageBuildLibs.push(soName);
    }
  }

  private addStdDepends(itemData: any, stdDepends: Set<string>): void {
    if (!Object.prototype.hasOwnProperty.call(itemData, STD_REQUIRES)) {
      return;
    }
    const stdRequires = itemData[STD_REQUIRES];
    if (!Array.isArray(stdRequires) || stdRequires.length === 0) {
      return;
    }
    stdRequires.forEach(depItem => {
      if (depItem === undefined || typeof depItem !== 'object') {
        return;
      }
      Object.keys(depItem).forEach((key) => {
        stdDepends.add(key);
      });
    });
  }

  private addDependCppLibs(binDependFiles: Map<string, string>): void {
    BIN_DEPEND_LIBS_MAP.forEach((dependPkgs, srcPkg) => {
      const srcPkgPath = binDependFiles.get(srcPkg);
      if (srcPkgPath === undefined) {
        return;
      }
      dependPkgs.forEach(pkgItem => binDependFiles.set(pkgItem, srcPkgPath));
    });
  }

  private async copyDemandDependLibs(binDependFiles: Map<string, string>, extendLibPath: string): Promise<void> {
    const mustCopyDependLibs: MustCopyLibParams[] = [];
    const tpcCopyDependExp = /^tpc(?<name>\..*)*$/;
    for (const [file, srcPath] of binDependFiles) {
      if (!checkIsValid(srcPath)) {
        continue;
      }
      if (tpcCopyDependExp.test(file)) {
        const copyDependLibsParam: MustCopyLibParams = {
          libs: TPC_DEPEND_LIBS,
          libPath: srcPath,
        };
        mustCopyDependLibs.push(copyDependLibsParam);
        break;
      }
    }
    for (const mustCopyLibsItem of mustCopyDependLibs) {
      await copySpecifiedFiles(mustCopyLibsItem.libs, mustCopyLibsItem.libPath, extendLibPath);
    }
  }

  private getAllModulesSrcPath(): string[] {
    const allModules = this.projectModel.getAllModules();
    if (allModules === null || allModules.length === 0) {
      return [];
    }
    const sourcePathArr = [];
    for (const subModule of allModules) {
      const moduleBuildOption = buildOptionManager.getTargetBuildOption(subModule.getName(), this.targetName);
      const tomlPath = moduleBuildOption.cangjieOptions?.path;
      if (tomlPath === null || tomlPath === undefined) {
        continue;
      }
      const normalTomlPath = normalizeCjpmTomlPath(tomlPath, subModule.getProjectDir());
      if (!fs.existsSync(normalTomlPath)) {
        continue;
      }
      const tomlContent = readTomlContent(normalTomlPath, this.accLogger);
      const srcPath = path.join(path.dirname(normalTomlPath), readTomlSrcDir(tomlContent));
      if (!fs.existsSync(srcPath)) {
        continue;
      }
      sourcePathArr.push(srcPath);
    }
    return sourcePathArr;
  }

  private identifyTestResultPath(stdOut: string, cangjiePathImpl: CangjiePathImpl, isCjLocalTest: boolean): void {
    if (!checkIsValid(stdOut) || !stdOut.includes(CJPM_TEST_SUCCESS_FLAG)) {
      return;
    }
    const lines = stdOut.split('\n');
    const pathRegexp = /Executable files at ['"](?<path1>.*)['"]|Executable files at (?<path2>.*)/;
    for (let i = lines.length - 1; i >= 0; i--) {
      const line = lines[i];
      if (!line.includes(CJPM_TEST_EXECUTABLE_FILES_PREFIX)) {
        continue;
      }
      const pathMatch = line.match(pathRegexp);
      if (!pathMatch) {
        continue;
      }
      const realBuildPath = (pathMatch[1] || pathMatch[2]).replace(/`/g, '');
      if (!checkIsValid(realBuildPath)) {
        continue;
      }
      const buildOhosTargetPath = path.join(cangjiePathImpl.getIntermediatesCjBuildTarget(), CjBuildDirConst.MOCK);
      const buildLocalTargetPath = path.join(cangjiePathImpl.getIntermediatesCjBuild(), CjBuildDirConst.MOCK);
      cangjiePathImpl.mockPath =
        areChildPath(buildOhosTargetPath, realBuildPath) || areChildPath(buildLocalTargetPath, realBuildPath) ?
          CjBuildDirConst.MOCK : '';
      break;
    }
    if (isCjLocalTest) {
      const localTestPath = path.join(this.projectModel.getProjectDir(), '.idea', '.deveco', 'cangjie', 'build-logs',
        'test');
      createDir(localTestPath);
      const testBuildJsonPath = path.join(localTestPath, 'testBuildCache.json');
      const content = {
        testBuildPath: cangjiePathImpl.mockPath,
      };
      fs.writeFileSync(testBuildJsonPath, JSON.stringify(content));
    }
  }

  private async copyDiffMockLibs(binDependFiles: Map<string, string>, cangjiePathImpl: CangjiePathImpl,
    ohosDestPath: string): Promise<void> {
    if (this.sdkCangjieComponent?.diffDependsJsonPath === undefined ||
      !fs.existsSync(this.sdkCangjieComponent.diffDependsJsonPath)) {
      // copy cwrapper mock libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getEscapeWrapperMockPathByAbi(), ohosDestPath);

      // copy cwrapper diff libs to ohos dir
      await copyLibsCommon(cangjiePathImpl.getEscapeWrapperDiffPathByAbi(), ohosDestPath);
      return;
    }

    // copy cwrapper diff libs to ohos dir
    // copy cwrapper mock libs to ohos dir
    for (const [packageName, packagePath] of binDependFiles) {
      const soName = `lib${packageName}.so`;
      const diffLibs = this.diffSoMap.get(soName);
      if (diffLibs !== undefined && diffLibs !== null && diffLibs.length > 0) {
        await retry(() => copySpecifiedFiles(diffLibs, cangjiePathImpl.getEscapeWrapperDiffPathByAbi(), ohosDestPath));
      }
      const mockLibs = this.mockSoMap.get(soName);
      if (mockLibs !== undefined && mockLibs !== null && mockLibs.length > 0) {
        await retry(() => copySpecifiedFiles(mockLibs, cangjiePathImpl.getEscapeWrapperMockPathByAbi(), ohosDestPath));
      }
    }
  }

  private initDiffMockDependMap(diffDependsJsonPath: string): void {
    if (!fs.existsSync(diffDependsJsonPath)) {
      return;
    }
    const dependsData: SOData = JSON.parse(fs.readFileSync(diffDependsJsonPath).toString());
    if (!Object.prototype.hasOwnProperty.call(dependsData, 'so_dependencies')) {
      return;
    }
    dependsData.so_dependencies.forEach(dependency => {
      const originalSo = dependency.original_so;
      const diffSo = dependency.diff_so;
      const mockSo = dependency.mock_so;
      this.diffSoMap.set(originalSo, diffSo);
      this.mockSoMap.set(originalSo, mockSo);
    });
  }

  private initOptimizationSettings(projectDir: string, env: NodeJS.ProcessEnv): void {
    if (getProjectOption(projectDir, 'generateOptimizationProfile', 'false') === 'true') {
      env.OVERRIDE_COMPILE_OPTION = '"--pgo-instr-gen=/data/storage/el2/base/cjpgo/pgo_%m_%p.profraw"';
    } else {
      const configPath = process.env.DEVECO_CANGJIE_CONFIG_PATH ?? '';
      const optimizationConfig = getCangjieOptimizationConfig(configPath);
      const useProfile = (this.targetService.isDebug() && optimizationConfig.debugUseProfile) ||
        (!this.targetService.isDebug() && optimizationConfig.releaseUseProfile);
      if (!useProfile) {
        return;
      }
      const isUseExistedProfdataFile = getProjectOption(projectDir, 'useExistedProfdataSelected', 'false');
      let profdataPath;
      if (isUseExistedProfdataFile === 'true') {
        profdataPath = getProjectOption(projectDir, 'profdataFilePath', '');
        if (profdataPath !== '') {
          const commonReplacements = new Map<string, string>([
            ['USER_HOME', getHomedir() ?? ''],
            ['PROJECT_DIR', projectDir],
          ]);
          const absolutePath = this.replacePathPlaceholders(profdataPath, commonReplacements);
          env.OVERRIDE_COMPILE_OPTION = `"--pgo-instr-use=${absolutePath}"`;
        }
      } else {
        profdataPath = getDefaultProfdataFile(projectDir, this.moduleName);
        if (profdataPath !== '' && fs.existsSync(profdataPath)) {
          env.OVERRIDE_COMPILE_OPTION = `"--pgo-instr-use=${profdataPath}"`;
        }
      }
    }
  }

  private replacePathPlaceholders(profilePath: string, replacements: Map<string, string>): string {
    if (!checkIsValid(profilePath)) {
      return profilePath;
    }
    let result = profilePath;
    const placeholderRegex = /\$(?<placeholderName>[A-Z0-9_]+)(?:\$)?/g;
    result = result.replace(placeholderRegex, (match, ...args) => {
      const groups = args[args.length - 1];
      const placeholderName = groups.placeholderName;
      const replacementValue = replacements.get(placeholderName);
      if (replacementValue !== undefined) {
        return replacementValue;
      }
      return match;
    });
    return result;
  }
}

interface BuildParams {
  isNeedExecCompile: boolean;
  commonCmdEnv: NodeJS.ProcessEnv;
  cjpm: string;
  cpuNums: number;
  filterMsgs: RegExp[];
  tomlName: string;
  moduleConfigTomlName: string;
}

interface MustCopyLibParams {
  libs: string[];
  libPath: string;
}

interface SODependency {
  original_so: string;
  path: string;
  mock_so: string[];
  diff_so: string[];
}

interface SOData {
  so_dependencies: SODependency[];
}
