/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import fs from 'fs';
import * as path from 'path';
import type {TargetSourceSetImpl} from '../../types/hvigor-imports';
import {
  buildOptionPath,
  CommonConst,
  DefaultTargetConst,
  FileSet,
  FileUtilOhos as FileUtil,
  InjectUtil,
  JsonUtil,
  ModuleJson,
  OhosLogger,
  PreBuild,
  resModelLoader,
  SourceSetModel,
  TargetTaskService,
  ValidateRegExp
} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {
  isCangjieHar,
  onlyCangjieModule,
  readModuleConfigToml,
  readModuleConfigTomlName,
  readPackageName,
  validateCangjieEntry
} from '../utils/cangjie-file-util';
import {CANGJIE_NAME, CJPM_TOML_NAME} from '../constants/constants';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';
import {CangjieLogger} from '../log/cangjie-logger';
import {getSdkComponent} from '../sdk/cangjie-sdk-info';
import {SdkCangjieComponent} from '../sdk/sdk-cangjie-component';
import {compareVersionsUpTo} from '../utils/common-utils';
import ModuleOptObj = ModuleJson.ModuleOptObj;

const _log = CangjieLogger.getLogger('cangjie-preBuild');

/**
 * preBuild Task
 *
 * @since 2022/1/10
 */
export class CangjiePreBuild extends PreBuild {
  private readonly supportDevices: string[] = ['default', 'phone', 'tablet'];
  private readonly versionPrefixLength: number = 3;
  private readonly checkCompatibleVersions = ['6', '0', '1'];

  constructor(taskService: TargetTaskService) {
    super(taskService, CangjieTaskNames.CANGJIE_PRE_BUILD);

    // cangjie ui dir
    const cangjieOption = this.targetService.getBuildOption().cangjieOptions;
    const profileFile: string = path.resolve(this.moduleModel.getProjectDir(), CommonConst.PROFILE_JSON5);
    const cangjieOptionPath = cangjieOption?.path;
    if (!InjectUtil.isOhosTest() && cangjieOptionPath !== undefined) {
      if (cangjieOptionPath === '') {
        _log.printErrorExit('CANGJIE_TOML_PATH_NOT_EXIST', [CJPM_TOML_NAME, profileFile], [[CJPM_TOML_NAME]]);
        return;
      }
      this.checkCjSourceDir(cangjieOptionPath, profileFile);
    }
    this.checkCangjieDeviceTypes(InjectUtil.isOhosTest());
    this.checkVersions();
    this.checkAbiFilters();
    this.checkDependenciesMatches();
  }

  declareInputFiles(): FileSet {
    const fileSet = super.declareInputFiles();
    const moduleConfigToml = readModuleConfigToml(this.targetService);
    if (fs.existsSync(moduleConfigToml)) {
      fileSet.addEntry(moduleConfigToml, {isDirectory: false});
    }
    return fileSet;
  }

  /**
   * 检查所有srcEntry字段是否以相对路径的形式配置
   * @private
   */
  protected checkAllSrcEntry(): void {
    const obj2srcEntryListMap = new Map<string, string[]>();
    const moduleJsonObj: ModuleOptObj = resModelLoader.getModuleJson(this.targetJsonPath);

    // module.json5->module.srcEntry
    if (moduleJsonObj.module.srcEntry) {
      obj2srcEntryListMap.set(this.MODULE_SRC_ENTRY, [moduleJsonObj.module.srcEntry]);
    }

    // module.json5->module.abilityObj.srcEntry
    this.getConfigAbilities(moduleJsonObj, obj2srcEntryListMap);

    // module.json5->module.extensionAbilities.srcEntry
    this.getExtensionAbilities(moduleJsonObj, obj2srcEntryListMap);

    // insight_intent.json->insightIntentsObj.srcEntry
    this.getConfigSrcEntry(obj2srcEntryListMap);

    this.checkSrcEntry(obj2srcEntryListMap);

    this.checkCangjieHsp();
  }

