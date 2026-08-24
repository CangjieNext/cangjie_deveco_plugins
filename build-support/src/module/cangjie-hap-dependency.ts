/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import * as path from 'path';
import * as fs from 'fs';
import {OhosLogger} from '../../types/hvigor-imports';
import {
  BIN_DEPENDENCIES,
  C,
  CJPM_TOML_NAME, DEMAND_BIN_PACKAGES,
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
import {checkIsValid, getCjpmConfigPath, replaceWithEnv} from '../utils/common-utils';
import {TOML} from '../utils/toml/toml-export';
import {createDir, linkFile} from '../utils/cangjie-file-util';
import {getTargetByAbi} from '../enums/cangjie-cpu-abi-enum';

/**
 * cangjie dependent dynamic library collection
 *
 * @since 2024/1/6
 */
export class CangjieHapDependency {
  private _log: OhosLogger = OhosLogger.getLogger(CangjieHapDependency.name);

  private includedModules: string[] = [];

  private existed: string[] = [];

  private readonly abi: string;

  private dependenciesMap: Map<string, string[]> = new Map();

  private readonly dependsEnv: any = {};

  constructor(abi: string, dependsEnv: any) {
    this.abi = abi;
    this.dependsEnv = dependsEnv;
  }

  public getTargetRequires(moduleTomlData: any, workspacePath: string, excludeFiles: string[]): void {
    const targetPlatform = getTargetByAbi(this.abi);
    if (!Object.prototype.hasOwnProperty.call(moduleTomlData, TARGET) ||
      !Object.prototype.hasOwnProperty.call(moduleTomlData[TARGET], targetPlatform)) {
      return;
    }
    if (Object.prototype.hasOwnProperty.call(moduleTomlData[TARGET][targetPlatform], BIN_DEPENDENCIES)) {
      this.getRequires(moduleTomlData[TARGET][targetPlatform][BIN_DEPENDENCIES], excludeFiles, workspacePath,
        moduleTomlData);
    }
    this.getDependencies(moduleTomlData[TARGET][targetPlatform], workspacePath, excludeFiles);
  }

  public getPackageRequires(moduleData: any, workspacePath: string, excludeFiles: string[]): void {
    if (!Object.prototype.hasOwnProperty.call(moduleData, PACKAGE_REQUIRES)) {
      return;
    }
    this.getRequires(moduleData[PACKAGE_REQUIRES], excludeFiles, workspacePath, moduleData);
  }

  public getForeignCRequires(moduleTomlData: any, workspacePath: string): void {
    if (!Object.prototype.hasOwnProperty.call(moduleTomlData, FFI) ||
      !Object.prototype.hasOwnProperty.call(moduleTomlData[FFI], C)) {
      return;
    }
    const fficRequires = moduleTomlData[FFI][C];
    for (const requireItem in fficRequires) {
      if (!Object.prototype.hasOwnProperty.call(fficRequires, requireItem)) {
        continue;
      }
      const fficRequireObj = fficRequires[requireItem];
      if (!Object.prototype.hasOwnProperty.call(fficRequireObj, PATH)) {
        continue;
      }
      let fficPath = fficRequireObj[PATH];
      const normalizePath = path.normalize(fficPath);
      if (!path.isAbsolute(normalizePath)) {
        fficPath = path.join(workspacePath, normalizePath);
      }
      fficPath = path.normalize(fficPath);
      if (fficPath.toString().length === 0) {
        continue;
      }
      const libPath = path.join(fficPath, `lib${requireItem}.so`);
      if (fs.existsSync(libPath)) {
        this.addDependencies(path.join(moduleTomlData[PACKAGE][NAME], FFI), libPath);
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
      this.includedModules.push(requireItem);
      const dependsItems = moduleData[DEPENDENCIES][requireItem];
      if (Object.prototype.hasOwnProperty.call(dependsItems, PATH)) {
        let pathModule = dependsItems[PATH];
        pathModule = path.normalize(pathModule);
        if (!path.isAbsolute(pathModule)) {
          pathModule = path.normalize(path.join(workspacePath, pathModule));
        }
        this.doFindAllDependencies(pathModule, excludeFiles);
      } else if (Object.prototype.hasOwnProperty.call(dependsItems, GIT)) {
        const pathModule = this.getPathByLockFile(workspacePath, requireItem);
        if (checkIsValid(pathModule)) {
          this.doFindAllDependencies(path.normalize(pathModule), excludeFiles);
        }
      } else {
        const repoVersion = dependsItems[requireItem];
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
    if (this.existed.includes(workspacePathUri)) {
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
    this.getPackageRequires(tomlData, workspacePathUri, excludeFiles);
    this.getTargetRequires(tomlData, workspacePathUri, excludeFiles);
    this.getForeignCRequires(tomlData, workspacePathUri);
  }

  public getPathByLockFile(workspacePath: string, moduleName: string): string {
    const cjpmLockPath = path.join(workspacePath, 'cjpm.lock');
    try {
      fs.accessSync(cjpmLockPath, fs.constants.R_OK);
    } catch (err) {
      this._log.warn(`The ${cjpmLockPath} file does not exist.`);
      return '';
    }
    const data = replaceWithEnv(fs.readFileSync(cjpmLockPath, 'utf8'), this.dependsEnv);
    let cjpmLockData = {} as any;
    try {
      cjpmLockData = new TOML().parse(data);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this._log.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${cjpmLockPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    if (Object.prototype.hasOwnProperty.call(cjpmLockData, REQUIRES) &&
      Object.prototype.hasOwnProperty.call(cjpmLockData[REQUIRES], moduleName) &&
      checkIsValid(cjpmLockData[REQUIRES][moduleName][GIT_COMMIT_ID])) {
      const gitConfigPath = getCjpmConfigPath('git');
      return path.join(gitConfigPath, moduleName, cjpmLockData[REQUIRES][moduleName][GIT_COMMIT_ID]);
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
        if ((isFilter && !this.includedModules.includes(file)) || file === '.cached') {
          continue;
        }
        await this.doCopyCompileLibs(filePath, path.join(parentFolder, file), false, destPath, isHar);
      } else if (isHar && (file.endsWith('.so') || file.endsWith('.cjo'))) {
        const fileDir = path.join(destPath, parentFolder);
        await linkFile(filePath, path.join(fileDir, file));
      } else if (file.endsWith('.so')) {
        await linkFile(filePath, path.join(destPath, file));
      } else {
        // do nothing
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

  private async copyLibFiles(file: string, buildOutputDir: string, dirName: string,
    isHarModule: boolean): Promise<void> {
    const fileName = path.basename(file);
    if (fs.statSync(file).isDirectory()) {
      const childFiles = fs.readdirSync(file);
      for (const childFile1 of
        childFiles.filter((childFile) => childFile.endsWith('.so') || childFile.endsWith('.cjo'))) {
        if (childFile1.endsWith('.so')) {
          await linkFile(path.join(file, childFile1), path.join(buildOutputDir, path.basename(childFile1)));
        } else if (isHarModule) {
          const fileDir = path.join(buildOutputDir, fileName);
          await linkFile(path.join(file, childFile1), path.join(fileDir, path.basename(childFile1)));
        } else {
          // do nothing
        }
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
    if (Object.prototype.hasOwnProperty.call(packageRequires, PATH_OPTION)) {
      const pathOptions = packageRequires[PATH_OPTION];
      for (let i = 0; i < pathOptions.length; i++) {
        let pathOptionItem = pathOptions[i];
        if (excludeFiles.some(excludeFile => path.resolve(pathOptionItem).startsWith(path.resolve(excludeFile)))) {
          continue;
        }
        const normalizePath = path.normalize(pathOptionItem);
        if (!path.isAbsolute(normalizePath)) {
          pathOptionItem = path.join(workspacePath, normalizePath);
        }
        if (pathOptionItem[pathOptionItem.length - 1] === path.sep) {
          pathOptionItem = pathOptionItem.substring(0, pathOptionItem.length - 1);
        }
        pathOptionItem = path.normalize(pathOptionItem);
        if (DEMAND_BIN_PACKAGES.some(excludeFile => pathOptionItem.endsWith(path.normalize(`/${excludeFile}`)))) {
          continue;
        }
        if (fs.existsSync(pathOptionItem)) {
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION), pathOptionItem);
        }
      }
    }

    if (Object.prototype.hasOwnProperty.call(packageRequires, PACKAGE_OPTION)) {
      const packageOptions = packageRequires[PACKAGE_OPTION];
      for (const packageItem in packageOptions) {
        if (!Object.prototype.hasOwnProperty.call(packageOptions, packageItem)) {
          continue;
        }
        let packageOption = packageOptions[packageItem];
        const normalizePath = path.normalize(packageOption);
        if (!path.isAbsolute(normalizePath)) {
          packageOption = path.join(workspacePath, normalizePath);
        }
        packageOption = path.normalize(packageOption);
        if (packageOption.toString().length === 0) {
          continue;
        }
        const libPath = path.join(path.dirname(packageOption), `lib${packageItem}.so`);
        if (fs.existsSync(libPath)) {
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION), libPath);
          this.addDependencies(path.join(moduleData[PACKAGE][NAME], PATH_OPTION, packageItem), packageOption);
        }
      }
    }
  }
}
