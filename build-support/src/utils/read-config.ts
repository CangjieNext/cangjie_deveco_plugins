/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import * as fs from 'fs';
import * as path from 'path';
import {checkIsValid} from './common-utils';
import {OhosLogger} from '../../types/hvigor-imports';

const rcLogger: OhosLogger = OhosLogger.getLogger('read-config');

export interface CangjieProfileConfig {
  debugUseProfile: boolean;
  releaseUseProfile: boolean;
}

/**
 * 读取 Cangjie 优化配置
 * @param configPath
 */
export function getCangjieOptimizationConfig(configPath: string): CangjieProfileConfig {
  // 定义默认值
  const defaultConfig: CangjieProfileConfig = {
    debugUseProfile: false,
    releaseUseProfile: false,
  };
  if (!checkIsValid(configPath)) {
    return defaultConfig;
  }
  const xmlFilePath = path.join(configPath, 'options', 'cangjie_optimization_profile_settings.xml');
  if (!fs.existsSync(xmlFilePath)) {
    rcLogger.debug(`Config file not found at ${xmlFilePath}, using defaults.`);
    return defaultConfig;
  }
  try {
    const xmlContent = fs.readFileSync(xmlFilePath, 'utf-8');
    const debugRegex = /<option\s+name="debugUseProfile"\s+value="(?<value>true|false)"\s*\/>/;
    const releaseRegex = /<option\s+name="releaseUseProfile"\s+value="(?<value>true|false)"\s*\/>/;
    const debugMatch = xmlContent.match(debugRegex);
    const releaseMatch = xmlContent.match(releaseRegex);
    return {
      debugUseProfile: debugMatch ? debugMatch.groups?.value === 'true' : defaultConfig.debugUseProfile,
      releaseUseProfile: releaseMatch ? releaseMatch.groups?.value === 'true' : defaultConfig.releaseUseProfile,
    };
  } catch (error) {
    rcLogger.debug(`Failed to read or parse Cangjie config: ${error}`);
    return defaultConfig;
  }
}

export function getProjectOption(projectRoot: string, keyName: string,
  defaultValue: string): string {
  const xmlFilePath = path.join(projectRoot, '.idea', 'projectOptimizationSettings.xml');
  if (!fs.existsSync(xmlFilePath)) {
    return defaultValue;
  }
  const xmlContent = fs.readFileSync(xmlFilePath, 'utf-8');
  const regex = new RegExp(`<option name="${keyName}" value="([^"]+)"\\s*/>`);
  const match = xmlContent.match(regex);
  if (match?.[1]?.trim()) {
    return match[1].trim();
  }
  return defaultValue;
}

export function getDefaultProfdataFile(projectRoot: string, moduleName: string): string {
  return path.join(projectRoot, '.idea', '.deveco', 'cangjie', 'cjpgos', moduleName,
    'merged.profdata');
}
