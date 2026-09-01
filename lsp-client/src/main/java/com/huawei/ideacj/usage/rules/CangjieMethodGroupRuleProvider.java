/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.usage.rules;

import com.intellij.openapi.project.Project;
import com.intellij.usages.impl.FileStructureGroupRuleProvider;
import com.intellij.usages.rules.UsageGroupingRule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CangjieMethodGroupRuleProvider
 *
 * @since 2025-10-29
 */
public class CangjieMethodGroupRuleProvider implements FileStructureGroupRuleProvider {
    @Override
    @Nullable
    public UsageGroupingRule getUsageGroupingRule(@NotNull Project project) {
        return new CangjieMethodGroupingRule();
    }
}
