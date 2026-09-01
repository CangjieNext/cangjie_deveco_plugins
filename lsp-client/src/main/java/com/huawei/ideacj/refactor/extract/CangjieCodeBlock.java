/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * CangjieCodeBlock
 *
 * @since 2025-06-10
 */
public class CangjieCodeBlock {
    /**
     * project
     */
    private Project project;

    /**
     * editor
     */
    private Editor editor;

    /**
     * statementsToExtract
     */
    private List<PsiElement> statementsToExtract = new ArrayList<>();

    /**
     * expressionToExtract
     */
    private CJPsiNode expressionToExtract;

    /**
     * originFunction
     */
    private CjFunctionDefinition originFunction;

    /**
     * false
     */
    private boolean shouldReplaceAll = false;

    /**
     * similarBlocks
     */
    private List<PsiElement> similarBlocks = new ArrayList<>();

    /**
     * name
     */
    private String name;

    /**
     * returnName
     */
    private String returnName;

    /**
     * returnType
     */
    private String returnType;

    /**
     * CangjieCodeBlock
     *
     * @param project Project
     * @param editor  Editor
     */
    public CangjieCodeBlock(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;
    }

    public List<PsiElement> getStatementsToExtract() {
        return statementsToExtract;
    }

    public void setStatementsToExtract(List<PsiElement> statementsToExtract) {
        this.statementsToExtract = statementsToExtract;
    }

    public List<PsiElement> getSimilarBlocks() {
        return similarBlocks;
    }

    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public CJPsiNode getExpressionToExtract() {
        return expressionToExtract;
    }

    public void setExpressionToExtract(CJPsiNode expressionToExtract) {
        this.expressionToExtract = expressionToExtract;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public CjFunctionDefinition getOriginFunction() {
        return originFunction;
    }

    public void setOriginFunction(CjFunctionDefinition originFunction) {
        this.originFunction = originFunction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnName() {
        return returnName;
    }

    public void setReturnName(String returnName) {
        this.returnName = returnName;
    }

    public boolean isShouldReplaceAll() {
        return shouldReplaceAll;
    }

    public void setShouldReplaceAll(boolean shouldReplaceAll) {
        this.shouldReplaceAll = shouldReplaceAll;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
