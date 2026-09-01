/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.importnode.CjImportAll;
import com.huawei.ideacj.language.psi.importnode.CjImportList;
import com.huawei.ideacj.language.psi.importnode.CjImportMulti;
import com.huawei.ideacj.language.psi.importnode.CjImportSingle;
import com.huawei.ideacj.language.psi.importnode.CjImportSpecified;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.packagenode.CjPackageHeader;
import com.huawei.ideacj.language.psi.packagenode.CjPackageNameIdentifier;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 查找所有路径以 package1.xxx.xx 开头的包名/导入语句
 *
 * @since 2025-08-28
 */
public class CangjiePackageImportMatcher {
    /**
     * 检查包名package语句是否匹配
     *
     * @param cjPackageHeader 包头元素
     * @param targetPrefix 目标前缀
     * @param targetPrefixSize 预计算的目标前缀大小
     * @return 匹配的元素 optional
     */
    public static Optional<PsiElement> getMatchingPackage(CjPackageHeader cjPackageHeader, List<String> targetPrefix,
        int targetPrefixSize) {
        // 快速失败检查
        if (cjPackageHeader == null || targetPrefix == null || targetPrefixSize == 0) {
            return Optional.empty();
        }
        CjPackageNameIdentifier nameIdentifier =
            PsiTreeUtil.getChildOfType(cjPackageHeader, CjPackageNameIdentifier.class);
        if (nameIdentifier == null) {
            return Optional.empty();
        }
        return checkPackageDeclare(nameIdentifier, targetPrefix, targetPrefixSize);
    }

