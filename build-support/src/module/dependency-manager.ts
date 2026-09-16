/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {BasePackageResolver, ModuleModel, ProjectModel} from '../../types/hvigor-imports';
import {
  CommonConst,
  ConfigJson,
  DependencyType,
  hvigorCore,
  Json5Reader,
  ModuleJson,
  ModuleType,
  NpmPackageResolver,
  OhosLogger,
  OhpmPackageResolver
} from '../../types/hvigor-imports';
import fs from 'fs';
import path from 'path';
import type {PackageJson} from 'type-fest';
import {CangjieLogger} from '../log/cangjie-logger';
import ConfigOptObj = ConfigJson.ConfigOptObj;
import ModuleOptObj = ModuleJson.ModuleOptObj;

export type OhosPackageJson = PackageJson & {
  ohos?: {
    org: string;
    artifactType: string;
  };
  artifactType?: string;
};

const log: OhosLogger = CangjieLogger.getLogger('dependency-manager');
const ohosLogger: OhosLogger = OhosLogger.getLogger('dependency-manager');

interface FileInfo {
  timeStamp: number;
  fileContent: any;
}

const json5Map: Map<string, FileInfo> = new Map();

function getJson5Obj(filePath: string): any {
  const timeStamp = fs.statSync(filePath).mtime.getTime();
  if (json5Map.get(filePath)?.timeStamp !== timeStamp) {
    json5Map.set(filePath, {
      timeStamp: timeStamp,
      fileContent: Json5Reader.getJson5Obj(filePath),
    });
  }

  // Safety: 上面已经保证curPkgJsonPath对应的value非空
  const fileInfo = json5Map.get(filePath);
  return fileInfo === undefined ? {} : fileInfo.fileContent;
}

/**
 * 管理har的依赖，提供获取当前模块依赖的数据信息
 *
 * @since 2024/6/26
 */
export class DependencyManager {
  private _allDependency: Set<string> = new Set<string>();
  private readonly _model: ModuleModel | ProjectModel;
  private readonly _projectModel: ProjectModel | undefined;
  private readonly _moduleName: string;
  private readonly _isFaMode: boolean;
  private readonly _isOhpmDependency: boolean;

  constructor(isFaMode: boolean, model: ModuleModel | ProjectModel, project?: ProjectModel) {
    this._model = model;
    this._projectModel = project;
    this._moduleName = model.getName();
    this._isFaMode = isFaMode;
    this._isOhpmDependency = this.isOhpmDependency();
  }

  checkModelDependencyInfo(): void {
    this._allDependency.clear();
    const packagePath: string = this._isOhpmDependency ?
      this._model.getOhPackageJson5Path() : this._model.getPackageJsonPath();
    const modulePkgObj = {moduleName: this._moduleName, modulePkgJsonPath: packagePath};
    this.collectHarDependency(modulePkgObj, true, true, true);
  }

  private isOhpmDependency(): boolean {
    return this._projectModel ? this._projectModel.isOhpmProject() : this._model.isOhpmProject();
  }

  private collectHarDependency(modulePkgObj: ModulePkgObj, collectLocalModules = true, ifCollectHsp = false,
    ifCollectDevDependenciesParam = false): void {
    const pkgQueue: ModulePkgObj[] = [modulePkgObj];
    let ifCollectDevDependencies = ifCollectDevDependenciesParam;
    while (pkgQueue.length > 0) {
      // queue中存放依赖中package.json的路径和模块名称
      const curModulePkgObj = pkgQueue.shift();
      if (curModulePkgObj === undefined || !fs.existsSync(curModulePkgObj.modulePkgJsonPath)) {
        continue;
      }
      log.debug(`modulePkgJsonPath: ${curModulePkgObj.modulePkgJsonPath}`);
      const curPkgObj = getJson5Obj(curModulePkgObj.modulePkgJsonPath);
      if (curPkgObj === null || curPkgObj === undefined) {
        continue;
      }

      // pkgName 代表package.json中dependency中的依赖key
      for (const pkgName in curPkgObj.dependencies) {
        if (!Object.prototype.hasOwnProperty.call(curPkgObj.dependencies, pkgName)) {
          continue;
        }
        this._allDependency.add(`${curModulePkgObj.modulePkgJsonPath}@${pkgName}`);
        this.collectDependency(pkgName, curModulePkgObj, pkgQueue, collectLocalModules, ifCollectHsp);
      }
      for (const pkgName in curPkgObj.dynamicDependencies) {
        if (!Object.prototype.hasOwnProperty.call(curPkgObj.dynamicDependencies, pkgName)) {
          continue;
        }
        this._allDependency.add(`${curModulePkgObj.modulePkgJsonPath}@${pkgName}`);
        this.collectDependency(pkgName, curModulePkgObj, pkgQueue, collectLocalModules, ifCollectHsp);
      }

      // 语境信息表也要收集直接依赖的devDependencies和devDependencies包中的dependencies和dynamicDependencies
      if (!ifCollectDevDependencies) {
        continue;
      }
      for (const pkgName in curPkgObj.devDependencies) {
        if (!Object.prototype.hasOwnProperty.call(curPkgObj.devDependencies, pkgName)) {
          continue;
        }
        this._allDependency.add(`${curModulePkgObj.modulePkgJsonPath}@${pkgName}`);
        this.collectDependency(pkgName, curModulePkgObj, pkgQueue, collectLocalModules, ifCollectHsp);
      }
      ifCollectDevDependencies = false;
    }
  }

