/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {CoreTask, TaskDetails} from '../../types/hvigor-imports';
import {
  ApiType,
  CmakeUtil,
  DefaultTargetConst,
  GlobalTaskCreator,
  HarTargetUtil,
  hvigorOrToolChainsChanged,
  OhosLogger,
  PreBuild,
  TargetTaskCreator,
  TargetTaskService,
  TaskNames
} from '../../types/hvigor-imports';
import {CangjieTaskNames} from './cangjie-task-names';
import {GenerateCangjieResource} from './generate-cangjie-resource';
import {CompileCangjie} from './compile-cangjie';
import {SyncCangjieResource} from './sync-cangjie-resource';
import {AfterCompileCangjie} from './after-compile-cangjie';
import {PreviewGenerateCangjieResource} from './preview-generate-cangjie-resource';
import {ProcessCangjieLibs} from './process-cangjie-libs';
import {CangjiePreBuild} from './cangjie-pre-build';
import {checkIsValid} from '../utils/common-utils';
import {GenerateCangjieInteropApi} from './generate-cangjie-interop-api';
import {CompileCangjieForIdl} from './compile-cangjie-for-idl';
import {GenerateTomlDependencies} from './sync/generate-toml-dependencies';
import fs from 'fs';
import {C, FFI, PATH} from '../constants/constants';
import path from 'path';
import {BeforeProcessLibs} from './before-process-libs';
import {readTomlContent} from '../utils/cangjie-file-util';
import {MoveCangjieLibs} from './move-cangjie-libs';
import {CangjieLogger} from '../log/cangjie-logger';
import {AddApiDependencies} from './systemapi/add-api-dependencies';
import {GenerateApiDependencies} from './systemapi/generate-api-dependencies';
import {UnitTestCompileCangjie} from './unitTest/unit-test-compile-cangjie';
import CommonTask = TaskNames.CommonTask;

const logger: OhosLogger = CangjieLogger.getLogger('cangjie-task-initialzers');

export class CangjiePreBuildCA extends TargetTaskCreator {
  provider = (): CoreTask => new CangjiePreBuild(this.targetService);

  declareDepends = (): string[] => hvigorOrToolChainsChanged(this.targetService.getSdkInfo()) ?
    [CommonTask.CLEAN.name] : [];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.CANGJIE_PRE_BUILD;

  afterConfigure = (): void => {
    this.depends = this.declareDepends().map(depend => `${this.node.getName()}:${depend}`);
  };
}

export class GenerateCangjieResourceCA extends TargetTaskCreator {
  declareDepends = (): string[] => [PreBuild.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.GENERATE_CJ_RESOURCE;

  provider = (): CoreTask => new GenerateCangjieResource(this.targetService);
}

export class PreviewGenerateCangjieResourceCA extends TargetTaskCreator {
  declareDepends = (): string[] => [PreBuild.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.PREVIEW_GENERATE_CJ_RESOURCE;

  provider = (): CoreTask => new PreviewGenerateCangjieResource(this.targetService);
}

export class CompileCangjieCA extends TargetTaskCreator {
  declareDepends = (): string[] => {
    const depends: string[] = [GenerateCangjieResource.name, TaskNames.Task.COMPILE_RESOURCE.name];
    getModuleDependencies(this.targetService, CangjieTaskNames.COMPILE_CJ_NODE.name, depends);
    if (getCppDepends(this.targetService).length > 0) {
      depends.push(BeforeProcessLibs.name);
    }
    return depends;
  };

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.COMPILE_CJ_NODE;

  provider = (): CoreTask => new CompileCangjie(this.targetService);
}

export class ProcessCangjieLibsCA extends TargetTaskCreator {
  declareDepends = (): string[] => [CompileCangjie.name, TaskNames.Task.PROCESS_LIB.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.PROCESS_CJ_LIBS;

  provider = (): CoreTask => new ProcessCangjieLibs(this.targetService);
}

export class AfterCompileCangjieCA extends TargetTaskCreator {
  declareDepends = (): string[] => [TaskNames.Task.DO_NATIVE_STRIP.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.AFTER_COMPILE_CANGJIE;

  provider = (): CoreTask => new AfterCompileCangjie(this.targetService);
}

export class MoveCangjieLibsCA extends TargetTaskCreator {
  declareDepends = (): string[] => [TaskNames.Task.PROCESS_LIB.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.MOVE_CANGJIE_LIBS;

  provider = (): CoreTask => new MoveCangjieLibs(this.targetService);
}


export class SyncCangjieResourceCA extends GlobalTaskCreator {
  declareDepends = (): string[] => [GenerateCangjieResource.name, GenerateTomlDependencies.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.SYNC_CJ_RESOURCE;

  provider = (): CoreTask => new SyncCangjieResource(this.service);
}

export class GenerateCangjieInteropApiCA extends GlobalTaskCreator {
  declareDepends = (): string[] => [CompileCangjieForIdl.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.GENERATE_CANGJIE_INTEROP_API;

  provider = (): CoreTask => new GenerateCangjieInteropApi(this.service);
}

export class CompileCangjieForIdlCA extends TargetTaskCreator {
  declareDepends = (): string[] => {
    const depends: string[] = [GenerateCangjieResource.name, TaskNames.Task.COMPILE_RESOURCE.name];
    getModuleDependencies(this.targetService, CangjieTaskNames.COMPILE_CANGJIE_FOR_IDL.name, depends);
    if (getCppDepends(this.targetService).length > 0) {
      depends.push(BeforeProcessLibs.name);
    }
    return depends;
  };

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.COMPILE_CANGJIE_FOR_IDL;

