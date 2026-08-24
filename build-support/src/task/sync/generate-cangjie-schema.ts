/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import fs from 'fs';
import path from 'path';
import {BuildProfileSchemaFileConst, OhosLogger, ValidatorStore} from '../../../types/hvigor-imports';
import {
  CANGJIE_OPTIONS_CONFIG,
  CANGJIE_OPTIONS_CONFIG_ZH,
  CANGJIE_OPTIONS_HAR_CONFIG,
  CANGJIE_OPTIONS_HAR_CONFIG_ZH,
  CJ_PLUGIN_CACHE_DIR
} from '../../constants/constants';
import {createDir} from '../../utils/cangjie-file-util';
import {CangjieLogger} from '../../log/cangjie-logger';

/**
 * generate cangjie schema json file
 *
 * @since 2025/3/9
 */
export class GenerateCangjieSchema {
  private static instance: GenerateCangjieSchema;
  private geCjSchemaLogger: OhosLogger = CangjieLogger.getLogger(GenerateCangjieSchema.name);
  private readonly supportedLanguages: SchemaLangConfig[] = [
    {
      suffix: '', // 默认英文，无后缀
      cangjieOptionsConfig: CANGJIE_OPTIONS_CONFIG,
      cangjieOptionsHarConfig: CANGJIE_OPTIONS_HAR_CONFIG,
    },
    {
      suffix: '_zh', // 中文
      cangjieOptionsConfig: CANGJIE_OPTIONS_CONFIG_ZH,
      cangjieOptionsHarConfig: CANGJIE_OPTIONS_HAR_CONFIG_ZH,
    },
  ];

  private readonly ideaConfigPath: string;

  private constructor(ideaConfigPath: string) {
    this.ideaConfigPath = path.resolve(ideaConfigPath, CJ_PLUGIN_CACHE_DIR);
  }

  public static getInstance(ideaConfigPath: string, isCangjiePluginEnabled = false): GenerateCangjieSchema {
    if (GenerateCangjieSchema.instance === undefined) {
      GenerateCangjieSchema.instance = new GenerateCangjieSchema(ideaConfigPath);
      if (isCangjiePluginEnabled) {
        GenerateCangjieSchema.instance.generateCangjieSchema();
      }
    } else if (isCangjiePluginEnabled) {
      if (!GenerateCangjieSchema.instance.checkSchemaExists()) {
        GenerateCangjieSchema.instance.generateCangjieSchema();
      }
    } else {
      // do nothing
    }
    return GenerateCangjieSchema.instance;
  }

  /**
   * delete schema
   */
  public deleteSchema(): void {
    const baseFiles = [
      this.getModuleSchema(),
      this.getHarSchema(),
      this.getProjectSchema(),
    ];
    for (const baseFile of baseFiles) {
      for (const lang of this.supportedLanguages) {
        const fileToDelete = this.getLocalizedPath(baseFile, lang.suffix);
        if (fs.existsSync(fileToDelete)) {
          fs.unlinkSync(fileToDelete);
        }
      }
    }
  }

  /**
   * replace schema
   */
  public replaceSchema(): void {
    const moduleSchema = this.getModuleSchema();
    if (fs.existsSync(moduleSchema)) {
      this.addValidator(moduleSchema, BuildProfileSchemaFileConst.HAP_MODULE_BUILD_PROFILE_SCHEMA_PATH, false);
    }
    const harSchema = this.getHarSchema();
    if (fs.existsSync(harSchema)) {
      this.addValidator(harSchema, BuildProfileSchemaFileConst.HAR_MODULE_BUILD_PROFILE_SCHEMA_PATH, false);
    }
    const projectSchema = this.getProjectSchema();
    if (fs.existsSync(projectSchema)) {
      this.addValidator(projectSchema, BuildProfileSchemaFileConst.PROJECT_BUILD_PROFILE_SCHEMA_PATH, false);
      this.addValidator(projectSchema, BuildProfileSchemaFileConst.PROJECT_BUILD_PROFILE_SCHEMA_PATH, true);
    }
  }

  private addValidator(schemaPath: string, schemaPathDest: string, changeAppSchema: boolean): void {
    const key = `${schemaPathDest}:${changeAppSchema}`;
    const validator = ValidatorStore.addValidator(schemaPath, changeAppSchema);
    ValidatorStore.getValidatorMap().set(key, validator);
  }

  private initUpdateContent(): Map<string, Map<string[], any>> {
    const updateSchemaMap = new Map<string, Map<string[], any>>();

    // 遍历所有语言配置，统一生成
    for (const lang of this.supportedLanguages) {
      this.generateConfigsForLang(lang, updateSchemaMap);
    }
    return updateSchemaMap;
  }

