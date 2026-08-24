/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {AbstractModulePlugin, AppPlugin, HapPlugin, HarPlugin, HspPlugin} from '../../types/hvigor-imports';
import {
  AbstractHapModulePlugin,
  AbstractHarModulePlugin,
  hvigor,
  type HvigorCoreNode,
  type Module,
  PluginFactory,
  type Project,
  TaskCreatorManager,
  TaskNames
} from '../../types/hvigor-imports';
import {CangjieHapPlugin} from './cangjie-hap-plugin';
import {CangjieHarPlugin} from './cangjie-har-plugin';
import {CangjieHspPlugin} from './cangjie-hsp-plugin';
import {
  GenerateApiDependenciesCA,
  GenerateCangjieInteropApiCA,
  SyncCangjieResourceCA
} from '../task/cangjie-task-initializer';
import {GenerateCangjieSchema} from '../task/sync/generate-cangjie-schema';
import path from 'path';
import {getHomedir} from '../utils/homedir';
import fs from 'fs';
import {CURRENT_IDE_VERSION} from '../constants/constants';
import {isCangjieModule} from '../utils/common-utils';
import {CangjieTaskNames} from '../task/cangjie-task-names';
import {CompileNodeGraphMatch} from '../utils/compile-node-graph-match';
import CommonHookTask = TaskNames.CommonHookTask;

export const ideaConfigPath = getIdeaConfigPath();
const isCangjiePluginEnabled = isPluginEnabled();

/**
 * register cangjie app task
 */
export function registerAppTask(project: Project, appPlugin: AppPlugin): AppPlugin {
  return appPlugin;
}

let hasBuiltDependencyTree = false;

function addCangjieHapTask(hapPlugin: HapPlugin, module: HvigorCoreNode): HapPlugin {
  if (isCangjieModule(hapPlugin)) {
    initGlobalTasks(module, hapPlugin);
  }
  hvigor.nodesEvaluated(async () => {
    const cjHapPlugin = new CangjieHapPlugin(hapPlugin);
    await cjHapPlugin.initCangjieTasks();
  });
  return hapPlugin;
}

/**
 * register cangjie hap task
 */
export function registerHapTask(module: Module, abstractHapPlugin: AbstractHapModulePlugin): HapPlugin {
  const hapPlugin: HapPlugin = <HapPlugin>abstractHapPlugin;
  if (!isCangjiePluginEnabled) {
    return hapPlugin;
  }
  return addCangjieHapTask(hapPlugin, module);
}

/**
 * register cangjie har task
 */
export function registerHarTask(module: Module, abstractHarPlugin: AbstractHarModulePlugin): HarPlugin {
  const harPlugin: HarPlugin = <HarPlugin>abstractHarPlugin;
  if (!isCangjiePluginEnabled) {
    return harPlugin;
  }
  return addCangjieHarTask(harPlugin, module);
}

/**
 * register cangjie hsp task
 */
export function registerHspTask(module: Module, hspPlugin: HspPlugin): HspPlugin {
  if (!isCangjiePluginEnabled) {
    return hspPlugin;
  }
  return addCangjieHspTask(hspPlugin, module);
}

/**
 * app task
 */
export function createAppTask(project: Project): AppPlugin {
  handleSchema();
  return PluginFactory.getAppPlugin(project);
}

/**
 * hap task
 */
export function createHapTask(module: Module): HapPlugin {
  handleSchema();
  const hapPlugin: HapPlugin = <HapPlugin>PluginFactory.getHapPlugin(module);
  return addCangjieHapTask(hapPlugin, module);
}

function addCangjieHarTask(harPlugin: HarPlugin, module: HvigorCoreNode): HarPlugin {
  if (isCangjieModule(harPlugin)) {
    initGlobalTasks(module, harPlugin);
  }
  hvigor.nodesEvaluated(async () => {
    const cjHarPlugin = new CangjieHarPlugin(harPlugin);
    await cjHarPlugin.initCangjieTasks();
  });
  return harPlugin;
}

/**
 * har task
 */
export function createHarTask(module: Module): HarPlugin {
  handleSchema();
  const harPlugin: HarPlugin = <HarPlugin>PluginFactory.getHarPlugin(module);
  return addCangjieHarTask(harPlugin, module);
}

