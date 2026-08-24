/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import * as path from 'path';
import * as fs from 'fs';
import {InjectUtil, OhosLogger, TargetTaskService} from '../../types/hvigor-imports';
import {
  BIN_DEPENDENCIES,
  C,
  CJPM_TOML_NAME,
  DEMAND_BIN_PACKAGES,
  DEPENDENCIES,
  FFI,
  GIT,
  GIT_COMMIT_ID,
  NAME,
  PACKAGE,
  PACKAGE_OPTION,
  PACKAGE_REQUIRES,
  PATH,
  PATH_OPTION,
  REQUIRES,
  TARGET
} from '../constants/constants';
import {checkIsValid, getCjpmConfigPath, getDemandSrcPackages, replaceWithEnv} from '../utils/common-utils';
import {TOML} from '../utils/toml/toml-export';
import {
  copySpecifiedFiles,
  createDir,
  linkFile,
  readModuleConfigTomlName,
  walkSyncWithFilter
} from '../utils/cangjie-file-util';
import {getTargetByAbi} from '../enums/cangjie-cpu-abi-enum';
import {CjBuildDirConst} from '../constants/cangjie-build-dir-const';
import {CangjieLogger} from '../log/cangjie-logger';

/**
 * cangjie dependent dynamic library collection
 *
 * @since 2024/1/6
 */
export class CangjieDependency {
  private _log: OhosLogger = CangjieLogger.getLogger(CangjieDependency.name);

  private includedModules: string[] = [];

  private existed: string[] = [];

  private _srcFilterHarPaths: string[] = [];

  private _binFilterHarPaths: string[] = [];

  private _packageName = '';

  private readonly abi: string;

  private readonly dependsEnv: NodeJS.ProcessEnv = {};

  private readonly targetTaskService: TargetTaskService;

  private dependenciesMap: Map<string, string[]> = new Map();

  private demandPackages: string[] = [];

  constructor(abi: string, dependsEnv: NodeJS.ProcessEnv, targetTaskService: TargetTaskService) {
    this.abi = abi;
    this.dependsEnv = dependsEnv;
    this.targetTaskService = targetTaskService;
    this.demandPackages = this.initDemandSrcPackages();
  }

  set binFilterHarPaths(value: string[]) {
    this._binFilterHarPaths = value;
  }

  set srcFilterHarPaths(value: string[]) {
    this._srcFilterHarPaths = value;
  }

  set packageName(value: string) {
    this._packageName = value;
  }

  public getTargetRequires(moduleData: any, workspacePath: string, excludeFiles: string[]): void {
    const targetPlatform = getTargetByAbi(this.abi);
    if (!Object.prototype.hasOwnProperty.call(moduleData, TARGET) ||
      !Object.prototype.hasOwnProperty.call(moduleData[TARGET], targetPlatform)) {
      return;
    }
    if (Object.prototype.hasOwnProperty.call(moduleData[TARGET][targetPlatform], BIN_DEPENDENCIES)) {
      this.getRequires(moduleData[TARGET][targetPlatform][BIN_DEPENDENCIES], excludeFiles, workspacePath, moduleData);
    }
    this.getDependencies(moduleData[TARGET][targetPlatform], workspacePath, excludeFiles);
  }

  public getPackageRequires(moduleData: any, workspacePath: string, excludeFiles: string[]): void {
    if (!Object.prototype.hasOwnProperty.call(moduleData, PACKAGE_REQUIRES)) {
      return;
    }
    this.getRequires(moduleData[PACKAGE_REQUIRES], excludeFiles, workspacePath, moduleData);
  }

  public getForeignCRequires(moduleData: any, workspacePath: string): void {
    if (!Object.prototype.hasOwnProperty.call(moduleData, FFI) ||
      !Object.prototype.hasOwnProperty.call(moduleData[FFI], C)) {
      return;
    }
    const foreignRequires = moduleData[FFI][C];
    for (const requireItem in foreignRequires) {
      if (!Object.prototype.hasOwnProperty.call(foreignRequires, requireItem)) {
        continue;
      }
      const foreignRequireObj = foreignRequires[requireItem];
      if (!Object.prototype.hasOwnProperty.call(foreignRequireObj, PATH)) {
        continue;
      }
      let pathModule = foreignRequireObj[PATH];
      const normalizePath = path.normalize(pathModule);
      if (!path.isAbsolute(normalizePath)) {
        pathModule = path.join(workspacePath, normalizePath);
      }
      pathModule = path.normalize(pathModule);
      if (pathModule.toString().length === 0) {
        continue;
      }
      const libPath = path.join(pathModule, `lib${requireItem}.so`);
      if (fs.existsSync(libPath)) {
        this.addDependencies(path.join(moduleData[PACKAGE][NAME], FFI), libPath);
      }
    }
  }

