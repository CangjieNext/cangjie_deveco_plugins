/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.copyright;

import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.TokenType;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.TreeTraversal;

import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdateAnyFileCopyright;
import com.maddyhome.idea.copyright.psi.UpdateCopyright;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.List;

/**
 * Cangjie Update File Copyright Provider
 *
 * @since 2025-07-01
 */
public class CangjieUpdateFileCopyrightProvider extends UpdateCopyrightsProvider {
    private static final Set<Language> CANGJIE_LANGUAGES = new HashSet<>(Arrays.asList(CangJieLanguage.INSTANCE));

    private static class MyUpdateCangjieFileCopyright extends UpdateAnyFileCopyright {
        public MyUpdateCangjieFileCopyright(Project project, Module module, VirtualFile root,
                                            CopyrightProfile options) {
            super(project, module, root, options);
        }

        @Override
        protected void scanFile() {
            List<PsiComment> comments = SyntaxTraverser.psiTraverser(getFile())
                    .withTraversal(TreeTraversal.LEAVES_DFS)
                    .traverse()
                    .skipWhile(node -> node == null || node.getNode().getElementType() == CangJieTypes.NL
                        || node.getNode().getElementType() == TokenType.WHITE_SPACE)
                    .takeWhile(Conditions.instanceOf(PsiComment.class, PsiWhiteSpace.class))
                    .filter(PsiComment.class)
                    .toList();
            checkComments(ContainerUtil.getLastItem(comments), true, comments);
        }

        @Override
        protected boolean accept() {
            return Optional.ofNullable(getFile())
                    .map(PsiElement::getLanguage)
                    .filter(CANGJIE_LANGUAGES::contains)
                    .isPresent();
        }
    }

    /**
     * Create Update Copyright Instance
     *
     * @param project Project
     * @param module Module
     * @param file VirtualFile
     * @param base FileType
     * @param options CopyrightProfile
     * @return UpdateCopyright
     */
    @Override
    public UpdateCopyright createInstance(Project project, Module module, VirtualFile file, FileType base,
                                          CopyrightProfile options) {
        return new MyUpdateCangjieFileCopyright(project, module, file, options);
    }
}
