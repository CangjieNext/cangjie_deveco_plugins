/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.usage;

import com.huawei.ace.ohos.usage.AceReferenceUsageTypeProvider;
import com.huawei.ace.usage.PsiElementTypeUtil;
import com.huawei.deveco.res.usage.ResourceSupplier;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CjReferenceUsageTypeProvider
 *
 * @since 2025/10/30
 */
public class CjReferenceUsageTypeProvider extends AceReferenceUsageTypeProvider {
    private static final Map<IElementType, UsageType> USAGE_IN_TYPE = new HashMap<>();

    private static final UsageType CANGJIE_RESOURCE_REFERENCE_USAGE_TYPE =
        new UsageType(new ResourceSupplier("Resource reference in Cangjie file"));

    static {
        USAGE_IN_TYPE.put(CangJieTypes.RULE_IMPORT_LIST, UsageType.CLASS_IMPORT);
        USAGE_IN_TYPE.put(CangJieTypes.RULE_PACKAGE_HEADER,
            new UsageType(new ResourceSupplier(CangjieBundle.message("lsp.usage.in.package.declaration"))));
        USAGE_IN_TYPE.put(CangJieTypes.MULTI_LINE_RAW_STRING_LITERAL, UsageType.LITERAL_USAGE);
        USAGE_IN_TYPE.put(CangJieTypes.MULTI_LINE_STR_TEXT, UsageType.LITERAL_USAGE);
        USAGE_IN_TYPE.put(CangJieTypes.LINE_STR_TEXT, UsageType.LITERAL_USAGE);
        USAGE_IN_TYPE.put(CangJieTypes.CHARACTER_LITERAL, UsageType.LITERAL_USAGE);
    }

    @Override
    @Nullable
    public UsageType getUsageType(PsiElement psiElement, UsageTarget @NotNull [] usageTargets) {
        // 尝试获取基于元素类型的使用类型
        return Optional.ofNullable(PsiElementTypeUtil.getElementType(psiElement)).map(USAGE_IN_TYPE::get)
            .orElseGet(() -> {
                // 如果元素类型没有对应的使用类型，检查文件类型
                PsiFile file = psiElement.getContainingFile();
                return file instanceof CangJiePsiFileRoot ? CANGJIE_RESOURCE_REFERENCE_USAGE_TYPE : null;
            });
    }

    @Override
    @Nullable
    public UsageType getUsageType(@NotNull PsiElement psiElement) {
        return this.getUsageType(psiElement, UsageTarget.EMPTY_ARRAY);
    }
}
