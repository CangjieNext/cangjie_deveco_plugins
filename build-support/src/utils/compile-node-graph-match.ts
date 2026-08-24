/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

import {
  DefaultTargetConst,
  globalData,
  hvigor,
  HvigorBuildConst,
  hvigorCore,
  instanceOf,
  Module,
  ModuleModel,
  OhosLogger,
  Project,
  ProjectModel,
  projectTaskDag,
  TaskNames
} from '../../types/hvigor-imports';
import fs from 'fs';
import {checkIsValid, replaceWithEnv} from './common-utils';
import path from 'path';
import {CANGJIE_OPTIONS, CJPM_TOML_NAME, DEPENDENCIES, NAME, PACKAGE, PATH} from '../constants/constants';
import {TOML} from './toml/toml-export';
import {CangjieLogger} from '../log/cangjie-logger';
import {configOhModulesEnv} from './env-util';
import CommonHookTask = TaskNames.CommonHookTask;

const PACKAGE_TASKS = [
  TaskNames.Task.PACKAGE_HAR.name,
  TaskNames.Task.PACKAGE_HAP.name,
  TaskNames.Task.PACKAGE_HSP.name,
  TaskNames.Task.FAST_PACKAGE.name,
  TaskNames.Task.FAST_PACKAGE_HSP.name,
];

/**
 * Analyze the compilation path of Cangjie.
 *
 * @since 2025/11/26
 */
export class CompileNodeGraphMatch {
  private static instance: CompileNodeGraphMatch;
  private tdtuLog: OhosLogger = CangjieLogger.getLogger(CompileNodeGraphMatch.name);
  private _mode = '';
  private dependsEnv: NodeJS.ProcessEnv = {};
  private _tomlTreeMap = new Map<string, TomlTreeNode>();
  private _moduleTomlNameMap = new Map<string, string>();
  private _roots: Set<string> = new Set();
  private _startPackageTasks: Set<string> = new Set();
  private moduleTargets: Map<string, string[]> = new Map<string, string[]>();

  get mode(): string {
    return this._mode;
  }

  get tomlTreeMap(): Map<string, TomlTreeNode> {
    return this._tomlTreeMap;
  }

  get moduleTomlNameMap(): Map<string, string> {
    return this._moduleTomlNameMap;
  }

  get roots(): Set<string> {
    return this._roots;
  }

  get startPackageTasks(): Set<string> {
    return this._startPackageTasks;
  }

  public static getInstance(): CompileNodeGraphMatch {
    if (CompileNodeGraphMatch.instance === undefined) {
      CompileNodeGraphMatch.instance = new CompileNodeGraphMatch();
    }
    return CompileNodeGraphMatch.instance;
  }

  /**
   * 从所有模块入口构建唯一的依赖树集合
   *
   * @param {ProjectModel} projectModel
   */
  public buildUniqueDependencyTrees(projectModel: ProjectModel): void {
    this._mode = '';
    this._tomlTreeMap.clear();
    this._moduleTomlNameMap.clear();
    this._roots.clear();
    this._startPackageTasks.clear();
    this.moduleTargets.clear();
    const allModules = projectModel.getAllModules();
    if (allModules === null || allModules.length === 0) {
      return;
    }
    this._mode = globalData.cliEnv.configProps.get('mode') ??
      (hvigor.isCommandEntryTask(CommonHookTask.ASSEMBLE_APP.name) ? 'project' : 'module');
    this.initTomlDependenciesTree(projectModel, allModules);
    this.initBuildStartTasks(projectModel);
    this.initRootTasks();
  }

  /**
   * 收集该节点所有祖先节点的 moduleTargets
   *
   * @param key Map 中的键 tomlName
   * @returns 所有收集到的 moduleTargets 集合
   */
  public collectParentModuleTargets(key: string): Set<string> {
    let currentNode = this._tomlTreeMap.get(key);
    const allTargets = new Set<string>();
    if (currentNode === undefined) {
      return allTargets;
    }
    currentNode = currentNode.parent;

    // 向上遍历 Parent
    while (currentNode !== undefined) {
      // 将当前节点的 targets 加入结果集
      if (currentNode.moduleTargets && currentNode.moduleTargets.length > 0) {
        currentNode.moduleTargets.forEach(target => allTargets.add(target));
      }
      currentNode = currentNode.parent;
    }
    return allTargets;
  }

