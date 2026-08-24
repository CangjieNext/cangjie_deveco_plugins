/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../../types/hvigor-imports';
import {TaskNames} from '../../../types/hvigor-imports';
import {AbstractCompileCangjie} from '../abstract-compile-cangjie';
import {CangjieTaskNames} from '../cangjie-task-names';

/**
 * compile cangjie task
 *
 * @since 2025/7/22
 */
export class UnitTestCompileCangjie extends AbstractCompileCangjie {
  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.UNIT_TEST_COMPILE_CJ_NODE, name: `${CangjieTaskNames.UNIT_TEST_COMPILE_CJ_NODE.name}`});
  }

  initTaskDepends(): void {
    this.declareDepends(TaskNames.Task.COMPILE_RESOURCE.name);
  }
}
