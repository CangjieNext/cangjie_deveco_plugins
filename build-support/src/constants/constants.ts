/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {VersionConst} from '../../types/hvigor-imports';

export const CJ_CONFIG = require(`${__dirname}/../../res/cangjie-build-config.json`);
export const CJ_COLLECT_LIBS_CONFIG = require(`${__dirname}/../../res/cangjie-collect-libs-config.json`);
export const CJ_SCHEMA_CONFIG = require(`${__dirname}/../../res/cangjie-schema-config.json`);
export const CJ_HAR_SCHEMA_CONFIG = require(`${__dirname}/../../res/cangjie-har-schema-config.json`);
export const CJ_SCHEMA_CONFIG_ZH = require(`${__dirname}/../../res/cangjie-schema-config_zh.json`);
export const CJ_HAR_SCHEMA_CONFIG_ZH = require(`${__dirname}/../../res/cangjie-har-schema-config_zh.json`);

export const CANGJIE_NAME = 'cangjie';
export const CJPM_TOML_NAME = 'cjpm.toml';
export const PACKAGE = 'package';
export const NAME = 'name';
export const VERSION = 'version';
export const DEPENDENCIES = 'dependencies';
export const REQUIRES = 'requires';
export const PATH = 'path';
export const GIT = 'git';
export const GIT_COMMIT_ID = 'commitId';
export const CJPM_DEFAULT_PATH = '.cjpm';
export const DEV_DEPENDENCIES = 'dev-dependencies';
export const PACKAGE_REQUIRES = 'package-requires';
export const TARGET = 'target';
export const PATH_OPTION = 'path-option';
export const PACKAGE_OPTION = 'package-option';
export const FFI = 'ffi';
export const C = 'c';
export const BIN_DEPENDENCIES = 'bin-dependencies';
export const SRC_DIR = 'src-dir';
export const CJ_PLUGIN_CACHE_DIR = 'cj-plugin-cache';
export const CJ_PLUGIN_CONFIG_FILE = 'cangjie-config.json';
export const CJ_ENV_CACHE_FILE = 'cj-env-cache.json';
export const ASAN_ENABLED_CONFIG = 'app.asanEnabled';
export const LIBRARY_FILE_REGEXP = '.*\\.so(.*[0-9])?$';
export const CJPM_DEBUG_ARGUMENT = '-g';
export const STD_REQUIRES = 'std-requires';
export const PROFILE = 'profile';
export const BUILD = 'build';
export const COMBINED = 'combined';

export const CURRENT_IDE_VERSION: string = getCurrentIdeVersion();
export const DEMAND_SRC_PACKAGES: string[] = CJ_CONFIG.demandSrcPackages;
export const DEMAND_BIN_PACKAGES: string[] = CJ_CONFIG.demandBinPackages;
export const LOADER_SO_LIBS: string[] = CJ_CONFIG.loaderSoLibs;
export const ASAN_RUNTIME_LIBS: string[] = CJ_CONFIG.asanRuntimeLibs;
export const STD_DEMAND_ALL_LIBS: string[] = CJ_CONFIG.stdDemandAllLibs;
export const BIN_DEPEND_LIBS_MAP = new Map<string, string[]>(Object.entries(CJ_CONFIG.binPkgDependLibsMapper));
export const DEFAULT_COPY_RUNTIME_LIBS_50: string[] = CJ_COLLECT_LIBS_CONFIG.defaultCopyRuntimeLibs50;
export const COMPATIBILITY_RUNTIME_LIBS_50: string[] = CJ_COLLECT_LIBS_CONFIG.compatibilityRuntimeLibs50;
export const ASAN_RUNTIME_DEPEND_LIBS: string[] = CJ_COLLECT_LIBS_CONFIG.asanRuntimeDependLibs;
export const STD_DEPEND_LIBS_MAP_50 = new Map<string, string[]>(
  Object.entries(CJ_COLLECT_LIBS_CONFIG.stdPkgDependLibsMapper50));
export const TPC_DEPEND_LIBS: string[] = CJ_CONFIG.tpcDependLibs;
export const CANGJIE_OPTIONS_CONFIG = CJ_SCHEMA_CONFIG.cangjieOptionsConfig;
export const CANGJIE_OPTIONS_HAR_CONFIG = CJ_HAR_SCHEMA_CONFIG.cangjieOptionsConfig;
export const CANGJIE_OPTIONS_CONFIG_ZH = CJ_SCHEMA_CONFIG_ZH.cangjieOptionsConfig;
export const CANGJIE_OPTIONS_HAR_CONFIG_ZH = CJ_HAR_SCHEMA_CONFIG_ZH.cangjieOptionsConfig;
export const CJPM_TEST_SUCCESS_FLAG = 'cjpm test success';
export const CJPM_TEST_EXECUTABLE_FILES_PREFIX = 'Executable files at';
export const CANGJIE_OPTIONS = 'cangjieOptions';

function getCurrentIdeVersion(): string {
  const parts = VersionConst.CURRENT_MODEL_VERSION.split('.');
  return parts.length >= 3 ? parts.slice(0, 2).join('.') : CJ_CONFIG.currentIdeVersion;
}
