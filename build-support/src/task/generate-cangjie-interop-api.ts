/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {ModuleTaskService, TargetTaskService} from '../../types/hvigor-imports';
import {AbstractModuleHookTask} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';

/**
 * generate cangjie idl
 */
export class GenerateCangjieInteropApi extends AbstractModuleHookTask {
  constructor(moduleTaskService: ModuleTaskService) {
    super(moduleTaskService, false, CangjieTaskNames.GENERATE_CANGJIE_INTEROP_API);
  }

  initTaskDepends(taskTargetService: TargetTaskService): void {
    this.dependsOn(
      `${taskTargetService.getTargetData().getTargetName()}@${CangjieTaskNames.COMPILE_CANGJIE_FOR_IDL.name}`);
  }
}
