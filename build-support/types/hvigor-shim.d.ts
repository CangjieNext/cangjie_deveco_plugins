/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

/**
 * 本地 ：把 @ohos/hvigor 的类型入口替换为「公开导出 + 运行时已 export 但 .d.ts 未声明的内部导出」。
 *
 * 工作方式：
 * - tsconfig.json 的 paths 把模块名 `@ohos/hvigor` 重定向到本文件（仅影响 TS 类型解析）。
 * - 本文件第一行 `export * from '../node_modules/@ohos/hvigor/index'` 拉取 index.d.ts 的公开导出。
 * - 之后逐行 re-export hvigor 运行时已 export、但官方 .d.ts 未声明的内部符号（来自 hvigor 自带 src 下的 .d.ts）。
 * - `@ohos/hvigor-common` 一行保持模块名写法，由 tsconfig.json 的 paths 兜底解析到 hvigor 嵌套位置。
 */
export * from '../node_modules/@ohos/hvigor/index';

/** @internal */
export {hvigorCore} from '../node_modules/@ohos/hvigor/src/base/external/core/hvigor-core';

/** @internal */
export {Json5Reader} from '../node_modules/@ohos/hvigor/src/base/util/json5-reader';

/** @internal */
export {iconv} from '../node_modules/@ohos/hvigor/src/common/util/iconv/index';

/** @internal */
export {FileSet} from '../node_modules/@ohos/hvigor/src/base/internal/snapshot/util/file-set';

/** @internal */
export {globalData} from '../node_modules/@ohos/hvigor/src/base/internal/data/global-data';

/** @internal */
export {projectTaskDag} from '../node_modules/@ohos/hvigor/src/base/internal/task/core/task-directed-acyclic-graph';

/** @internal */
export {CoreTask} from '../node_modules/@ohos/hvigor/src/base/external/task/core-task';

/** @internal */
export {DefaultTask} from '../node_modules/@ohos/hvigor/src/base/external/task/default-task';

/** @internal */
export {HvigorLogger} from '../node_modules/@ohos/hvigor/src/base/log/hvigor-log';

/** @internal */
export {IncrementalExecTask} from '../node_modules/@ohos/hvigor/src/base/external/task/incremental-exec-task';

/** @internal */
export type {TaskDetails} from '../node_modules/@ohos/hvigor/src/base/internal/task/interface/task-details-interface';

/** @internal */
export type {TaskInputValue} from '../node_modules/@ohos/hvigor/src/base/internal/snapshot/util/task-input-value-entry';

/** @internal */
export type {
  HvigorCoreNode, Project, Module
} from '../node_modules/@ohos/hvigor/src/base/external/core/hvigor-core-node';