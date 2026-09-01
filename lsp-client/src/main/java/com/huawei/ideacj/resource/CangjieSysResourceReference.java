/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import com.huawei.ace.common.CommonBaseReference;
import com.huawei.ace.ohos.reference.SysResourceBean;
import com.huawei.ace.ohos.reference.SysResourceManager;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * CangjieSysResourceReference
 *
 * @since 2024/10/15
 */
public class CangjieSysResourceReference extends CommonBaseReference {
    private final PsiElement element;

    private final ModuleModel module;

    public CangjieSysResourceReference(PsiElement element, TextRange range, ModuleModel module) {
        super(element, range, false);
        this.element = element;
        this.module = module;
    }

    @Override
    @NotNull
    public ResolveResult[] multiResolve(boolean isIncompleteCode) {
        return ResolveCache.getInstance(getElement().getProject())
                .resolveWithCaching(this, (ref, tempIncompleteCode) -> ref.resolveInner(), false, isIncompleteCode);
    }

    @NotNull
    private ResolveResult[] resolveInner() {
        String[] names = StringUtil.unquoteString(element.getText())
                .split(CangjieResourceUtil.RESOURCE_SEPARATOR_REG_REGULAR);
        if (names.length != CangjieResourceUtil.RESOURCE_SIZE) {
            return ResolveResult.EMPTY_ARRAY;
        }
        Set<SysResourceBean.SysResourceElement> sysResourceElements = SysResourceManager.getInstance(
                element.getProject()).getSysResourceElements(names[1]);
        return sysResourceElements.stream()
                .filter(element -> StringUtil.equals(element.getName(), names[2]))
                .findFirst()
                .map(SysResourceBean.SysResourceElement::getOriginElement)
                .map(PsiElementResolveResult::new)
                .map(psiElementResolveResult -> new ResolveResult[] {psiElementResolveResult})
                .orElse(ResolveResult.EMPTY_ARRAY);
    }

    @Override
    @Nullable
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);
        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }
}
