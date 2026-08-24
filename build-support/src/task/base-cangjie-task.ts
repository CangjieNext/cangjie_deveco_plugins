/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {CangjieOpt, TargetTaskService, TaskDetails} from '../../types/hvigor-imports';
import {
  ApiType,
  CommonConst,
  DefaultTargetConst,
  hvigorCore,
  InjectUtil,
  OhosHapTask,
  OhosLogger
} from '../../types/hvigor-imports';
import {checkIsValid} from '../utils/common-utils';
import path from 'path';
import {CANGJIE_NAME, CJPM_TOML_NAME} from '../constants/constants';
import fs from 'fs';
import {getSdkComponent} from '../sdk/cangjie-sdk-info';
import {SdkCangjieComponent} from '../sdk/sdk-cangjie-component';
import {hasEtsModule, linkFile, matchFiles, onlyCangjieModule} from '../utils/cangjie-file-util';
import {CangjieLogger} from '../log/cangjie-logger';
import {CompileAppTypeEnum} from '../enums/compile-app-type-enum';
import {CangjieModuleOptions} from '../module/cangjie-module-options';
import {CompileNodeGraphMatch} from '../utils/compile-node-graph-match';

/**
 * cangjie task base class
 *
 * @since 2024/1/6
 */
export abstract class BaseCangjieTask extends OhosHapTask {
  protected cjSource = '';
  protected cangjieOption?: CangjieOpt;
  protected cjpmTomlPath = '';
  protected sdkCangjieComponent?: SdkCangjieComponent;
  protected abiFilters: string[] = [];
  protected customCjpmArguments: string[] = [];
  protected cjModuleOptions: CangjieModuleOptions;
  private baseLogger: OhosLogger = CangjieLogger.getLogger(BaseCangjieTask.name);

  protected constructor(targetService: TargetTaskService, taskDetails: TaskDetails) {
    super(targetService, taskDetails);
    this.cangjieOption = this.targetService.getBuildOption()?.cangjieOptions;
    this.cjModuleOptions = new CangjieModuleOptions(this.cangjieOption);
    this.abiFilters = this.cjModuleOptions.abiFilters;
    this.customCjpmArguments = this.cjModuleOptions.arguments;

    // cangjie ui dir
    const moduleDir = this.moduleModel.getProjectDir();
    const ohosTestCangjieDir = path.resolve(moduleDir, 'src', DefaultTargetConst.OHOS_TEST_TARGET, 'cangjie');
    const ohosTestTomlPath = path.resolve(ohosTestCangjieDir, CJPM_TOML_NAME);
    const localTestCangjieDir = path.resolve(moduleDir, 'src', 'test', 'cangjie');
    const localTestTomlPath = path.resolve(localTestCangjieDir, CJPM_TOML_NAME);
    const unNeedCompile = ((!checkIsValid(this.cangjieOption) ||
        !Object.prototype.hasOwnProperty.call(this.cangjieOption, 'path'))) &&
      (InjectUtil.isOhosTest() && (!fs.existsSync(ohosTestCangjieDir) || !fs.existsSync(ohosTestTomlPath))) &&
      (InjectUtil.isLocalTest() && (!fs.existsSync(localTestCangjieDir) || !fs.existsSync(localTestTomlPath)));
    if (unNeedCompile) {
      return;
    }
    this.initCangjieTomlPath(moduleDir, localTestCangjieDir, localTestTomlPath, ohosTestCangjieDir, ohosTestTomlPath);
  }

  taskShouldDo(): boolean {
    return this.moduleModel !== undefined && this.moduleModel.getApiType() === ApiType.STAGE &&
      checkIsValid(this.cjpmTomlPath);
  }

  /**
   * Initializing the Cangjie SDK Path
   * @protected
   */
  protected initCangjieSdk(): void {
    const component = getSdkComponent(CANGJIE_NAME, this.targetData.getApiMeta().compileSdkVersion.version);
    if (component !== undefined) {
      this.sdkCangjieComponent = new SdkCangjieComponent(component);
    }
  }

  protected async processCjos(srcPath: string, destPath: string): Promise<void> {
    if (!fs.existsSync(srcPath)) {
      return;
    }
    const regex = /^.*\.cjo\d*$/;
    const paths: string[] = matchFiles(srcPath, regex);
    for (const cjoFile of paths) {
      const filePath = path.resolve(destPath, path.basename(path.dirname(cjoFile)));
      await linkFile(cjoFile, path.resolve(filePath, path.basename(cjoFile)));
    }
  }

  protected initCjSourceDir(cangjieOptionPath: string): void {
    if (!cangjieOptionPath.endsWith(CJPM_TOML_NAME)) {
      return;
    }
    if (path.isAbsolute(cangjieOptionPath)) {
      this.cjSource = path.dirname(cangjieOptionPath);
      this.cjpmTomlPath = cangjieOptionPath;
    } else {
      const tomlPath = path.resolve(this.service.getModuleModel().getProjectDir(), cangjieOptionPath);
      this.cjSource = path.dirname(tomlPath);
      this.cjpmTomlPath = tomlPath;
    }
  }

