/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {AbstractModulePlugin, Module, ModuleModel, TargetTaskService} from '../../types/hvigor-imports';
import {TaskContainer, TaskCreatorManager, TaskNames} from '../../types/hvigor-imports';
import {initSdkInfo} from '../sdk/cangjie-sdk-info';
import {CangjieTaskNames} from '../task/cangjie-task-names';
import {
  AddApiDependenciesCA,
  AfterCompileCangjieCA,
  BeforeProcessLibsCA,
  CangjiePreBuildCA,
  CompileCangjieCA,
  CompileCangjieForIdlCA,
  GenerateCangjieResourceCA,
  GenerateTomlDependenciesCA,
  MoveCangjieLibsCA,
  PreviewGenerateCangjieResourceCA,
  ProcessCangjieLibsCA,
  UnitTestCompileCangjieCA
} from '../task/cangjie-task-initializer';
import {checkIsValid, isCangjieModule} from '../utils/common-utils';

/**
 * cangjie plugin basic class
 *
 * @since 2024/1/6
 */
export class BaseCangjiePlugin {
  protected modulePlugin: AbstractModulePlugin;

  protected _module: Module | undefined;

  protected _moduleModel: ModuleModel | undefined;

  protected _needExecTargetServiceList: TargetTaskService[] | undefined;

  constructor(modulePlugin: AbstractModulePlugin) {
    this.modulePlugin = modulePlugin;
    this._moduleModel = this.modulePlugin.getModuleModel();
    this._module = this._moduleModel?.getModule();
    this._needExecTargetServiceList = this.modulePlugin.getNeedExecTargetServiceList();
  }

  async initTasks(): Promise<void> {
    if (this._needExecTargetServiceList === undefined || this._module === undefined) {
      return;
    }
    const taskContainer = this._module.getTaskContainer();
    const creatorManager: TaskCreatorManager = new TaskCreatorManager(taskContainer);
    const moduleDir = this._moduleModel?.getProjectDir();
    if (moduleDir === undefined) {
      return;
    }
    const hasCangjie = isCangjieModule(this.modulePlugin);
    if (!hasCangjie && (this._moduleModel?.isHapModule() || this._moduleModel?.isHspModule())) {
      for (const target of this._needExecTargetServiceList) {
        const targetName = target.getTargetData().getTargetName();
        if (taskContainer.hasTask(`${targetName}@${CangjieTaskNames.MOVE_CANGJIE_LIBS.name}`)) {
          continue;
        }

        // add move cangjie libs task
        const moveCangjieLibsCA = new MoveCangjieLibsCA(target, false);
        creatorManager.registry(moveCangjieLibsCA);
        moveCangjieLibsCA.afterConfigure();
        const doNativeStrip = taskContainer.findTask(`${targetName}@${TaskNames.Task.DO_NATIVE_STRIP.name}`);
        doNativeStrip?.dependsOn(`${targetName}@${CangjieTaskNames.MOVE_CANGJIE_LIBS.name}`);
      }
      return;
    }
    if (!hasCangjie) {
      return;
    }
    await this.doAddCangjieTasks(creatorManager, taskContainer);
  }

