/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {ModulePathInfoIml} from '@ohos/hvigor-ohos-plugin/src/common/iml/module-path-info-iml';
import path from 'path';
import {CjBuildDirConst} from '../constants/cangjie-build-dir-const';
import {AbiEnum, getTargetByAbi} from '../enums/cangjie-cpu-abi-enum';
import {CJPM_DEBUG_ARGUMENT} from '../constants/constants';

/**
 * Cangjie Common Path
 *
 * @since 2024/8/1
 */
export class CangjieCommonPath {
  protected readonly _targetName: string;
  protected readonly _modulePathInfo: ModulePathInfoIml;
  protected readonly _abi: string;
  protected _buildMode: string = CjBuildDirConst.RELEASE;
  protected _mockPath = '';

  constructor(targetName: string, modulePathInfo: ModulePathInfoIml, abi: string = AbiEnum.ARM64_V8A,
    customCjpmArgs: string[] = []) {
    this._targetName = targetName;
    this._modulePathInfo = modulePathInfo;
    this._abi = abi;
    this._buildMode = customCjpmArgs.includes(CJPM_DEBUG_ARGUMENT) ? CjBuildDirConst.DEBUG : CjBuildDirConst.RELEASE;
  }

  get abi(): string {
    return this._abi;

  }

  get buildMode(): string {
    return this._buildMode;
  }

  get mockPath(): string {
    return this._mockPath;
  }

  set mockPath(value: string) {
    this._mockPath = value;
  }

  getIntermediatesCjBuild(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName);
  }

  getIntermediatesCjpmHistory(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, CjBuildDirConst.CJPM_HISTORY);
  }

  getIntermediatesCjBuildTarget(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, getTargetByAbi(this._abi));
  }

  getIntermediatesCjBuildMacroRelease(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, this._buildMode);
  }

  getIntermediatesCjBuildRelease(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, getTargetByAbi(this._abi), this._mockPath, this._buildMode);
  }

  getIntermediatesCjBuildLogRelease(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, getTargetByAbi(this._abi), this._mockPath, this._buildMode, CjBuildDirConst.BUILD_LOG);
  }

  getIntermediatesUnitTestBin(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_BUILD,
      this._targetName, getTargetByAbi(this._abi), this._mockPath, this._buildMode, CjBuildDirConst.UNIT_TEST_BIN);
  }

  getIntermediatesCjLibsRoot(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS);
  }

  getIntermediatesCjLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName);
  }

  getIntermediatesCjLibsAbi(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi);
  }

  getIntermediatesCjCompileLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.COMPILELIBS);
  }

  getIntermediatesCjUnitLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.UNIT_TEST_BIN);
  }

  getIntermediatesCjOhosLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.OHOS);
  }

  getIntermediatesCjRuntimeLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.RUNTIME);
  }

  getIntermediatesCjBinLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.BIN_LIBS);
  }

  getIntermediatesCjAsanLibs(): string {
    return path.join(this._modulePathInfo.getModuleBuildIntermediates(), CjBuildDirConst.CJ, CjBuildDirConst.CJ_LIBS,
      this._targetName, this._abi, CjBuildDirConst.ASAN);
  }

  getIntermediatesProcessLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi);
  }

  getIntermediatesProcessCompileLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi, CjBuildDirConst.COMPILELIBS);
  }

  getIntermediatesProcessBinLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi, CjBuildDirConst.BIN_LIBS);
  }

  getIntermediatesProcessOhosLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi, CjBuildDirConst.OHOS);
  }

  getIntermediatesProcessRuntimeLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi, CjBuildDirConst.RUNTIME);
  }

  getIntermediatesProcessAsanLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesProcessLibs(), this._abi, CjBuildDirConst.ASAN);
  }

  getIntermediatesStrippedLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesStrippedLibsDir(), this._abi);
  }

  getIntermediatesStrippedOhosLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesStrippedLibsDir(), this._abi, CjBuildDirConst.OHOS);
  }

  getIntermediatesStrippedRuntimeLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesStrippedLibsDir(), this._abi, CjBuildDirConst.RUNTIME);
  }

  getIntermediatesStrippedBinLibs(): string {
    return path.join(this._modulePathInfo.getIntermediatesStrippedLibsDir(), this._abi, CjBuildDirConst.BIN_LIBS);
  }

  getExtendLibPath(isHarModule: boolean): string {
    return isHarModule ? this.getIntermediatesCjLibsAbi() : this.getIntermediatesCjBinLibs();
  }

  getRuntimeOutLibPath(asanEnabled: boolean, isHarModule: boolean): string {
    if (asanEnabled) {
      return this.getIntermediatesCjRuntimeLibs();
    }
    return isHarModule ? this.getIntermediatesCjLibsAbi() : this.getIntermediatesCjCompileLibs();
  }
}
