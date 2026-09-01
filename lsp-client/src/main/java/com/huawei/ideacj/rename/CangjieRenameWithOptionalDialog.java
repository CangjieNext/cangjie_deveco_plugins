/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.rename;

import static com.huawei.ideacj.lsp.utils.CommonUtils.isValidIdentifier;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.rename.RenameWithOptionalReferencesDialog;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JComponent;

/**
 * The type Cangjie rename with optional dialog.
 *
 * @since 2025-09-09
 */
public class CangjieRenameWithOptionalDialog extends RenameWithOptionalReferencesDialog {
    private final SearchScope predefinedScope;

    /**
     * Instantiates a new Cangjie rename with optional dialog.
     *
     * @param project the project
     * @param psiElement the psi element
     * @param nameSuggestionContext the name suggestion context
     * @param editor the editor
     */
    public CangjieRenameWithOptionalDialog(@NotNull Project project, @NotNull PsiElement psiElement,
        @Nullable PsiElement nameSuggestionContext, Editor editor) {
        super(project, psiElement, nameSuggestionContext, editor);
        this.predefinedScope = GlobalSearchScope.projectScope(project);
    }

    @Override
    protected boolean getSearchForReferences() {
        return this.getPsiElement() instanceof PsiFile
            ? RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE
            : RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY;
    }

    @Override
    protected void setSearchForReferences(boolean isSelected) {
        if (getPsiElement() instanceof PsiFile) {
            RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE = isSelected;
        } else {
            RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY = isSelected;
        }
    }

    @Override
    @Nullable
    protected JComponent createSearchScopePanel() {
        return new JPanel();
    }

    @Override
    @NotNull
    public SearchScope getRefactoringScope() {
        if (!(predefinedScope instanceof GlobalSearchScope globalSearchScope)) {
            return predefinedScope;
        }
        // 创建一个委托的全局搜索范围，添加文件过滤逻辑
        return getSearchScope(globalSearchScope);
    }

    @Override
    protected void canRun() throws ConfigurationException {
        String newName = getNewName();
        if (StringUtils.isBlank(newName) || !isValidIdentifier(newName)) {
            throw new ConfigurationException(LangBundle.message("dialog.message.valid.identifier", this.getNewName()));
        }
        super.canRun();
    }

    @NotNull
    private DelegatingGlobalSearchScope getSearchScope(GlobalSearchScope globalSearchScope) {
        return new DelegatingGlobalSearchScope(globalSearchScope) {
            @Override
            public boolean contains(@NotNull VirtualFile file) {
                // 首先检查原始范围是否包含该文件
                if (!super.contains(file)) {
                    return false;
                }
                return !file.getName().endsWith(".cj.macrocall");
            }
        };
    }
}