  protected checkSrcEntry(obj2srcEntryListMap: Map<string, string[]>): void {
    obj2srcEntryListMap.forEach((srcEntryList: string[] | undefined, objName: string) => {
      if (!srcEntryList?.length) {
        return;
      }
      const onlyCangjie = onlyCangjieModule(this.moduleModel.getProjectDir());
      const jsonPath = objName === this.INSIGHT_INTENT_SRC_ENTRY ? this.insightIntentJsonPath : this.targetJsonPath;
      const rootPackage = this.getCangjieRootPackage(_log);
      srcEntryList?.forEach(srcEntry => {
        const isInvalid = onlyCangjie ? validateCangjieEntry(rootPackage, srcEntry) :
          ValidateRegExp.RELATIVE_PATH_REG_EXP.test(srcEntry);
        if (!isInvalid) {
          onlyCangjie ?
            _log.printErrorExit('ABILITIES_SRC_ENTRY_IS_INCORRECT', [srcEntry, jsonPath]) :
            _log.printErrorExit('MODULE_SRC_ENTRY_PACKAGE_IS_INCORRECT', [objName, jsonPath]);
        }
      });
    });
  }

  protected validateModuleSrcEntry(logger: OhosLogger, moduleJsonObj: ModuleJson.ModuleOptObj): void {
    const srcEntry = moduleJsonObj.module.srcEntry || moduleJsonObj.module.srcEntrance;
    if (srcEntry === undefined) {
      return;
    }
    const sourceSetModel = this.moduleModel.getSourceSetByTargetName(this.targetData.getTargetName());
    if (moduleJsonObj.module.srcEntry === undefined) {
      _log.warn(`Field "module.srcEntrance" has been deprecated. Use "module.srcEntry" instead.       
      at ${(sourceSetModel as TargetSourceSetImpl).getModuleTargetRes().getJsonPath()}`);
    }
    this.checkCangjieEntry(srcEntry, sourceSetModel, _log);
  }

  protected checkVersions(): void {
    if (this.targetSdkVersion !== undefined && this.targetSdkVersion < 12) {
      _log.printErrorExit('LOW_API_VERSION_ERROR', [this.projectModel.getProfilePath()]);
    }
    if (this.compatibleApiVersion < 12) {
      _log.printErrorExit('COMPATIBLE_SDK_VERSION_ERROR', [this.projectModel.getProfilePath()]);
    }
    const component = getSdkComponent(CANGJIE_NAME, this.targetData.getApiMeta().compileSdkVersion.version);
    if (component === undefined) {
      return;
    }
    const sdkCangjieComponent = new SdkCangjieComponent(component);
    const isNotCompatible = this.compileApiVersion >= 22 && !sdkCangjieComponent.isEscapeSdk &&
      this.compatibleApiVersion < 22;
    if (isNotCompatible) {
      _log.printErrorExit('COMPATIBLE_SDK_LOWER_THAN_22_VERSION_ERROR', [this.projectModel.getProfilePath()]);
    }
    const isUnSupportCompatible = this.compileApiVersion >= 22 && sdkCangjieComponent.isEscapeSdk &&
      this.compatibleApiVersion < 20;
    if (isUnSupportCompatible) {
      _log.printErrorExit('COMPATIBLE_SDK_LOWER_THAN_20_VERSION_ERROR', [this.projectModel.getProfilePath()]);
    }
  }

