/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import fs from 'fs';
import path from 'path';
import {BaseCangjieTask} from '../base-cangjie-task';
import {
  DefaultTargetConst,
  Dependency,
  DependencyType,
  HmosSdkLoader,
  OhosLogger,
  OhosSdkLoader,
  ProjectBuildProfile,
  SdkComponentType,
  TargetTaskService,
  TaskNames
} from '../../../types/hvigor-imports';
import {checkIsValid, dependToPlaceHolder, formatForwardSlashPath} from '../../utils/common-utils';
import {
  BIN_DEPENDENCIES,
  CJPM_TOML_NAME,
  DEPENDENCIES,
  NAME,
  PACKAGE,
  PATH_OPTION,
  TARGET,
  VERSION
} from '../../constants/constants';
import {TOML} from '../../utils/toml/toml-export';
import {CangjieTaskNames} from '../cangjie-task-names';
import {
  buildLogRelatePath,
  cangjieSrcRelatePath,
  createDir,
  hasCangjieModule,
  readPackageName,
  syscapApiConfigPath
} from '../../utils/cangjie-file-util';
import {AbiEnum, getTargetByAbi, isValueInAbiEnum} from '../../enums/cangjie-cpu-abi-enum';
import {CjBuildDirConst} from '../../constants/cangjie-build-dir-const';
import {CangjieLogger} from '../../log/cangjie-logger';
import {BitUpdateFlag, BitUpdateFlagUtils} from '../../enums/bit-update-flag';

/**
 * generate toml dependencies
 * No comparison is performed. The original dependency is deleted, and the new dependency is added.
 * Analyze whether it is source code dependency or SO dependency.
 * Use the existing interface of the Hvigor to obtain the local dependency and determine whether the dependency is the
 * source code folder or so+cjo.
 *
 * @since 2024/7/29
 */
