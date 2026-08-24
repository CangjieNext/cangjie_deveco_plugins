/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {InjectUtil, OhosLogger} from '../../types/hvigor-imports';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';

/**
 * cjpm command
 *
 * @since 2025/10/10
 */
export class CjpmCommandBuilder {
  private commands: string[] = [];
  private readonly logger?: OhosLogger;

  constructor(baseCommand: string, logger?: OhosLogger) {
    this.commands.push(baseCommand);
    this.logger = logger;
  }

  public buildCmd(): this {
    this.commands.push('build');
    return this;
  }

  public testCmd(noRun = false): this {
    this.commands.push('test');
    if (noRun) {
      this.commands.push('--no-run');
    }
    return this;
  }

  public withOhosTestIfNeeded(isOhosTest: boolean): this {
    if (isOhosTest) {
      this.withCoverage();
    }
    return this;
  }

  public withCoverage(): this {
    if (InjectUtil.isOhosTestCoverage()) {
      if (!InjectUtil.isLocalTest()) {
        this.commands.push('--withCoverage');
      }
      this.commands.push('--coverage');
    } else {
      if (!InjectUtil.isLocalTest()) {
        this.commands.push('--withoutCoverage');
      }
    }
    return this;
  }

  public withTarget(target: string): this {
    if (!InjectUtil.isLocalTest()) {
      this.commands.push(`--target=${target}`);
    }
    return this;
  }

  public withParallelJobs(cpuNums: number): this {
    this.commands.push(`-j${cpuNums}`);
    return this;
  }

  public withTargetDir(targetDir: string): this {
    this.commands.push(`--target-dir=${targetDir}`);
    return this;
  }

  public withBuildMode(isDebug: boolean): this {
    this.commands.push(isDebug ? '--debug' : '--release');
    return this;
  }

  public withAsan(abi: string, asanEnabled: boolean): this {
    if (abi === AbiEnum.ARM64_V8A && asanEnabled) {
      this.commands.push('--asan');
    }
    return this;
  }

  public withCustomArgs(args: string[]): this {
    this.commands.push(...args);
    return this;
  }

  public build(): string[] {
    if (this.logger !== undefined) {
      this.logger.debug(`Cangjie build command: ${this.commands.join(' ')}`);
    }
    return [...this.commands];
  }
}
