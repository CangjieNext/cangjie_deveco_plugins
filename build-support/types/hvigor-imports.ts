/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

/**
 * 工程内 @ohos/hvigor 与 @ohos/hvigor-ohos-plugin 的唯一导入入口。
 */

export {isWindows, isMac, isLinux} from '../src/utils/system-util';
export {formatTime} from '../src/utils/time-util';
export {HvigorBuildConst} from '../src/constants/build-const';
export {HVIGOR_PROCESS_EVENT_ID} from '../src/constants/event-id-options';
export {formatTimeToNumPair} from '../src/utils/time-util';
export type {BooleanCallback} from '../src/common/callback-type';
export {BuildDirConst, BuildArtifactConst} from '../src/constants/build-directory-const';
export {
  CommonConst, DefaultTargetConst, ValidateRegExp
} from '../src/constants/common-const';
export {ModuleType} from '../src/enums/module-type-enum';
export {ApiType} from '../src/enums/hap-extra-info';
export {DependencyType} from '../src/enums/dependency-interface';
export {SdkComponentType} from '../src/enums/sdk-component-type';
export {TaskGroupType, TaskNames} from '../src/constants/task-names';
export {LogCombineType} from '../src/enums/log-combine-type';

// ===== 来自 @ohos/hvigor(浅路径,经 shim 解析)=====
export * from '@ohos/hvigor';

// ===== 来自 @ohos/hvigor/src/...(深路径)=====
export type {TaskContainer} from '@ohos/hvigor/src/base/internal/task/interface/task-container-interface';
export {instanceOf} from '@ohos/hvigor/src/base/util/class-identify-util';
export {hvigorProcess} from '@ohos/hvigor/src/base/internal/lifecycle/hvigor-process';

// ===== 来自 @ohos/hvigor-ohos-plugin/src/...(深路径)=====
export {PluginFactory} from '@ohos/hvigor-ohos-plugin/src/plugin/factory/plugin-factory';
export type {HapPlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/hap-plugin';
export type {HarPlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/har-plugin';
export type {HspPlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/hsp-plugin';
export type {AppPlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/app-plugin';
export {AbstractHapModulePlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/common/abstract-hap-module-plugin';
export {AbstractHarModulePlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/common/abstract-har-module-plugin';
export type {AbstractModulePlugin} from '@ohos/hvigor-ohos-plugin/src/plugin/common/abstract-module-plugin';
export {ModulePathInfoIml} from '@ohos/hvigor-ohos-plugin/src/common/iml/module-path-info-iml';
export {buildOptionPath} from '@ohos/hvigor-ohos-plugin/src/common/build-option-path-info';

export {VersionConst} from '@ohos/hvigor-ohos-plugin/src/const/version-const';
export {BuildProfileSchemaFileConst} from '@ohos/hvigor-ohos-plugin/src/const/common-const';
export type {ModuleModel} from '@ohos/hvigor-ohos-plugin/src/model/module/module-model';
export {CoreModuleModelImpl} from '@ohos/hvigor-ohos-plugin/src/model/module/core-module-model-impl';
export type {ProjectModel} from '@ohos/hvigor-ohos-plugin/src/model/project/project-model';
export type {TargetSourceSetModel} from '@ohos/hvigor-ohos-plugin/src/model/source-set/source-set-model';
export {SourceSetModel} from '@ohos/hvigor-ohos-plugin/src/model/source-set/source-set-model';
export type {TargetSourceSetImpl} from '@ohos/hvigor-ohos-plugin/src/model/source-set/target-source-set-impl';
export type {CangjieOpt} from '@ohos/hvigor-ohos-plugin/src/options/build/build-opt';
export {ProjectBuildProfile} from '@ohos/hvigor-ohos-plugin/src/options/build/project-build-profile';
export {ConfigJson} from '@ohos/hvigor-ohos-plugin/src/options/configure/config-json-options';
export {ModuleJson} from '@ohos/hvigor-ohos-plugin/src/options/configure/module-json-options';
export {Dependency} from '@ohos/hvigor-ohos-plugin/src/project/dependency/core/dependency-interface';
export {buildOptionManager} from '@ohos/hvigor-ohos-plugin/src/project/build-option/build-mode-manager';
export {OhosSdkLoader} from '@ohos/hvigor-ohos-plugin/src/sdk/ohos-sdk-loader';
export {HmosSdkLoader} from '@ohos/hvigor-ohos-plugin/src/sdk/hmos-sdk-loader';
export {AbstractModuleHookTask} from '@ohos/hvigor-ohos-plugin/src/tasks/hook/abstract-module-hook-task';
export {
  TaskCreatorManager, GlobalTaskCreator, TargetTaskCreator
} from '@ohos/hvigor-ohos-plugin/src/tasks/task-creator';
export type {TargetTaskService} from '@ohos/hvigor-ohos-plugin/src/tasks/service/target-task-service';
export type {ModuleTaskService} from '@ohos/hvigor-ohos-plugin/src/tasks/service/module-task-service';
export {PreBuild} from '@ohos/hvigor-ohos-plugin/src/tasks/pre-build';
export {OhosHapTask} from '@ohos/hvigor-ohos-plugin/src/tasks/task/ohos-hap-task';
export {OhosLogger} from '@ohos/hvigor-ohos-plugin/src/utils/log/ohos-logger';
export {FileUtil as FileUtilOhos} from '@ohos/hvigor-ohos-plugin/src/utils/file-util';
export {InjectUtil} from '@ohos/hvigor-ohos-plugin/src/utils/inject-util';
export {JsonUtil} from '@ohos/hvigor-ohos-plugin/src/utils/json-util';
export {ValidatorStore} from '@ohos/hvigor-ohos-plugin/src/utils/validate/validator-store';
export {resModelLoader} from '@ohos/hvigor-ohos-plugin/src/utils/loader/file/res-model-loader';
export {NpmPackageResolver} from '@ohos/hvigor-ohos-plugin/src/utils/resolver/npm-package-resolver';
export {OhpmPackageResolver} from '@ohos/hvigor-ohos-plugin/src/utils/resolver/ohpm-package-resolver';
export type {BasePackageResolver} from '@ohos/hvigor-ohos-plugin/src/utils/resolver/base-package-resolver';
export {CmakeUtil} from '@ohos/hvigor-ohos-plugin/src/utils/cmake/cmake-util';
export {HarTargetUtil} from '@ohos/hvigor-ohos-plugin/src/utils/har-target-util';
export {hvigorOrToolChainsChanged} from '@ohos/hvigor-ohos-plugin/src/utils/meta-util';
