/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../types/hvigor-imports';
import {TaskNames} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {AbstractGenerateCangjieResource} from './abstract-generate-cangjie-resource';

/**
 * generate cangjie resource
 *
 * @since 2024/1/6
 */
export class GenerateCangjieResource extends AbstractGenerateCangjieResource {
  constructor(taskService: TargetTaskService) {
    super(taskService, CangjieTaskNames.GENERATE_CJ_RESOURCE);
  }

  initTaskDepends(): void {
    this.declareDepends(TaskNames.Task.PRE_BUILD.name);
  }
}
