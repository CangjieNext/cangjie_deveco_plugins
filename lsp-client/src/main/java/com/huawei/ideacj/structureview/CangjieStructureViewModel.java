/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.structureview;

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
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * CangjieStructureViewModel
 *
 * @since 2024-02-06
 */
public class CangjieStructureViewModel extends StructureViewModelBase
        implements StructureViewModel.ElementInfoProvider {
    private static final List<Class> SHOW_FIELDS = Arrays.asList(CjInterfaceDefinition.class, CjClassDefinition.class,
            CjStructDefinition.class, CjExtendDefinition.class, CjEnumDefinition.class, CjFunctionDefinition.class,
            CjTypeAlias.class, CjMainDefinition.class, CjPropertyDefinition.class, CjOperatorFunctionDefinition.class,
            CjClassInit.class, CjStructInit.class, CjClassPrimaryInit.class);

    private static final Sorter KINDSORTER = new Sorter() {
        private static final String ID = "KIND";

        private final Comparator<Object> myComparator = new Comparator<>() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return getWeight(o1) - getWeight(o2);
            }

            private int getWeight(final Object object) {
                Object obj = Optional.ofNullable(ObjectUtils.tryCast(object, StructureViewTreeElement.class))
                        .map(StructureViewTreeElement::getValue)
                        .orElse(null);
                if (isLevelOneNode(obj)) {
                    return 1;
                }
                if (isLevelTwoNode(obj)) {
                    return 2;
                }
                return 3;
            }
        };

        @Override
        public Comparator<Object> getComparator() {
            return myComparator;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        @NotNull
        public ActionPresentation getPresentation() {
            return new ActionPresentationData(IdeBundle.message("action.sort.by.type"),
                    IdeBundle.message("action.sort.by.type"), AllIcons.ObjectBrowser.Sorted);
        }

        @Override
        @NotNull
        public String getName() {
            return ID;
        }
    };

    private static Filter showFieldsFilter = new Filter() {
        /**
         * constant ID
         */
        @NonNls
        static final String ID = "SHOW_FIELDS";

        @Override
        public boolean isVisible(TreeElement treeNode) {
            if (!(treeNode instanceof CangjieStructureViewElement)) {
                return true;
            }
            final PsiElement element = ((CangjieStructureViewElement) treeNode).getRealElement();

            if (element != null && SHOW_FIELDS.contains(element.getClass())) {
                return true;
            }

            return element instanceof CjFunctionDefinition;
        }

        @Override
        public boolean isReverted() {
            return true;
        }

        @Override
        @NotNull
        public ActionPresentation getPresentation() {
            return new ActionPresentationData(IdeBundle.message("action.structureview.show.fields"), null,
                    AllIcons.Nodes.Field);
        }

        @Override
        @NotNull
        public String getName() {
            return ID;
        }
    };

    public CangjieStructureViewModel(PsiFile psiFile) {
        super(psiFile, new CangjieStructureViewElement(psiFile));
    }

    private static boolean isLevelOneNode(Object obj) {
        return obj instanceof CjClassDefinition || obj instanceof CjInterfaceDefinition
                || obj instanceof CjStructDefinition || obj instanceof CjEnumDefinition;
    }

    private static boolean isLevelTwoNode(Object obj) {
        return obj instanceof CjFunctionDefinition || obj instanceof CjMainDefinition
                || obj instanceof CjClassInit || obj instanceof CjClassPrimaryInit
                || obj instanceof CjOperatorFunctionDefinition
                || obj instanceof CjStructInit || obj instanceof CjStructPrimaryInit;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    /**
     * get Sorters
     *
     * @return Sorter[]
     */
    @Override
    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[]{KINDSORTER, Sorter.ALPHA_SORTER};
    }

    @Override
    @NotNull
    public Filter[] getFilters() {
        return new Filter[]{showFieldsFilter};
    }
}