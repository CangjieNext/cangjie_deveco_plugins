/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.ui.AbstractMemberSelectionPanel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorFactory;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JScrollPane;

/**
 * 仓颉成员选择面板。
 * 负责组装分割线标题和成员表格。
 *
 * @since 2026-05-28
 */
public class CangjieMemberSelectionPanel extends AbstractMemberSelectionPanel<PsiElement, CangjieMemberInfo> {
    private static final long serialVersionUID = 1L;

    private final CangjieMemberSelectionTable myTable;

    /**
     * 构造仓颉成员选择面板。
     *
     * @param title 面板上方的标题（如：Members to form interface）
     * @param memberInfoList 成员列表数据源
     */
    public CangjieMemberSelectionPanel(String title, List<CangjieMemberInfo> memberInfoList) {
        super();
        setLayout(new BorderLayout());

        // 1. 初始化模型（Model）
        CangjieMemberSelectionTable.CangjieMemberInfoModel model
                = new CangjieMemberSelectionTable.CangjieMemberInfoModel(memberInfoList);

        // 2. 初始化表格（Table）
        myTable = new CangjieMemberSelectionTable(memberInfoList, model);

        // 3. 创建滚动面板包裹表格
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);

        // 4. 组装 UI：上方是带标题的分割线，中间是表格
        // SeparatorFactory.createSeparator 会生成 IntelliJ 原生的“文字 + 横线”装饰
        add(SeparatorFactory.createSeparator(title, myTable), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 获取内部持有的表格实例。
     *
     * @return 返回组件内部持有的仓颉成员选择表格对象实例
     */
    @Override
    public CangjieMemberSelectionTable getTable() {
        return myTable;
    }
}