    /**
     * 检查导入语句是否匹配
     *
     * @param cjImportListElement the cj import list element
     * @param targetPrefix the target prefix
     * @param targetPrefixSize the target prefix size
     * @return the List
     */
    public static List<PsiElement> getMatchingImports(CjImportList cjImportListElement,
        List<String> targetPrefix, int targetPrefixSize) {
        if (cjImportListElement == null || targetPrefix == null || targetPrefixSize == 0) {
            return Collections.emptyList();
        }
        // 查找importAllOrSpecified节点
        Collection<? extends CJPsiNode> cjImportPsiNodes =
            PsiTreeUtil.findChildrenOfAnyType(cjImportListElement, CjImportSpecified.class, CjImportAll.class,
                CjImportMulti.class);

        if (cjImportPsiNodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 检查不同类型的导入
        List<PsiElement> psiElements = new ArrayList<>();
        for (PsiElement cjImportPsiNode : cjImportPsiNodes) {
            if (cjImportPsiNode instanceof CjImportSpecified || cjImportPsiNode instanceof CjImportAll) {
                Optional<PsiElement> result =
                    checkImportSpecifiedOrAll(cjImportPsiNode, targetPrefix, targetPrefixSize);
                result.ifPresent(psiElements::add);
                continue;
            }
            if (cjImportPsiNode instanceof CjImportMulti) {
                psiElements.addAll(findMatchesInMultiImport(cjImportPsiNode, targetPrefix, targetPrefixSize));
            }
        }
        return psiElements;
    }

    /**
     * 检查CjImportSpecified/CjImportAll类型
     *
     * @param element the element
     * @param targetPrefix the target prefix
     * @param targetPrefixSize the target prefix size
     * @return the optional
     */
    private static Optional<PsiElement> checkImportSpecifiedOrAll(PsiElement element, List<String> targetPrefix,
        int targetPrefixSize) {
        List<PsiElement> path = extractIdentifierPath(element);
        return hasTargetPrefix(path, targetPrefix, targetPrefixSize);
    }

    /**
     * 检查CjImportMulti类型（多重导入）
     *
     * @param multi the multi
     * @param targetPrefix the target prefix
     * @param targetPrefixSize the target prefix size
     * @return the list
     */
    private static List<PsiElement> findMatchesInMultiImport(PsiElement multi, List<String> targetPrefix,
        int targetPrefixSize) {
        // 获取花括号前的前缀
        List<PsiElement> prefix = extractPrefixBeforeBraceOptimized(multi);
        final int prefixSize = prefix.size();
        if (prefixSize >= targetPrefixSize) {
            Optional<PsiElement> psiElement = hasTargetPrefix(prefix, targetPrefix, targetPrefixSize);
            return psiElement.map(List::of).orElse(Collections.emptyList());
        }
        if (prefixSize > 0) {
            Optional<PsiElement> matchElementOpt =
                hasTargetPrefixCommon(prefix, targetPrefix, targetPrefixSize, prefixSize);
            if (matchElementOpt.isEmpty()) {
                return Collections.emptyList();
            }
        }
        // 计算还需要匹配的剩余目标路径
        List<String> remainingPath = targetPrefix.subList(prefixSize, targetPrefixSize);
        return checkMultiWithoutPrefix(multi, remainingPath, targetPrefixSize - prefixSize);
    }

    /**
     * 检查多重导入
     *
     * @param multi the multi
     * @param remainingPath the remaining path
     * @param remainingSize the remaining size
     * @return the list
     */
    private static List<PsiElement> checkMultiWithoutPrefix(PsiElement multi, List<String> remainingPath,
        int remainingSize) {
        PsiElement[] children = multi.getChildren();
        List<PsiElement> psiElements = new ArrayList<>();
        for (PsiElement child : children) {
            if (child instanceof CjImportSpecified || child instanceof CjImportAll || child instanceof CjImportSingle) {
                Optional<PsiElement> matchElementOpt = checkImportSpecifiedOrAll(child, remainingPath, remainingSize);
                matchElementOpt.ifPresent(psiElements::add);
            }
        }
        return psiElements;
    }

    /**
     * 提取元素中的标识符路径
     *
     * @param element the element
     * @return the list
     */
    private static List<PsiElement> extractIdentifierPath(PsiElement element) {
        CjIdentifier[] cjIdentifiers = PsiTreeUtil.getChildrenOfType(element, CjIdentifier.class);
        if (cjIdentifiers == null || cjIdentifiers.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(cjIdentifiers);
    }

    /**
     * 提取花括号前的前缀标识符
     *
     * @param multi the multi
     * @return the list
     */
    private static List<PsiElement> extractPrefixBeforeBraceOptimized(PsiElement multi) {
        PsiElement[] children = multi.getChildren();
        List<PsiElement> prefix = new ArrayList<>(5);
        for (PsiElement child : children) {
            // 提前终止条件
            if ("{".equals(child.getText())) {
                break;
            }
            if (child instanceof CjIdentifier) {
                prefix.add(child);
            }
        }
        return prefix.isEmpty() ? Collections.emptyList() : prefix;
    }

    /**
     * 检查package name identifier类型
     *
     * @param element the element
     * @param targetPrefix the target prefix
     * @param targetPrefixSize the target prefix size
     * @return the optional
     */
    private static Optional<PsiElement> checkPackageDeclare(PsiElement element, List<String> targetPrefix,
        int targetPrefixSize) {
        List<PsiElement> path = extractIdentifierPath(element);
        return hasTargetPrefix(path, targetPrefix, targetPrefixSize);
    }

    /**
     * 检查路径是否以目标前缀开头
     *
     * @param packages the packages
     * @param targetPrefix the target prefix
     * @param targetPrefixSize the target prefix size
     * @return the optional
     */
    private static Optional<PsiElement> hasTargetPrefix(List<PsiElement> packages, List<String> targetPrefix,
        int targetPrefixSize) {
        return hasTargetPrefixCommon(packages, targetPrefix, packages.size(), targetPrefixSize);
    }

    /**
     * 通用的前缀匹配方法
     *
     * @param packages the packages
     * @param targetPrefix the target prefix
     * @param packageSize the package size
     * @param targetPrefixSize the target prefix size
     * @return the optional
     */
    private static Optional<PsiElement> hasTargetPrefixCommon(List<PsiElement> packages, List<String> targetPrefix,
        int packageSize, int targetPrefixSize) {
        // 快速失败：大小检查
        if (packageSize < targetPrefixSize) {
            return Optional.empty();
        }
        // 对于常见的短前缀，直接比较
        if (targetPrefixSize == 1) {
            return targetPrefix.getFirst().equals(packages.getFirst().getText())
                ? Optional.of(packages.getFirst())
                : Optional.empty();
        }
        // 对于常见的短前缀，直接比较
        if (targetPrefixSize == 2) {
            return targetPrefix.get(0).equals(packages.get(0).getText()) && targetPrefix.get(1)
                .equals(packages.get(1).getText()) ? Optional.of(packages.get(1)) : Optional.empty();
        }
        // 一般情况：循环比较
        for (int i = 0; i < targetPrefixSize; i++) {
            PsiElement element = packages.get(i);
            if (element == null) {
                return Optional.empty();
            }
            String packageText = element.getText();
            if (!targetPrefix.get(i).equals(packageText)) {
                return Optional.empty();
            }
        }
        return Optional.of(packages.get(targetPrefixSize - 1));
    }
}