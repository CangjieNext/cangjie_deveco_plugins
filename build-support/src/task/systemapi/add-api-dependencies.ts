/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import fs from 'fs';
import {BaseCangjieTask} from '../base-cangjie-task';
import {OhosLogger, TargetTaskService} from '../../../types/hvigor-imports';
import {checkIsValid, getSeamlessPath} from '../../utils/common-utils';
import {BIN_DEPENDENCIES, PATH_OPTION, TARGET} from '../../constants/constants';
import {TOML} from '../../utils/toml/toml-export';
import {CangjieTaskNames} from '../cangjie-task-names';
import {hasArktsCangjieModule} from '../../utils/cangjie-file-util';
import {CangjieLogger} from '../../log/cangjie-logger';

/**
 * generate api dependencies
 *
 * @since 2024/12/15
 */
export class AddApiDependencies extends BaseCangjieTask {
  private aadLogger: OhosLogger = CangjieLogger.getLogger(AddApiDependencies.name);

  private tpcEnvMap: Map<string, string> = new Map<string, string>([
    ['aarch64-linux-ohos', '${AARCH64_TPC_LIBS}'],
    ['x86_64-linux-ohos', '${X86_TPC_LIBS}'],
    ['x86_64-w64-mingw32', '${X86_64_WIN_TPC_MACRO_LIBS}'],
    ['aarch64-apple-darwin', '${AARCH64_DARWIN_TPC_MACRO_LIBS}'],
    ['x86_64-apple-darwin', '${X86_64_DARWIN_TPC_MACRO_LIBS}']]);

  constructor(taskService: TargetTaskService) {
    super(taskService,
      {...CangjieTaskNames.ADD_API_DEPENDENCIES, name: `${CangjieTaskNames.ADD_API_DEPENDENCIES.name}`});
  }

  initTaskDepends(): void {
    // do nothing
  }

  protected doTaskAction(): void {
    this.addApiDependencies();
  }

  private addApiDependencies(): void {
    if (!hasArktsCangjieModule(this.moduleModel.getProjectDir())) {
      return;
    }
    const seamlessPath = getSeamlessPath(this.targetService.getTargetData().getApiMeta().compileSdkVersion.version,
      this.projectModel.getProjectDir());
    if (!checkIsValid(seamlessPath)) {
      return;
    }
    const hapToml = new TOML();
    const tomlData = this.readToml(hapToml);
    const targetDepends = Object.prototype.hasOwnProperty.call(tomlData, TARGET) ? tomlData[TARGET] : {};
    const isModified = this.doAddApiDepends(targetDepends);
    if (isModified) {
      tomlData[TARGET] = targetDepends;
      fs.writeFileSync(this.cjpmTomlPath, hapToml.stringify(tomlData));
    }
  }

  private readToml(hapToml: TOML): any {
    const data = fs.readFileSync(this.cjpmTomlPath, 'utf8');
    let tomlData = {} as any;
    try {
      tomlData = hapToml.parse(data);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this.aadLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${this.cjpmTomlPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    return tomlData;
  }

  private doAddApiDepends(targetDepends: any): boolean {
    let isModified = false;
    for (const [target, envName] of this.tpcEnvMap.entries()) {
      if (this.updateTomlContent(targetDepends, target, envName)) {
        isModified = true;
      }
    }
    return isModified;
  }

  private updateTomlContent(targetDepends: any, targetApi: string, envName: string): boolean {
    if (!Object.prototype.hasOwnProperty.call(targetDepends, targetApi)) {
      targetDepends[targetApi] = {};
    }
    if (!Object.prototype.hasOwnProperty.call(targetDepends[targetApi], BIN_DEPENDENCIES)) {
      targetDepends[targetApi][BIN_DEPENDENCIES] = {};
    }
    if (!Object.prototype.hasOwnProperty.call(targetDepends[targetApi][BIN_DEPENDENCIES], PATH_OPTION)) {
      targetDepends[targetApi][BIN_DEPENDENCIES][PATH_OPTION] = [];
    }
    const pathOptions = targetDepends[targetApi][BIN_DEPENDENCIES][PATH_OPTION];
    if (!pathOptions.includes(envName)) {
      pathOptions.push(envName);
      return true;
    }
    return false;
  }
}
