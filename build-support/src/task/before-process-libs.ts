/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {TargetTaskService, TaskNames} from '../../types/hvigor-imports';
import {BuildDirConst} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {BaseCangjieTask} from './base-cangjie-task';
import {getCppDepends} from './cangjie-task-initializer';
import path from 'path';
import fs from 'fs';
import {areChildPath, linkFile} from '../utils/cangjie-file-util';
import {retry} from '../utils/common-utils';

/**
 * before process libs
 *
 * @since 2024/1/6
 */
export class BeforeProcessLibs extends BaseCangjieTask {
  private cppDependLibs: string[] = [];

  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.BEFORE_PROCESS_LIBS, name: `${CangjieTaskNames.BEFORE_PROCESS_LIBS.name}`});
  }

  initTaskDepends(): void {
    this.declareDepends(TaskNames.Task.BUILD_NATIVE_WITH_NINJA.name);
  }

  protected async doTaskAction(): Promise<void> {
    this.cppDependLibs = getCppDepends(this.targetService);
    if (super.taskShouldDo() && this.cppDependLibs.length > 0) {
      for (const abi of this.abiFilters) {
        // copy cpp build so libs to libs/${target}/${abi}
        if (this.cppDependLibs.length > 0) {
          await retry(() => this.doCopyCppLibs(path.join(this.pathInfo.getIntermediatesCppOutPut(), abi),
            path.join(this.moduleModel.getProjectDir(), BuildDirConst.LIBS, abi), this.cppDependLibs, abi));
        }
      }
    }
  }

  protected async doCopyCppLibs(folderPath: string, destPath: string, cppDependLibs: string[],
    abi: string): Promise<void> {
    if (!fs.existsSync(folderPath) || cppDependLibs.length === 0) {
      return;
    }
    for (let file of cppDependLibs) {
      file = file.replace('${ABI}', abi);
      if (!areChildPath(destPath, file)) {
        continue;
      }
      const relativePath = path.relative(destPath, file);
      const srcPath = path.join(folderPath, relativePath);
      if (!fs.existsSync(srcPath)) {
        continue;
      }
      await linkFile(srcPath, file);
    }
  }
}
