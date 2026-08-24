/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {OhosLogger} from '../../types/hvigor-imports';
import {CangjieLogger} from '../log/cangjie-logger';

export enum AbiEnum {
  ARM64_V8A = 'arm64-v8a',
  X86_64 = 'x86_64',
}

export enum AbiTargetEnum {
  ARM64_V8A = 'aarch64-linux-ohos',
  X86_64 = 'x86_64-linux-ohos',
}

const cangjieCpuAbiLogger: OhosLogger = CangjieLogger.getLogger('cangjieCpuAbiEnum');

export function getTargetByAbi(abi: string): AbiTargetEnum {
  switch (abi) {
    case AbiEnum.ARM64_V8A:
      return AbiTargetEnum.ARM64_V8A;
    case AbiEnum.X86_64:
      return AbiTargetEnum.X86_64;
    default:
      reportAbiError(cangjieCpuAbiLogger, abi);
      return AbiTargetEnum.ARM64_V8A;
  }
}

export function reportAbiError(logger: OhosLogger, abi: string): void {
  logger.printErrorExit('CANGJIE_ABI_FILTERS_ERROR', [abi]);
}

export function isValueInAbiEnum(value: string, enumObj: typeof AbiEnum): boolean {
  return Object.values(enumObj).includes(value as AbiEnum);
}
