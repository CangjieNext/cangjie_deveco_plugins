/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import path from 'path';
import {execSync} from 'child_process';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';
import {AbstractModulePlugin, CangjieOpt, isMac, isWindows, OhosLogger} from '../../types/hvigor-imports';
import os from 'os';
import {
  CANGJIE_OPTIONS,
  CJPM_DEFAULT_PATH,
  CJPM_TOML_NAME,
  CURRENT_IDE_VERSION,
  DEMAND_SRC_PACKAGES
} from '../constants/constants';
import fs from 'fs';
import {ideaConfigPath} from '../plugin/cangjie-plugin-factory';
import {CangjieLogger} from '../log/cangjie-logger';

export function checkIsValid(val: unknown): boolean {
  if (val === null || val === undefined) {
    return false;
  }
  if (typeof val === 'string' && val === '') {
    return false;
  }
  if (typeof val === 'number' && val === 0) {
    return false;
  }
  if (typeof val === 'boolean') {
    return val;
  }
  return true;
}

export function formatForwardSlashPath(filePath: string): string {
  if (!checkIsValid(filePath)) {
    return '';
  }
  return path.normalize(filePath).split(path.sep).join('/');
}

export function formatPathEnv(filePath: string): string {
  if (!checkIsValid(filePath)) {
    return '';
  }
  return path.normalize(replaceWithEnv(filePath)).split(path.sep).join('/');
}

export function replaceWithEnv(param: string, dependsEnv = {}): string {
  if (!checkIsValid(param)) {
    return '';
  }
  const env: NodeJS.ProcessEnv = {...process.env, ...dependsEnv};
  return param.replace(/\${(?<envVar>\w+)}/g, (match, envVar) => {
    const envPath = env[envVar]?.replace(/\\/g, '/');
    return envPath === undefined ? '' : envPath;
  });
}

export function killProcessAndChildren(pid: number): void {
  // Obtains the PIDs of all child processes.
  let childPids: number[] = [];
  try {
    const stdout = execSync(`pgrep -P ${pid}`, {windowsHide: true, env: {PATH: `/bin/${path.delimiter}/usr/bin/`}}).
      toString();
    childPids = stdout.split('\n').filter(line => line !== '').map(Number);
  } catch (error) {
    // do nothing
  }

  // Recursively end all subprocesses.
  for (const childPid of childPids) {
    if (!checkIsValid(childPid)) {
      continue;
    }
    killProcessAndChildren(childPid);
  }

  // kill the current process
  try {
    execSync(`kill -9 ${pid}`, {windowsHide: true, env: {PATH: `/bin/${path.delimiter}/usr/bin/`}});
  } catch (error) {
    // do nothing
  }
}

export function matchPattern(input: string, pattern: string): boolean {
  const regex = new RegExp(`^${pattern.replace(/\*/g, '.*')}$`);
  return regex.test(input);
}

export async function delay(ms: number): Promise<void> {
  await new Promise(resolve => {
    setTimeout(resolve, ms);
  });
}

export function dependToPlaceHolder(dependName: string): string {
  if (dependName === null || dependName === undefined) {
    return '';
  }
  return formatEnvName(dependName.split('/').join('_'));
}

export function formatEnvName(envName: string): string {
  if (envName === null || envName === undefined) {
    return '';
  }
  let rightEnvName = envName;
  if (rightEnvName.startsWith('@')) {
    rightEnvName = rightEnvName.substring(1);
  }
  return rightEnvName.replace(/[-+.]/g, '_');
}

const ccuLogger: OhosLogger = CangjieLogger.getLogger('cangjie-common-utils');

export async function retry(fn: () => void | Promise<void>, retries = 1, delayMs = 500): Promise<void> {
  for (let i = 0; i < retries + 1; i++) {
    try {
      await fn();
      return;
    } catch (e) {
      if (i < retries) {
        await new Promise(res => setTimeout(res, delayMs));
        continue;
      }
      if (!(e instanceof Error)) {
        ccuLogger.printErrorExit('COPY_FILE_FAILED', ['Unknown error']);
        return;
      }
      if (e.name === 'AdaptorError') {
        throw e;
      }
      ccuLogger.printErrorExit('COPY_FILE_FAILED', [e.message]);
    }
  }
}

export function getSeamlessPath(sdkVersion: number, projectPath: string): string {
  // Path configured with environment variables
  const systemLibs = process.env.DEVECO_CANGJIE_TPC_LIBS;
  if (systemLibs !== undefined && systemLibs !== '') {
    return systemLibs;
  }

  const projectVersion = CURRENT_IDE_VERSION;
  if (projectVersion === undefined) {
    return '';
  }

  // Path configured on the cangjie SDK GUI
  const devecoSdkLocation = getDevecoSdkLocation(projectVersion, projectPath);
  if (checkIsValid(devecoSdkLocation)) {
    return devecoSdkLocation;
  }

  // Path configured with environment variables
  const libraryPath = process.env.DEVECO_CANGJIE_PATH;
  if (libraryPath !== undefined && libraryPath !== null && libraryPath !== '') {
    const cjEnvPath = convertSdkPath(libraryPath, projectVersion);
    if (fs.existsSync(cjEnvPath)) {
      return cjEnvPath;
    }
  }

  // Default Path
  let defaultPath = isWindows() ? process.env.USERPROFILE : process.env.HOME;
  if (defaultPath !== undefined) {
    defaultPath = convertSdkPath(defaultPath, projectVersion);
    if (fs.existsSync(defaultPath)) {
      return defaultPath;
    }
  }
  return '';
}

