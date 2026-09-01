/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.codefolding;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.folding.BaseResourceFoldingBuilder;
import com.huawei.deveco.res.ohos.folding.PresentResource;
import com.huawei.deveco.res.ohos.rrm.LocalResourceRepository;
import com.huawei.deveco.res.ohos.rrm.ResourceRepositoryManager;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.language.psi.othersnode.CjValueArgument;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.language.visitor.CangjieBasePsiVisitor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * use for check Cangjie RelevantFile
 *
 * @since 2026/3/28
 */
public class CangjieResourceFoldingBuilder extends BaseResourceFoldingBuilder {
    private static final String BOOLEAN_PREFIX = "app.boolean.";
    private static final String FLOAT_PREFIX = "app.float.";
    private static final String INTEGER_PREFIX = "app.integer.";
    private static final String STRING_PREFIX = "app.string.";
    private static final String COLOR_PREFIX = "app.color.";
    private static final String AT_R = "@r";

    @Override
    public FoldingDescriptor[] createFoldRegions(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof CangJiePsiFileRoot cangjiePsiFileRoot)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        FileType fileType = cangjiePsiFileRoot.getFileType();
        if (!(fileType instanceof com.huawei.ideacj.filetypes.CangjieCodeFile)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        List<FoldingDescriptor> foldingDescriptors = Lists.newArrayList();
        cangjiePsiFileRoot.accept(new CangjieVisitor(foldingDescriptors));
        return foldingDescriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    @Override
    public PresentResource getPresentResourceString(PsiElement psiElement) {
        if (!(psiElement instanceof CjMacroTokens cjMacroTokens)) {
            return PresentResource.NONE;
        }
        return findCangjieReference(cjMacroTokens);
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return false;
    }

    class CangjieVisitor extends CangjieBasePsiVisitor {
        List<FoldingDescriptor> foldingDescriptors;

        public CangjieVisitor(List<FoldingDescriptor> foldingDescriptors) {
            this.foldingDescriptors = foldingDescriptors;
        }

        @Override
        public void visitElement(@NotNull final PsiElement element) {
            super.visitElement(element);
            if (element instanceof CjValueArgument) {
                processCallExpression((CjValueArgument) element);
            } else {
                element.acceptChildren(this);
            }
        }


        private void processCallExpression(@NotNull CjValueArgument cjValueArgument) {
            String text = cjValueArgument.getText();
            if (!text.startsWith(AT_R)) {
                cjValueArgument.acceptChildren(this);
                return;
            }

            CjMacroTokens expression = PsiTreeUtil.findChildOfAnyType(cjValueArgument, CjMacroTokens.class);
            if (expression == null) {
                cjValueArgument.acceptChildren(this);
                return;
            }

            PresentResource presentResource = findCangjieReference(expression);
            if (presentResource != PresentResource.NONE) {
                FoldingDescriptor descriptor = presentResource.getDescriptor();
                if (descriptor != null) {
                    descriptor.setPlaceholderText(getPlaceholderText(descriptor.getElement()));
                    foldingDescriptors.add(descriptor);
                }
            }
            cjValueArgument.acceptChildren(this);
        }
    }


    private PresentResource findCangjieReference(PsiElement element) {
        String value = StringUtil.unquoteString(element.getText());

        if (value.startsWith(BOOLEAN_PREFIX)) {
            String name = "Boolean_" + value.substring((BOOLEAN_PREFIX).length());
            return createdPresentResourceInternal(ResourceType.BOOLEAN, name, element);
        }

        if (value.startsWith(FLOAT_PREFIX)) {
            String name = "Float_" + value.substring((FLOAT_PREFIX).length());
            return createdPresentResourceInternal(ResourceType.FLOAT, name, element);
        }

        if (value.startsWith(INTEGER_PREFIX)) {
            String name = "Integer_" + value.substring((INTEGER_PREFIX).length());
            return createdPresentResourceInternal(ResourceType.INTEGER, name, element);
        }

        if (value.startsWith(STRING_PREFIX)) {
            String name = "String_" + value.substring((STRING_PREFIX).length());
            return createdPresentResourceInternal(ResourceType.STRING, name, element);
        }

        if (value.startsWith(COLOR_PREFIX)) {
            String name = "Color_" + value.substring((COLOR_PREFIX).length());
            return createdPresentResourceInternal(ResourceType.COLOR, name, element);
        }

        return PresentResource.NONE;
    }

    PresentResource createdPresentResourceInternal(@NotNull ResourceType type, @NotNull String name,
                                                   @NotNull PsiElement foldElement) {
        Optional<LocalResourceRepository> appResource = getAppResource(foldElement);
        LocalResourceRepository appResources = appResource.orElse(null);
        if (appResources != null && appResources.hasResources(type)) {
            ASTNode node = foldElement.getNode();
            if (node != null) {
                TextRange textRange = foldElement.getTextRange();
                HashSet<Object> dependencies = new HashSet<>();
                dependencies.add(foldElement);
                PresentResource acePresentResource = new PresentResource(type, name.replace(":", ""), appResources,
                        new FoldingDescriptor(node, textRange, null, dependencies), foldElement);
                dependencies.add(acePresentResource);
                return acePresentResource;
            }
        }
        return PresentResource.NONE;
    }

    private Optional<LocalResourceRepository> getAppResource(@NotNull PsiElement foldElement) {
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(foldElement);
        if (module == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ResourceRepositoryManager.getAppResources(module));
    }
}