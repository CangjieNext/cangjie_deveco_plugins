/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */
/* eslint-disable no-console */
'use strict';

const fs = require('fs');
const path = require('path');
const chalk = require('chalk');

const TAG = 'verify-hvigor-exports';
const SHIM_PATH = path.join(__dirname, '..', 'types', 'hvigor-shim.d.ts');

// ─── 列宽配置 ────────────────────────────────────────────────────────
const COL = {
  kind: 6,    // value / type
  status: 8,  // "✔  OK"  / "✖  MISS"
  symbol: 26,
  module: 120,
};

// ─── 工具函数 ────────────────────────────────────────────────────────

/** 计算字符串可见宽度(去掉 ANSI 颜色码后的长度) */
function visibleLength(str) {
  // eslint-disable-next-line no-control-regex
  return str.replace(/\u001b\[[0-9;]*m/g, '').length;
}

/** 左对齐补齐到指定宽度(基于可见宽度,兼容带色文本) */
function pad(str, width) {
  const gap = width - visibleLength(str);
  return gap > 0 ? str + ' '.repeat(gap) : str;
}

/** 输出一整行表格(自动加左侧缩进) */
function row(cells) {
  console.log(`  ${cells.join('  ')}`);
}

/** 分隔线(每列一段 ─,长度与列宽一致) */
function tableDivider() {
  const seg = w => chalk.dim('─'.repeat(w));
  row([seg(COL.kind), seg(COL.status), seg(COL.symbol), seg(COL.module)]);
}

function printSkip(message) {
  console.warn(`  ${chalk.bgYellow.black.bold(' SKIP ')} ${chalk.yellow(message)}`);
}

function printError(title, details) {
  console.error(`  ${chalk.bgRed.white.bold(' ERROR ')} ${chalk.red.bold(title)}`);
  if (details) console.error(chalk.red(`         ${details}`));
}

function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** 把 `{ A, type B as C, D }` 形式的花括号内容拆成清洗后的符号名列表 */
function splitNamedExports(rawNames) {
  return rawNames
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
    .map(n => n.replace(/^type\s+/, '').split(/\s+as\s+/)[0].trim())
    .filter(Boolean);
}

/**
 * 从 shim 提取指定 kind 的具名 export 符号(运行时 value 或纯类型 type)。
 * kind='value' 匹配 `export { X } from 'path'`(排除整体 `export type {...}`)。
 * kind='type'  匹配 `export type { X } from 'path'`。
 * 均跳过 `export *`(wildcard,无法具名校验)。
 *
 * @param {string} content shim 文件内容
 * @param {'value'|'type'} kind 提取的导出种类
 * @returns {{name: string, module: string}[]}
 */
function extractNamedExports(content, kind) {
  const re = kind === 'type'
    ? /^export\s+type\s+\{([^}]*)}\s+from\s+'([^']*)'/gm
    : /^export\s+(?!type\b)\{([^}]*)}\s+from\s+'([^']*)'/gm;

  const symbols = [];
  let m;
  while ((m = re.exec(content)) !== null) {
    for (const name of splitNamedExports(m[1])) {
      symbols.push({name, module: m[2]});
    }
  }
  return symbols;
}

/**
 * 把 shim 里的 from 路径解析成实际的 .d.ts 文件绝对路径。
 * 处理 .js 后缀(去掉)和目录(找 index.d.ts)两种情况。
 */
function resolveDts(fromPath) {
  const p = path.resolve(path.dirname(SHIM_PATH), fromPath).replace(/\.js$/, '');
  if (fs.existsSync(`${p}.d.ts`)) return `${p}.d.ts`;
  if (fs.existsSync(path.join(p, 'index.d.ts'))) return path.join(p, 'index.d.ts');
  return null;
}

/**
 * 检查某个 type 符号在对应 .d.ts 文件里是否有 export 声明。
 * 支持两种形式:
 *   1. 直接声明:export interface/type/class/const/enum/function/abstract class X
 *   2. re-export:export { X, ... } from '...'
 */
