/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {ModulePathInfoIml, OhosLogger} from '../../types/hvigor-imports';
import {checkIsValid} from '../utils/common-utils';
import {SdkCangjieComponent} from '../sdk/sdk-cangjie-component';
import {CangjieCommonPath} from './cangjie-common-path';
import {AbiEnum, reportAbiError} from '../enums/cangjie-cpu-abi-enum';
import fs from 'fs';
import {CangjieLogger} from '../log/cangjie-logger';
import path from 'path';
import {ideaConfigPath} from '../plugin/cangjie-plugin-factory';
import {CJ_ENV_CACHE_FILE, CJ_PLUGIN_CACHE_DIR} from '../constants/constants';

/**
 * Cangjie Path Info
 *
 * @since 2024/8/1
 */
export class CangjiePathImpl extends CangjieCommonPath {
  private readonly _sdkComponent: SdkCangjieComponent;
  private implLogger: OhosLogger = CangjieLogger.getLogger(CangjiePathImpl.name);

  constructor(targetName: string, modulePathInfo: ModulePathInfoIml, abi: string, sdkComponent: SdkCangjieComponent,
    customCjpmArguments: string[]) {
    super(targetName, modulePathInfo, abi, customCjpmArguments);
    this._sdkComponent = sdkComponent;
  }

  getBuildEnvByAbi(tpcPath: string): NodeJS.ProcessEnv {
    const env = {
      CANGJIE_HOME: this._sdkComponent.buildToolsPath,
      LD_LIBRARY_PATH: `${this._sdkComponent.runtimeLibPath}:${this._sdkComponent.toolsLib}${checkIsValid(
        process.env.LD_LIBRARY_PATH) ? `:${process.env.LD_LIBRARY_PATH}` : ''}`,
      DEVECO_CANGJIE_HOME: this._sdkComponent.sdkRoot,
      AARCH64_LIBS: this._sdkComponent.aarch64OhosLibPath,
      AARCH64_MACRO_LIBS: this._sdkComponent.macroLibPath,
      AARCH64_KIT_LIBS: fs.existsSync(this._sdkComponent.aarch64KitPath) ? this._sdkComponent.aarch64KitPath : '',
      X86_64_OHOS_LIBS: this._sdkComponent.x86OhosLibPath,
      X86_64_OHOS_MACRO_LIBS: this._sdkComponent.macroLibPath,
      X86_64_OHOS_KIT_LIBS: fs.existsSync(this._sdkComponent.x86KitPath) ? this._sdkComponent.x86KitPath : '',
    };
    const tpcEnv = this.getTpcEnvPathByAbi(tpcPath);
    const configEnvs = this.getOtherConfigEnvs();
    return {...env, ...tpcEnv, ...configEnvs};
  }

  getTpcEnvPathByAbi(tpcPath: string): NodeJS.ProcessEnv {
    if (!checkIsValid(tpcPath)) {
      const processEnv = process.env;
      return {
        AARCH64_TPC_LIBS: processEnv.AARCH64_TPC_LIBS ?? '',
        X86_TPC_LIBS: processEnv.X86_TPC_LIBS ?? '',
        X86_64_WIN_TPC_MACRO_LIBS: processEnv.X86_64_WIN_TPC_MACRO_LIBS ?? '',
        AARCH64_DARWIN_TPC_MACRO_LIBS: processEnv.AARCH64_DARWIN_TPC_MACRO_LIBS ?? '',
        X86_64_DARWIN_TPC_MACRO_LIBS: processEnv.X86_64_DARWIN_TPC_MACRO_LIBS ?? '',
        AARCH64_SYSTEM_LIBS: processEnv.AARCH64_SYSTEM_LIBS ?? '',
        X86_64_LINUX_TPC_MACRO_LIBS: processEnv.X86_64_LINUX_TPC_MACRO_LIBS ?? '',
      };
    }
    return {
      AARCH64_TPC_LIBS: path.resolve(tpcPath, 'runtime', 'linux_ohos_aarch64_llvm', 'tpc'),
      X86_TPC_LIBS: path.resolve(tpcPath, 'runtime', 'linux_ohos_x86_64_llvm', 'tpc'),
      X86_64_WIN_TPC_MACRO_LIBS: path.resolve(tpcPath, 'build', 'x86_64-w64-mingw32', 'tpc'),
      AARCH64_DARWIN_TPC_MACRO_LIBS: path.resolve(tpcPath, 'build', 'darwin_aarch64_llvm', 'tpc'),
      X86_64_DARWIN_TPC_MACRO_LIBS: path.resolve(tpcPath, 'build', 'darwin_x86_64_llvm', 'tpc'),
      AARCH64_SYSTEM_LIBS: tpcPath,
      X86_64_LINUX_TPC_MACRO_LIBS: path.resolve(tpcPath, 'build', 'linux_x86_64_llvm', 'tpc'),
    };
  }