  private collectDependency(pkgName: string, curModulePkgObj: ModulePkgObj, pkgQueue: ModulePkgObj[],
    collectLocalModules: boolean, ifCollectHsp: boolean): void {
    // 该依赖npm包的package.json路径
    const packageResolver: BasePackageResolver = this._isOhpmDependency ?
      new OhpmPackageResolver() : new NpmPackageResolver();
    const devPkgJsonPath = packageResolver.resolvePackagePath(pkgName,
      path.dirname(curModulePkgObj.modulePkgJsonPath), ohosLogger);
    if (devPkgJsonPath === undefined || !fs.existsSync(devPkgJsonPath)) {
      log.printErrorExit('DEPENDENCY_NOT_INSTALLED_ERROR',
        [this._moduleName, this._isOhpmDependency ? CommonConst.OH_PACKAGE_JSON5 : CommonConst.PACKAGE_JSON]);
      return;
    }
    const devPkgJsonObj: PackageJson = getJson5Obj(devPkgJsonPath);
    const dependencyType =
      this.getHarmonyDependencyType(devPkgJsonPath, devPkgJsonObj, curModulePkgObj.modulePkgJsonPath, pkgName) ??
      this.getSODependencyType(devPkgJsonObj.name) ??
      (this._allDependency.has(`${devPkgJsonPath}@${pkgName}`) ? undefined : DependencyType.DEPENDENCY_TYPE_OTHER);
    if (dependencyType === undefined || this.isDynamicDepend(dependencyType)) {
      return;
    }
    const pkgPath = path.dirname(devPkgJsonPath);
    this._allDependency.add(`${devPkgJsonPath}@${pkgName}`);

    // 这里由于时序问题不能用ModuleModel来获取所有的子模块,只需要HvigorNode路径可以使用project对象获取
    const modules = hvigorCore.getProject()?.getAllSubModules();
    if (!modules) {
      return;
    }

    // 收集项目中所有模块的srcPath，跟pkgPath进行比对,识别出本地依赖于远程依赖，
    // 判断依赖的路径与模块的路径是否相同，硬链接方案已废弃，不再判断ino值
    const moduleObj = {moduleName: '', modulePkgJsonPath: devPkgJsonPath};
    for (const module of modules) {
      if (module.getNodeDir() !== pkgPath) {
        continue;
      }
      if (!collectLocalModules) {
        return;
      }
      moduleObj.moduleName = module.getName();
      break;
    }

    // hsp直接剪枝，不收集依赖的hsp的依赖，har和npm、ohpm包都需要继续向下收集
    if (this.isNeedCollect(dependencyType, ifCollectHsp)) {
      pkgQueue.push(moduleObj);
    }
  }

  private isDynamicDepend(dependencyType: DependencyType): boolean {
    return dependencyType === DependencyType.DEPENDENCY_TYPE_HAP || dependencyType ===
      DependencyType.DEPENDENCY_TYPE_SO;
  }

  private isNeedCollect(dependencyType: DependencyType, ifCollectHsp: boolean): boolean {
    return dependencyType === DependencyType.DEPENDENCY_TYPE_HAR ||
      dependencyType === DependencyType.DEPENDENCY_TYPE_OTHER || ifCollectHsp;
  }

  /**
   * 通过config.json/module.json5判断当前模块是否为鸿蒙依赖（har/hsp）
   *
   * @param {string} pkgPath 当前模块的路径
   * @returns {boolean}
   * @private
   */
  private getDependencyTypeByProfile(pkgPath: string): DependencyType | undefined {
    const module5File: string = path.resolve(pkgPath, 'src', 'main', CommonConst.MODULE_JSON5);
    const moduleFile: string = path.resolve(pkgPath, 'src', 'main', CommonConst.MODULE_JSON);
    const configFile: string = path.resolve(pkgPath, 'src', 'main', CommonConst.CONFIG_JSON);
    return this.checkHarOrHspStatus(module5File, false) ??
      this.checkHarOrHspStatus(moduleFile, false) ??
      this.checkHarOrHspStatus(configFile, true);
  }

