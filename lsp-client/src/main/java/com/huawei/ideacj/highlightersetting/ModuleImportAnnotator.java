/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.highlightersetting;

import com.huawei.ace.language.psi.impl.JavaScriptImportSpecifierImpl;
import com.huawei.ace.language.psi.impl.JavaScriptStringLiteralImpl;
import com.huawei.cangjie.projectmgmt.utils.CangjieModulePathType;
import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.ideacj.lsp.utils.CangjieBundle;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * ModuleImportAnnotator
 *
 * @since 2026-05-06
 */
public class ModuleImportAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder annotationHolder) {
        PsiFile file = element.getContainingFile();
        // 检查是否ets文件
        if (!isEtsFile(file)) {
            return;
        }

        if (!(element instanceof JavaScriptStringLiteralImpl literal)) {
            return;
        }

        // 检查该字符串是否属于 import 声明
        PsiElement parent = literal.getParent();
        if (!(parent instanceof JavaScriptImportSpecifierImpl)) {
            return;
        }

        Project project = element.getProject();
        ProjectModel projectModel = CommonProjectUtil.getProjectModel(project);
        if (projectModel == null) {
            return;
        }

        // 是否仓颉混合工程
        if (!isCangjieHybridProject(projectModel)) {
            return;
        }

        // APILevel是否低于18
        if (!isApiLevelBelow18(projectModel)) {
            return;
        }

        String modulePath = literal.getText();
        // 判断是否直接导入的仓颉模块
        if (!isImportCangjieSo(modulePath, projectModel)) {
            return;
        }

        // 标红波浪线，鼠标悬停提示语
        annotationHolder.newAnnotation(HighlightSeverity.ERROR,
                        CangjieBundle.message("lsp.import.method.invalid.message"))
                .range(literal.getTextRange())
                .create();
    }

    @Override
    public boolean isDumbAware() {
        return Annotator.super.isDumbAware();
    }

    private boolean isEtsFile(PsiFile file) {
        if (file == null) {
            return false;
        }
        return "ets".equals(file.getVirtualFile().getExtension());
    }

    private boolean isCangjieHybridProject(ProjectModel projectModel) {
        return FileUtils.isCangjieProject(projectModel) && isContainEtsModule(projectModel);
    }

    private boolean isContainEtsModule(ProjectModel projectModel) {
        for (ModuleModel model : projectModel.getModuleModelList()) {
            if (model instanceof OhosModuleModel && FileUtils.isContainEtsModule((OhosModuleModel) model)) {
                return true;
            }
        }
        return false;
    }

    private boolean isApiLevelBelow18(ProjectModel projectModel) {
        return projectModel.getFullCompatibleSdkVersion().getMajor() < 18;
    }

    private boolean isImportCangjieSo(String modulePath, ProjectModel projectModel) {
        if (modulePath == null || modulePath.length() < 2) {
            return false;
        }
        // 去除modulePath前后引号
        String moduleName = removeFirstAndLastChar(modulePath);
        if (!moduleName.endsWith(".so")) {
            return false;
        }
        for (ModuleModel model : projectModel.getModuleModelList()) {
            if (model instanceof OhosModuleModel && FileUtils.isContainEtsModule((OhosModuleModel) model)) {
                String cangjieModuleName = FileUtils.getCangjieModuleName((OhosModuleModel) model,
                        CangjieModulePathType.MAIN);
                if (StringUtil.isNotEmpty(cangjieModuleName) && moduleName.startsWith("lib" + cangjieModuleName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String removeFirstAndLastChar(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }
}