  protected async copyUnitTests(ohosLibPath: string | undefined, destPath: string,
    unitTestPackageName: string): Promise<void> {
    if (!InjectUtil.isOhosTest() || ohosLibPath === undefined || !fs.existsSync(ohosLibPath)) {
      return;
    }
    const libFiles = fs.readdirSync(ohosLibPath);
    for (const file of libFiles) {
      const isUnNeedCopy = !file.startsWith(unitTestPackageName) || file.endsWith('.cj') || file.endsWith('.cjo') ||
        file.endsWith('.bchir2') || file.endsWith('.flag');
      if (isUnNeedCopy) {
        continue;
      }
      const destFile = path.resolve(destPath, file);
      await linkFile(path.resolve(ohosLibPath, file), destFile);
    }
  }

  protected getAppJson5Path(): string {
    return path.join(this.projectModel.getProjectDir(), CommonConst.APP_SCOPE, CommonConst.APP_CONFIG);
  }

  protected getCompileAppType(): CompileAppTypeEnum {
    const compileSdkVersion = this.targetData.getApiMeta().compileSdkVersion.version;
    if (compileSdkVersion <= 21) {
      return CompileAppTypeEnum.COMPILE_SDK_VER_LTE_21;
    }
    const compatibleSdkVersion = this.targetData.getApiMeta().compatibleSdkVersion.version;
    if (compatibleSdkVersion < 23) {
      return CompileAppTypeEnum.COMPATIBLE_SDK_VER_LT_23;
    }
    return CompileAppTypeEnum.COMPATIBLE_SDK_VER_EQ_22;
  }

  protected isCjLocalTest(): boolean {
    return InjectUtil.isLocalTest() && hvigorCore.getExtraConfig().get('isCangjie') === 'true';
  }

  protected needCallCjpmBuild(): boolean {
    const closeSmartCjAppBuild = process.env.SMART_CJ_APP_BUILD === 'false';
    const openSmartCjHapBuild = process.env.SMART_CJ_HAP_BUILD === 'true';
    const compileNodeGraphMatch = CompileNodeGraphMatch.getInstance();
    if (compileNodeGraphMatch.mode === 'project') {
      if (closeSmartCjAppBuild) {
        return true;
      }
    } else {
      if (!openSmartCjHapBuild) {
        return true;
      }
    }
    const moduleTomlNameMap = compileNodeGraphMatch.moduleTomlNameMap;
    const moduleName = this.moduleModel.getName();
    const tomlName = moduleTomlNameMap.get(moduleName);
    if (tomlName === undefined) {
      return true;
    }
    const ancestorNodes = compileNodeGraphMatch.collectParentModuleTargets(tomlName);
    if (ancestorNodes.size === 0) {
      return true;
    }
    const tomlTreeMap = compileNodeGraphMatch.tomlTreeMap;
    const rootTomlNameSet = compileNodeGraphMatch.roots;
    if (!tomlTreeMap.has(tomlName) || rootTomlNameSet.has(tomlName)) {
      return true;
    }
    const startPackageTasks = compileNodeGraphMatch.startPackageTasks;
    if (startPackageTasks.size === 0) {
      return true;
    }
    const currentTask = `${moduleName}:${this.targetName}@CompileCangjie`;
    for (const startTask of startPackageTasks) {
      const hasPathToNode = compileNodeGraphMatch.hasPathToNode(startTask, currentTask, ancestorNodes);
      if (!hasPathToNode) {
        return true;
      }
    }
    return false;
  }

  private initCangjieTomlPath(moduleDir: string, localTestCangjieDir: string, localTestTomlPath: string,
    ohosTestCangjieDir: string, ohosTestTomlPath: string): void {
    const cangjieOptionPath = this.cangjieOption?.path;
    const isTargetOhosTest = this.targetName === DefaultTargetConst.OHOS_TEST_TARGET;
    const isBuildTest = InjectUtil.isOhosTest() && ((isTargetOhosTest && hasEtsModule(moduleDir)) ||
      onlyCangjieModule(moduleDir));
    if (InjectUtil.isLocalTest()) {
      this.cjSource = fs.existsSync(localTestCangjieDir) ? localTestCangjieDir : '';
      this.cjpmTomlPath = fs.existsSync(localTestTomlPath) ? localTestTomlPath : '';
    } else if (isBuildTest) {
      this.cjSource = fs.existsSync(ohosTestCangjieDir) ? ohosTestCangjieDir : '';
      this.cjpmTomlPath = fs.existsSync(ohosTestTomlPath) ? ohosTestTomlPath : '';
    } else {
      // do nothing
    }
    if (!checkIsValid(this.cjpmTomlPath)) {
      if (cangjieOptionPath === undefined || cangjieOptionPath === null || cangjieOptionPath === '') {
        return;
      }
      this.initCjSourceDir(cangjieOptionPath);
    }
  }
}