  protected checkCangjieDeviceTypes(isOhosTest: boolean): void {
    const checkDeviceTypes = this.targetService.getBuildOption()?.cangjieOptions?.checkDeviceTypes;
    if (checkDeviceTypes === undefined || checkDeviceTypes === true) {
      const moduleJson5 = path.resolve(this.moduleModel.getProjectDir(), 'src',
        isOhosTest ? DefaultTargetConst.OHOS_TEST_TARGET : 'main', 'module.json5');
      if (this.moduleModel.getDeviceTypes().some(item => !this.supportDevices.includes(item))) {
        _log.printErrorExit('SUPPORT_DEVICE_TYPES_CONFIG_ERROR', [moduleJson5]);
      }
    }
    if (!onlyCangjieModule(this.moduleModel.getProjectDir())) {
      return;
    }
    const hspDependencies = this.service.getHspDependencies();
    const moduleDeviceTypes = this.targetService.getTargetData().getTargetDeviceType();
    const deviceTypeDefault = 'default';
    const deviceTypePhone = 'phone';
    for (let index = 0; index < hspDependencies.length; index++) {
      const dependency = hspDependencies[index];
      const moduleJsonObj = dependency.getModuleJsonObj();
      const moduleObj = moduleJsonObj?.module;
      if (moduleObj === undefined) {
        continue;
      }
      const hspDeviceTypes = moduleObj.deviceTypes ?? [];

      // 目前default和phone等价
      if (hspDeviceTypes.includes(deviceTypeDefault) || hspDeviceTypes.includes(deviceTypePhone)) {
        hspDeviceTypes.push(deviceTypePhone);
        hspDeviceTypes.push(deviceTypeDefault);
      }
      const diffDeviceTypes = moduleDeviceTypes.filter((deviceType) => {
        return !hspDeviceTypes.includes(deviceType);
      });
      if (diffDeviceTypes.length > 0) {
        _log.printErrorExit('CURRENT_MODULE_NOT_SUPPORT_THE_DEVICE_TYPE',
          [moduleObj.type, moduleObj.name, this.moduleName, diffDeviceTypes.join(', ')]);
      }
    }
  }

  protected checkAbiFilters(): void {
    const abiFilterArr = this.targetService.getBuildOption()?.cangjieOptions?.abiFilters;
    if (abiFilterArr === undefined || abiFilterArr === null) {
      return;
    }
    if (!Array.isArray(abiFilterArr) || abiFilterArr.length === 0) {
      return;
    }
    const abiValues = Object.values(AbiEnum);
    for (const abi of abiFilterArr) {
      if (!abiValues.includes(abi as AbiEnum)) {
        _log.printErrorExit('ABI_FILTERS_NOT_SUPPORTED',
          [abi, buildOptionPath.getTargetBuildOptPath(this.moduleModel, this.targetName, 'abiFilters')],
          [[AbiEnum.ARM64_V8A, AbiEnum.X86_64]]);
      }
    }
  }

  protected checkCjSourceDir(cangjieOptionPath: string, profileFile: string): void {
    if (!cangjieOptionPath.endsWith(CJPM_TOML_NAME)) {
      _log.printErrorExit('CANGJIE_TOML_PATH_CONFIG_ERROR', [CJPM_TOML_NAME, profileFile], [[CJPM_TOML_NAME]]);
    }
    let cjpmTomlPath = '';
    if (path.isAbsolute(cangjieOptionPath)) {
      cjpmTomlPath = cangjieOptionPath;
    } else {
      cjpmTomlPath = path.resolve(this.service.getModuleModel().getProjectDir(), cangjieOptionPath);
    }

    // cjpm config json
    if (!fs.existsSync(cjpmTomlPath)) {
      _log.printErrorExit('CANGJIE_TOML_PATH_CONFIG_ERROR', [CJPM_TOML_NAME, profileFile], [[CJPM_TOML_NAME]]);
    }
  }

  private getCangjieRootPackage(logger: OhosLogger): string {
    if (InjectUtil.isOhosTest() && !this.moduleModel.isHapModule()) {
      const testTomlPath = path.resolve(this.moduleModel.getProjectDir(), 'src',
        DefaultTargetConst.OHOS_TEST_TARGET, 'cangjie', CJPM_TOML_NAME);
      if (fs.existsSync(testTomlPath)) {
        return readPackageName(testTomlPath, logger);
      }
    }
    return readModuleConfigTomlName(this.targetService, logger);
  }

