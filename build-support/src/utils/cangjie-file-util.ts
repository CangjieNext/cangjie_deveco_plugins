/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import fs from 'fs';
import path from 'path';
import {TOML} from './toml/toml-export';
import {ASAN_ENABLED_CONFIG, NAME, PACKAGE, SRC_DIR} from '../constants/constants';
import {checkIsValid} from './common-utils';
import {FileUtilOhos as FileUtil, Json5Reader, OhosLogger, TargetTaskService} from '../../types/hvigor-imports';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';
import {CjBuildDirConst} from '../constants/cangjie-build-dir-const';

const cfuLogger: OhosLogger = OhosLogger.getLogger('cangjie-file-util');

/**
 * Clear Folder Contents
 * @param {string} filePath
 */
export function clearDir(filePath: string): void {
  try {
    fs.rmSync(filePath, {recursive: true, force: true, maxRetries: 3});
  } catch (e) {
    cfuLogger.printErrorExit('DELETE_FILE_ERROR', [filePath]);
  }
}

export function matchFiles(dir: string, regex: RegExp): string[] {
  const files: string[] = [];
  traverseDir(dir, regex, files);
  return files;
}

export function traverseDir(currentDir: string, regex: RegExp, files: string[]): void {
  if (!checkIsValid(currentDir) || !fs.existsSync(currentDir)) {
    return;
  }
  fs.readdirSync(currentDir).forEach((file) => {
    const filePath = path.join(currentDir, file);
    const stats = fs.statSync(filePath);
    if (stats.isDirectory()) {
      traverseDir(filePath, regex, files);
    } else if (regex.test(file)) {
      files.push(filePath);
    } else {
      // do nothing
    }
  });
}

export function createDir(fileDir: string): boolean {
  if (!fs.existsSync(fileDir)) {
    fs.mkdirSync(fileDir, {recursive: true});
    return true;
  }
  return false;
}

export function deleteExtensionFiles(dir: string, extension: string): void {
  const files = fs.readdirSync(dir);
  if (files === null || files === undefined || files.length === 0) {
    return;
  }
  files.forEach(file => {
    // 获取文件的完整路径
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      deleteExtensionFiles(fullPath, extension);
    } else if (stat.isFile() && file.endsWith(extension)) {
      fs.unlinkSync(fullPath);
    } else {
      // do nothing
    }
  });
}

export function buildLogRelatePath(): string {
  return path.join('.idea', '.deveco', 'cangjie', 'build-logs');
}

export function cangjieSrcRelatePath(): string {
  return path.join('src', 'main', 'cangjie');
}

export function syscapApiConfigPath(): string {
  return path.join('.idea', '.deveco', 'cangjie', 'syscap_api_config.json');
}

export function onlyCangjieModule(moduleDir: string): boolean {
  return !hasEtsModule(moduleDir) && hasCangjieModule(moduleDir);
}

export function hasEtsModule(moduleDir: string): boolean {
  return fs.existsSync(path.resolve(moduleDir, 'src', 'main', 'ets'));
}

export function hasCangjieModule(moduleDir: string): boolean {
  return checkIsValid(moduleDir) && fs.existsSync(path.resolve(moduleDir, cangjieSrcRelatePath()));
}

export function hasArktsCangjieModule(moduleDir: string): boolean {
  return hasEtsModule(moduleDir) && hasCangjieModule(moduleDir);
}

/**
 * Compare whether two paths are the same.
 * @param path1 First path
 * @param path2 Second path
 * @returns If path2 is a subpath of path1, true is returned. Otherwise, false is returned.
 */
export function areChildPath(path1: string, path2: string): boolean {
  // Resolve paths to absolute paths and normalize
  const normalizedPath1 = path.normalize(path.resolve(path1));
  const normalizedPath2 = path.normalize(path.resolve(path2));

  // Compare Two Paths
  return normalizedPath2.startsWith(normalizedPath1);
}

export function readTomlContent(tomlPath: string, logger: OhosLogger): any {
  let tomlData = {} as any;
  if (!fs.existsSync(tomlPath)) {
    return tomlData;
  }
  const data = fs.readFileSync(tomlPath, 'utf8');
  try {
    tomlData = new TOML().parse(data);
  } catch (e: any) {
    const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
    logger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
      [codeContent, `${tomlPath}:${e.errorLine}:${e.errorColumn}`]);
  }
  return tomlData;
}

export function readTomlStringValue(tomlPath: string, keys: string[], logger: OhosLogger): string {
  const tomlData = readTomlContent(tomlPath, logger);
  const value = keys.reduce((acc, key) => acc?.[key], tomlData);
  return (value as string) ?? '';
}

export function readPackageName(tomlPath: string, logger: OhosLogger): string {
  const tomlData = readTomlContent(tomlPath, logger);
  if (!Object.prototype.hasOwnProperty.call(tomlData, PACKAGE) ||
    !Object.prototype.hasOwnProperty.call(tomlData[PACKAGE], NAME)) {
    return '';
  }
  return tomlData[PACKAGE][NAME];
}

