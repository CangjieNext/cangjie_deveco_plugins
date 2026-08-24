/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {BooleanCallback} from '../../types/hvigor-imports';
import {
  formatTime,
  formatTimeToNumPair,
  HVIGOR_PROCESS_EVENT_ID,
  hvigorProcess,
  iconv,
  isWindows,
  LogCombineType,
  OhosLogger
} from '../../types/hvigor-imports';
import {type ChildProcess, execSync, spawn, type SpawnOptionsWithoutStdio, type SpawnSyncOptions} from 'child_process';
import fs from 'fs';
import * as os from 'os';
import path from 'path';
import {checkIsValid, killProcessAndChildren} from './common-utils';
import {CangjieLogger} from '../log/cangjie-logger';

/**
 * Tool class for executing commands.
 *
 * @since 2024-01-14
 */
export class ProcessUtil {
  private _log: OhosLogger = CangjieLogger.getLogger(ProcessUtil.name);
  private readonly _moduleName: string;
  private readonly _taskName: string;
  private readonly _errLog: string;
  private readonly _ohosCharset: string;
  private readonly _solution: string;
  private readonly _defaultOptions: SpawnSyncOptions = {
    encoding: null,
    windowsHide: true,
  };

  constructor(moduleName = 'root', taskName = 'defaultTask', errLog = '01101000 Tools execution failed.',
    solution = 'Please check the message from tools.',
    charset = isWindows() ? 'GBK' : 'utf-8') {
    this._moduleName = moduleName;
    this._taskName = taskName;
    this._errLog = errLog;
    this._solution = solution;
    this._ohosCharset = charset;
  }

  private static makeError(stdout: string, stderr: string, errorObj: any, exitCode: number | null, command: string,
    escapedCommand: string): any {
    const spawnMessage = exitCode !== undefined ? `Command failed with exit code ${exitCode}: ${command}` :
      `failed: ${command}`;
    const isError = Object.prototype.toString.call(errorObj) === '[object Error]';
    const shortMessage = isError ? `${spawnMessage}\n${errorObj.message}` : spawnMessage;
    const message = [shortMessage, stderr, stdout].filter(Boolean).join('\n');
    let error = errorObj;
    if (isError) {
      error.originalMessage = error.message;
      error.message = message;
    } else {
      error = new Error(message);
    }

    error.shortMessage = shortMessage;
    error.command = command;
    error.escapedCommand = escapedCommand;
    error.exitCode = exitCode;
    error.stdout = stdout;
    error.stderr = stderr;

    if ('bufferedData' in error) {
      delete error.bufferedData;
    }

    error.failed = true;
    return error;
  }

  async execute(executeOptions: ExecuteOptions): Promise<string> {
    const {command, options, filterMsgs, combine = LogCombineType.WARN, specialLogCallBack} = executeOptions;
    const mergedOptions = this.processOptionsFactory(options);
    const result: ExecuteResult = {stdout: '', stderr: '', errBuffer: Buffer.from('')};

    try {
      this.validateExecuteFile(command[0]);
      const start = Number(process.hrtime.bigint());
      const spawnChildProcess = spawn(command[0], command.slice(1), mergedOptions);
      spawnChildProcess.stdout?.on('data', (data) => {
        const stdoutStr = iconv.decode(<Buffer>data, this._ohosCharset);
        this._log.debug(stdoutStr);
        result.stdout += stdoutStr;
      });
      spawnChildProcess.stderr?.on('data', (data) => {
        result.errBuffer = Buffer.concat([result.errBuffer, <Buffer>data]);
      });
      this.setupProcessFailHandler(spawnChildProcess);

      await this.handleProcessCompletion({
        spawnChildProcess,
        result,
        filterMsgs,
        combine,
        specialLogCallBack,
        command,
        start,
      });
      return result.stdout;
    } catch (e) {
      this.handleException(e, combine);
      return '';
    }
  }

