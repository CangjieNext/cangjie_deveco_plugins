/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ideacj.resource.CangjieResourceUtil.RESOURCE_COMPLETION_PATTERN;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.DefaultCompletionContributor;

/**
 * CangjieResourceCompletionContributor
 *
 * @since 2024/10/6
 */
public class CangjieResourceCompletionContributor extends DefaultCompletionContributor {
    public CangjieResourceCompletionContributor() {
        extend(CompletionType.BASIC, RESOURCE_COMPLETION_PATTERN, new CangjieResourceCompletionProvider());
    }
}
