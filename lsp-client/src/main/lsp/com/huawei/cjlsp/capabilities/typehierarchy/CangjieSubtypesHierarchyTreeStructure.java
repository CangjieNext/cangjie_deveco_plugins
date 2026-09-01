/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.typehierarchy;

import static com.huawei.cjlsp.capabilities.typehierarchy.TypeHierarchyUtils.initNodeDescriptorIcon;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Cangjie subtypes hierarchy tree structure.
 *
 * @since 2022-01-19
 */
public class CangjieSubtypesHierarchyTreeStructure extends HierarchyTreeStructure {
    /**
     * Instantiates a new Cangjie subtypes hierarchy tree structure.
     *
     * @param project    the project
     * @param psiElement the psi element
     */
    protected CangjieSubtypesHierarchyTreeStructure(@NotNull Project project, @NotNull PsiElement psiElement) {
        super(project, new CangjieTypeHierarchyNodeDescriptor(null, psiElement, true, null, null));
    }

    // The node of the child class is generated layer by layer and added to the tree displayed on the interface.
    @Override
    protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        if (!(descriptor instanceof CangjieTypeHierarchyNodeDescriptor)) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        CangjieTypeHierarchyNodeDescriptor cangjieDescriptor = (CangjieTypeHierarchyNodeDescriptor) descriptor;
        final PsiElement targetElement = descriptor.getPsiElement();
        if (targetElement == null) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        initNodeDescriptorIcon(targetElement, descriptor);
        List<TypeHierarchyItem> items = new ArrayList<>();
        if (cangjieDescriptor.isBase()) {
            items = TypeHierarchyUtils.sendPrepareTypeHierarchy(targetElement);
        } else {
            items.add(cangjieDescriptor.getFromItem());
        }
        if (items == null || items.isEmpty()) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        TypeHierarchyItem item = items.get(0);
        List<TypeHierarchyItem> children = TypeHierarchyUtils.sendChildTypes(item, targetElement, true);
        if (children == null) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }

        List<CangjieTypeHierarchyNodeDescriptor> result = new ArrayList<>();
        // Generate a new node based on the mapping between items and elements.
        for (TypeHierarchyItem childItem : children) {
            Location location = new Location(childItem.getUri(), childItem.getSelectionRange());
            PsiElement newElement = TypeHierarchyUtils.turnLocation2PsiElement(location, myProject,
                    new Location(item.getUri(), item.getSelectionRange()));
            if (newElement == null) {
                return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
            }
            CangjieTypeHierarchyNodeDescriptor nodeDescriptor = new CangjieTypeHierarchyNodeDescriptor(descriptor,
                    newElement, false, childItem.getName(), childItem);
            result.add(nodeDescriptor);
        }
        return ArrayUtil.toObjectArray(result);
    }
}