  private getExtensionAbilities(moduleJsonObj: ModuleJson.ModuleOptObj,
    obj2srcEntryListMap: Map<string, string[]>): void {
    const extensionAbilities = moduleJsonObj.module.extensionAbilities;
    const extensionAbilityObjSrcEntryList: string[] = [];
    if (extensionAbilities) {
      for (const orgExtensionAbilityObj of extensionAbilities) {
        if (orgExtensionAbilityObj.srcEntry) {
          extensionAbilityObjSrcEntryList.push(orgExtensionAbilityObj.srcEntry);
        }
      }
      obj2srcEntryListMap.set(this.MODULE_EXTENSION_ABILITIES_SRC_ENTRY, extensionAbilityObjSrcEntryList);
    }
  }

  private getConfigAbilities(moduleJsonObj: ModuleJson.ModuleOptObj, obj2srcEntryListMap: Map<string, string[]>): void {
    const moduleAbilities = moduleJsonObj.module.abilities;
    const abilityObjSrcEntryList: string[] = [];
    if (moduleAbilities?.length) {
      for (const abilityItem of moduleAbilities) {
        if (abilityItem.srcEntry) {
          abilityObjSrcEntryList.push(abilityItem.srcEntry);
        }
      }
      obj2srcEntryListMap.set(this.MODULE_ABILITIES_SRC_ENTRY, abilityObjSrcEntryList);
    }
  }

  private getConfigSrcEntry(obj2srcEntryListMap: Map<string, string[]>): void {
    const insightIntentObjSrcEntryList: string[] = [];
    if (fs.existsSync(this.insightIntentJsonPath)) {
      const insightIntentDataJson = JsonUtil.getJson5Obj(this.insightIntentJsonPath);
      for (const intent of insightIntentDataJson.insightIntents) {
        if (intent.srcEntry) {
          insightIntentObjSrcEntryList.push(intent.srcEntry);
        }
      }
      obj2srcEntryListMap.set(this.INSIGHT_INTENT_SRC_ENTRY, insightIntentObjSrcEntryList);
    }
  }

  private checkCangjieEntry(srcEntry: string, sourceSetModel: SourceSetModel, logger: OhosLogger): void {
    const srcEntryPath = path.isAbsolute(srcEntry) ? srcEntry :
      path.resolve(sourceSetModel.getSourceSetRoot(), srcEntry);
    if (onlyCangjieModule(this.moduleModel.getProjectDir())) {
      const rootPackage = this.getCangjieRootPackage(logger);
      if (validateCangjieEntry(rootPackage, srcEntry)) {
        return;
      }
      const targetSourceSet = sourceSetModel as TargetSourceSetImpl;
      logger.printErrorExit('MODULE_SRC_ENTRY_IS_NOT_ROOT_PACKAGE',
        [srcEntry, targetSourceSet.getModuleTargetRes().getJsonPath()]);
    } else if (!FileUtil.fileExists(srcEntryPath)) {
      const targetSourceSet = sourceSetModel as TargetSourceSetImpl;
      logger.printErrorExit('MODULE_SRC_ENTRY_IS_NOT_FOUND',
        [srcEntry, targetSourceSet.getModuleTargetRes().getJsonPath()], [[], [srcEntry]]);
    }
  }

  private checkCangjieHsp(): void {
    if (this.moduleModel.isHspModule() && onlyCangjieModule(this.moduleModel.getProjectDir())) {
      _log.printErrorExit('ONLY_CANGJIE_HSP_NOT_SUPPORTED_ERROR', [this.moduleModel.getName()]);
    }
  }

  private checkDependenciesMatches(): void {
    const component = getSdkComponent(CANGJIE_NAME, this.targetData.getApiMeta().compileSdkVersion.version);
    if (component === undefined) {
      return;
    }
    const cangjieComponent = new SdkCangjieComponent(component);
    let needExit = false;
    const packageJson = this.isOhpmDependency() ? CommonConst.OH_PACKAGE_JSON5 : CommonConst.PACKAGE_JSON;
    if (cangjieComponent.isEscapeSdk) {
      needExit = this.checkEscapeDepends(packageJson);
    } else {
      const componentVersion = this.sdkInfo.getToolchainsComponentVersion();
      if (componentVersion === undefined) {
        return;
      }
      const componentVersionArr = componentVersion.split('.');
      if (componentVersionArr.length < this.versionPrefixLength) {
        return;
      }
      needExit = this.checkDefaultDepends(packageJson, componentVersionArr);
    }
    if (needExit) {
      _log._printAllExit();
    }
  }