  public getDependencies(moduleData: any, workspacePath: string, excludeFiles: string[]): void {
    if (!Object.prototype.hasOwnProperty.call(moduleData, DEPENDENCIES)) {
      return;
    }
    for (const requireItem in moduleData[DEPENDENCIES]) {
      if (!Object.prototype.hasOwnProperty.call(moduleData[DEPENDENCIES], requireItem)) {
        continue;
      }
      const requireItems = moduleData[DEPENDENCIES][requireItem];
      if (Object.prototype.hasOwnProperty.call(requireItems, PATH)) {
        let pathModule = requireItems[PATH];
        pathModule = path.normalize(pathModule);
        if (!path.isAbsolute(pathModule)) {
          pathModule = path.normalize(path.join(workspacePath, pathModule));
        }
        this.doFindAllDependencies(pathModule, excludeFiles);
      } else if (Object.prototype.hasOwnProperty.call(requireItems, GIT)) {
        const pathModule = this.getPathByLockFile(workspacePath, requireItem);
        if (checkIsValid(pathModule)) {
          this.doFindAllDependencies(path.normalize(pathModule), excludeFiles);
        }
      } else {
        const repoVersion = requireItems[requireItem];
        const cjpmConfigPath = getCjpmConfigPath('repository');
        const pathModule = path.join(cjpmConfigPath, `${requireItem}-${repoVersion}`);
        if (checkIsValid(pathModule)) {
          this.doFindAllDependencies(path.normalize(pathModule), excludeFiles);
        }
      }
    }
  }

  public doFindAllDependencies(workspacePathUriParam: string, excludeFiles: string[]): void {
    const workspacePathUri = path.normalize(workspacePathUriParam);
    if (!fs.existsSync(workspacePathUri)) {
      this._log.printErrorExit('TOML_DEPENDENCIES_PATH_NOT_EXIST', [workspacePathUri]);
    }
    if (this.existed.includes(workspacePathUri) || this._srcFilterHarPaths.includes(workspacePathUri)) {
      return;
    }
    this.existed.push(workspacePathUri);
    const tomlPath = path.join(workspacePathUri, CJPM_TOML_NAME);
    if (!fs.existsSync(tomlPath)) {
      return;
    }
    const data = fs.readFileSync(tomlPath, 'utf8');
    let tomlData = {} as any;
    try {
      const formatData = replaceWithEnv(data, this.dependsEnv);
      tomlData = new TOML().parse(formatData);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this._log.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${tomlPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    this.includedModules.push(tomlData[PACKAGE][NAME]);
    this.getDependencies(tomlData, workspacePathUri, excludeFiles);
    if (!this._binFilterHarPaths.includes(workspacePathUri)) {
      this.getPackageRequires(tomlData, workspacePathUri, excludeFiles);
      this.getTargetRequires(tomlData, workspacePathUri, excludeFiles);
      this.getForeignCRequires(tomlData, workspacePathUri);
    }
  }

  public getPathByLockFile(workspacePath: string, moduleName: string): string {
    const cjpmConfigPath = getCjpmConfigPath('git');
    const cjpmLockPath = path.join(workspacePath, 'cjpm.lock');
    try {
      fs.accessSync(cjpmLockPath, fs.constants.R_OK);
    } catch (err) {
      this._log.warn(`The ${cjpmLockPath} file does not exist.`);
      return '';
    }
    const data = replaceWithEnv(fs.readFileSync(cjpmLockPath, 'utf8'), this.dependsEnv);
    let moduleLockData = {} as any;
    try {
      moduleLockData = new TOML().parse(data);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this._log.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${cjpmLockPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    if (Object.prototype.hasOwnProperty.call(moduleLockData, REQUIRES) &&
      Object.prototype.hasOwnProperty.call(moduleLockData[REQUIRES], moduleName) &&
      checkIsValid(moduleLockData[REQUIRES][moduleName][GIT_COMMIT_ID])) {
      return path.join(cjpmConfigPath, moduleName, moduleLockData[REQUIRES][moduleName][GIT_COMMIT_ID]);
    }
    return '';
  }

  public async doCopyCompileLibs(folderPath: string, parentFolder: string, isFilter: boolean, destPath: string,
    isHar: boolean): Promise<void> {
    if (!checkIsValid(folderPath) || !fs.existsSync(folderPath)) {
      return;
    }
    const files = fs.readdirSync(folderPath);
    for (const file of files) {
      const filePath = path.join(folderPath, file);
      const isDirectory = fs.statSync(filePath).isDirectory();
      if (isDirectory) {
        const isUnNeedCopy = (isFilter && !this.includedModules.includes(file)) || file === '.cached' ||
          this.demandPackages.includes(file);
        if (isUnNeedCopy) {
          continue;
        }
        await this.doCopyCompileLibs(filePath, path.join(parentFolder, file), false, destPath, isHar);
        continue;
      }
      const isNeedCopyHarSo = isHar && (file.endsWith('.so') || file.endsWith('.cjo'));
      if (isNeedCopyHarSo) {
        const fileDir = path.join(destPath, CjBuildDirConst.BIN_LIBS, parentFolder);
        await linkFile(filePath, path.join(fileDir, file));
        continue;
      }
      if (file.endsWith('.so')) {
        await linkFile(filePath, path.join(destPath, file));
      }
    }
  }

  public async doCopyDependencies(buildOutputDir: string, isHarModule: boolean): Promise<void> {
    for (const [srcFile, files] of this.dependenciesMap) {
      createDir(buildOutputDir);
      for (const file of files) {
        await this.copyLibFiles(file, buildOutputDir, path.basename(srcFile), isHarModule);
      }
    }
  }

  public async doCopyMacroCjoForBinHar(folderPath: string, destPath: string): Promise<void> {
    if (!checkIsValid(folderPath) || !fs.existsSync(folderPath)) {
      return;
    }
    const macroCjos = walkSyncWithFilter(folderPath, '.cjo', this.demandPackages);
    await copySpecifiedFiles(macroCjos, folderPath, destPath);
  }

  private async copyLibFiles(file: string, buildOutputDir: string, dirName: string,
    isHarModule: boolean): Promise<void> {
    const fileName = path.basename(file);
    if (fs.statSync(file).isDirectory()) {
      const childFiles = fs.readdirSync(file);
      const soFilterLibs = childFiles.filter((childFile) => childFile.endsWith('.so'));
      for (const childFile1 of soFilterLibs) {
        await linkFile(path.join(file, childFile1), path.join(buildOutputDir, path.basename(childFile1)));
      }
    } else {
      if (file.endsWith('.so')) {
        await linkFile(file, path.join(buildOutputDir, fileName));
      } else if (isHarModule) {
        const fileDir = path.join(buildOutputDir, dirName);
        await linkFile(file, path.join(fileDir, fileName));
      } else {
        // do nothing
      }
    }
  }

  private addDependencies(key: string, value: string): void {
    if (this.dependenciesMap.has(key)) {
      this.dependenciesMap.get(key)?.push(value);
    } else {
      const libs: string[] = [];
      libs.push(value);
      this.dependenciesMap.set(key, libs);
    }
  }

  private getRequires(packageRequires: any, excludeFiles: string[], workspacePath: string, moduleData: any): void {
    this.getPathOptions(packageRequires, excludeFiles, workspacePath, moduleData);
    this.getPackageOptions(packageRequires, workspacePath, moduleData);
  }

  private getPackageOptions(packageRequires: any, workspacePath: string, moduleData: any): void {
    if (Object.prototype.hasOwnProperty.call(packageRequires, PACKAGE_OPTION)) {
      const packageOptions = packageRequires[PACKAGE_OPTION];
      for (const requireItem in packageOptions) {
        if (!Object.prototype.hasOwnProperty.call(packageOptions, requireItem)) {
          continue;
        }
        let packageOption = packageOptions[requireItem];
        const normalizePath = path.normalize(packageOption);
        if (!path.isAbsolute(normalizePath)) {
          packageOption = path.join(workspacePath, normalizePath);
        }
        packageOption = path.normalize(packageOption);
        if (packageOption.toString().length === 0) {
          continue;
        }
        const libPath = path.join(path.dirname(packageOption), `lib${requireItem}.so`);
        if (fs.existsSync(libPath)) {
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION), libPath);
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION, requireItem), packageOption);
        }
      }
    }
  }

