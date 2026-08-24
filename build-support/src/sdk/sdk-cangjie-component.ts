/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import path from 'path';
import {isLinux, isMac, isWindows} from '../../types/hvigor-imports';
import {SdkComponent} from './cangjie-sdk-info';
import os from 'os';
import fs from 'fs';

/**
 * cangjie sdk instance
 *
 * @since 2024-01-14
 */
export class SdkCangjieComponent {
  private readonly _buildToolsPath: string;
  private readonly _toolsBin: string;
  private readonly _toolsLib: string;
  private readonly _apiPath: string;
  private readonly _sdkRoot: string;
  private readonly _aarch64OhosLibPath: string;
  private readonly _aarch64KitPath: string;
  private readonly _x86OhosLibPath: string;
  private readonly _x86KitPath: string;
  private readonly _macroLibPath: string;
  private readonly _runtimeLibPath: string;
  private readonly _aarch64RuntimeOhosLib: string;
  private readonly _x86RuntimeOhosLib: string;
  private readonly _aarch64AsanRuntimeOhosLib: string;
  private readonly _x86AsanRuntimeOhosLib: string;
  private readonly _aarch64ApiCompatibility: string;
  private readonly _aarch64WrapperMockLib: string;
  private readonly _x86ApiCompatibility: string;
  private readonly _x86WrapperMockLib: string;
  private readonly _aarch64WrapperDiffLib: string;
  private readonly _x86WrapperDiffLib: string;
  private readonly _aarch64ApiMockLib: string;
  private readonly _x86ApiMockLib: string;
  private readonly _aarch64ApiCompatibleLib: string;
  private readonly _x86ApiCompatibleLib: string;
  private readonly _aarch64RuntimeCompatibilityLib: string;
  private readonly _x86RuntimeCompatibilityLib: string;
  private readonly _aarch64RuntimeMockLib: string;
  private readonly _x86RuntimeMockLib: string;
  private readonly _cjcPath: string;
  private readonly _escapeAarch64WrapperMockLib: string;
  private readonly _escapeX86WrapperMockLib: string;
  private readonly _escapeAarch64WrapperDiffLib: string;
  private readonly _escapeAarch64RuntimeMockLib: string;
  private readonly _escapeX86WrapperDiffLib: string;
  private readonly _escapeAarch64ApiMockLib: string;
  private readonly _escapeX86ApiMockLib: string;
  private readonly _escapeX86RuntimeMockLib: string;
  private readonly _diffDependsJsonPath:string;
  private readonly _isEscapeSdk: boolean;

