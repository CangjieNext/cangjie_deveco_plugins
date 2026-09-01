/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.structureview;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.language.psi.toplevel.CjTypeAlias;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;

import com.intellij.psi.PsiElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * BFS 遍历 PSI 树策略类
 *
 * @since 2025-11-17
 */
public class BFSFetchShowKindStrategy implements FetchShowKindStrategy {
    private final PsiElement base;
    private final Map<Class<?>, Predicate<PsiElement>> showKindCheckers;

    public BFSFetchShowKindStrategy(PsiElement base) {
        this.base = base;
        this.showKindCheckers = new HashMap<>();
        initShowKindCheckers();
    }

    private void initShowKindCheckers() {
        showKindCheckers.put(CangJiePsiFileRoot.class, this::isRootShowKind);
        showKindCheckers.put(CjInterfaceDefinition.class, this::isInterfaceShowKind);
        showKindCheckers.put(CjClassDefinition.class, this::isClassShowKind);
        showKindCheckers.put(CjStructDefinition.class, this::isStructShowKind);
        showKindCheckers.put(CjExtendDefinition.class, this::isExtendShowKind);
        showKindCheckers.put(CjMacroDefinition.class, this::isMarcoShowKind);
        showKindCheckers.put(CjEnumDefinition.class, this::isEnumShowKind);
    }

    // 判断当前节点是不是base节点的showKind
    @Override
    public boolean isShowKind(PsiElement base, PsiElement current) {
        Predicate<PsiElement> checker = showKindCheckers.get(base.getClass());
        return checker != null && checker.test(current);
    }

    private boolean isRootShowKind(PsiElement topLevelNode) {
        return topLevelNode instanceof CjClassDefinition
                || topLevelNode instanceof CjInterfaceDefinition
                || topLevelNode instanceof CjFunctionDefinition
                || topLevelNode instanceof CjVariableDeclaration
                || topLevelNode instanceof CjEnumDefinition
                || topLevelNode instanceof CjStructDefinition
                || topLevelNode instanceof CjTypeAlias
                || topLevelNode instanceof CjExtendDefinition
                || topLevelNode instanceof CjMacroDefinition
                || topLevelNode instanceof CjMainDefinition;
    }

    private boolean isMarcoShowKind(PsiElement member) {
        return member instanceof CjVariableDeclaration;
    }

    private boolean isEnumShowKind(PsiElement member) {
        return member instanceof CjVariableDeclaration
                || member instanceof CjFunctionDefinition;
    }

    private boolean isExtendShowKind(PsiElement member) {
        return member instanceof CjFunctionDefinition
                || member instanceof CjPropertyDefinition
                || member instanceof CjOperatorFunctionDefinition;
    }

    private boolean isClassShowKind(PsiElement member) {
        return member instanceof CjVariableDeclaration
                || member instanceof CjFunctionDefinition
                || member instanceof CjClassInit
                || member instanceof CjClassPrimaryInit
                || member instanceof CjPropertyDefinition
                || member instanceof CjOperatorFunctionDefinition;
    }

    private boolean isInterfaceShowKind(PsiElement member) {
        return member instanceof CjFunctionDefinition
                || member instanceof CjOperatorFunctionDefinition
                || member instanceof CjPropertyDefinition;
    }

    private boolean isStructShowKind(PsiElement member) {
        return member instanceof CjVariableDeclaration
                || member instanceof CjFunctionDefinition
                || member instanceof CjStructInit
                || member instanceof CjStructPrimaryInit
                || member instanceof CjPropertyDefinition
                || member instanceof CjOperatorFunctionDefinition;
    }


    @Override
    public List<CJPsiNode> getShowCjPsiNodes(List<CJPsiNode> cjPsiNodeList) {
        List<CJPsiNode> members = new ArrayList<>();
        Queue<PsiElement> queue = new ArrayDeque<>(cjPsiNodeList);

        while (!queue.isEmpty()) {
            PsiElement current = queue.poll();
            // 遍历base节点的应该展示的类型
            if (isShowKind(base, current) && current instanceof CJPsiNode) {
                members.add((CJPsiNode) current);
                continue; // 已经是目标节点，无需再遍历子节点
            }

            PsiElement child = current.getFirstChild();
            while (child != null) {
                queue.offer(child);
                child = child.getNextSibling();
            }
        }
        return members;
    }
}