export function convertSdkPath(sdkPath: string, projectVersion: string): string {
  return path.resolve(sdkPath, '.cangjie-sdk', 'tpc-hmos', projectVersion.replaceAll('.', ''), 'tpc');
}

export function initAbiFilters(cangjieOptions: CangjieOpt): string[] {
  const abiFilterArr = cangjieOptions?.abiFilters;
  let abiFilters: string[] = [];
  if (abiFilterArr === undefined || abiFilterArr === null) {
    abiFilters.push(AbiEnum.ARM64_V8A);
    return abiFilters;
  }
  if (!Array.isArray(abiFilterArr) || abiFilterArr.length === 0) {
    abiFilters.push(AbiEnum.ARM64_V8A);
    return abiFilters;
  }
  const abiValues = Object.values(AbiEnum);
  for (const abi of abiFilterArr) {
    if (abiValues.includes(abi as AbiEnum) && !abiFilters.includes(abi)) {
      abiFilters.push(abi);
    }
  }
  if (isMac() && os.arch() === 'arm64') {
    abiFilters = abiFilters.filter(item => item !== AbiEnum.X86_64);
  }
  if (abiFilters.length === 0) {
    abiFilters.push(AbiEnum.ARM64_V8A);
  }
  return abiFilters;
}

export function isCangjieModule(modulePlugin: AbstractModulePlugin): boolean {
  const needExecTargetServiceList = modulePlugin?.getNeedExecTargetServiceList();
  if (needExecTargetServiceList === undefined) {
    return false;
  }
  for (const target of needExecTargetServiceList) {
    if (Object.prototype.hasOwnProperty.call(target.getBuildOption(), CANGJIE_OPTIONS)) {
      return true;
    }
  }
  return false;
}

export function getDemandSrcPackages(rootTomlNames: Set<string>): string[] {
  return DEMAND_SRC_PACKAGES.filter(value => !rootTomlNames.has(value));
}

export function getCjpmConfigPath(type: string): string {
  const cjpmConfig = process.env.CJPM_CONFIG;
  if (cjpmConfig !== undefined && cjpmConfig !== null && cjpmConfig !== '') {
    return path.join(cjpmConfig, type);
  }
  const localAppData = isWindows() ? process.env.USERPROFILE : process.env.HOME;
  return localAppData === undefined ? path.join(CJPM_DEFAULT_PATH, type) :
    path.join(localAppData, CJPM_DEFAULT_PATH, type);
}

export function getCustomCjpmArguments(args?: string[] | string): string[] {
  if (args === undefined) {
    return [];
  }
  if (typeof args === 'string') {
    const argsTrim = args.trim();
    if (argsTrim === '') {
      return [];
    }
    return [...argsTrim.split(/\s+/).filter(arg => arg !== '')];
  }
  return [...args].flatMap((arg) => String(arg).trim().split(/\s+/)).filter(arg => arg !== '');
}

export function normalizeCjpmTomlPath(cangjieOptionPath: string, projectDir: string): string {
  if (!cangjieOptionPath.endsWith(CJPM_TOML_NAME)) {
    return '';
  }
  if (path.isAbsolute(cangjieOptionPath)) {
    return cangjieOptionPath;
  } else {
    return path.resolve(projectDir, cangjieOptionPath);
  }
}

export function getDevecoSdkLocation(projectVersion: string, projectPath: string): string {
  const otherXmlPath = path.join(ideaConfigPath, 'options', 'other.xml');
  if (!fs.existsSync(otherXmlPath)) {
    return '';
  }
  let fileContent = fs.readFileSync(otherXmlPath, 'utf-8');
  fileContent = fileContent.replaceAll('&quot;', '"');
  const lines = fileContent.split(/\r?\n/);
  const regex = /"cangjie\.seamless\.location"\s*:\s*"(?<location>[^"]+)"/;
  for (let i = 0; i < lines.length; i++) {
    const match = lines[i].match(regex);
    if (!match || !match.groups || !checkIsValid(match.groups.location)) {
      continue;
    }
    const seamlessLocation = path.join(match.groups.location, projectVersion.replaceAll('.', ''), 'tpc');
    if (!fs.existsSync(seamlessLocation)) {
      continue;
    }
    return seamlessLocation;
  }
  return '';
}

export function compareVersionsPrefix(versionArrSrc: string[], versionArrDest: string[],
  prefixLength: number): boolean {
  if (versionArrSrc.length < prefixLength || versionArrDest.length < prefixLength) {
    return false;
  }
  for (let i = 0; i < prefixLength; i++) {
    if (versionArrSrc[i] !== versionArrDest[i]) {
      return false;
    }
  }
  return true;
}

export function compareVersionsUpTo(versionArrSrc: string[], versionArrDest: string[], length: number): number {
  for (let i = 0; i < length; i++) {
    const strSrc = versionArrSrc[i] ?? '0';
    const strDest = versionArrDest[i] ?? '0';
    let numSrc = parseInt(strSrc, 10);
    let numDest = parseInt(strDest, 10);
    if (isNaN(numSrc)) {
      numSrc = 0;
    }
    if (isNaN(numDest)) {
      numDest = 0;
    }
    if (numSrc > numDest) {
      return 1;
    }
    if (numSrc < numDest) {
      return -1;
    }
  }
  return 0;
}
