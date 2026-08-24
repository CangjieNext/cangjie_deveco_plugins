/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {OhosLogger} from '../../types/hvigor-imports';
import {CangjieAdaptorError} from './cangjie-adaptor-error';
import {
  CangjieAdaptorErrorMessage,
  ErrorCode,
  ErrorCodeDescription,
  UNDEFINED_CAUSE,
  UNDEFINED_CODE,
  UNDEFINED_DESC,
  UNDEFINED_POS
} from './cangjie-types';
import {CangjieErrorDetail} from './cangjie-error-info';
import {CangjieErrorCommonAdapter} from './cangjie-error-common-adapter';
import {CangjieErrorAdaptor} from './cangjie-error-adaptor';

/**
 * cangjie日志的适配器
 *
 * @since 2025-03-15
 */
export class CangjieLogger extends OhosLogger {
  constructor(category?: string) {
    super(category);
  }

  /**
   * 获取对于类别的HvigorLogger实例
   *
   * @param {string} category 默认是default
   * @return {HvigorLogger}
   */
  public static getLogger(category?: string): CangjieLogger {
    return new CangjieLogger(category);
  }

  /**
   * 三段式打印错误信息(阻塞构建)
   * @param errorId 错误码表文件(如：hvigor.json)中的key
   * @param messages message格式化字符串
   * @param solutions solutions格式化字符串，二维数组，每一行对应solution中的一行
   */
  printErrorExit(errorId: string, messages?: unknown[], solutions?: unknown[][]): void {
    const errorAdaptor = this.formatCjErrorAdaptor(errorId, messages, solutions);
    const errorMessage = this.combinePhase(errorAdaptor.getErrorMessage());
    throw new CangjieAdaptorError(errorMessage);
  }

  /**
   * 将传入的object对象 实例化成三段是
   * @param adaptorErrorMessage 不传description时，错误码描述信息按类别展示
   * @returns {string}
   */
  combinePhase(adaptorErrorMessage: CangjieAdaptorErrorMessage): string {
    if (adaptorErrorMessage.solutions) {
      const error = {
        code: adaptorErrorMessage.code,
        cause: adaptorErrorMessage.message,
        position: '',
        solutions: adaptorErrorMessage.solutions,
        moreInfo: adaptorErrorMessage.moreInfo,
        description: '',
      };
      let errorTypeCode;
      if (error.code !== undefined) {
        errorTypeCode = this.getErrorTypeCodeFromErrorCode(error.code);
      }
      if (errorTypeCode) {
        const errorCodeDescription = ErrorCodeDescription[errorTypeCode as ErrorCode];
        error.description = errorCodeDescription ?? error.description;
      }
      const errorInfo: CangjieErrorDetail = this.getRealError(error);
      const hvigorErrorCommonAdapter: CangjieErrorCommonAdapter = new CangjieErrorCommonAdapter();
      return hvigorErrorCommonAdapter.combinePhase(errorInfo);
    }
    return adaptorErrorMessage.message;
  }

  /**
   * 从错误码截取错误归属码
   * @param {string} errorCode
   * @returns {string}
   */
  getErrorTypeCodeFromErrorCode(errorCode: string): string {
    if (!errorCode || errorCode.length < 5) {
      return '';
    }
    return errorCode.slice(3, 5);
  }

  /**
   * 将传入的object对象 实例化成HvigorErrorInfo
   * @param error
   * @returns
   */
  getRealError(error: any): CangjieErrorDetail {
    if (error instanceof CangjieErrorDetail) {
      return error;
    } else {
      return new CangjieErrorDetail({
        code: error?.code || UNDEFINED_CODE,
        cause: error?.cause || UNDEFINED_CAUSE,
        description: error?.description || UNDEFINED_DESC,
        position: error?.position || UNDEFINED_POS,
        solutions: error?.solutions || [],
        moreInfo: error?.moreInfo,
      });
    }
  }

  protected getCjAdaptor(errorId: string): CangjieErrorAdaptor {
    return new CangjieErrorAdaptor(errorId);
  }

  protected formatCjErrorAdaptor(errorId: string, messages?: unknown[], solutions?: unknown[][]): CangjieErrorAdaptor {
    let errorAdaptor = this.getCjAdaptor(errorId);
    if (messages) {
      errorAdaptor = errorAdaptor.formatMessage(...messages);
    }
    if (solutions) {
      solutions.forEach((args, index) => {
        errorAdaptor = errorAdaptor.formatSolutions(index, ...args);
      });
    }

    return errorAdaptor;
  }
}