  private generateConfigsForLang(lang: SchemaLangConfig, resultMap: Map<string, Map<string[], any>>): void {
    const {suffix, cangjieOptionsConfig, cangjieOptionsHarConfig} = lang;
    const updateHarConfigs: Map<string[], any> = new Map<string[], any>();
    updateHarConfigs.set(['definitions', 'buildOption', 'propertyNames', 'enum'], 'cangjieOptions');
    updateHarConfigs.set(['definitions', 'buildOption', 'properties'], cangjieOptionsHarConfig);
    updateHarConfigs.set(['properties', 'buildOption', 'propertyNames', 'enum'], 'cangjieOptions');
    updateHarConfigs.set(['then', 'properties', 'buildOption', 'propertyNames', 'enum'], 'cangjieOptions');

    const updateModuleConfigs: Map<string[], any> = new Map<string[], any>();
    updateModuleConfigs.set(['definitions'], cangjieOptionsConfig);
    updateModuleConfigs.set(['definitions', 'buildOption', 'propertyNames', 'enum'], 'cangjieOptions');
    updateModuleConfigs.set(['definitions', 'buildOption', 'properties'], {
      cangjieOptions: {
        $ref: '#/definitions/cangjieOptions',
      },
    });
    updateModuleConfigs.set(['definitions', 'buildOptionSetItems', 'propertyNames', 'enum'], 'cangjieOptions');
    updateModuleConfigs.set(['definitions', 'buildOptionSetItems', 'properties'], {
      cangjieOptions: {
        $ref: '#/definitions/cangjieOptions',
      },
    });

    const updateProjectConfigs: Map<string[], any> = new Map<string[], any>();
    updateProjectConfigs.set(['definitions', 'buildOption', 'propertyNames', 'enum'], 'cangjieOptions');
    updateProjectConfigs.set(['definitions', 'buildOption', 'properties'], cangjieOptionsConfig);

    resultMap.set(this.getLocalizedPath(BuildProfileSchemaFileConst.HAR_MODULE_BUILD_PROFILE_SCHEMA_PATH, suffix),
      updateHarConfigs);
    resultMap.set(this.getLocalizedPath(BuildProfileSchemaFileConst.HAP_MODULE_BUILD_PROFILE_SCHEMA_PATH, suffix),
      updateModuleConfigs);
    resultMap.set(this.getLocalizedPath(BuildProfileSchemaFileConst.PROJECT_BUILD_PROFILE_SCHEMA_PATH, suffix),
      updateProjectConfigs);
  }

  private generateCangjieSchema(): void {
    try {
      createDir(this.ideaConfigPath);
      const updateSchemaMap = this.initUpdateContent();
      for (const [schemaPath, configMap] of updateSchemaMap) {
        if (!fs.existsSync(schemaPath)) {
          continue;
        }
        const schemaData = JSON.parse(fs.readFileSync(schemaPath, 'utf-8'));
        for (const [keys, value] of configMap) {
          this.addProperty(schemaData, keys, value);
        }
        const newContent = JSON.stringify(schemaData, null, 2);
        const destSchemaPath = path.join(this.ideaConfigPath, path.basename(schemaPath));
        let contentChanged = true;
        if (fs.existsSync(destSchemaPath)) {
          const oldContent = fs.readFileSync(destSchemaPath, 'utf-8');
          contentChanged = oldContent !== newContent;
        }
        if (contentChanged) {
          this.geCjSchemaLogger.debug(`Start update cangjie schema file :${destSchemaPath}`);
          fs.writeFileSync(destSchemaPath, newContent, 'utf-8');
        }
      }
    } catch (error) {
      this.geCjSchemaLogger.debug('Generate cangjie schema file failed.');
    }
  }

  private addProperty(schema: any, keys: string[], newProperty: string | { [key: string]: any }): void {
    let current = schema;
    for (const key of keys) {
      if (!current[key]) {
        current[key] = {};
      }
      current = current[key];
    }
    if (typeof newProperty === 'string') {
      if (!current.includes(newProperty)) {
        current.push(newProperty);
      }
    } else {
      for (const prop of Object.keys(newProperty)) {
        if (!Object.prototype.hasOwnProperty.call(current, prop)) {
          current[prop] = newProperty[prop];
        }
      }
    }
  }

  private checkSchemaExists(): boolean {
    const baseFiles = [
      GenerateCangjieSchema.instance.getModuleSchema(),
      GenerateCangjieSchema.instance.getHarSchema(),
      GenerateCangjieSchema.instance.getProjectSchema(),
    ];
    for (const baseFile of baseFiles) {
      for (const lang of GenerateCangjieSchema.instance.supportedLanguages) {
        const fileToDelete = GenerateCangjieSchema.instance.getLocalizedPath(baseFile, lang.suffix);
        if (!fs.existsSync(fileToDelete)) {
          return false;
        }
      }
    }
    return true;
  }

  private getLocalizedPath(originalPath: string, suffix: string): string {
    if (suffix === '') {
      return originalPath;
    }
    const dir = path.dirname(originalPath);
    const ext = path.extname(originalPath);
    const name = path.basename(originalPath, ext);
    return path.join(dir, `${name}${suffix}${ext}`);
  }

  private getProjectSchema(): string {
    return path.resolve(this.ideaConfigPath,
      path.basename(BuildProfileSchemaFileConst.PROJECT_BUILD_PROFILE_SCHEMA_PATH));
  }

  private getHarSchema(): string {
    return path.resolve(this.ideaConfigPath,
      path.basename(BuildProfileSchemaFileConst.HAR_MODULE_BUILD_PROFILE_SCHEMA_PATH));
  }

  private getModuleSchema(): string {
    return path.resolve(this.ideaConfigPath,
      path.basename(BuildProfileSchemaFileConst.HAP_MODULE_BUILD_PROFILE_SCHEMA_PATH));
  }
}

interface SchemaLangConfig {
  suffix: string; // 文件后缀，如 '' (英文), '_zh', '_ja'
  cangjieOptionsConfig: any; // 对应 CANGJIE_OPTIONS_CONFIG 或 _ZH
  cangjieOptionsHarConfig: any; // 对应 CANGJIE_OPTIONS_HAR_CONFIG 或 _ZH
}
