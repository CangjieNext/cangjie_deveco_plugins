/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {AppPlugin, HapPlugin, HarPlugin, HspPlugin, Module, Project} from './types/hvigor-imports';
import {AbstractHapModulePlugin, AbstractHarModulePlugin} from './types/hvigor-imports';
import {
  createAppTask,
  createHapTask,
  createHarTask,
  createHspTask,
  handleSchema,
  registerAppTask,
  registerHapTask,
  registerHarTask,
  registerHspTask
} from './src/plugin/cangjie-plugin-factory';

/**
 * 生成或删除仓颉schema
 */
export function handleCangjieSchema(): void {
  handleSchema();
}

/**
 * register cangjie app task
 * @param project 项目
 * @param appPlugin app级别的接口和任务的plugin
 */
export function registerCangjieAppTask(project: Project, appPlugin: AppPlugin): AppPlugin {
  return registerAppTask(project, appPlugin);
}

/**
 * register cangjie hap task
 * @param module 模块
 * @param abstractHapPlugin hap级别的接口和任务的plugin
 */
export function registerCangjieHapTask(module: Module, abstractHapPlugin: AbstractHapModulePlugin): HapPlugin {
  return registerHapTask(module, abstractHapPlugin);
}

/**
 * register cangjie hsp task
 * @param module 模块
 * @param hspPlugin hsp级别的接口和任务的plugin
 */
export function registerCangjieHspTask(module: Module, hspPlugin: HspPlugin): HspPlugin {
  return registerHspTask(module, hspPlugin);
}

/**
 * register cangjie har task
 * @param module 模块
 * @param abstractHarPlugin har级别的接口和任务的plugin
 */
export function registerCangjieHarTask(module: Module, abstractHarPlugin: AbstractHarModulePlugin): HarPlugin {
  return registerHarTask(module, abstractHarPlugin);
}


/**
 * Stage app plugin
 *
 * @param module hvigorProject
 */
export const appTasks = (module: Project): AppPlugin => {
  return createAppTask(module);
};

/**
 * Stage hap plugin
 *
 * @param module hvigorModule
 */
export const hapTasks = (module: Module): HapPlugin => {
  return createHapTask(module);
};

/**
 * Stage har plugin
 *
 * @param module hvigorModule
 */
export const harTasks = (module: Module): HarPlugin => {
  return createHarTask(module);
};

/**
 * Stage hsp plugin
 *
 * @param module hvigorModule
 */
export const hspTasks = (module: Module): HspPlugin => {
  return createHspTask(module);
};
