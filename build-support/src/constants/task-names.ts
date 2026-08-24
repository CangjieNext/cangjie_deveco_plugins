/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import type {TaskDetails} from '../../types/hvigor-imports';

export enum TaskGroupType {
  RESOURCES_TASK_GROUP = 'Resources',
  VERIFICATION_TASK_GROUP = 'Verification',
}

/** 注册的 Hvigor-ohos-plugin 任务名的统一管理类 */
export namespace TaskNames {
  export class Task {
    static readonly UNIT_TEST_BUILD: TaskDetails = {
      name: 'UnitTestBuild',
      description: 'Build a unit test.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly PRE_BUILD: TaskDetails = {
      name: 'PreBuild',
      description: 'Pre-build in the stage model.',
      group: 'Verification',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly COMPILE_RESOURCE: TaskDetails = {
      name: 'CompileResource',
      description: 'Compile project resources in the stage model.',
      group: 'Resources',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly PACKAGE_HAP: TaskDetails = {
      name: 'PackageHap',
      description: 'Build the HAP package in the stage model.',
      group: 'Package',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly FAST_PACKAGE: TaskDetails = {
      name: 'FastPackageHap',
      description: 'Fast build the HAP package in the stage model.',
      group: 'Package',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly PACKAGE_HSP: TaskDetails = {
      name: 'PackageHsp',
      description: 'Build the HSP package in the stage model.',
      group: 'Package',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly FAST_PACKAGE_HSP: TaskDetails = {
      name: 'FastPackageHsp',
      description: 'Fast Build the HSP package in the stage model.',
      group: 'Package',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly BUILD_NATIVE_WITH_NINJA: TaskDetails = {
      name: 'BuildNativeWithNinja',
      description: 'Compile CPP source with Ninja in the stage model.',
      group: 'Native',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly PACKAGE_HAR: TaskDetails = {
      name: 'PackageHar',
      description: 'Build the HAR package in the stage model.',
      group: 'Package',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly PROCESS_LIB: TaskDetails = {
      name: 'ProcessLibs',
      description: 'Process .so files in the stage model.',
      group: 'Resources',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly DO_NATIVE_STRIP: TaskDetails = {
      name: 'DoNativeStrip',
      description: 'Strip .so files to decrease size.',
      group: 'Native',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly CACHE_NATIVE_LIBS: TaskDetails = {
      name: 'CacheNativeLibs',
      description: 'cache native strip .so fileInfo',
      group: 'Native',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
  }

  export class CommonTask {
    static readonly CLEAN: TaskDetails = {
      name: 'clean',
      description: 'Clear the cache information.',
      group: 'Other',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
  }

  export class CommonHookTask {
    static readonly BUILD_PREVIEWER_RES: TaskDetails = {
      name: 'buildPreviewerResource',
      description: 'Build the preview resources.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly ASSEMBLE_APP: TaskDetails = {
      name: 'assembleApp',
      description: 'Assemble the task for the packaged app.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly ASSEMBLE_HAP: TaskDetails = {
      name: 'assembleHap',
      description: 'Assemble the task for the packaged HAP file.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly ASSEMBLE_HSP: TaskDetails = {
      name: 'assembleHsp',
      description: 'Assemble the task for the packaged HSP file.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
    static readonly ASSEMBLE_HAR: TaskDetails = {
      name: 'assembleHar',
      description: 'Assemble the task for the packaged HAR file.',
      group: 'Hook',
      sourcePluginId: 'hvigor-ohos-plugin'
    };
  }
}
