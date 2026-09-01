/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.huawei.ideacj.language.CangjieIcons;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.classMembers.MemberInfoBase;

import javax.swing.Icon;

/**
 * 仓颉成员信息类，用于在提取接口对话框的列表中展示类/结构体的成员。
 *
 * @since 2026-04-02
 */
public class CangjieMemberInfo extends MemberInfoBase<PsiElement> {
    private final String mySignature;

    private final boolean isStatic;

    private final String visibility;

    /**
     * 构造函数。
     *
     * @param member      PSI 元素（通常对应仓颉的函数节点）
     * @param signature   该成员的完整签名（从 LSP 后端获取，如 "func attack(power: Int64): Unit"）
     * @param isStatic    是否为静态成员
     * @param visibility  访问修饰符级别
     */
    public CangjieMemberInfo(PsiElement member, String signature, boolean isStatic, String visibility) {
        super(member);
        this.mySignature = signature;

        // 设置在列表中显示的文本，通常直接显示方法签名
        this.displayName = signature != null ? signature : "<unknown>";

        this.isStatic = isStatic;
        this.visibility = visibility;
    }

    /**
     * 获取成员图标。
     *
     * @return 对应的 Icon 图标对象
     */
    public Icon getIcon() {
        if (this.mySignature.startsWith("<: ")) {
            return CangjieIcons.CANGJIE_INTERFACE;
        }
        return CangjieIcons.CANGJIE_FUNCTION;
    }

    /**
     * 获取成员的完整签名。
     * 该值将作为参数发送回 C++ 后端的 ExtractInterface::Apply 执行重构。
     *
     * @return 签名字符串
     */
    public String getSignature() {
        return mySignature;
    }

    /**
     * 校验成员是否有效。
     *
     * @return 如果成员非空且有效则返回 true
     */
    public boolean isValid() {
        return getMember() != null && getMember().isValid();
    }

    /**
     * 重写 equals 确保在 MemberSelectionTable 中行为正确。
     *
     * @param obj 待比较的外部对象
     * @return 如果特征及元素一致则返回 true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CangjieMemberInfo other)) {
            return false;
        }
        return mySignature.equals(other.mySignature) && getMember().equals(other.getMember());
    }

    @Override
    public int hashCode() {
        return mySignature.hashCode();
    }

    /**
     * 设置显示的文本名称。
     *
     * @param name 待设置的目标名称
     */
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    /**
     * 获取成员的访问修饰符。
     *
     * @return 访问修饰符字符串: "public", "protected", "private", 或 null
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * 获取成员是否为静态成员。
     *
     * @return 如果是静态成员则返回 true
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * 判断当前成员的可见性是否为 private。
     *
     * @return 如果修饰符为 private 则返回 true
     */
    public boolean isPrivate() {
        return visibility.equals("private");
    }
}