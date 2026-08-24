/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {HspPlugin} from '../../types/hvigor-imports';
import {BaseCangjiePlugin} from './base-cangjie-plugin';

/**
 * cangjie hsp plugin
 *
 * @since 2024/1/6
 */
export class CangjieHspPlugin extends BaseCangjiePlugin {
  private hspPlugin: HspPlugin;

  constructor(hspPlugin: HspPlugin) {
    super(hspPlugin);
    this.hspPlugin = hspPlugin;
  }

  async initCangjieTasks(): Promise<void> {
    await this.initTasks();
  }
}