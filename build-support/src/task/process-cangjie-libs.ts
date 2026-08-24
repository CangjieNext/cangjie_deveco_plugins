/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../types/hvigor-imports';
import {OhosLogger, TaskNames} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {AbstractProcessCangjieLibs} from './abstract-process-cangjie-libs';
import {CangjiePathImpl} from '../common/cangjie-path-impl';
import {readPackageName} from '../utils/cangjie-file-util';
import {retry} from '../utils/common-utils';
import {CangjieLogger} from '../log/cangjie-logger';

/**
 * process cangjie libs task
 *
 * @since 2024/5/6
 */
export class ProcessCangjieLibs extends AbstractProcessCangjieLibs {
  private pclLogger: OhosLogger = CangjieLogger.getLogger(ProcessCangjieLibs.name);

  private readonly _moduleDir: string;

  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.PROCESS_CJ_LIBS, name: `${CangjieTaskNames.PROCESS_CJ_LIBS.name}`});
    this._moduleDir = this.moduleModel.getProjectDir();
  }

  initTaskDepends(): void {
    this.declareDepends(CangjieTaskNames.COMPILE_CJ_NODE.name, TaskNames.Task.PROCESS_LIB.name);
  }

  protected async doTaskAction(): Promise<void> {
    await super.doTaskAction();
    this.initCangjieSdk();
    if (this.sdkCangjieComponent === undefined) {
      return;
    }
    const tomlName = readPackageName(this.cjpmTomlPath, this.pclLogger);

    // For arkts+cangjie har, need to copy the so file to the intermediates/libs directory.
    for (const abiFilter of this.abiFilters) {
      const cangjiePathImpl = new CangjiePathImpl(this.targetName, this.pathInfo, abiFilter, this.sdkCangjieComponent,
        this.customCjpmArguments);
      await retry(() => this.copyUnitTests(cangjiePathImpl.getIntermediatesCjUnitLibs(),
        cangjiePathImpl.getIntermediatesProcessLibs(), tomlName));
    }
  }
}
