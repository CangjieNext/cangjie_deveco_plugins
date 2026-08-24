/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {spawnSync} from 'child_process';
import {isLinux, isMac, isWindows} from '../../types/hvigor-imports';

const WIN_CODE_CN = 2052 as const;

export enum countryEnum {
  CN = 'cn',
  EN = 'en'
}

let countryCache: countryEnum | undefined;

/**
 * 获取系统语言
 */
export function getOsLanguage(): countryEnum {
  if (countryCache) {
    return countryCache;
  }

  let local: countryEnum = countryEnum.CN;
  if (isWindows()) {
    local = getWinLocale();
  } else if (isMac()) {
    local = getMacLocale();
  } else {
    local = getLinuxLocale();
  }
  if (process.env['user.country']) {
    local = process.env['user.country'].toString() === 'CN' ? countryEnum.CN : countryEnum.EN;
  }
  countryCache = local;
  return local;
}

/**
 * windows系统执行 wmic os get locale
 */
function getWinLocale(): countryEnum {
  const result = spawnSync('wmic', ['os', 'get', 'locale']);
  if (result.status !== 0) {
    return countryEnum.CN;
  }
  const code = Number.parseInt(result.stdout.toString().replace('Locale', ''), 16);
  if (code === WIN_CODE_CN) {
    return countryEnum.CN;
  }
  return countryEnum.EN;
}

/**
 * mac系统执行 defaults read -globalDomain AppleLocale
 */
function getMacLocale(): countryEnum {
  const result = spawnSync('defaults', ['read', '-globalDomain', 'AppleLocale']);
  if (result.status !== 0) {
    return countryEnum.CN;
  }
  if (result.stdout.toString().indexOf('zh_CN') >= 0) {
    return countryEnum.CN;
  }
  return countryEnum.EN;
}

function getLinuxLocale(): countryEnum {
  const result = spawnSync('locale');
  if (result.status !== 0) {
    return countryEnum.CN;
  }
  const env: { [key: string]: string } = {};
  for (const definition of result.stdout.toString().split('\n')) {
    const [key, value] = definition.split('=');
    env[key] = value?.replace(/^"|"$/g, '') ?? '';
  }
  const local = env.LC_ALL || env.LC_MESSAGES || env.LANG || env.LANGUAGE;
  if (local.indexOf('zh_CN') >= 0) {
    return countryEnum.CN;
  }
  return countryEnum.EN;
}