function addCangjieHspTask(hspPlugin: HspPlugin, module: HvigorCoreNode): HspPlugin {
  if (isCangjieModule(hspPlugin)) {
    initGlobalTasks(module, hspPlugin);
  }
  hvigor.nodesEvaluated(async () => {
    const cjHspPlugin = new CangjieHspPlugin(hspPlugin);
    await cjHspPlugin.initCangjieTasks();
  });
  return hspPlugin;
}

/**
 * hsp task
 */
export function createHspTask(module: Module): HspPlugin {
  handleSchema();
  const hspPlugin: HspPlugin = <HspPlugin>PluginFactory.getHspPlugin(module);
  return addCangjieHspTask(hspPlugin, module);
}

/**
 * replace schema
 */
export function handleSchema(): void {
  const instance = GenerateCangjieSchema.getInstance(ideaConfigPath, isCangjiePluginEnabled);
  if (isCangjiePluginEnabled) {
    instance.replaceSchema();
  } else {
    instance.deleteSchema();
  }
}

/**
 * sync cangjie resource task
 *
 * @param {HvigorCoreNode} module
 * @param {AbstractModulePlugin} plugin
 */
function initGlobalTasks(module: HvigorCoreNode, plugin: AbstractModulePlugin): void {
  const taskContainer = module.getTaskContainer();
  if (taskContainer.hasTask(CangjieTaskNames.SYNC_CJ_RESOURCE.name)) {
    return;
  }
  const creatorManager: TaskCreatorManager = new TaskCreatorManager(taskContainer);
  const moduleTaskService = plugin.getTaskService();
  if (moduleTaskService === undefined) {
    return;
  }
  const syncCangjieResourceCA = new SyncCangjieResourceCA(moduleTaskService, plugin);
  creatorManager.registry(syncCangjieResourceCA);
  syncCangjieResourceCA.afterConfigure();

  const generateCangjieInteropApiCA = new GenerateCangjieInteropApiCA(moduleTaskService, plugin);
  creatorManager.registry(generateCangjieInteropApiCA);
  generateCangjieInteropApiCA.afterConfigure();

  const generateApiDependenciesCA = new GenerateApiDependenciesCA(moduleTaskService, plugin);
  creatorManager.registry(generateApiDependenciesCA);
  generateApiDependenciesCA.afterConfigure();
  hvigor.taskGraphResolved(node => {
    if (hasBuiltDependencyTree) {
      return;
    }
    hasBuiltDependencyTree = true;
    const commandEntryTasks = node.getCommandEntryTask();
    const isNotNeedSmartBuildInit = commandEntryTasks === undefined ||
      !(commandEntryTasks.includes(CommonHookTask.ASSEMBLE_HAP.name) ||
        commandEntryTasks.includes(CommonHookTask.ASSEMBLE_HAR.name) ||
        commandEntryTasks.includes(CommonHookTask.ASSEMBLE_HSP.name) ||
        commandEntryTasks.includes(CommonHookTask.ASSEMBLE_APP.name));
    if (isNotNeedSmartBuildInit) {
      return;
    }
    const projectModel = plugin.getProjectModel();
    if (projectModel !== undefined) {
      CompileNodeGraphMatch.getInstance().buildUniqueDependencyTrees(projectModel);
    }
  });
  hvigor.buildFinished(buildResult => {
    hasBuiltDependencyTree = false;
  });
}

function isPluginEnabled(): boolean {
  if (isCommandToolLines()) {
    return true;
  }
  const cangjiePluginEnabled = process.env.DEVECO_CANGJIE_PLUGIN_ENABLED;
  return cangjiePluginEnabled === undefined ? false : cangjiePluginEnabled === 'true';
}

function isCommandToolLines(): boolean {
  const defaultPath = process.env.DEVECO_CANGJIE_PATH;
  if (defaultPath !== undefined && defaultPath !== '') {
    if (fs.existsSync(path.join(defaultPath, 'oh-uni-package.json')) ||
      fs.existsSync(path.join(defaultPath, 'uni-package.json'))) {
      return true;
    }
  }
  return false;
}

function getIdeaConfigPath(): string {
  const configPath = process.env.DEVECO_CANGJIE_CONFIG_PATH;
  if (configPath !== undefined && configPath !== '') {
    return path.normalize(configPath);
  }
  return getCjConfigParentPath();
}

function getCjConfigParentPath(): string {
  const homedir = getHomedir();
  if (homedir === null) {
    return '';
  }
  return path.resolve(homedir, '.cangjie', 'build', CURRENT_IDE_VERSION);
}