export class GenerateTomlDependencies extends BaseCangjieTask {
  private gtdLogger: OhosLogger = CangjieLogger.getLogger(GenerateTomlDependencies.name);
  private oldDependencies: Map<string, CjDependency> = new Map<string, CjDependency>();
  private newDependencies: Map<string, CjDependency> = new Map<string, CjDependency>();
  private readonly _isOhpmDependency: boolean;

  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.GENERATE_TOML_DEPENDENCIES, name: `${CangjieTaskNames.GENERATE_TOML_DEPENDENCIES.name}`});
    this._isOhpmDependency = this.isOhpmDependency();
  }

  initTaskDepends(): void {
    this.declareDepends(TaskNames.Task.PRE_BUILD.name);
  }

  protected async doTaskAction(): Promise<void> {
    this.analyzedDependencies();
    await this.updateSysCapApiConfigs();
  }

  /**
   * 将oh-package.json5的依赖解析成key、依赖模式（本地、har/远程）、路径[]格式
   * har/远程要识别是arm64-v8a(aarch64-linux-ohos)、x86_64(待确认)
   * 判断依赖是否仓颉：源码文件夹、so+cjo
   */
  private analyzedDependencies(): void {
    const harDependencies = this.service.getDirectDependencies();
    for (const dependency of harDependencies) {
      const isUnNeedAnalyzed = dependency.getDependencyType() !== DependencyType.DEPENDENCY_TYPE_HAR ||
        !checkIsValid(dependency.getLastDependencyName()) || !dependency.isModuleDependency() ||
        this.newDependencies.has(dependency.getDependencyName());
      if (isUnNeedAnalyzed) {
        continue;
      }
      if (dependency.isByteCodeHarDependency()) {
        this.analyzedPackDepends(dependency);
      } else {
        this.analyzedSourceDepends(dependency);
      }
    }
    this.analyzedPackDependsDepends();
    const data = fs.readFileSync(this.cjpmTomlPath, 'utf8');
    let tomlData = {} as any;
    const hapToml = new TOML();
    try {
      tomlData = hapToml.parse(data);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this.gtdLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${this.cjpmTomlPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    const sourceDepends = Object.prototype.hasOwnProperty.call(tomlData, DEPENDENCIES) ? tomlData[DEPENDENCIES] : {};
    const targetDepends = Object.prototype.hasOwnProperty.call(tomlData, TARGET) ? tomlData[TARGET] : {};
    const oldDependPath = path.join(this.projectModel.getProjectDir(), buildLogRelatePath(),
      `${this.moduleName}Dependencies.json`);
    if (fs.existsSync(oldDependPath)) {
      this.jsonToMap(fs.readFileSync(oldDependPath).toString());
    }

    const bitUpdateFlag = this.updateDepends(sourceDepends, targetDepends);
    if (BitUpdateFlagUtils.has(bitUpdateFlag, BitUpdateFlag.FIRST)) {
      tomlData[DEPENDENCIES] = sourceDepends;
    }
    if (BitUpdateFlagUtils.has(bitUpdateFlag, BitUpdateFlag.SECOND)) {
      tomlData[TARGET] = targetDepends;
    }
    if (bitUpdateFlag !== 0) {
      fs.writeFileSync(this.cjpmTomlPath, hapToml.stringify(tomlData));
    }
    this.writeOldDependFile();
  }

  private analyzedPackDependsDepends(): void {
    const harDependencies = this.service.getRemoteHarDependencies();
    const dependencyMap = new Map(harDependencies.filter((dependItem) => dependItem.isByteCodeHarDependency()).
      map((dependItem) => [dependItem.getDependencyName(), dependItem]));
    for (const dependency of harDependencies) {
      if (!dependency.isByteCodeHarDependency()) {
        continue;
      }
      const dependencies = dependency.getDependencies();
      if (!checkIsValid(dependencies)) {
        continue;
      }
      for (const [dependencyName, dependencyPath] of Object.entries(dependencies)) {
        if (!checkIsValid(dependencyName) || !checkIsValid(dependencyPath)) {
          continue;
        }
        const childDependency = dependencyMap.get(dependencyName);
        if (childDependency === undefined) {
          continue;
        }
        this.analyzedPackDepends(childDependency);
      }
    }
  }

  private writeOldDependFile(): void {
    const cangjieLogPath = path.join(this.projectModel.getProjectDir(), '.idea', '.deveco', 'cangjie', 'build-logs');
    createDir(cangjieLogPath);
    const bakJsonPath = path.join(cangjieLogPath, `${this.moduleName}Dependencies.json`);
    const obj: Record<string, any> = {};
    this.newDependencies.forEach((value, key) => {
      obj[key] = value;
    });
    fs.writeFileSync(bakJsonPath, JSON.stringify(obj));
  }

  private addNewDepends(dependency: CjDependency, sourceDepends: any, targetDepends: any): BitUpdateFlag {
    let updateFlag = BitUpdateFlag.NONE;
    if (dependency.dependType === DependType.LOCAL_SOURCE) {
      if (!Object.prototype.hasOwnProperty.call(sourceDepends, dependency.tomlName)) {
        sourceDepends[dependency.tomlName] = {path: `${dependency.localDependPaths[0]}`};
        updateFlag = BitUpdateFlagUtils.set(updateFlag, BitUpdateFlag.FIRST);
      }
    } else {
      const newTargetDepends = dependency.targetDepends;
      if (newTargetDepends === null || newTargetDepends === undefined || newTargetDepends.length === 0) {
        return updateFlag;
      }
      if (this.doAddNewDepends(newTargetDepends, targetDepends)) {
        updateFlag = BitUpdateFlagUtils.set(updateFlag, BitUpdateFlag.SECOND);
      }
    }
    return updateFlag;
  }

  private doAddNewDepends(newTargetDepends: TargetDepend[], targetDepends: any): boolean {
    let isUpdate = false;
    for (let i = 0; i < newTargetDepends.length; i++) {
      const newTargetDepend = newTargetDepends[i];
      if (!Object.prototype.hasOwnProperty.call(newTargetDepend, 'targetOs') ||
        !Object.prototype.hasOwnProperty.call(newTargetDepend, 'dependPaths')) {
        continue;
      }
      const targetOs = newTargetDepend.targetOs;
      const newDependPaths = newTargetDepend.dependPaths;
      if (newDependPaths === null || newDependPaths === undefined || newDependPaths.length === 0) {
        continue;
      }
      if (!Object.prototype.hasOwnProperty.call(targetDepends, targetOs)) {
        targetDepends[targetOs] = {};
      }
      if (!Object.prototype.hasOwnProperty.call(targetDepends[targetOs], BIN_DEPENDENCIES)) {
        targetDepends[targetOs][BIN_DEPENDENCIES] = {};
      }
      if (!Object.prototype.hasOwnProperty.call(targetDepends[targetOs][BIN_DEPENDENCIES], PATH_OPTION)) {
        targetDepends[targetOs][BIN_DEPENDENCIES][PATH_OPTION] = [];
      }
      const pathOptions = targetDepends[targetOs][BIN_DEPENDENCIES][PATH_OPTION];
      newDependPaths.filter(newDependPath => !this.hasSamePaths(pathOptions, newDependPath)).
        forEach(newDependPath => {
          pathOptions.push(newDependPath);
          isUpdate = true;
        });
    }
    return isUpdate;
  }

  private removeOldDepends(dependency: CjDependency, sourceDepends: any, targetDepends: any): BitUpdateFlag {
    let updateFlag = BitUpdateFlag.NONE;
    if (dependency.dependType === DependType.LOCAL_SOURCE) {
      if (Object.prototype.hasOwnProperty.call(sourceDepends, dependency.tomlName)) {
        Reflect.deleteProperty(sourceDepends, dependency.tomlName);
        updateFlag = BitUpdateFlagUtils.set(updateFlag, BitUpdateFlag.FIRST);
      }
    } else {
      const oldTargetDepends = dependency.targetDepends;
      if (oldTargetDepends === null || oldTargetDepends === undefined || oldTargetDepends.length === 0) {
        return updateFlag;
      }
      if (this.doRemoveOldDepends(oldTargetDepends, targetDepends)) {
        updateFlag = BitUpdateFlagUtils.set(updateFlag, BitUpdateFlag.FIRST);
      }
    }
    return updateFlag;
  }

  private doRemoveOldDepends(oldTargetDepends: TargetDepend[], targetDepends: any): boolean {
    let isUpdate = false;
    for (let i = 0; i < oldTargetDepends.length; i++) {
      const oldTargetDepend = oldTargetDepends[i];
      if (!Object.prototype.hasOwnProperty.call(targetDepends, oldTargetDepend.targetOs) ||
        !Object.prototype.hasOwnProperty.call(targetDepends[oldTargetDepend.targetOs], BIN_DEPENDENCIES) ||
        !Object.prototype.hasOwnProperty.call(targetDepends[oldTargetDepend.targetOs][BIN_DEPENDENCIES],
          PATH_OPTION)) {
        continue;
      }
      const pathOptions = targetDepends[oldTargetDepend.targetOs][BIN_DEPENDENCIES][PATH_OPTION];
      if (!Array.isArray(pathOptions) || !Object.prototype.hasOwnProperty.call(oldTargetDepend, 'dependPaths')) {
        continue;
      }
      for (const oldDependPath of oldTargetDepend.dependPaths) {
        if (this.removeSamePaths(pathOptions, oldDependPath)) {
          isUpdate = true;
        }
      }
    }
    return isUpdate;
  }

  private analyzedPackDepends(dependency: Dependency): void {
    const dependencyName = dependency.getDependencyName();
    const dependencyRootPath = dependency.getDependencyRootPath();
    if (!fs.existsSync(dependencyRootPath)) {
      return;
    }
    const libDir = path.resolve(dependencyRootPath, 'libs');
    if (!fs.existsSync(libDir)) {
      return;
    }
    const files = fs.readdirSync(libDir);
    const targetDepends: TargetDepend[] = [];
    for (const file of files) {
      const harDependPaths = new Set<string>();
      if (!isValueInAbiEnum(file, AbiEnum)) {
        continue;
      }
      this.collectPathOptions(path.resolve(libDir, file, CjBuildDirConst.BIN_LIBS),
        path.join('libs', file, CjBuildDirConst.BIN_LIBS), harDependPaths);
      if (harDependPaths.size === 0) {
        continue;
      }
      const dependPaths = Array.from(harDependPaths,
        itemPath => formatForwardSlashPath(path.join(`$\{${dependToPlaceHolder(dependencyName)}}`, itemPath)));
      targetDepends.push({
        targetOs: getTargetByAbi(file),
        dependPaths: dependPaths,
      });
    }
    const newDependency: CjDependency = {
      name: dependencyName,
      tomlName: '',
      version: '',
      dependType: DependType.HAR_PACK,
      localDependPaths: [],
      targetDepends: targetDepends,
    };
    this.newDependencies.set(dependencyName, newDependency);
  }

  private analyzedSourceDepends(dependency: Dependency): void {
    const dependencyName = dependency.getDependencyName();
    const dependencyRootPath = dependency.getDependencyRootPath();
    if (!hasCangjieModule(dependencyRootPath)) {
      return;
    }
    let srcPath = '';
    let harTomlPath = path.resolve(dependencyRootPath, srcPath, CJPM_TOML_NAME);
    if (!fs.existsSync(harTomlPath)) {
      srcPath = cangjieSrcRelatePath();
      harTomlPath = path.resolve(dependencyRootPath, srcPath, CJPM_TOML_NAME);
      if (!fs.existsSync(harTomlPath)) {
        return;
      }
    }
    const data = fs.readFileSync(harTomlPath, 'utf8');
    let tomlData = {} as any;
    try {
      tomlData = new TOML().parse(data);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this.gtdLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${harTomlPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    const sourceDependPath = path.join(`$\{${dependToPlaceHolder(dependencyName)}}`, srcPath);
    const newDependency: CjDependency = {
      name: dependencyName,
      tomlName: tomlData[PACKAGE][NAME],
      version: tomlData[PACKAGE][VERSION],
      dependType: DependType.LOCAL_SOURCE,
      localDependPaths: [formatForwardSlashPath(sourceDependPath)],
      targetDepends: [],
    };
    this.newDependencies.set(dependencyName, newDependency);
  }

  private collectPathOptions(dirPath: string, relativePath: string, cjoPathSet: Set<string>): void {
    if (!fs.existsSync(dirPath) || !fs.statSync(dirPath).isDirectory()) {
      return;
    }
    const files = fs.readdirSync(dirPath);
    let hasCjo = false;
    let hasSo = false;
    for (const file of files) {
      const curPath = path.resolve(dirPath, file);
      if (fs.lstatSync(curPath).isDirectory()) {
        this.collectPathOptions(curPath, path.join(relativePath, file), cjoPathSet);
      } else if (file.endsWith('.cjo')) {
        hasCjo = true;
      } else if (file.endsWith('.so')) {
        hasSo = true;
      } else {
        // do nothing
      }
    }
    if (hasCjo && hasSo) {
      cjoPathSet.add(relativePath);
    }
  }

  private jsonToMap(jsonStr: string): void {
    const obj = JSON.parse(jsonStr);
    if (typeof obj !== 'object' || obj === null) {
      return;
    }
    Object.keys(obj).forEach((key) => {
      const objElement = obj[key];
      this.oldDependencies.set(key, objElement === undefined ? {} : objElement);
    });
  }

  private isOhpmDependency(): boolean {
    return this.projectModel ? this.projectModel.isOhpmProject() : this.moduleModel.isOhpmProject();
  }

  private removeSamePaths(pathOptions: any, removePath: string): boolean {
    let isUpdate = false;
    for (let index = pathOptions.length - 1; index >= 0; index--) {
      if (this.areSamePath(pathOptions[index], removePath)) {
        pathOptions.splice(index, 1);
        isUpdate = true;
      }
    }
    return isUpdate;
  }

  private hasSamePaths(pathOptions: any, srcPath: string): boolean {
    for (const pathOption of pathOptions) {
      if (this.areSamePath(pathOption, srcPath)) {
        return true;
      }
    }
    return false;
  }

  private areSamePath(path1: string, path2: string): boolean {
    const normalizedPath1 = path.normalize(path1);
    const normalizedPath2 = path.normalize(path2);
    const absolutePath1 = path.resolve(normalizedPath1);
    const absolutePath2 = path.resolve(normalizedPath2);
    return absolutePath1 === absolutePath2;
  }

  /**
   * 在工程sync的时候，在.idea/.deveco/cangjie目录下生成syscap_api_config.json文件，
   * 编译时透传给cjc，用于对APILevel和Syscap检查
   */
  private async updateSysCapApiConfigs(): Promise<void> {
    const sdkVersion = this.targetData.getApiMeta().compileSdkVersion;
    const components = [SdkComponentType.ETS];
    let hmsDeviceApi = '';
    let ohosDeviceApi = '';
    if (this.sdkInfo.isOhos) {
      ohosDeviceApi = await this.getOhosDeviceApi(sdkVersion, components);
    } else {
      const hosSdkComponents = await HmosSdkLoader.getInstance().getHmsSdkComponents(sdkVersion, components);
      const hmsEtsSdk = hosSdkComponents.get(SdkComponentType.ETS);
      const hmsEtsDir = hmsEtsSdk?.getLocation();
      if (hmsEtsDir === undefined) {
        return;
      }
      hmsDeviceApi = path.join(hmsEtsDir, 'api', 'device-define');
      const ohosSdkComponents = await HmosSdkLoader.getInstance().getHmosSdkComponents(sdkVersion, components);
      const ohosEtsSdk = ohosSdkComponents.get(SdkComponentType.ETS);
      const ohosEtsDir = ohosEtsSdk?.getLocation();
      if (ohosEtsDir === undefined) {
        return;
      }
      ohosDeviceApi = path.join(ohosEtsDir, 'api', 'device-define');
    }

    this.initCangjieSdk();
    if (this.sdkCangjieComponent === undefined) {
      return;
    }

    const modules = this.moduleModel.getAllModules();
    const apiJsonPath = path.join(this.projectModel.getProjectDir(), syscapApiConfigPath());
    const data = new Map();
    const modulesMap = new Map();
    modules.forEach(module => {
      const moduleMap = new Map();
      const deviceSysCapMap = new Map();
      const deviceTypes = module.getDeviceTypes();
      deviceTypes.forEach(async deviceType => {
        const deviceSysCap = this.getDeviceSysCap(hmsDeviceApi, ohosDeviceApi, deviceType);
        deviceSysCapMap.set(deviceType, deviceSysCap);
      });
      const deviceSysCapObj = Object.fromEntries(deviceSysCapMap);
      moduleMap.set('deviceSysCap', deviceSysCapObj);
      const moduleObj = Object.fromEntries(moduleMap);
      modulesMap.set(module.getName(), moduleObj);
      const moduleDir = module.getProjectDir();
      const ohosTestCangjieDir = path.resolve(moduleDir, 'src', DefaultTargetConst.OHOS_TEST_TARGET, 'cangjie');
      const ohosTestTomlPath = path.resolve(ohosTestCangjieDir, CJPM_TOML_NAME);
      const ohosTestPackageName =
          readPackageName(ohosTestTomlPath, this.gtdLogger).replace(/^ohos_app_cangjie_/, '');
      if (ohosTestPackageName !== '') {
        modulesMap.set(ohosTestPackageName, moduleObj);
      }
      const localTestCangjieDir = path.resolve(moduleDir, 'src', 'test', 'cangjie');
      const localTestTomlPath = path.resolve(localTestCangjieDir, CJPM_TOML_NAME);
      const localTestPackageName =
          readPackageName(localTestTomlPath, this.gtdLogger).replace(/^ohos_app_cangjie_/, '');
      if (localTestPackageName !== '') {
        modulesMap.set(localTestPackageName, moduleObj);
      }
    });
    const modulesObj = Object.fromEntries(modulesMap);
    data.set('Modules', modulesObj);
    data.set('apiLevel', this.targetData.getApiMeta().compatibleSdkVersion.fullVersion);
    const dataObj = Object.fromEntries(data);
    createDir(path.dirname(apiJsonPath));
    fs.writeFileSync(apiJsonPath, JSON.stringify(dataObj, null, 2));
  }

  private async getOhosDeviceApi(sdkVersion: ProjectBuildProfile.ApiMeta,
    components: SdkComponentType[]): Promise<string> {
    const sdkComponents = await OhosSdkLoader.getInstance().getOhosSdkComponents(sdkVersion, components);
    let ohosEtsSdk;
    for (const [key, value] of sdkComponents) {
      if (key.getPath() === SdkComponentType.ETS && key.getFullApiVersion().getMajor() === sdkVersion.version) {
        ohosEtsSdk = value;
        break;
      }
    }
    const ohosEtsDir = ohosEtsSdk?.getLocation();
    return ohosEtsDir === undefined ? '' : path.join(ohosEtsDir, 'api', 'device-define');
  }

  private getDeviceSysCap(hmsDeviceApi: string, ohosDeviceApi: string, deviceName: string): string[] {
    const deviceSysCap: string[] = [];
    if (fs.existsSync(hmsDeviceApi)) {
      const hmsEntries = fs.readdirSync(hmsDeviceApi, { withFileTypes: true });
      hmsEntries.forEach(file => {
        if (!file.isDirectory() && file.name.includes(deviceName)) {
          deviceSysCap.push(path.join(hmsDeviceApi, file.name));
        }
      });
    }
    if (fs.existsSync(ohosDeviceApi)) {
      const ohosEntries = fs.readdirSync(ohosDeviceApi, { withFileTypes: true });
      ohosEntries.forEach(file => {
        if (!file.isDirectory() && file.name.includes(deviceName)) {
          deviceSysCap.push(path.join(ohosDeviceApi, file.name));
        }
      });
    }
    return deviceSysCap;
  }

  private updateDepends(sourceDepends: any, targetDepends: any): BitUpdateFlag {
    let updateFlag = BitUpdateFlag.NONE;
    for (const [moduleName, oldDependency] of this.oldDependencies) {
      const newDependency = this.newDependencies.get(moduleName);
      if (newDependency === undefined) {
        updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
      } else {
        if (oldDependency.tomlName !== newDependency.tomlName || oldDependency.dependType !==
          newDependency.dependType) {
          updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
          updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
        } else if (oldDependency.dependType === DependType.LOCAL_SOURCE) {
          updateFlag |= this.updateLocalDepends(oldDependency, newDependency, sourceDepends, targetDepends);
        } else if (oldDependency.dependType === DependType.HAR_PACK) {
          updateFlag |= this.updateTargetDepends(oldDependency, newDependency, sourceDepends, targetDepends);
        } else {
          // do nothing
        }
      }
    }
    for (const [moduleName, newDependency] of this.newDependencies) {
      if (!this.oldDependencies.has(moduleName)) {
        updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
      }
    }
    return updateFlag;
  }

  private updateLocalDepends(oldDependency: CjDependency, newDependency: CjDependency, sourceDepends: any,
    targetDepends: any): BitUpdateFlag {
    let updateFlag = BitUpdateFlag.NONE;
    const oldDependPaths = oldDependency.localDependPaths ?? [];
    const newDependPaths = newDependency.localDependPaths ?? [];
    if (!this.isSameArr(oldDependPaths, newDependPaths)) {
      updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
      updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
    }
    return updateFlag;
  }

  private updateTargetDepends(oldDependency: CjDependency, newDependency: CjDependency, sourceDepends: any,
    targetDepends: any): BitUpdateFlag {
    let updateFlag = BitUpdateFlag.NONE;
    const oldTargetDependMap = new Map(
      (oldDependency.targetDepends ?? []).map(td => [td.targetOs, td.dependPaths ?? []]));
    const newTargetDependMap = new Map(
      (newDependency.targetDepends ?? []).map(td => [td.targetOs, td.dependPaths ?? []]));
    if (oldTargetDependMap.size === newTargetDependMap.size) {
      for (const [targetOs, oldTargetDependPaths] of oldTargetDependMap) {
        const newTargetDependPaths = newTargetDependMap.get(targetOs);
        if (newTargetDependPaths === undefined) {
          updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
          updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
          break;
        }
        if (!this.isSameArr(oldTargetDependPaths, newTargetDependPaths)) {
          updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
          updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
          break;
        }
      }
    } else {
      updateFlag |= this.removeOldDepends(oldDependency, sourceDepends, targetDepends);
      updateFlag |= this.addNewDepends(newDependency, sourceDepends, targetDepends);
    }
    return updateFlag;
  }

  private isSameArr(subDependPaths: string[], dependPaths: string[]): boolean {
    return subDependPaths.length === dependPaths.length &&
      subDependPaths.every(dependPath => dependPaths.includes(dependPath));
  }
}

export enum DependType {
  LOCAL_SOURCE = 'local_source',
  HAR_PACK = 'har_pack',
}

export interface CjDependency {
  name: string;
  version: string;
  tomlName: string;
  dependType: DependType;
  targetDepends: TargetDepend[];
  localDependPaths: string[];
}

export interface TargetDepend {
  dependPaths: string[];
  targetOs: string;
}
