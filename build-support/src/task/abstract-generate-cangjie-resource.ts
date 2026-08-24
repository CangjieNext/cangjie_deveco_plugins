/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import path from 'path';
import fs from 'fs';
import type {TargetSourceSetModel, TargetTaskService} from '../../types/hvigor-imports';
import {
  DefaultTargetConst,
  FileSet,
  InjectUtil,
  OhosLogger,
  type TaskDetails,
  TaskInputValue,
  ValidateRegExp
} from '../../types/hvigor-imports';
import {BaseCangjieTask} from './base-cangjie-task';
import {NAME, PACKAGE} from '../constants/constants';
import {checkIsValid, replaceWithEnv} from '../utils/common-utils';
import {
  createDir,
  onlyCangjieModule,
  readModuleConfigToml,
  readTomlSrcDir,
  validateCangjieEntry
} from '../utils/cangjie-file-util';
import {TOML} from '../utils/toml/toml-export';
import {CangjieLogger} from '../log/cangjie-logger';

/**
 * abstract generate cangjie resource
 *
 * @since 2024/1/6
 */
export abstract class AbstractGenerateCangjieResource extends BaseCangjieTask {
  private agcrLogger: OhosLogger = CangjieLogger.getLogger(AbstractGenerateCangjieResource.name);

  protected constructor(taskService: TargetTaskService, taskDetails: TaskDetails) {
    super(taskService, taskDetails);
  }

  declareInputFiles(): FileSet {
    if (!onlyCangjieModule(this.moduleModel.getProjectDir())) {
      return new FileSet();
    }
    const fileSet = super.declareInputFiles();
    const moduleJson5 = path.resolve(this.moduleModel.getProjectDir(), 'src', 'main', 'module.json5');
    if (fs.existsSync(moduleJson5)) {
      fileSet.addEntry(moduleJson5, {isDirectory: false});
    }
    if (fs.existsSync(this.cjpmTomlPath)) {
      fileSet.addEntry(this.cjpmTomlPath, {isDirectory: false});
    }
    if (InjectUtil.isOhosTest()) {
      const testModuleJson = path.join(this.moduleModel.getProjectDir(), 'src', DefaultTargetConst.OHOS_TEST_TARGET,
        'module.json5');
      if (fs.existsSync(testModuleJson)) {
        fileSet.addEntry(testModuleJson, {isDirectory: false});
      }
      const moduleConfigToml = readModuleConfigToml(this.targetService);
      if (fs.existsSync(moduleConfigToml)) {
        fileSet.addEntry(moduleConfigToml, {isDirectory: false});
      }
    }
    return fileSet;
  }

  declareInputs(): Map<string, TaskInputValue> {
    const map = new Map<string, TaskInputValue>();
    map.set('isOhosTest', InjectUtil.isOhosTest());
    return map;
  }

  protected doTaskAction(): void {
    if (!onlyCangjieModule(this.moduleModel.getProjectDir())) {
      return;
    }
    if (InjectUtil.isOhosTest() && this.moduleModel.isHapModule()) {
      const cangjieOptionPath = this.cangjieOption?.path;
      if (cangjieOptionPath === undefined || cangjieOptionPath === null || cangjieOptionPath === '') {
        return;
      }
      this.initCjSourceDir(cangjieOptionPath);
    }
    const packageName = this.readTomlName();
    if (!this.moduleModel.isHapModule() && !InjectUtil.isOhosTest()) {
      return;
    }
    this.generateAbilityStageRegisters(packageName);
    this.generateAbilityRegisters(packageName);
  }

  protected isDependHarModule(): boolean {
    return !(this.moduleModel.isHarModule() &&
      this.targetService.getTargetData().getTargetName() !== DefaultTargetConst.OHOS_TEST_TARGET);
  }