function checkTypeExists(dtsPath, name) {
  const content = fs.readFileSync(dtsPath, 'utf8');
  const n = escapeRegExp(name);
  const declRe = new RegExp(
    `export\\s+(?:declare\\s+)?(?:interface|type|class|const|enum|function|abstract\\s+class)\\s+${n}\\b`
  );
  const reExportRe = new RegExp(`export\\s+\\{[^}]*\\b${n}\\b[^}]*\\}\\s+from\\s+'`);
  return declRe.test(content) || reExportRe.test(content);
}

/** 校验单个 value 符号:是否存在于 @ohos/hvigor 的运行时 exports 里 */
function verifyValueSymbol({name, module}, hvigorExports) {
  return {kind: 'value', name, module, ok: name in hvigorExports};
}

/** 校验单个 type 符号:解析对应 .d.ts 后检查是否有导出声明 */
function verifyTypeSymbol({name, module}) {
  const dtsPath = resolveDts(module);
  const ok = dtsPath ? checkTypeExists(dtsPath, name) : false;
  return {kind: 'type', name, module, ok};
}

function findHvigorPkg() {
  let dir = __dirname;
  for (let i = 0; i < 10; i++) {
    const candidate = path.join(dir, 'node_modules', '@ohos', 'hvigor', 'package.json');
    if (fs.existsSync(candidate)) return path.dirname(candidate);
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return null;
}

/** 截断过长的模块名,保留尾部(通常尾部更有区分度) */
function truncate(str, max) {
  if (str.length <= max) return str;
  return `…${str.slice(str.length - max + 1)}`;
}

/** 打印结果表格(表头 + 分隔线 + 每行数据) */
function printResultTable(results) {
  row([
    pad(chalk.bold('KIND'), COL.kind),
    pad(chalk.bold('STATUS'), COL.status),
    pad(chalk.bold('SYMBOL'), COL.symbol),
    pad(chalk.bold('MODULE'), COL.module),
  ]);
  tableDivider();

  for (const r of results) {
    const kind = chalk.dim(r.kind);
    const status = r.ok ? chalk.green('✔  OK') : chalk.red('✖  MISS');
    const symbol = r.ok ? r.name : chalk.red(r.name);
    const module = chalk.dim(truncate(r.module, COL.module));

    row([
      pad(kind, COL.kind),
      pad(status, COL.status),
      pad(symbol, COL.symbol),
      pad(module, COL.module),
    ]);
  }
}

function main() {
  console.log();
  console.log(`  ${chalk.bold.cyan(TAG)}`);
  console.log();

  if (!fs.existsSync(SHIM_PATH)) {
    printSkip(`shim not found: ${SHIM_PATH}`);
    console.log();
    return;
  }

  const content = fs.readFileSync(SHIM_PATH, 'utf8');
  const valueSymbols = extractNamedExports(content, 'value');
  const typeSymbols = extractNamedExports(content, 'type');

  if (valueSymbols.length === 0 && typeSymbols.length === 0) {
    printSkip('no exports found in shim');
    console.log();
    return;
  }

  const hvigorPath = findHvigorPkg();
  if (!hvigorPath) {
    printSkip('@ohos/hvigor not found in node_modules');
    console.log();
    return;
  }

  let hvigorExports;
  try {
    hvigorExports = require(hvigorPath);
  } catch (e) {
    printError('unable to load @ohos/hvigor', e.message);
    console.log();
    process.exit(1);
  }

  // ─── 逐项判定:value 校验运行时 export,type 校验 .d.ts 声明 ──────
  const results = [
    ...valueSymbols.map(s => verifyValueSymbol(s, hvigorExports)),
    ...typeSymbols.map(s => verifyTypeSymbol(s)),
  ];

  const passed = results.filter(r => r.ok).length;
  const failed = results.length - passed;

  printResultTable(results);

  // ─── 汇总 ──────────────────────────────────────────────────────────
  console.log();
  const summary = [
    chalk.green(`${passed} passed`),
    failed > 0 ? chalk.red(`${failed} failed`) : chalk.dim('0 failed'),
  ].join(chalk.dim(', '));
  console.log(`  ${summary}  ${chalk.dim(
    `(total ${results.length}: ${valueSymbols.length} value + ${typeSymbols.length} type)`)}`);
  console.log();

  if (failed > 0) {
    printError('hvigor export verification failed');
    console.error(chalk.yellow('  Check the Hvigor version and types/hvigor-shim.d.ts'));
    console.log();
    process.exit(1);
  }
}

main();