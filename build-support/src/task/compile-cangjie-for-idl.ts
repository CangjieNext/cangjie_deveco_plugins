/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TargetTaskService} from '../../types/hvigor-imports';
import {FileUtilOhos as FileUtil, OhosLogger} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {AbstractCompileCangjie} from './abstract-compile-cangjie';
import fs from 'fs';
import path from 'path';
import {deleteExtensionFiles, readTomlContent, readTomlSrcDir} from '../utils/cangjie-file-util';
import {CangjieCommonPath} from '../common/cangjie-common-path';
import {CangjieLogger} from '../log/cangjie-logger';
import {CangjiePathImpl} from '../common/cangjie-path-impl';

/**
 * compile cangjie for idl task
 *
 * @since 2024/1/6
 */
export class CompileCangjieForIdl extends AbstractCompileCangjie {
  private ccfiLogger: OhosLogger = CangjieLogger.getLogger(CompileCangjieForIdl.name);

  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.COMPILE_CANGJIE_FOR_IDL, name: `${CangjieTaskNames.COMPILE_CANGJIE_FOR_IDL.name}`});
  }

  protected async doTaskAction(): Promise<void> {
    this.initCangjieSdk();
    const abiFilter = this.abiFilters[0];
    const cangjieCommonPath = new CangjieCommonPath(this.targetName, this.pathInfo, abiFilter,
      this.customCjpmArguments);
    this.clearIdlFiles(cangjieCommonPath);
    this.abiFilters = [abiFilter];
    await this.executeCangjieCompile(true);
  }

  protected clearIdlFiles(cangjieCommonPath: CangjieCommonPath): void {
    if (this.sdkCangjieComponent === undefined) {
      return;
    }

    // delete d.ts
    const libIdlPath = path.resolve(this.cjSource, readTomlSrcDir(readTomlContent(this.cjpmTomlPath, this.ccfiLogger)),
      'ark_interop_api');
    if (fs.existsSync(libIdlPath)) {
      deleteExtensionFiles(libIdlPath, '.d.ts');
    }

    // delete cangjie build
    FileUtil.deleteFile(cangjieCommonPath.getIntermediatesCjBuildTarget());
  }

  /**
   * Copying Cangjie Compiled Products to the Target Directory
   * @protected
   */
  protected async copyResultFile(cangjiePathImpl: CangjiePathImpl, dependsEnv: any, tomlName: string): Promise<void> {
    // do nothing
  }

  protected async copyDemandLibsCommon(cangjiePathImpl: CangjiePathImpl, tomlName: string,
    moduleConfigTomlName: string): Promise<void> {
    // do nothing
  }
}
