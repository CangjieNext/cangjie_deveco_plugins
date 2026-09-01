/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.structureview;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;

import com.intellij.psi.PsiElement;

import java.util.List;

/**
 * 遍历 PSI 的策略接口
 *
 * @since 2025-11-07
 */
public interface FetchShowKindStrategy {
    /**
     * 当前 current 节点是否展示在 base 节点下
     *
     * @param base 父亲节点
     * @param current 儿子节点
     * @return 儿子节点是否属于父亲节点
     */
    boolean isShowKind(PsiElement base, PsiElement current);

    /**
     * 获取应该展示的节点
     *
     * @param cjPsiNodeList 节点列表
     * @return 节点集合
     */
    List<CJPsiNode> getShowCjPsiNodes(List<CJPsiNode> cjPsiNodeList);
}
