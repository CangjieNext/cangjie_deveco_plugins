/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */
interface Option {
  verbose?: boolean;
  precise?: boolean;
}

const shortDesc = ['h', 'min', 's', 'ms', 'μs', 'ns'];
const longDesc = ['hour', 'minute', 'second', 'millisecond', 'microsecond', 'nanosecond'];
const convertArray = [60 * 60, 60, 1, 1e6, 1e3, 1];
const MIN_TIME = '1 ms ';
const unit2Index = new Map([
  ['hour', 0], ['hours', 0],
  ['minute', 1], ['minutes', 1],
  ['second', 2], ['seconds', 2],
  ['millisecond', 3], ['milliseconds', 3],
  ['microsecond', 4], ['microseconds', 4],
  ['nanosecond', 5], ['nanoseconds', 5],
]);

function prettyHrtime(sourceArray: number[], options?: Option): string {
  if (!Array.isArray(sourceArray) || sourceArray.length !== 2) {
    return '';
  }
  if (typeof sourceArray[0] !== 'number' || typeof sourceArray[1] !== 'number') {
    return '';
  }
  const verbose = options?.verbose ?? false;
  const precise = options?.precise ?? false;
  if (sourceArray[1] < 0) {
    const totalSecs = sourceArray[0] + sourceArray[1] / 1e9;
    sourceArray[0] = parseInt(totalSecs.toString());
    sourceArray[1] = parseFloat((totalSecs % 1).toPrecision(9)) * 1e9;
  }
  return doCalc(sourceArray, verbose, precise);
}

function doCalc(sourceArray: number[], verbose: boolean, precise: boolean): string {
  let ret = '';
  for (let i = 0; i < 6; i++) {
    let sourceAtStep = sourceArray[i < 3 ? 0 : 1];
    if (i !== 3 && i !== 0) {
      sourceAtStep %= convertArray[i - 1];
    }
    if (i === 2) {
      sourceAtStep += sourceArray[1] / 1e9;
    }
    let valueAtStep = sourceAtStep / convertArray[i];
    if (valueAtStep < 1) {
      continue;
    }
    if (verbose) {
      valueAtStep = Math.floor(valueAtStep);
    }
    let stringAtStep: string;
    if (precise) {
      stringAtStep = valueAtStep.toString();
    } else {
      const decimals = valueAtStep >= 10 ? 0 : 2;
      stringAtStep = valueAtStep.toFixed(decimals);
    }
    if (stringAtStep.indexOf('.') > -1 && stringAtStep[stringAtStep.length - 1] === '0') {
      stringAtStep = stringAtStep.replace(/\.?0+$/, '');
    }
    if (ret) {
      ret += ' ';
    }
    ret += stringAtStep;
    if (verbose) {
      ret += ` ${longDesc[i]}`;
      if (stringAtStep !== '1') {
        ret += 's';
      }
    } else {
      ret += ` ${shortDesc[i]}`;
    }
    if (!verbose) {
      break;
    }
  }
  return ret;
}

function splitTime(source: string): string {
  const timePieces = source.split(' ');
  if (timePieces.length < 1 || timePieces.length % 2 !== 0) {
    return source;
  }
  let time = '';
  for (let i = 0; i < timePieces.length; i += 2) {
    let value = timePieces[i];
    const index = unit2Index.get(timePieces[i + 1]);
    if (index === undefined) {
      return source;
    }
    const unit = shortDesc[index];
    if (i === 0 && index > 3) {
      return MIN_TIME;
    }
    if (index > 3) {
      break;
    }
    if (unit === 'ms' && i < timePieces.length - 2) {
      const parsedValue = parseInt(value, 10);
      if (Number.isNaN(parsedValue)) {
        return source;
      }
      value = String(parsedValue + 1);
    }
    time += `${value} ${unit} `;
  }
  return time;
}

export function formatTime(time: [number, number]): string {
  return splitTime(prettyHrtime(time, {verbose: true}));
}

/** 将 ns 转化为 [Number, Number] 格式,第一个 Number 单位是 s,第二个是 ns */
export function formatTimeToNumPair(time: number): [number, number] {
  const seconds = Math.floor(time / 1e9);
  return [seconds, time - seconds * 1e9];
}