  private checkDefaultDepends(packageJson: string, componentVersionArr: string[]): boolean {
    let needExit = false;
    const directDependencies = this.service.getDirectDependencies();
    const ohModulePath = path.resolve(this.projectModel.getProjectDir(), CommonConst.OH_MODULES).normalize();
    for (const dependency of directDependencies) {
      const dependencyRootPath = dependency.getDependencyRootPath();
      const isNotRemoteCjDepend = dependency.isLocal() || dependency.isSODependency() ||
        !isCangjieHar(dependencyRootPath) || !dependency.isModuleDependency() ||
        !dependencyRootPath.normalize().startsWith(ohModulePath);
      if (isNotRemoteCjDepend) {
        continue;
      }
      if (fs.existsSync(path.join(dependencyRootPath, 'libs', 'compile-info.json'))) {
        needExit = true;
        _log._buildError('The currently used default Cangjie SDK cannot depend on the dependency ' +
          `'${dependency.getDependencyName()}' compiled by the compatible Cangjie SDK at its ${packageJson}.`).
          _record();
      }
      const compileSdkVersion = dependency.getModuleJsonObj()?.app?.compileSdkVersion;
      if (compileSdkVersion === undefined) {
        continue;
      }
      const compileSdkVersionArr = compileSdkVersion.split('.');
      if (compileSdkVersionArr.length < this.versionPrefixLength) {
        continue;
      }
      if (compareVersionsUpTo(compileSdkVersionArr, this.checkCompatibleVersions, this.versionPrefixLength) < 0) {
        needExit = true;
        const devecoVersion = componentVersionArr.slice(0, this.versionPrefixLength).join('.');
        _log._buildError(
          `The current version of DevEco Studio is ${devecoVersion}, and the dependency ` +
          `'${dependency.getDependencyName()}' is required to be compiled with 6.0.1 or later.`).
          _record();
      }
    }
    return needExit;
  }

  private checkEscapeDepends(packageJson: string): boolean {
    let needExit = false;
    const directDependencies = this.service.getDirectDependencies();
    const ohModulePath = path.resolve(this.projectModel.getProjectDir(), CommonConst.OH_MODULES).normalize();
    for (const dependency of directDependencies) {
      const dependencyRootPath = dependency.getDependencyRootPath();
      const isNotRemoteCjDepend = !isCangjieHar(dependencyRootPath) || dependency.isLocal() ||
        dependency.isSODependency() || !dependencyRootPath.normalize().startsWith(ohModulePath);
      if (isNotRemoteCjDepend) {
        continue;
      }
      const compileSdkVersion = dependency.getModuleJsonObj()?.app?.compileSdkVersion;
      if (compileSdkVersion === undefined) {
        continue;
      }
      const compileSdkVersionArr = compileSdkVersion.split('.');
      if (compileSdkVersionArr.length < this.versionPrefixLength) {
        continue;
      }
      if (compareVersionsUpTo(compileSdkVersionArr, this.checkCompatibleVersions, this.versionPrefixLength) < 0) {
        continue;
      }
      if (!fs.existsSync(path.join(dependencyRootPath, 'libs', 'compile-info.json'))) {
        needExit = true;
        _log._buildError(
          'The currently used compatible Cangjie SDK cannot depend on the dependency ' +
          `'${dependency.getDependencyName()}' compiled by the default Cangjie SDK at its ${packageJson}.`).
          _record();
      }
    }
    return needExit;
  }

  private isOhpmDependency(): boolean {
    return this.projectModel ? this.projectModel.isOhpmProject() : this.moduleModel.isOhpmProject();
  }
}