  provider = (): CoreTask => new CompileCangjieForIdl(this.targetService);
}

export class GenerateTomlDependenciesCA extends TargetTaskCreator {
  declareDepends = (): string[] => [PreBuild.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.GENERATE_TOML_DEPENDENCIES;

  provider = (): CoreTask => new GenerateTomlDependencies(this.targetService);
}

export class BeforeProcessLibsCA extends TargetTaskCreator {
  declareDepends = (): string[] => [TaskNames.Task.BUILD_NATIVE_WITH_NINJA.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.BEFORE_PROCESS_LIBS;

  provider = (): CoreTask => new BeforeProcessLibs(this.targetService);
}

export class AddApiDependenciesCA extends TargetTaskCreator {
  declareDepends = (): string[] => [];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.ADD_API_DEPENDENCIES;

  provider = (): CoreTask => new AddApiDependencies(this.targetService);
}

export class GenerateApiDependenciesCA extends GlobalTaskCreator {
  declareDepends = (): string[] => [AddApiDependencies.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.GENERATE_API_DEPENDENCIES;

  provider = (): CoreTask => new GenerateApiDependencies(this.service);
}

export class UnitTestCompileCangjieCA extends TargetTaskCreator {
  declareDepends = (): string[] => [TaskNames.Task.COMPILE_RESOURCE.name];

  declareTaskDetail = (): TaskDetails => CangjieTaskNames.UNIT_TEST_COMPILE_CJ_NODE;

  provider = (): CoreTask => new UnitTestCompileCangjie(this.targetService);
}

function getModuleDependencies(targetService: TargetTaskService, taskName: string, depends: string[]): void {
  const moduleService = targetService.getModuleService();
  const projectModel = moduleService.getProjectModel();
  const depHarTargets = HarTargetUtil.calDepHarTargets(targetService);
  depHarTargets.forEach((targetName, harName) => {
    const targetTaskService =
      projectModel.getTarget(harName, targetName) ??
      projectModel.getTarget(harName, DefaultTargetConst.DEFAULT_TARGET) ??
      projectModel.getTarget(harName);
    if (checkIsValid(targetTaskService?.getBuildOption().cangjieOptions?.path)) {
      depends.push(`${harName}:${targetName}@${taskName}`);
    }
  });
}

export function getCppDepends(targetService: TargetTaskService): string[] {
  const moduleModel = targetService.getModuleService().getModuleModel();
  const targetData = targetService.getTargetData();
  const buildOption = targetService.getBuildOption();
  const depends: string[] = [];
  if (moduleModel.getApiType() !== ApiType.STAGE ||
    !CmakeUtil.nativeTaskCondition(moduleModel, targetData, buildOption.externalNativeOptions)) {
    return depends;
  }
  const targetName = targetData.getTargetName();
  let tomlPath = buildOption.cangjieOptions?.path;
  if (targetName === DefaultTargetConst.OHOS_TEST_TARGET || tomlPath === undefined) {
    return depends;
  }
  if (!path.isAbsolute(tomlPath)) {
    tomlPath = path.resolve(moduleModel.getProjectDir(), tomlPath);
  }
  if (!fs.existsSync(tomlPath)) {
    return depends;
  }
  const tomlData = readTomlContent(tomlPath, logger);
  if (!Object.prototype.hasOwnProperty.call(tomlData, FFI) || !Object.prototype.hasOwnProperty.call(tomlData[FFI], C)) {
    return depends;
  }
  const foreignRequires = tomlData[FFI][C];
  if (foreignRequires === undefined || Object.keys(foreignRequires).length <= 0) {
    return depends;
  }
  getFfiLibRequires(moduleModel.getProjectDir(), foreignRequires, tomlPath, depends);
  return depends;
}

function getFfiLibRequires(moduleDir: string, foreignRequires: any, tomlPath: any,
  depends: string[]): void {
  const defaultLibPath = path.join(moduleDir, 'libs');
  Object.keys(foreignRequires).forEach((libFileName) => {
    let libFilePath = foreignRequires[libFileName][PATH];
    if (!checkIsValid(libFilePath) || tomlPath === undefined) {
      return;
    }
    if (!path.isAbsolute(libFilePath)) {
      libFilePath = path.resolve(path.dirname(tomlPath), libFilePath);
    }
    if (!libFilePath.startsWith(defaultLibPath)) {
      return;
    }
    depends.push(path.join(libFilePath, `lib${libFileName}.so`));
  });
}