  private async doAddCangjieTasks(creatorManager: TaskCreatorManager, taskContainer: TaskContainer): Promise<void> {
    if (this._needExecTargetServiceList === undefined) {
      return;
    }
    for (const target of this._needExecTargetServiceList) {
      const targetName = target.getTargetData().getTargetName();
      if (taskContainer.hasTask(`${targetName}@${CangjieTaskNames.COMPILE_CJ_NODE.name}`)) {
        continue;
      }
      const doNativeStrip = taskContainer.findTask(`${targetName}@${TaskNames.Task.DO_NATIVE_STRIP.name}`);

      // add after compiler cangjie task
      const moveCangjieLibsCA = new MoveCangjieLibsCA(target, false);
      creatorManager.registry(moveCangjieLibsCA);
      moveCangjieLibsCA.afterConfigure();
      doNativeStrip?.dependsOn(`${targetName}@${CangjieTaskNames.MOVE_CANGJIE_LIBS.name}`);

      // add generate cangjie resource task
      const generateCangjieResourceCA = new GenerateCangjieResourceCA(target, false);
      creatorManager.registry(generateCangjieResourceCA);
      generateCangjieResourceCA.afterConfigure();

      // add previewer generate cangjie resource task
      const previewGenerateCangjieResourceCA = new PreviewGenerateCangjieResourceCA(target, false);
      creatorManager.registry(previewGenerateCangjieResourceCA);
      previewGenerateCangjieResourceCA.afterConfigure();
      const buildPreviewerRes = taskContainer.findTask(TaskNames.CommonHookTask.BUILD_PREVIEWER_RES.name);
      buildPreviewerRes?.dependsOn(`${targetName}@${CangjieTaskNames.PREVIEW_GENERATE_CJ_RESOURCE.name}`);

      // add unittest compile cangjie
      const unittestCompileCangjieCA = new UnitTestCompileCangjieCA(target, false);
      creatorManager.registry(unittestCompileCangjieCA);
      unittestCompileCangjieCA.afterConfigure();
      const unitTestBuild = taskContainer.findTask(TaskNames.Task.UNIT_TEST_BUILD.name);
      unitTestBuild?.dependsOn(`${targetName}@${CangjieTaskNames.UNIT_TEST_COMPILE_CJ_NODE.name}`);

      // add compile cangjie idl task
      const compileCangjieForIdlCA = new CompileCangjieForIdlCA(target, false);
      creatorManager.registry(compileCangjieForIdlCA);
      compileCangjieForIdlCA.afterConfigure();

      const generateTomlDependenciesCA = new GenerateTomlDependenciesCA(target, false);
      creatorManager.registry(generateTomlDependenciesCA);
      generateTomlDependenciesCA.afterConfigure();

      const addApiDependenciesCA = new AddApiDependenciesCA(target, false);
      creatorManager.registry(addApiDependenciesCA);
      addApiDependenciesCA.afterConfigure();

      if (!checkIsValid(target?.getBuildOption().cangjieOptions?.path)) {
        continue;
      }

      // disabled pre-build, enable cangjie-pre-build
      const preBuildTask = taskContainer.findTask(`${targetName}@${TaskNames.Task.PRE_BUILD.name}`);
      preBuildTask?.setEnabled(false);
      const cangjiePreBuildCA = new CangjiePreBuildCA(target, false);
      creatorManager.registry(cangjiePreBuildCA);
      cangjiePreBuildCA.afterConfigure();
      preBuildTask?.dependsOn(`${targetName}@${CangjieTaskNames.CANGJIE_PRE_BUILD.name}`);

      // add before process libs task
      const beforeProcessLibsCA = new BeforeProcessLibsCA(target, false);
      creatorManager.registry(beforeProcessLibsCA);
      beforeProcessLibsCA.afterConfigure();

      // add compile cangjie task
      const compileCangjieCA = new CompileCangjieCA(target, false);
      const processCangjieLibsCA = new ProcessCangjieLibsCA(target, false);
      creatorManager.registry(compileCangjieCA);
      creatorManager.registry(processCangjieLibsCA);
      compileCangjieCA.afterConfigure();
      processCangjieLibsCA.afterConfigure();
      doNativeStrip?.dependsOn(`${targetName}@${CangjieTaskNames.PROCESS_CJ_LIBS.name}`);
      const processLibs = taskContainer.findTask(`${targetName}@${TaskNames.Task.PROCESS_LIB.name}`);
      processLibs?.dependsOn(`${targetName}@${CangjieTaskNames.COMPILE_CJ_NODE.name}`);

      // add after compiler cangjie task
      const afterCompileCangjieCA = new AfterCompileCangjieCA(target, false);
      creatorManager.registry(afterCompileCangjieCA);
      afterCompileCangjieCA.afterConfigure();
      const cacheNativeLibs = taskContainer.findTask(`${targetName}@${TaskNames.Task.CACHE_NATIVE_LIBS.name}`);
      cacheNativeLibs?.dependsOn(`${targetName}@${CangjieTaskNames.AFTER_COMPILE_CANGJIE.name}`);

      // init cangjie sdk
      await initSdkInfo(target);
    }
  }
}