  constructor(component: SdkComponent) {
    this._sdkRoot = component.location;

    // api
    this._apiPath = path.resolve(this._sdkRoot, 'api');
    const apiLibPath = path.resolve(this._apiPath, 'lib');

    // aarch64
    const apiLibAarch64Path = path.join(apiLibPath, 'linux_ohos_aarch64_cjnative');
    this._aarch64OhosLibPath = path.join(apiLibAarch64Path, 'ohos');
    this._aarch64KitPath = path.join(apiLibAarch64Path, 'kit');

    // x86
    const apiLibX86Path = path.join(apiLibPath, 'linux_ohos_x86_64_cjnative');
    this._x86OhosLibPath = path.join(apiLibX86Path, 'ohos');
    this._x86KitPath = path.join(apiLibX86Path, 'kit');

    this._macroLibPath = path.join(this._apiPath, 'macro', 'ohos');

    // build-tools
    this._buildToolsPath = path.resolve(this._sdkRoot, 'build-tools');
    this._cjcPath = path.resolve(this._buildToolsPath, 'bin');
    this._toolsBin = path.resolve(this._buildToolsPath, 'tools', 'bin');
    this._toolsLib = path.resolve(this._buildToolsPath, 'tools', 'lib');

    // runtime
    this._runtimeLibPath = path.resolve(this._buildToolsPath, 'runtime', 'lib', this.getRuntimeLib());
    this._aarch64RuntimeOhosLib = path.resolve(this._buildToolsPath, 'runtime', 'lib', 'linux_ohos_aarch64_cjnative');
    this._x86RuntimeOhosLib = path.resolve(this._buildToolsPath, 'runtime', 'lib', 'linux_ohos_x86_64_cjnative');

    // asan
    this._aarch64AsanRuntimeOhosLib = path.resolve(this._aarch64RuntimeOhosLib, 'asan');
    this._x86AsanRuntimeOhosLib = path.resolve(this._x86RuntimeOhosLib, 'asan');

    // 6.0.0 api compatibility
    const compatibilityPath = path.resolve(this._sdkRoot, 'compatibility');
    const apiCompatibilityPath = path.resolve(compatibilityPath, 'api');
    this._aarch64ApiCompatibility = path.resolve(apiCompatibilityPath, 'linux_ohos_aarch64_cjnative');
    this._aarch64WrapperMockLib = path.resolve(this._aarch64ApiCompatibility, 'c_wrapper_mock');
    this._aarch64WrapperDiffLib = path.resolve(this._aarch64ApiCompatibility, 'c_wrapper_diff');
    this._aarch64ApiMockLib = path.resolve(this._aarch64ApiCompatibility, 'api_mock');
    this._aarch64ApiCompatibleLib = path.resolve(this._aarch64ApiCompatibility, 'api_compatible');
    this._x86ApiCompatibility = path.resolve(apiCompatibilityPath, 'linux_ohos_x86_64_cjnative');
    this._x86WrapperMockLib = path.resolve(this._x86ApiCompatibility, 'c_wrapper_mock');
    this._x86WrapperDiffLib = path.resolve(this._x86ApiCompatibility, 'c_wrapper_diff');
    this._x86ApiMockLib = path.resolve(this._x86ApiCompatibility, 'api_mock');
    this._x86ApiCompatibleLib = path.resolve(this._x86ApiCompatibility, 'api_compatible');

    //  6.0.0 std compatibility
    const runtimeCompatibilityPath = path.resolve(compatibilityPath, 'runtime');
    this._aarch64RuntimeCompatibilityLib = path.resolve(runtimeCompatibilityPath, 'linux_ohos_aarch64_cjnative');
    this._x86RuntimeCompatibilityLib = path.resolve(runtimeCompatibilityPath, 'linux_ohos_x86_64_cjnative');
    const runtimeLibCompatibilityPath = path.resolve(compatibilityPath, 'runtime', 'lib');
    this._aarch64RuntimeMockLib =
      path.resolve(runtimeLibCompatibilityPath, 'linux_ohos_aarch64_cjnative', 'runtime_mock');
    this._x86RuntimeMockLib = path.resolve(runtimeLibCompatibilityPath, 'linux_ohos_x86_64_cjnative', 'runtime_mock');

    // compatible sdk
    this._escapeAarch64WrapperMockLib = path.resolve(apiLibAarch64Path, 'c_wrapper_mock');
    this._escapeAarch64WrapperDiffLib = path.resolve(apiLibAarch64Path, 'c_wrapper_diff');
    this._escapeAarch64ApiMockLib = path.resolve(apiLibAarch64Path, 'api_mock');
    this._escapeX86WrapperMockLib = path.resolve(apiLibX86Path, 'c_wrapper_mock');
    this._escapeX86WrapperDiffLib = path.resolve(apiLibX86Path, 'c_wrapper_diff');
    this._escapeX86ApiMockLib = path.resolve(apiLibX86Path, 'api_mock');
    this._escapeAarch64RuntimeMockLib = path.resolve(this._aarch64RuntimeOhosLib, 'runtime_mock');
    this._escapeX86RuntimeMockLib = path.resolve(this._x86RuntimeOhosLib, 'runtime_mock');

    this._diffDependsJsonPath = path.resolve(apiLibPath, 'so_dependencies.json');

    this._isEscapeSdk = fs.existsSync(this._escapeAarch64WrapperMockLib);
  }

  get buildToolsPath(): string {
    return this._buildToolsPath;
  }

  get toolsBin(): string {
    return this._toolsBin;
  }

  get toolsLib(): string {
    return this._toolsLib;
  }

  get apiPath(): string {
    return this._apiPath;
  }