  /**
   * 迭代收集指定任务下的所有打包任务 (使用 DFS)
   *
   * @param startTask 起始任务名称
   */
  public getPackageTasks(startTask: string): void {
    // 使用栈来存储待处理的任务，初始放入起始任务
    const stack: string[] = [startTask];

    // 记录已访问节点，防止死循环
    const visited = new Set<string>();
    while (stack.length > 0) {
      // 弹出当前要处理的任务
      const currentTask = stack.pop();
      if (currentTask === undefined) {
        continue;
      }
      if (visited.has(currentTask)) {
        continue;
      }
      visited.add(currentTask);

      // 判断当前任务是否符合收集条件
      if (PACKAGE_TASKS.some(suffix => currentTask.endsWith(`@${suffix}`))) {
        this._startPackageTasks.add(currentTask);
      }

      // 获取子任务并入栈
      const childrenIterator = projectTaskDag.getChildren(currentTask).values();
      for (const childTask of childrenIterator) {
        if (!visited.has(childTask)) {
          stack.push(childTask);
        }
      }
    }
  }

  /**
   * 从起始节点触发，检查：
   * 1. 是否能到达目标节点
   * 2. 在遍历子节点是否经过任意一个祖先节点
   *
   * @param {string} startNode 起始节点
   * @param {string} targetNode 目标节点
   * @param {Set<string>} ancestorNodes 祖先节点集合
   * @returns {boolean} 是否同时满足上述两个条件
   */
  public hasPathToNode(startNode: string, targetNode: string, ancestorNodes: Set<string>): boolean {
    // 快速检查：如果祖先节点集为空，直接返回false
    if (ancestorNodes.size === 0) {
      return false;
    }

    // 使用BFS查找路径
    const visited = new Set<string>();
    const queue: string[] = [startNode];

    // 跟踪是否找到目标节点和是否经过祖先节点
    let foundTargetNode = false;
    let passedThroughAncestor = false;
    while (queue.length > 0) {
      const current = queue.shift();
      if (current === undefined) {
        continue;
      }

      // 已访问过的节点不再处理
      if (visited.has(current)) {
        continue;
      }
      visited.add(current);

      // 检查是否为目标节点
      if (current === targetNode) {
        foundTargetNode = true;
      }

      // 检查是否是祖先节点集合中的节点
      if (ancestorNodes.has(current)) {
        passedThroughAncestor = true;
      }

      // 如果两个条件都满足，提前返回true
      if (foundTargetNode && passedThroughAncestor) {
        return true;
      }

      // 将当前节点的所有未访问的子节点加入队列
      const neighbors = projectTaskDag.getChildren(current);
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor)) {
          queue.push(neighbor);
        }
      }
    }

    // 未找到当前节点返回true；找到当前节点，未经过必经节点，返回false；两者都找到，返回true
    return foundTargetNode ? (foundTargetNode && passedThroughAncestor) : true;
  }

  private initBuildStartTasks(projectModel: ProjectModel): void {
    const toRunTasks = globalData.cliOpts._;
    const project = projectModel.getProject();
    const executeTasks: string[] = [];
    let nodes: Module[] = [];
    if (this._mode === 'project') {
      nodes = [projectModel.getProject()];
    } else {
      const modules = hvigorCore.getExtraConfig().get('module');
      if (modules === undefined) {
        nodes = project.getAllSubModules();
      } else {
        this.getSpecifiedModules(modules, project, nodes);
      }
    }
    if (nodes.length === 0) {
      return;
    }
    toRunTasks.forEach((toRunTaskName) => {
      const toRunTaskPaths: string[] = nodes.filter((node) => node.hasTask(toRunTaskName)).map(
        (node) => this.union(instanceOf(node, HvigorBuildConst.PROJECT_MODE) ? '' : node.getName(), toRunTaskName));
      executeTasks.push(...toRunTaskPaths);
    });
    if (executeTasks.length === 0) {
      return;
    }
    executeTasks.forEach((executeTask) => {
      this.getPackageTasks(executeTask);
    });
  }

  private getSpecifiedModules(modules: string, project: Project, nodes: Module[]): void {
    modules.split(',').forEach((moduleAndTargetName: string) => {
      const values: string[] = moduleAndTargetName.split('@');
      const moduleName = values[0];
      if (moduleName === undefined || moduleName === '') {
        return;
      }
      const module = project.findModuleByName(moduleName);
      if (module === undefined) {
        return;
      }
      nodes.push(module);
    });
  }

  private initTomlDependenciesTree(projectModel: ProjectModel, allModules: ModuleModel[]): void {
    configOhModulesEnv(projectModel, this.dependsEnv);
    for (const moduleItem of allModules) {
      const moduleName = moduleItem.getName();
      const targetNames = new Set<string>();
      this.collectCompileCangjieTask(moduleItem, targetNames);
      if (targetNames.size === 0) {
        continue;
      }
      this.moduleTargets.set(moduleName, [...targetNames]);
      for (const targetName of targetNames) {
        const target =
          projectModel.getTarget(moduleName, targetName) ??
          projectModel.getTarget(moduleName, DefaultTargetConst.DEFAULT_TARGET) ??
          projectModel.getTarget(moduleName);
        if (target === undefined || !Object.prototype.hasOwnProperty.call(target.getBuildOption(), CANGJIE_OPTIONS)) {
          continue;
        }
        const cangjieOptionPath = target.getBuildOption()?.cangjieOptions?.path;
        if (cangjieOptionPath === undefined) {
          continue;
        }
        let cjpmTomlPath = '';
        if (path.isAbsolute(cangjieOptionPath)) {
          cjpmTomlPath = cangjieOptionPath;
        } else {
          cjpmTomlPath = path.resolve(moduleItem.getProjectDir(), cangjieOptionPath);
        }
        this.getDependencies(path.dirname(cjpmTomlPath), undefined, moduleName);
        break;
      }
    }
  }

  private initRootTasks(): void {
    this._tomlTreeMap.forEach((node, tomlName) => {
      if (node.parent?.tomlName === undefined) {
        this._roots.add(tomlName);
      }
    });
  }

  private collectCompileCangjieTask(moduleItem: ModuleModel, targetNames: Set<string>): void {
    const taskPaths = moduleItem.getModule().getTaskContainer().getTaskPaths();
    if (taskPaths.length === 0) {
      return;
    }
    for (const taskPath of taskPaths) {
      if (!taskPath.endsWith('@CompileCangjie')) {
        continue;
      }
      const taskPathArr: string[] = taskPath.split('@');
      const targetName = taskPathArr[0];
      if (checkIsValid(targetName)) {
        targetNames.add(targetName);
      }
    }
  }

  private getDependencies(workspacePathUriParam: string, parentNode: TomlTreeNode | undefined,
    moduleName: string): void {
    // 初始化栈
    const stack: DependencyTask[] = [{pathUri: workspacePathUriParam, parentNode: parentNode, moduleName: moduleName}];
    while (stack.length > 0) {
      const task = stack.pop();
      if (task === undefined) {
        continue;
      }
      const {pathUri: currentUriParam, parentNode: currentParent, moduleName: currentModuleName} = task;
      const workspacePathUri = path.normalize(currentUriParam);
      if (!fs.existsSync(workspacePathUri)) {
        this.tdtuLog.printErrorExit('TOML_DEPENDENCIES_PATH_NOT_EXIST', [workspacePathUri]);
      }
      const tomlPath = path.join(workspacePathUri, CJPM_TOML_NAME);
      if (!fs.existsSync(tomlPath)) {
        continue;
      }
      const data = fs.readFileSync(tomlPath, 'utf8');
      let tomlData = {} as any;
      try {
        const formatData = replaceWithEnv(data, this.dependsEnv);
        tomlData = new TOML().parse(formatData);
      } catch (e: any) {
        const codeContent = checkIsValid(e.errorCodeBlock) ? e.errorCodeBlock : '';
        this.tdtuLog.printErrorExit('TOML_CONTENT_CONFIG_ERROR',
          [codeContent, `${tomlPath}:${e.errorLine}:${e.errorColumn}`]);
      }
      const packageName = tomlData[PACKAGE][NAME];
      if (currentModuleName !== '' && !this._moduleTomlNameMap.has(currentModuleName)) {
        this._moduleTomlNameMap.set(currentModuleName, packageName);
      }

      // 检查节点是否已存在
      const existTreeNode = this._tomlTreeMap.get(packageName);
      if (existTreeNode !== undefined) {
        if (currentParent !== undefined) {
          const children = currentParent.children;
          children.add(existTreeNode);
          existTreeNode.parent = currentParent;
        }
        continue;
      }
      const tomlTreeNode = new TomlTreeNode(packageName, tomlPath);
      if (currentModuleName !== '') {
        this.generateModuleTargets(currentModuleName, tomlTreeNode);
      }
      this._tomlTreeMap.set(packageName, tomlTreeNode);

      // 连接父节点
      if (currentParent !== undefined) {
        currentParent.children.add(tomlTreeNode);
        tomlTreeNode.parent = currentParent;
      }

      // 检查是否有依赖项
      if (!Object.prototype.hasOwnProperty.call(tomlData, DEPENDENCIES)) {
        continue;
      }
      const dependencies = tomlData[DEPENDENCIES];
      for (const requireItem in dependencies) {
        if (!Object.prototype.hasOwnProperty.call(dependencies, requireItem)) {
          continue;
        }
        const requireItems = dependencies[requireItem];
        if (!Object.prototype.hasOwnProperty.call(requireItems, PATH)) {
          continue;
        }
        let dependencyPath = requireItems[PATH];
        dependencyPath = path.normalize(dependencyPath);
        if (!path.isAbsolute(dependencyPath)) {
          dependencyPath = path.normalize(path.join(workspacePathUri, dependencyPath));
        }
        stack.push({pathUri: dependencyPath, parentNode: tomlTreeNode, moduleName: ''});
      }
    }
  }

  private generateModuleTargets(moduleName: string, tomlTreeNode: TomlTreeNode): void {
    const targetNames = this.moduleTargets.get(moduleName);
    if (targetNames === undefined) {
      return;
    }
    targetNames.forEach((targetName) => {
      tomlTreeNode.moduleTargets.push(`${moduleName}:${targetName}@CompileCangjie`);
    });
  }

  /**
   * 拼接module name和task name
   *
   * @param {string} moduleName 如果是project则为“”,如果是module则是moduleName
   * @param {string} taskName taskName
   * @returns {string} taskPath
   */
  private union(moduleName: string, taskName: string): string {
    return moduleName.concat(':').concat(taskName);
  }
}

interface DependencyTask {
  pathUri: string;
  parentNode?: TomlTreeNode;
  moduleName: string;
}

/**
 * 表示依赖树中的一个节点
 */
class TomlTreeNode {
  tomlName: string;
  tomlPath: string;
  moduleTargets: string[] = [];
  children: Set<TomlTreeNode> = new Set<TomlTreeNode>();
  parent: TomlTreeNode | undefined;

  constructor(tomlName: string, tomlPath: string) {
    this.tomlName = tomlName;
    this.tomlPath = tomlPath;
  }

  /**
   * 用于在Set中进行去重比较的方法
   */
  equals(other: TomlTreeNode): boolean {
    return this.tomlName === other.tomlName && this.tomlPath === other.tomlPath;
  }
}
