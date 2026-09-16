/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

export class CommonConst {
  static readonly PACKAGE_JSON: string = 'package.json';
  static readonly OH_PACKAGE_JSON5: string = 'oh-package.json5';
  static readonly OH_MODULES: string = 'oh_modules';
  static readonly PROFILE_JSON5: string = 'build-profile.json5';
  static readonly MODULE_JSON: string = 'module.json';
  static readonly MODULE_JSON5: string = 'module.json5';
  static readonly CONFIG_JSON: string = 'config.json';
  static readonly APP_CONFIG: string = 'app.json5';
  static readonly APP_SCOPE: string = 'AppScope';
}

export class DefaultTargetConst {
  static readonly DEFAULT_TARGET: string = 'default';
  static readonly OHOS_TEST_TARGET: string = 'ohosTest';
}

export class ValidateRegExp {
  static readonly RELATIVE_PATH_REG_EXP = /\.{1,2}\/.*$/;
  static readonly STARTUP_TASK_SRC_ENTRY_REG_EXP: RegExp = /^\.(?:ets|ts|js)$/;
}