export function readPackageNameDirect(tomlData: any): string {
  if (tomlData === undefined || !Object.prototype.hasOwnProperty.call(tomlData, PACKAGE) ||
    !Object.prototype.hasOwnProperty.call(tomlData[PACKAGE], NAME)) {
    return '';
  }
  return tomlData[PACKAGE][NAME];
}

export function validateCangjieEntry(rootPackage: string, srcEntry: string): boolean {
  return checkIsValid(rootPackage) && checkIsValid(srcEntry) && srcEntry.startsWith(`${rootPackage}.`) &&
    srcEntry.substring(rootPackage.length + 1).length > 0 &&
    !(srcEntry.substring(rootPackage.length + 1).includes('.'));
}

export async function copyLibsCommon(libPath: string, destPath: string): Promise<void> {
  if (libPath === undefined || !fs.existsSync(libPath)) {
    return;
  }
  if (!fs.existsSync(destPath)) {
    fs.mkdirSync(destPath, {recursive: true});
  }
  const childFiles = fs.readdirSync(libPath, {withFileTypes: true});
  for (const file of childFiles) {
    const fileName = file.name;
    if (file.isDirectory() || path.extname(fileName) !== '.so') {
      continue;
    }
    await linkFile(path.resolve(libPath, fileName), path.resolve(destPath, fileName));
  }
}

export function readModuleConfigTomlName(targetService: TargetTaskService,
  logger: OhosLogger): string {
  const tomlPath = readModuleConfigToml(targetService);
  if (!fs.existsSync(tomlPath)) {
    return '';
  }
  const packageName = readPackageName(tomlPath, logger);
  if (!checkIsValid(packageName)) {
    logger.printErrorExit('TOML_CONTENT_CONFIG_ERROR', [`    ${PACKAGE}.${NAME} is invalid`, tomlPath]);
    return '';
  }
  return packageName;
}

export function readModuleConfigToml(targetService: TargetTaskService): string {
  let tomlPath = targetService.getBuildOption().cangjieOptions?.path;
  if (tomlPath === undefined) {
    return '';
  }
  if (!path.isAbsolute(tomlPath)) {
    const moduleModel = targetService.getModuleService().getModuleModel();
    tomlPath = path.resolve(moduleModel.getProjectDir(), tomlPath);
  }
  if (fs.existsSync(tomlPath)) {
    return tomlPath;
  }
  return '';
}

export function readTomlSrcDir(tomlData: any): string {
  if (tomlData === undefined ||
    !Object.prototype.hasOwnProperty.call(tomlData, PACKAGE) ||
    !Object.prototype.hasOwnProperty.call(tomlData[PACKAGE], SRC_DIR)) {
    return 'src';
  }
  const srcDir = tomlData[PACKAGE][SRC_DIR];
  if (srcDir === null || srcDir === '') {
    return 'src';
  }
  return srcDir;
}

export function readAsanEnabled(appJsonPath: string, logger: OhosLogger): boolean {
  if (!fs.existsSync(appJsonPath)) {
    logger.debug(`'${appJsonPath}' is not exist.`);
    return false;
  }
  const json5Obj = Json5Reader.getJson5Obj(appJsonPath);
  if (json5Obj === undefined) {
    return false;
  }
  return Boolean(Json5Reader.getJson5ObjProp(json5Obj, ASAN_ENABLED_CONFIG));
}

export async function copyAllSubFilesWithDir(srcPath: string, destPath: string): Promise<void> {
  if (!fs.existsSync(srcPath)) {
    return;
  }
  const files = walkSync(srcPath);
  for (const filePath of files) {
    const stripFileAbsolutePath = path.resolve(destPath, filePath);
    const source = path.resolve(srcPath, filePath);
    if (!fs.lstatSync(source).isDirectory()) {
      await linkFile(source, stripFileAbsolutePath);
    }
  }
}

export async function copyAllSubFilesNoDir(srcPath: string, destPath: string): Promise<void> {
  if (!fs.existsSync(srcPath)) {
    return;
  }
  const files = walkSync(srcPath);
  for (const filePath of files) {
    const stripFileAbsolutePath = path.resolve(destPath, path.basename(filePath));
    const source = path.resolve(srcPath, filePath);
    if (!fs.lstatSync(source).isDirectory()) {
      await linkFile(source, stripFileAbsolutePath);
    }
  }
}

export function walkSync(dirPath: string): string[] {
  const dirStack = ['.'];
  const files: string[] = [];
  while (dirStack.length) {
    const rel = dirStack.pop();
    if (rel === undefined) {
      continue;
    }
    const abs = path.join(dirPath, rel);
    const childFiles = fs.readdirSync(abs, {withFileTypes: true});
    for (const dirent of childFiles) {
      const nextRel = path.join(rel, dirent.name);
      if (dirent.isDirectory()) {
        dirStack.push(nextRel);
      } else if (dirent.isFile()) {
        files.push(nextRel.replace(/^[./\\]+/, ''));
      } else {
        // do nothing
      }
    }
  }
  return files;
}