  private getPathOptions(packageRequires: any, excludeFiles: string[], workspacePath: string, moduleData: any): void {
    if (Object.prototype.hasOwnProperty.call(packageRequires, PATH_OPTION)) {
      const pathOptions = packageRequires[PATH_OPTION];
      for (let i = 0; i < pathOptions.length; i++) {
        let pathOption = pathOptions[i];
        if (this._binFilterHarPaths.includes(path.normalize(pathOption)) ||
          excludeFiles.some(excludeFile => path.resolve(pathOption).startsWith(path.resolve(excludeFile)))) {
          continue;
        }
        const normalizePath = path.normalize(pathOption);
        if (!path.isAbsolute(normalizePath)) {
          pathOption = path.join(workspacePath, normalizePath);
        }
        if (pathOption[pathOption.length - 1] === path.sep) {
          pathOption = pathOption.substring(0, pathOption.length - 1);
        }
        pathOption = path.normalize(pathOption);
        if (DEMAND_BIN_PACKAGES.some(excludeFile => pathOption.endsWith(path.normalize(`/${excludeFile}`)))) {
          continue;
        }
        if (fs.existsSync(pathOption)) {
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION), pathOption);
        }
      }
    }
  }

  private initDemandSrcPackages(): string[] {
    const rootTomlNames = new Set<string>([this.packageName]);
    if (InjectUtil.isOhosTest()) {
      const moduleConfigTomlName = readModuleConfigTomlName(this.targetTaskService, this._log);
      if (checkIsValid(moduleConfigTomlName)) {
        rootTomlNames.add(moduleConfigTomlName);
      }
    }
    return getDemandSrcPackages(rootTomlNames);
  }
}
