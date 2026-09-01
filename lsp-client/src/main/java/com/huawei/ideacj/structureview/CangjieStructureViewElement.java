/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.structureview;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CangjieStructureViewElement
 *
 * @since 2024-02-06
 */
public class CangjieStructureViewElement implements StructureViewTreeElement {
    private final NavigatablePsiElement myElement;

    public CangjieStructureViewElement(NavigatablePsiElement element) {
        this.myElement = element;
    }

    @Override
    public Object getValue() {
        return myElement;
    }

    @Override
    public void navigate(boolean isRequestFocus) {
        // 1. 检查类型并使用模式匹配
        if (!(myElement instanceof PsiNameIdentifierOwner identifierOwner)) {
            return;
        }

        // 2. 获取并校验标识符
        PsiElement nameIdentifier = identifierOwner.getNameIdentifier();
        if (nameIdentifier == null) {
            return;
        }

        // 3. 优先尝试直接跳转
        if (nameIdentifier instanceof NavigatablePsiElement) {
            ((NavigatablePsiElement) nameIdentifier).navigate(isRequestFocus);
            return;
        }

        // 4. 处理非直接跳转的情况
        PsiFile containingFile = nameIdentifier.getContainingFile();
        if (containingFile == null) {
            return;
        }

        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return;
        }

        // 5. 使用 OpenFileDescriptor 跳转
        OpenFileDescriptor descriptor = new OpenFileDescriptor(
                nameIdentifier.getProject(),
                virtualFile,
                nameIdentifier.getTextOffset()
        );

        if (descriptor.canNavigate()) {
            descriptor.navigate(isRequestFocus);
        } else {
            // 兜底逻辑
            myElement.navigate(isRequestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        if (myElement == null) {
            return false;
        }
        return myElement.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return myElement.canNavigateToSource();
    }

    /**
     * method getUpToDateElement
     *
     * @return PsiElement
     */
    public Optional<PsiElement> getUpToDateElement() {
        boolean isValid = myElement.isValid();
        if (!isValid) {
            return Optional.empty();
        }
        return Optional.of(myElement);
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return new CangjieItemPresentation(myElement);
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        return ReadAction.compute(() -> {
            if (getUpToDateElement().isEmpty()) {
                return EMPTY_ARRAY;
            }

            List<CJPsiNode> children = Arrays.stream(myElement.getChildren())
                    .filter(child -> child instanceof CJPsiNode)
                    .map(child -> (CJPsiNode) child)
                    .collect(Collectors.toList());

            // 策略模式
            FetchShowKindStrategy strategy = new BFSFetchShowKindStrategy(myElement);
            List<CJPsiNode> members = strategy.getShowCjPsiNodes(children);

            return members.stream()
                    .map(CangjieStructureViewElement::new)
                    .toArray(TreeElement[]::new);
        });
    }

    public PsiElement getRealElement() {
        return myElement;
    }
}