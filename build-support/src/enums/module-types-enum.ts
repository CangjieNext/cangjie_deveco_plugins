/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {CoreModuleModelImpl, OhosLogger} from '../../types/hvigor-imports';
import {CangjieLogger} from '../log/cangjie-logger';

export enum ModuleTypeEnum {
  HAP = 'hap',
  HSP = 'hsp',
  HAR = 'har',
}

const cangjieModuleTypesLogger: OhosLogger = CangjieLogger.getLogger('cangjieModuleTypesEnum');

export function getModuleType(moduleModel: CoreModuleModelImpl): ModuleTypeEnum {
  if (moduleModel.isHapModule()) {
    return ModuleTypeEnum.HAP;
  } else if (moduleModel.isHarModule()) {
    return ModuleTypeEnum.HAR;
  } else if (moduleModel.isHspModule()) {
    return ModuleTypeEnum.HSP;
  } else {
    reportModuleTypeError(cangjieModuleTypesLogger);
    return ModuleTypeEnum.HAP;
  }
}

export function reportModuleTypeError(logger: OhosLogger): void {
  logger.printErrorExit('MODULE_TYPE_ERROR');
}
