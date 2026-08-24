/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import os from 'os';

const WINDOWS_OS_NAME = 'Windows_NT';
const LINUX_OS_NAME = 'Linux';
const MAC_OS_NAME = 'Darwin';

export function isWindows(): boolean {
  return os.type() === WINDOWS_OS_NAME;
}

export function isLinux(): boolean {
  return os.type() === LINUX_OS_NAME;
}

export function isMac(): boolean {
  return os.type() === MAC_OS_NAME;
}
