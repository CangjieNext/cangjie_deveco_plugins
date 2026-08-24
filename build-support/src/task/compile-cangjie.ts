/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {AbstractCompileCangjie} from './abstract-compile-cangjie';

/**
 * compile cangjie task
 *
 * @since 2024/1/6
 */
export class CompileCangjie extends AbstractCompileCangjie {
  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.COMPILE_CJ_NODE, name: `${CangjieTaskNames.COMPILE_CJ_NODE.name}`});
  }
}