  get sdkRoot(): string {
    return this._sdkRoot;
  }

  get runtimeLibPath(): string {
    return this._runtimeLibPath;
  }

  get aarch64RuntimeOhosLib(): string {
    return this._aarch64RuntimeOhosLib;
  }

  get x86RuntimeOhosLib(): string {
    return this._x86RuntimeOhosLib;
  }

  get aarch64AsanRuntimeOhosLib(): string {
    return this._aarch64AsanRuntimeOhosLib;
  }

  get x86AsanRuntimeOhosLib(): string {
    return this._x86AsanRuntimeOhosLib;
  }

  get cjcPath(): string {
    return this._cjcPath;
  }

  get aarch64OhosLibPath(): string {
    return this._aarch64OhosLibPath;
  }

  get aarch64KitPath(): string {
    return this._aarch64KitPath;
  }

  get x86OhosLibPath(): string {
    return this._x86OhosLibPath;
  }

  get x86KitPath(): string {
    return this._x86KitPath;
  }

  get macroLibPath(): string {
    return this._macroLibPath;
  }

  get aarch64ApiCompatibility(): string {
    return this._aarch64ApiCompatibility;
  }

  get x86ApiCompatibility(): string {
    return this._x86ApiCompatibility;
  }

  get aarch64WrapperMockLib(): string {
    return this._aarch64WrapperMockLib;
  }

  get x86WrapperMockLib(): string {
    return this._x86WrapperMockLib;
  }

  get aarch64WrapperDiffLib(): string {
    return this._aarch64WrapperDiffLib;
  }

  get x86WrapperDiffLib(): string {
    return this._x86WrapperDiffLib;
  }

  get aarch64ApiMockLib(): string {
    return this._aarch64ApiMockLib;
  }

  get x86ApiMockLib(): string {
    return this._x86ApiMockLib;
  }

  get aarch64ApiCompatibleLib(): string {
    return this._aarch64ApiCompatibleLib;
  }

  get x86ApiCompatibleLib(): string {
    return this._x86ApiCompatibleLib;
  }

  get aarch64RuntimeCompatibilityLib(): string {
    return this._aarch64RuntimeCompatibilityLib;
  }

  get x86RuntimeCompatibilityLib(): string {
    return this._x86RuntimeCompatibilityLib;
  }

  get aarch64RuntimeMockLib(): string {
    return this._aarch64RuntimeMockLib;
  }

  get x86RuntimeMockLib(): string {
    return this._x86RuntimeMockLib;
  }

  get escapeAarch64WrapperMockLib(): string {
    return this._escapeAarch64WrapperMockLib;
  }

  get escapeAarch64WrapperDiffLib(): string {
    return this._escapeAarch64WrapperDiffLib;
  }

  get escapeAarch64ApiMockLib(): string {
    return this._escapeAarch64ApiMockLib;
  }

  get escapeX86WrapperMockLib(): string {
    return this._escapeX86WrapperMockLib;
  }

  get escapeX86WrapperDiffLib(): string {
    return this._escapeX86WrapperDiffLib;
  }

  get escapeX86ApiMockLib(): string {
    return this._escapeX86ApiMockLib;
  }

  get escapeAarch64RuntimeMockLib(): string {
    return this._escapeAarch64RuntimeMockLib;
  }

  get escapeX86RuntimeMockLib(): string {
    return this._escapeX86RuntimeMockLib;
  }

  get diffDependsJsonPath(): string {
    return this._diffDependsJsonPath;
  }

  get isEscapeSdk(): boolean {
    return this._isEscapeSdk;
  }

  private getRuntimeLib(): string {
    if (isWindows()) {
      return 'windows_x86_64_cjnative';
    } else if (isLinux()) {
      if (os.arch() === 'arm64') {
        return 'linux_aarch64_cjnative';
      } else {
        return 'linux_x86_64_cjnative';
      }
    } else if (isMac()) {
      if (os.arch() === 'arm64') {
        return 'darwin_aarch64_cjnative';
      } else {
        return 'darwin_x86_64_cjnative';
      }
    } else {
      return '';
    }
  }
}