  /**
   * Input Parameter options Required for Generating execa
   *
   * @param options - Input parameters
   */
  processOptionsFactory(options: SpawnSyncOptions | undefined | SpawnOptionsWithoutStdio): SpawnSyncOptions {
    const pathEnv: string | undefined = process.env.PATH;
    if (options) {
      options.windowsHide = true;
    }
    if (options?.env) {
      options.env = {
        ...process.env,
        ...options.env,
      };
    }
    if (options?.env?.path && pathEnv) {
      options.env.path = pathEnv + (isWindows() ? ';' : ':') + options.env.path;
    }

    return {
      ...this._defaultOptions,
      ...options,
    };
  }

  private handleException(e: any, combine: LogCombineType): void {
    if (e.stderr?.length > 0) {
      // Print the command content to facilitate fault demarcation and locating.
      // Process sensitive information such as the password.
      const regex = /(?<pwdKeyName>-keystorePwd|-keyPwd) \S+/g;
      const command = e.command?.replace(regex, '$<pwdKeyName> ***');
      this._log.debug(`ERROR: command = ${command}`);
      this._log._buildError(combine === LogCombineType.IGNORE ? `${this._errLog}${os.EOL}${e.message}` :
        `${this._errLog}${os.EOL}${e.stderr}`);
    } else {
      this._log._buildError(`${this._errLog}${os.EOL}${e.message}`);
    }
    this._log._detail(this._solution);
    this._log._printErrorAndExit(this._moduleName);
  }

  private validateExecuteFile(file: string): void {
    if (file === 'java' || file === 'javac') {
      return;
    }
    if (!fs.existsSync(path.normalize(file))) {
      this._log.printErrorExit('FILE_NOT_FOUNF_OR_UNEXCUTABLE', [file], [[file], [file]]);
    }
  }

  private setupProcessFailHandler(process: ChildProcess): void {
    hvigorProcess.prependListener(HVIGOR_PROCESS_EVENT_ID.FAILED, () => {
      if (process.exitCode !== null || process.pid === undefined) {
        return;
      }
      this.killProcess(process.pid);
    });
  }

  private killProcess(pid: number): void {
    try {
      if (isWindows()) {
        execSync(`taskkill /T /F /PID ${pid}`,
          {windowsHide: true, env: {Path: 'C:\\Windows;C:\\Windows\\system32'}, stdio: 'ignore'});
      } else {
        killProcessAndChildren(pid);
      }
    } catch (e) {
      // do nothing
    }
  }

  private async handleProcessCompletion(options: CompletionOptions): Promise<void> {
    const {spawnChildProcess, result, filterMsgs, combine, specialLogCallBack, command, start} = options;

    return new Promise<void>((resolve, reject) => {
      spawnChildProcess.once('close', (code) => {
        this._log.debug(
          `cjpm build after: ${formatTime(formatTimeToNumPair(Number(process.hrtime.bigint()) - start))}`
        );
        this.handleStdout(result);
        this.processStderr(result, filterMsgs);

        if (code === 0) {
          this.handleSuccessfulCompletion(result, specialLogCallBack, combine, resolve);
        } else {
          reject(ProcessUtil.makeError(result.stdout.trim(), result.stderr.trim(), '', code, command.join(' '),
            command.join(' ')));
        }
      });
    });
  }

  private handleStdout(result: ExecuteResult): void {
    if (checkIsValid(result.stdout) &&
      result.stdout.includes('\'module.json\' file has been replaced automatically by \'cjpm.toml\'')) {
      const reg = /\r?\ncjpm build success\r?\n/;
      if (reg.test(result.stdout)) {
        result.stdout = result.stdout.replace(reg, '');
      }
      this._log.warn(`Cangjie ${result.stdout}`);
    }
  }