  getOtherConfigEnvs(): NodeJS.ProcessEnv {
    if (!checkIsValid(ideaConfigPath) || !fs.existsSync(ideaConfigPath)) {
      return {};
    }
    const envFilePath = path.resolve(ideaConfigPath, CJ_PLUGIN_CACHE_DIR, CJ_ENV_CACHE_FILE);
    if (!fs.existsSync(envFilePath)) {
      return {};
    }
    try {
      const envContext = JSON.parse(fs.readFileSync(envFilePath, 'utf8'));
      if (!envContext.env || typeof envContext.env !== 'object') {
        return {};
      }
      return envContext.env;
    } catch (e) {
      this.implLogger.debug('Failed to read the Cangjie environment variable json file.');
      return {};
    }
  }

  getRuntimeOhosPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64RuntimeOhosLib',
      [AbiEnum.X86_64]: 'x86RuntimeOhosLib',
    }, 'aarch64RuntimeOhosLib');
  }

  getOhosLibsPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64OhosLibPath',
      [AbiEnum.X86_64]: 'x86OhosLibPath',
    }, 'aarch64OhosLibPath');
  }

  getKitLibsPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64KitPath',
      [AbiEnum.X86_64]: 'x86KitPath',
    }, 'aarch64KitPath');
  }

  getAsanRuntimeLibsPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64AsanRuntimeOhosLib',
      [AbiEnum.X86_64]: 'x86AsanRuntimeOhosLib',
    }, 'aarch64AsanRuntimeOhosLib');
  }

  getWrapperMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64WrapperMockLib',
      [AbiEnum.X86_64]: 'x86WrapperMockLib',
    }, 'aarch64WrapperMockLib');
  }

  getWrapperDiffPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64WrapperDiffLib',
      [AbiEnum.X86_64]: 'x86WrapperDiffLib',
    }, 'aarch64WrapperDiffLib');
  }

  getApiMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64ApiMockLib',
      [AbiEnum.X86_64]: 'x86ApiMockLib',
    }, 'aarch64ApiMockLib');
  }

  getApiCompatibilityByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64ApiCompatibility',
      [AbiEnum.X86_64]: 'x86ApiCompatibility',
    }, 'aarch64ApiCompatibility');
  }

  getApiCompatiblePathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64ApiCompatibleLib',
      [AbiEnum.X86_64]: 'x86ApiCompatibleLib',
    }, 'aarch64ApiCompatibleLib');
  }

  getRuntimeCompatibilityPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64RuntimeCompatibilityLib',
      [AbiEnum.X86_64]: 'x86RuntimeCompatibilityLib',
    }, 'aarch64RuntimeCompatibilityLib');
  }

  getRuntimeMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'aarch64RuntimeMockLib',
      [AbiEnum.X86_64]: 'x86RuntimeMockLib',
    }, 'aarch64RuntimeMockLib');
  }

  getEscapeWrapperMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'escapeAarch64WrapperMockLib',
      [AbiEnum.X86_64]: 'escapeX86WrapperMockLib',
    }, 'escapeAarch64WrapperMockLib');
  }

  getEscapeWrapperDiffPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'escapeAarch64WrapperDiffLib',
      [AbiEnum.X86_64]: 'escapeX86WrapperDiffLib',
    }, 'escapeAarch64WrapperDiffLib');
  }

  getEscapeApiMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'escapeAarch64ApiMockLib',
      [AbiEnum.X86_64]: 'escapeX86ApiMockLib',
    }, 'escapeAarch64ApiMockLib');
  }

  getEscapeRuntimeMockPathByAbi(): string {
    return this.getPathByAbi({
      [AbiEnum.ARM64_V8A]: 'escapeAarch64RuntimeMockLib',
      [AbiEnum.X86_64]: 'escapeX86RuntimeMockLib',
    }, 'escapeAarch64RuntimeMockLib');
  }

  /**
   * Get property path by ABI mapping.
   * @param propMap - ABI to property name mapping
   * @param fallbackProperty - fallback property name when ABI not found (typically ARM64)
   */
  private getPathByAbi(propMap: Partial<Record<string, keyof SdkCangjieComponent>>,
    fallbackProperty: keyof SdkCangjieComponent): string {
    const propertyName = propMap[this._abi];
    if (!propertyName) {
      reportAbiError(this.implLogger, this._abi);
      return this._sdkComponent[fallbackProperty] as string;
    }
    return this._sdkComponent[propertyName] as string;
  }
}