export function walkSyncWithFilter(dirPath: string, extendName: string, demandPkgs: string[]): string[] {
  if (!fs.existsSync(dirPath)) {
    return [];
  }
  const dirStack = ['.'];
  const files: string[] = [];
  while (dirStack.length) {
    const rel = dirStack.pop();
    if (rel === undefined) {
      continue;
    }
    const abs = path.join(dirPath, rel);
    const childFiles = fs.readdirSync(abs, {withFileTypes: true});
    for (const dirent of childFiles) {
      const nextRel = path.join(rel, dirent.name);
      if (dirent.isDirectory() && !demandPkgs.includes(dirent.name)) {
        dirStack.push(nextRel);
      } else if (dirent.isFile() && dirent.name.endsWith(extendName)) {
        files.push(nextRel.replace(/^[./\\]+/, ''));
      } else {
        // do nothing
      }
    }
  }
  return files;
}

export async function linkFile(srcFile: string, destFile: string): Promise<void> {
  const realFilePath = getRealFilePath(srcFile);
  if (!(await FileUtil.exists(realFilePath))) {
    return;
  }
  try {
    fs.rmSync(destFile, {recursive: true, force: true, maxRetries: 3});
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Unknown error';
    cfuLogger.printErrorExit('DELETE_FILE_FAILED', [destFile, message]);
  }
  try {
    await FileUtil.linkFile(realFilePath, destFile);
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Unknown error';
    cfuLogger.printErrorExit('COPY_FILE_FAILED', [message]);
  }
}

export async function copySpecifiedFiles(files: string[], srcPath: string, destPath: string): Promise<void> {
  if (srcPath === undefined || !fs.existsSync(srcPath) || files.length === 0) {
    return;
  }
  for (const file of files) {
    const libSo = path.resolve(srcPath, file);
    if (!fs.existsSync(libSo)) {
      continue;
    }
    const destFile = path.resolve(destPath, file);
    await linkFile(libSo, destFile);
  }
}

async function hasAnyFile(dirPath: string): Promise<boolean> {
  const entries = fs.readdirSync(dirPath, {withFileTypes: true});
  let hasFiles = false;
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isFile() || entry.isSymbolicLink()) {
      hasFiles = true;
    } else if (entry.isDirectory()) {
      const subDirHasFiles = await hasAnyFile(fullPath);
      if (subDirHasFiles) {
        hasFiles = true;
      } else {
        fs.rmSync(fullPath, {recursive: true, force: true});
      }
    }
  }
  return hasFiles;
}

/**
 * 检查目录是否为空（无文件），如果是则删除
 * @param dirPath 目录路径
 * @returns 如果目录被删除返回 true，否则返回 false
 */
export async function deleteIfNoFiles(dirPath: string): Promise<boolean> {
  if (!fs.existsSync(dirPath)) {
    return false;
  }
  const containsFiles = await hasAnyFile(dirPath);
  if (!containsFiles) {
    fs.rmSync(dirPath, {recursive: true, force: true});
    return true;
  }
  return false;
}

function getRealFilePath(libSoParam: string): string {
  let libSo = libSoParam;
  if (fs.lstatSync(libSo).isSymbolicLink()) {
    let libSoSrcPath = fs.readlinkSync(libSo);
    if (!path.isAbsolute(libSoSrcPath)) {
      libSoSrcPath = path.resolve(path.dirname(libSo), libSoSrcPath);
    }
    libSo = libSoSrcPath;
  }
  return libSo;
}

export function isCangjieHar(dirPath: string): boolean {
  if (!fs.existsSync(dirPath) || !fs.statSync(dirPath).isDirectory()) {
    return false;
  }
  if (fs.existsSync(path.join(dirPath, cangjieSrcRelatePath()))) {
    return true;
  }
  for (const key in AbiEnum) {
    if (!Object.prototype.hasOwnProperty.call(AbiEnum, key)) {
      continue;
    }
    const abi = AbiEnum[key as keyof typeof AbiEnum];
    const binDependPath = path.join(dirPath, 'libs', abi, CjBuildDirConst.BIN_LIBS);
    if (!fs.existsSync(binDependPath)) {
      continue;
    }
    const files = fs.readdirSync(binDependPath);
    for (const file of files) {
      const childDir = path.join(binDependPath, file);
      if (!fs.existsSync(childDir) || !fs.statSync(childDir).isDirectory()) {
        return false;
      }
      const childFiles = fs.readdirSync(childDir);
      if (childFiles.some(childFile => childFile.endsWith('.cjo'))) {
        return true;
      }
    }
  }
  return false;
}