  /**
   * 检查当前模块的moduleType是否标记为har或hsp，且model与hap是否一致
   *
   * @param {string} runtimeJson config.json/module.json5
   * @param {boolean} harIsFAMode har包是否为FA model
   * @returns {boolean}
   * @private
   */
  private checkHarOrHspStatus(runtimeJson: string, harIsFAMode: boolean): DependencyType | undefined {
    if (!fs.existsSync(runtimeJson)) {
      return undefined;
    }
    const profileJson: ModuleOptObj | ConfigOptObj = getJson5Obj(runtimeJson);

    // hsp只支持stage模型
    const isHsp = (profileJson as ModuleOptObj).module?.type === ModuleType.Shared;
    const isHar = (profileJson as ModuleOptObj).module?.type === ModuleType.Har ||
      (profileJson as ConfigOptObj).module?.distro?.moduleType === ModuleType.Har;
    const isHap = (profileJson as ModuleOptObj).module?.type === ModuleType.Entry ||
      (profileJson as ModuleOptObj).module?.type === ModuleType.Feature ||
      (profileJson as ConfigOptObj).module?.distro?.moduleType === ModuleType.Entry ||
      (profileJson as ConfigOptObj).module?.distro?.moduleType === ModuleType.Feature;
    return this.getDependencyType(isHar, isHsp, harIsFAMode, isHap);
  }

  private getDependencyType(isHar: boolean, isHsp: boolean, harIsFAMode: boolean,
    isHap: boolean): DependencyType | undefined {
    // 发现har或hsp包的模型与hap不同
    if ((isHar || isHsp) && (this._isFaMode !== harIsFAMode)) {
      return undefined;
    }
    if (isHsp) {
      return DependencyType.DEPENDENCY_TYPE_HSP;
    }
    if (isHar) {
      return DependencyType.DEPENDENCY_TYPE_HAR;
    }
    if (isHap) {
      return DependencyType.DEPENDENCY_TYPE_HAP;
    }
    return undefined;
  }

  /**
   * 1. 根据config.json/module.json5判断是不是鸿蒙依赖（har/hsp）
   * 2. 根据包管理的关键特征判断是否是har报
   * 3. 考虑循环依赖，用set过滤重复项，向queue中添加package.json路径
   *
   * @param {string} pkgJsonPath 当前模块依赖的npm的package.json/oh-package.json5路径
   * @param {OhosPackageJson} devPkgJsonObj 当前模块依赖的package.json对象
   * @param curPkgJsonPath
   * @param pkgName
   * @returns {boolean}
   * @private
   */
  private getHarmonyDependencyType(pkgJsonPath: string, devPkgJsonObj: OhosPackageJson,
    curPkgJsonPath: string, pkgName: string): DependencyType | undefined {
    // package.json/oh-package.json5不存在时,说明不是har包
    if (!pkgJsonPath || devPkgJsonObj === null) {
      return undefined;
    }
    const pkgPath = path.dirname(pkgJsonPath);
    const isHarPack = this.isHarForDiffPackManagement(pkgPath, devPkgJsonObj);
    const dependencyTypeByProfile = this.getDependencyTypeByProfile(pkgPath);
    const checkType = isHarPack && this._allDependency.has(`${pkgJsonPath}@${pkgName}`) && pkgJsonPath ===
      curPkgJsonPath && dependencyTypeByProfile === DependencyType.DEPENDENCY_TYPE_HSP;
    if (checkType) {
      return undefined;
    }
    if (!isHarPack || !devPkgJsonObj.name || this._allDependency.has(`${pkgJsonPath}@${pkgName}`)) {
      return undefined;
    }
    return dependencyTypeByProfile;
  }

  /**
   * npm和ohpm的不同包管理机制下,判断一个依赖是否为har包
   * 1.ohpm只需要判断依赖中是否包含oh-package.json5
   * 2.npm需要判断package.json5中是否包含ohos字段
   *
   * @param {string} pkgPath
   * @param {OhosPackageJson} devPkgJsonPathObj
   * @returns {boolean}
   * @private
   */
  private isHarForDiffPackManagement(pkgPath: string, devPkgJsonPathObj: OhosPackageJson): boolean {
    if (this._isOhpmDependency) {
      return fs.existsSync(pkgPath);
    } else {
      return fs.existsSync(pkgPath) && devPkgJsonPathObj.ohos !== undefined;
    }
  }

  private getSODependencyType(pkgName: string | undefined): DependencyType | undefined {
    return pkgName?.includes('.so') ? DependencyType.DEPENDENCY_TYPE_SO : undefined;
  }
}

interface ModulePkgObj {
  moduleName: string;
  modulePkgJsonPath: string;
}