  private processStderr(result: ExecuteResult, filterMsgs?: RegExp[]): void {
    if (result.errBuffer.length > 0) {
      const stderr = this.decodeStderr(result.errBuffer);
      let error = stderr.replace(/^error(?:\x1b\[0m)?:/gm, '\x1b[31merror\x1b[0m:');
      error = this.extractErrors(error);

      filterMsgs?.forEach(item => {
        error = error.replace(item, '');
      });
      result.stderr = error.trim();
    }
  }

  private extractErrors(errorOutput: string): string {
    // 正则表达式解释：
    // 1. (\x1b\[31merror\x1b\[0m:): 匹配错误开头（红色"error"文本）
    // 2. ([\s\S]*?): 非贪婪匹配任意字符（包括换行）
    // 3. (?=\r\r\n\r\r\n|\x1b\[31merror\x1b\[0m:|$): 前瞻断言，匹配结束条件
    const errorRegex = /(?<errorItem>\x1b\[31merror\x1b\[0m:[\s\S]*?)(?=\r?\r?\n\r?\r?\n|\x1b\[31merror\x1b\[0m:|$)/g;
    const errors: string[] = [];
    let match;
    let lastIndex = 0;
    let resContent = '';

    let firstMatchStartIndex = -1;
    const tempMatch = errorRegex.exec(errorOutput);
    if (tempMatch) {
      firstMatchStartIndex = tempMatch.index;
      errorRegex.lastIndex = 0;
    }
    let preContent = '';
    if (firstMatchStartIndex > 0) {
      preContent = errorOutput.substring(0, firstMatchStartIndex).trim();
      lastIndex = firstMatchStartIndex;
    }
    while ((match = errorRegex.exec(errorOutput)) !== null) {
      const error = match.groups?.errorItem;
      if (error !== undefined) {
        lastIndex = errorRegex.lastIndex;
        errors.push(this.addFoldContent(error));
      }
    }
    resContent = errors.join('\r\r\n\r\r\n');
    if (preContent.length > 0) {
      resContent = `${preContent}\r\r\n${resContent}`;
    }
    const summaryContent = errorOutput.slice(lastIndex);
    resContent += summaryContent;
    return resContent;
  }

  private addFoldContent(content: string): string {
    if (content.includes('the error occurs after the macro is expanded')) {
      return content.replace(/\n/g, '\n\u001b[36m[Error Detail]\u001b[0m ');
    }
    return content;
  }

  private decodeStderr(errBuffer: Buffer): string {
    const regex =
      /(?<errGeneratedNum>\d+) error(?<gNums>s)? generated, (?<errPrintedNum>\d+) error(?<pNums>s)? printed\./;
    const stderr = iconv.decode(errBuffer, this._ohosCharset);
    return isWindows() && regex.test(stderr) ? iconv.decode(errBuffer, 'utf-8') : stderr;
  }

  private handleSuccessfulCompletion(
    result: ExecuteResult,
    specialLogCallBack?: BooleanCallback,
    combine?: LogCombineType,
    resolve?: () => void
  ): void {
    if (specialLogCallBack?.(result, this._ohosCharset)) {
      resolve?.();
      return;
    }
    if (combine === LogCombineType.DEBUG) {
      this._log.debug(result.stderr);
    }
    if (combine === LogCombineType.WARN) {
      this._log.warn(result.stderr);
    }
    resolve?.();
  }
}

interface ExecuteResult {
  stdout: string;
  stderr: string;
  errBuffer: Buffer;
}

interface ExecuteOptions {
  command: string[];
  options?: SpawnSyncOptions;
  filterMsgs?: RegExp[];
  combine?: LogCombineType;
  specialLogCallBack?: BooleanCallback;
}

interface CompletionOptions {
  spawnChildProcess: ChildProcess;
  result: ExecuteResult;
  filterMsgs?: RegExp[];
  combine: LogCombineType;
  specialLogCallBack?: BooleanCallback;
  command: string[];
  start: number;
}
