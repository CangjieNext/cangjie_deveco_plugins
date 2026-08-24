/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TaskDetails} from '../../types/hvigor-imports';
import {TaskGroupType} from '../../types/hvigor-imports';

/**
 * cangjie task names
 *
 * @since 2024/1/6
 */
export class CangjieTaskNames {
  static readonly COMPILE_CJ_NODE: TaskDetails = {
    name: 'CompileCangjie',
    description: 'Compile cangjie components.',
  };

  static readonly GENERATE_CJ_RESOURCE: TaskDetails = {
    name: 'GenerateCangjieResource',
    description: 'Generate cangjie register file.',
  };

  static readonly PROCESS_CJ_LIBS: TaskDetails = {
    name: 'ProcessCangjieLibs',
    description: 'Process cangjie .so files in the stage model.',
  };

  static readonly PREVIEW_GENERATE_CJ_RESOURCE: TaskDetails = {
    name: 'PreviewGenerateCangjieResource',
    description: 'Generate cangjie preview register file.',
  };

  static readonly AFTER_COMPILE_CANGJIE: TaskDetails = {
    name: 'AfterCompileCangjie',
    description: 'Process .cjo files in the stage model.',
    group: TaskGroupType.RESOURCES_TASK_GROUP,
  };

  static readonly MOVE_CANGJIE_LIBS: TaskDetails = {
    name: 'MoveCangjieLibs',
    description: 'Move cangjie har libs.',
  };

  static readonly SYNC_CJ_RESOURCE: TaskDetails = {
    name: 'SyncCangjieResource',
    description: 'Sync cangjie resource.',
  };

  static readonly CANGJIE_PRE_BUILD: TaskDetails = {
    name: 'CangjiePreBuild',
    description: 'Pre-build in the stage model.',
    group: TaskGroupType.VERIFICATION_TASK_GROUP,
  };

  static readonly GENERATE_CANGJIE_INTEROP_API: TaskDetails = {
    name: 'GenerateCangjieInteropApi',
    description: 'Generate cangjie-arkts interop api.',
  };

  static readonly COMPILE_CANGJIE_FOR_IDL: TaskDetails = {
    name: 'CompileCangjieForIdl',
    description: 'Compile cangjie for generate idl.',
  };

  static readonly GENERATE_TOML_DEPENDENCIES: TaskDetails = {
    name: 'GenerateTomlDependencies',
    description: 'Generate toml dependencies.',
  };

  static readonly BEFORE_PROCESS_LIBS: TaskDetails = {
    name: 'BeforeProcessLibs',
    description: 'Before process libs.',
  };

  static readonly ADD_API_DEPENDENCIES: TaskDetails = {
    name: 'AddApiDependencies',
    description: 'Add api dependencies.',
  };

  static readonly GENERATE_API_DEPENDENCIES: TaskDetails = {
    name: 'GenerateApiDependencies',
    description: 'Generate api dependencies.',
  };

  static readonly UNIT_TEST_COMPILE_CJ_NODE: TaskDetails = {
    name: 'UnitTestCompileCangjie',
    description: 'Unit test compile cangjie components.',
  };
}
