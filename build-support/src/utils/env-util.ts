/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {Json5Reader, ProjectModel} from '../../types/hvigor-imports';
import path from 'path';
import fs from 'fs';
import {dependToPlaceHolder, formatEnvName} from './common-utils';
import {cangjieSrcRelatePath} from './cangjie-file-util';
import {AbiEnum} from '../enums/cangjie-cpu-abi-enum';

export function configOhModulesEnv(projectModel: ProjectModel, env: NodeJS.ProcessEnv): void {
  const ohpmPath = path.join(projectModel.getProjectDir(), 'oh_modules', '.ohpm');
  if (!fs.existsSync(ohpmPath)) {
    return;
  }
  const childAllFiles = fs.readdirSync(ohpmPath);
  if (childAllFiles.length === 0) {
    return;
  }
  for (const childFile of childAllFiles) {
    if (!fs.lstatSync(path.join(ohpmPath, childFile)).isDirectory() || childFile === 'oh_modules') {
      continue;
    }
    const splitIndex = childFile.startsWith('@') ? childFile.indexOf('@', 1) : childFile.indexOf('@');
    if (splitIndex === -1) {
      continue;
    }
    let dependName = childFile.substring(0, splitIndex);
    const dependNameArr = dependName.split('+');
    dependName = formatEnvName(dependName);
    const dependPath = path.join(ohpmPath, childFile, 'oh_modules', ...dependNameArr);
    if (!fs.existsSync(path.join(dependPath, cangjieSrcRelatePath())) && !hasLibs(dependPath)) {
      continue;
    }
    env[dependName] = dependPath;
  }
  configLocalModulesEnv(projectModel, env);
}

function hasLibs(dependPath: string): boolean {
  for (const key in AbiEnum) {
    if (Object.prototype.hasOwnProperty.call(AbiEnum, key)) {
      const abi = AbiEnum[key as keyof typeof AbiEnum];
      if (fs.existsSync(path.join(dependPath, 'libs', abi))) {
        return true;
      }
    }
  }
  return false;
}

export function configLocalModulesEnv(projectModel: ProjectModel, env: NodeJS.ProcessEnv): void {
  const allModules = projectModel.getAllModules();
  for (const moduleItem of allModules) {
    if (moduleItem.isHapModule()) {
      continue;
    }
    const ohPackageJson5Path = moduleItem.isOhpmProject() ? moduleItem.getOhPackageJson5Path() :
      moduleItem.getPackageJsonPath();
    if (!fs.existsSync(ohPackageJson5Path)) {
      continue;
    }
    const json = Json5Reader.getJson5Obj(ohPackageJson5Path);
    const packageName: string | undefined = json?.name;
    if (packageName === undefined || packageName === '') {
      continue;
    }
    const packageEnvName = dependToPlaceHolder(packageName);
    if (Object.prototype.hasOwnProperty.call(env, packageEnvName)) {
      continue;
    }
    env[packageEnvName] = moduleItem.getProjectDir();
  }
}
