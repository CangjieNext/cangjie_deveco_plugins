/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService, TaskDetails} from '../../types/hvigor-imports';
import {InjectUtil, OhosLogger} from '../../types/hvigor-imports';
import {BaseCangjieTask} from './base-cangjie-task';
import fs from 'fs';
import {CangjieCommonPath} from '../common/cangjie-common-path';
import {CangjieLogger} from '../log/cangjie-logger';
import {copyAllSubFilesNoDir, copyAllSubFilesWithDir} from '../utils/cangjie-file-util';

/**
 * process cangjie libs task
 *
 * @since 2024/5/6
 */
export abstract class AbstractProcessCangjieLibs extends BaseCangjieTask {
  private apclLogger: OhosLogger = CangjieLogger.getLogger(AbstractProcessCangjieLibs.name);

  protected constructor(taskService: TargetTaskService, taskDetails: TaskDetails) {
    super(taskService, taskDetails);
  }

  protected async doTaskAction(): Promise<void> {
    const isHarModule = this.moduleModel.isHarModule() && !InjectUtil.isOhosTest();
    if (!isHarModule) {
      return;
    }
    for (const abiFilter of this.abiFilters) {
      const cangjiePathImpl = new CangjieCommonPath(this.targetName, this.pathInfo, abiFilter,
        this.customCjpmArguments);
      await this.processCjos(cangjiePathImpl.getIntermediatesCjBinLibs(),
        cangjiePathImpl.getIntermediatesProcessBinLibs());
    }
  }

  protected async copyProcessCangjieLibs(libsPath: string, destPath: string,
    unTile: boolean): Promise<void> {
    if (libsPath === undefined || !fs.existsSync(libsPath)) {
      return;
    }
    if (unTile) {
      await copyAllSubFilesWithDir(libsPath, destPath);
    } else {
      await copyAllSubFilesNoDir(libsPath, destPath);
    }
    this.apclLogger.debug(`Succeeded in copying cangjie libs to ${destPath}.`);
  }
}
