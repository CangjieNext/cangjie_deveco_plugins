/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {CangjieOpt} from '../../types/hvigor-imports';
import {getCustomCjpmArguments, initAbiFilters} from '../utils/common-utils';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';

/**
 * manage cangjie options config
 *
 * @since 2025/7/16
 */
export class CangjieModuleOptions {
  private readonly _path: string;
  private readonly _abiFilters: string[];
  private readonly _strictCheckDependencies: boolean;
  private readonly _arguments: string[];
  private readonly _flattenLibs: boolean;
  private readonly _collectSDKLibs?: boolean;

  constructor(cangjieOpt?: CangjieOpt) {
    const configPath = cangjieOpt?.path;
    this._path = configPath === undefined ? '' : configPath;
    if (cangjieOpt === undefined) {
      this._abiFilters = [AbiEnum.ARM64_V8A];
    } else {
      this._abiFilters = initAbiFilters(cangjieOpt);
    }
    const configStrictCheckDependencies = cangjieOpt?.strictCheckDependencies;
    this._strictCheckDependencies =
      configStrictCheckDependencies === undefined ? true : configStrictCheckDependencies === true;
    const configArguments: any = cangjieOpt?.arguments;
    this._arguments = configArguments === undefined ? [] : getCustomCjpmArguments(configArguments);
    const configFlattenLibs = cangjieOpt?.flattenLibs;
    this._flattenLibs = configFlattenLibs === undefined ? false : configFlattenLibs === true;
    this._collectSDKLibs = cangjieOpt?.collectSDKLibs as boolean | undefined;
  }

  get path(): string {
    return this._path;
  }

  get abiFilters(): string[] {
    return this._abiFilters;
  }

  get strictCheckDependencies(): boolean {
    return this._strictCheckDependencies;
  }

  get arguments(): string[] {
    return this._arguments;
  }

  get flattenLibs(): boolean {
    return this._flattenLibs;
  }

  get collectSDKLibs(): boolean | undefined {
    return this._collectSDKLibs;
  }
}
