/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.ScopeNode;
import com.huawei.ideacj.filetypes.CangjieMacroCallFileType;
import com.huawei.ideacj.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.lsp.utils.CangjieMacrocallLanguage;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

/**
 * CangjieMacrocallPsiFileRoot
 *
 * @since 2024/04/30
 */
public class CangjieMacrocallPsiFileRoot extends PsiFileBase {
    /**
     * Entry macroExpression, the way to get class name, at most one in one file
     */
    private CjMacroExpression entryExpression = null;

    /**
     * MainEntry macroExpression, the way to get class name, at most one in one module
     */
    private CjMacroExpression mainEntryExpression = null;

    /**
     * mainEntryNodeList, the way to diagnostic MainEntry in one module and one file
     */
    private final List<LeafPsiElement> mainEntryNodeList = new ArrayList<>();

    /**
     * entryNodeList, the way to diagnostic Entry in one file
     */
    private final List<LeafPsiElement> entryNodeList = new ArrayList<>();

    /**
     * Preview macroExpression, the way to get class name
     */
    private final List<CjMacroExpression> previewExpressionList = new ArrayList<>();

    public CangjieMacrocallPsiFileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CangjieMacrocallLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return CangjieMacroCallFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Sample Language file";
    }

    @Override
    public Icon getIcon(int flags) {
        return AllIcons.FileTypes.Text;
    }

    /**
     * Return null since a file scope has no enclosing scope. It is
     * not itself in a scope.
     *
     * @return scope node
     */
    @Nullable
    @Override
    public ScopeNode getContext() {
        return null;
    }

    /**
     * Return CjTranslationUnit
     *
     * @return CjTranslationUnit
     */
    @Nullable
    public CjTranslationUnit getTranslationUnit() {
        return PsiTreeUtil.getChildOfType(this, CjTranslationUnit.class);
    }

    /**
     * Clean node message in this file
     */
    public void clearNodeMessage() {
        this.entryExpression = null;
        this.mainEntryExpression = null;
        this.entryNodeList.clear();
        this.mainEntryNodeList.clear();
        this.previewExpressionList.clear();
    }

    public CjMacroExpression getEntryExpression() {
        return entryExpression;
    }

    public void setEntryExpression(CjMacroExpression entryExpression) {
        this.entryExpression = entryExpression;
    }

    public CjMacroExpression getMainEntryExpression() {
        return mainEntryExpression;
    }

    public void setMainEntryExpression(CjMacroExpression mainEntryExpression) {
        this.mainEntryExpression = mainEntryExpression;
    }

    public List<LeafPsiElement> getMainEntryNodeList() {
        return mainEntryNodeList;
    }

    /**
     * insert node into list
     *
     * @param mainEntryNode @MainEntry block in this file
     */
    public void insertMainEntryNodeList(LeafPsiElement mainEntryNode) {
        this.mainEntryNodeList.add(mainEntryNode);
    }

    public List<LeafPsiElement> getEntryNodeList() {
        return entryNodeList;
    }

    /**
     * insert node into list
     *
     * @param entryNode  @Entry block in this file
     */
    public void insertEntryNodeList(LeafPsiElement entryNode) {
        this.entryNodeList.add(entryNode);
    }

    public List<CjMacroExpression> getPreviewExpressionList() {
        return previewExpressionList;
    }

    /**
     * insert node into list
     *
     * @param previewExpression @preview block in this file
     */
    public void insertPreviewExpressionList(CjMacroExpression previewExpression) {
        this.previewExpressionList.add(previewExpression);
    }

    /**
     * get @MainEntry and @Entry num in this file
     *
     * @return total @MainEntry and @Entry
     */
    public int getEntryTotal() {
        return this.mainEntryNodeList.size() + this.entryNodeList.size();
    }
}
