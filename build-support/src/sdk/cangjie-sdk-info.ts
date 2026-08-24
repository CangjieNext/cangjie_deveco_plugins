/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../types/hvigor-imports';
import {isWindows, OhosLogger, OhosSdkLoader, ProjectBuildProfile, SdkComponentType} from '../../types/hvigor-imports';
import {CANGJIE_NAME, CURRENT_IDE_VERSION} from '../constants/constants';
import os from 'os';
import path from 'path';
import fs from 'fs';
import {CangjieLogger} from '../log/cangjie-logger';
import {checkIsValid, delay} from '../utils/common-utils';

export const componentMap: Map<string, any> = new Map();
const _log: OhosLogger = CangjieLogger.getLogger('CangjieSdkInfo');

/**
 * init sdk info
 * @param {TargetTaskService} taskService
 * @returns {Promise<void>}
 */
export async function initSdkInfo(taskService: TargetTaskService): Promise<void> {
  const compileSdkVersion = taskService.getTargetData().getApiMeta().compileSdkVersion;
  const sdkVersion = compileSdkVersion.version;
  const isHarmonyOs = taskService.getTargetData().isHarmonyOS();
  if (isHarmonyOs) {
    if (getHarmonySdkPath(compileSdkVersion)) {
      return;
    }
    throwNotFoundException(CANGJIE_NAME, sdkVersion);
    process.exit(-1);
  } else {
    if (await getOpenHarmonySdkPath(compileSdkVersion)) {
      return;
    }
    await throwNotFoundExceptionDelay(CANGJIE_NAME, sdkVersion);
  }
}

export function getSdkComponent(sdkPath: string, version: number): any {
  const pathAndApi = getSdkKey(sdkPath, version);
  for (const [key, component] of componentMap) {
    if (key === pathAndApi) {
      return component;
    }
  }
  return undefined;
}

async function getOpenHarmonySdkPath(sdkVersion: ProjectBuildProfile.ApiMeta): Promise<boolean> {
  const defaultPath = process.env.CANGJIE_OPEN_HARMONY_SDK_PATH ?? process.env.DEVECO_CANGJIE_PATH;
  if (defaultPath !== undefined && defaultPath !== '') {
    const sdkUniJsonPath = getSdkUniJsonPath(defaultPath);
    if (checkIsValid(sdkUniJsonPath)) {
      setCangjieSdk(defaultPath, sdkVersion.version);
      return true;
    }
  } else {
    const components = [SdkComponentType.ETS];
    const sdkComponents = await OhosSdkLoader.getInstance().getOhosSdkComponents(sdkVersion, components);
    let ohosEtsSdk;
    for (const [key, value] of sdkComponents) {
      if (key.getPath() === SdkComponentType.ETS && key.getFullApiVersion().getMajor() === sdkVersion.version) {
        ohosEtsSdk = value;
        break;
      }
    }
    const ohosEtsDir = ohosEtsSdk?.getLocation();
    if (ohosEtsDir !== undefined && fs.existsSync(ohosEtsDir)) {
      const cangjieSdkDir = path.join(path.dirname(ohosEtsDir), CANGJIE_NAME);
      const sdkUniJsonPath = getSdkUniJsonPath(cangjieSdkDir);
      if (checkIsValid(sdkUniJsonPath)) {
        setCangjieSdk(cangjieSdkDir, sdkVersion.version);
        return true;
      }
    }
  }
  return false;
}

function getHarmonySdkPath(sdkVersion: ProjectBuildProfile.ApiMeta): boolean {
  let defaultPath = process.env.CANGJIE_COMPATIBLE_SDK_PATH ?? process.env.CANGJIE_HARMONY_SDK_PATH ??
    process.env.DEVECO_CANGJIE_PATH ?? process.env.DEVECO_CANGJIE_HOME;
  if (defaultPath !== undefined && defaultPath !== '') {
    const sdkUniJsonPath = getSdkUniJsonPath(defaultPath);
    if (checkIsValid(sdkUniJsonPath)) {
      setCangjieSdk(defaultPath, sdkVersion.version);
      return true;
    }
  }
  const cangjieSdkDir = getCustomCangjieSdk();
  if (checkIsValid(cangjieSdkDir) && checkIsValid(getSdkUniJsonPath(cangjieSdkDir))) {
    setCangjieSdk(cangjieSdkDir, sdkVersion.version);
    return true;
  }
  if (!checkIsValid(defaultPath)) {
    defaultPath = isWindows() ? process.env.USERPROFILE : process.env.HOME;
  }
  if (defaultPath !== undefined && defaultPath !== '') {
    const location = path.join(defaultPath, '.cangjie-sdk', CURRENT_IDE_VERSION, CANGJIE_NAME);
    if (fs.existsSync(location)) {
      setCangjieSdk(location, sdkVersion.version);
      return true;
    }
  }
  return false;
}

function throwNotFoundException(sdkPathName: string, apiVersion: number): void {
  let msg = '';
  if (apiVersion === 0) {
    msg += `${os.EOL}\t\t${sdkPathName}`;
  } else {
    msg += `${os.EOL}\t\t${sdkPathName}:${apiVersion}`;
  }
  _log._buildError(`01103031 Configuration Error\nError Message: Unable to find the following components:${msg}\n
* Try the following:
  > Please verify the integrity of your SDK.`).
    _printError('root');
  process.exit(-1);
}

export function getSdkKey(sdkPath: string, version: number): string {
  return `path:${sdkPath},apiVersion:${version}`;
}

async function throwNotFoundExceptionDelay(sdkPathName: string, apiVersion: number): Promise<void> {
  throwNotFoundException(sdkPathName, apiVersion);
  await delay(300);
  process.exit(-1);
}

function setCangjieSdk(cangjieDir: string, apiVersion: number): void {
  const cangjieComponent: SdkComponent = {
    location: cangjieDir,
    componentApiVersion: apiVersion,
  };
  componentMap.set(getSdkKey(CANGJIE_NAME, apiVersion), cangjieComponent);
}

export interface SdkComponent {
  location: string;
  componentApiVersion: number;
}

function getSdkUniJsonPath(sdkLocation: string): string {
  if (sdkLocation === undefined || sdkLocation === '' || !fs.existsSync(sdkLocation)) {
    return '';
  }
  let uniJsonPath = path.join(sdkLocation, 'uni-package.json');
  if (fs.existsSync(uniJsonPath)) {
    return uniJsonPath;
  }
  uniJsonPath = path.join(sdkLocation, 'oh-uni-package.json');
  if (fs.existsSync(uniJsonPath)) {
    return uniJsonPath;
  }
  return '';
}

function getCustomCangjieSdk(): string {
  const devecoCjSdkPath = process.env.DEVECO_CANGJIE_SDK_PATH;
  return (devecoCjSdkPath === undefined || devecoCjSdkPath === '') ? '' : path.normalize(devecoCjSdkPath);
}