  protected generateAbilityRegisters(packageName: string): void {
    const targetSourceSetModel: TargetSourceSetModel =
      this.targetService.getTargetData().getModuleSourceSetModel() as TargetSourceSetModel;
    const targetRes = targetSourceSetModel.getModuleTargetRes();
    const targetModuleOptObj = targetRes.getModuleJsonOpt();
    const abilityList = targetModuleOptObj.module.abilities;
    if (!checkIsValid(abilityList)) {
      return;
    }
    let importPackages = '';
    let registerAbilities = '';
    abilityList?.forEach(ability => {
      const name = ability.name;
      const srcEntry = ability.srcEntry;
      if (srcEntry !== undefined && ValidateRegExp.STARTUP_TASK_SRC_ENTRY_REG_EXP.test(path.extname(srcEntry))) {
        return;
      }
      if (srcEntry === undefined || !validateCangjieEntry(packageName, srcEntry)) {
        this.agcrLogger.printErrorExit('ABILITIES_SRC_ENTRY_IS_INCORRECT',
          [srcEntry, path.resolve(this.moduleModel.getProjectDir(), 'src', 'main', 'module.json5')]);
        return;
      }
      if (!(srcEntry.startsWith(`${packageName}.`) && srcEntry?.substring(packageName.length + 1).indexOf('.') ===
        -1)) {
        importPackages += `import ${srcEntry}\n`;
      }
      const abilityName = srcEntry.substring(srcEntry.lastIndexOf('.') + 1);
      registerAbilities +=
        `let ${name.toUpperCase()}_REGISTER_RESULT = UIAbility.registerCreator("${name}", {=> ${abilityName}()})\n`;
    });
    const defaultImport = `package ${packageName}\n\nimport kit.AbilityKit.UIAbility\n`;
    const cangjieSrcPath = path.resolve(this.cjSource, readTomlSrcDir(this.readTomlContent()));
    if (!fs.existsSync(cangjieSrcPath)) {
      return;
    }
    const registerFilePath = path.join(cangjieSrcPath, 'ability_mainability_entry.cj');
    if (checkIsValid(registerAbilities)) {
      createDir(cangjieSrcPath);
      registerAbilities = `${defaultImport}${importPackages}\n${registerAbilities}`;
      fs.writeFileSync(registerFilePath, registerAbilities);
    } else if (fs.existsSync(registerFilePath)) {
      fs.unlinkSync(registerFilePath);
    } else {
      // do nothing
    }
  }

  protected generateAbilityStageRegisters(packageName: string): void {
    const targetSourceSetModel: TargetSourceSetModel =
      this.targetService.getTargetData().getModuleSourceSetModel() as TargetSourceSetModel;
    const module = targetSourceSetModel.getModuleTargetRes().getModuleJsonOpt().module;
    const name = module.name;
    const srcEntry = module.srcEntry;
    if (!checkIsValid(srcEntry) ||
      (srcEntry !== undefined && ValidateRegExp.STARTUP_TASK_SRC_ENTRY_REG_EXP.test(path.extname(srcEntry)))) {
      return;
    }
    if (srcEntry === undefined || !validateCangjieEntry(packageName, srcEntry)) {
      const errMsg = srcEntry === undefined ? 'Missing required property \'module-srcEntry\' . At file: ' :
        `Module-srcEntry '${srcEntry}' is incorrect. At file: `;
      this.agcrLogger.printErrorExit('MODULE_SRC_ENTRY_IS_INCORRECT',
        [errMsg + path.resolve(this.moduleModel.getProjectDir(), 'src', 'main', 'module.json5')]);
      return;
    }
    let registerAbilities = `package ${packageName}\n\n`;
    if (!(srcEntry.startsWith(`${packageName}.`) && srcEntry?.substring(packageName.length + 1).indexOf('.') === -1)) {
      registerAbilities += `import ${srcEntry}\n`;
    }
    registerAbilities += 'import kit.AbilityKit.AbilityStage\n\n';
    const abilityName = srcEntry.substring(srcEntry.lastIndexOf('.') + 1);
    registerAbilities += `let ${name.toUpperCase()}_STAGE_REGISTER_RESULT` +
      ` = AbilityStage.registerCreator("${name}", {=> ${abilityName}()})\n`;
    const cangjieSrcPath = path.resolve(this.cjSource, readTomlSrcDir(this.readTomlContent()));
    if (!fs.existsSync(cangjieSrcPath)) {
      return;
    }
    const registerFilePath = path.join(cangjieSrcPath, `module_${this.moduleName}_entry.cj`);
    if (checkIsValid(registerAbilities)) {
      createDir(cangjieSrcPath);
      fs.writeFileSync(registerFilePath, registerAbilities);
    } else if (fs.existsSync(registerFilePath)) {
      fs.unlinkSync(registerFilePath);
    } else {
      // do nothing
    }
  }

  private readTomlContent(): any {
    const data = fs.readFileSync(this.cjpmTomlPath, 'utf8');
    let tomlData = {} as any;
    try {
      const formatData = replaceWithEnv(data);
      tomlData = new TOML().parse(formatData);
    } catch (e: any) {
      const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
      this.agcrLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [codeContent, `${this.cjpmTomlPath}:${e.errorLine}:${e.errorColumn}`]);
    }
    return tomlData;
  }

  private readTomlName(): string {
    const tomlData = this.readTomlContent();
    if (!Object.prototype.hasOwnProperty.call(tomlData, PACKAGE) ||
      !Object.prototype.hasOwnProperty.call(tomlData[PACKAGE], NAME)) {
      this.agcrLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [`     ${PACKAGE}.${NAME} is invalid`, this.cjpmTomlPath]);
    }
    const name = tomlData[PACKAGE][NAME];
    if (!checkIsValid(name)) {
      this.agcrLogger.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
        [`    ${PACKAGE}.${NAME} is invalid`, this.cjpmTomlPath]);
    }
    return name;
  }
}
