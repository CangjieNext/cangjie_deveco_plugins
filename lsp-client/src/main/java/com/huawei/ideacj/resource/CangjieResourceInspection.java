/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ace.constants.ArkTSConstants.LEFT_SQUARE_BRACKET;
import static com.huawei.ace.constants.ArkTSConstants.RIGHT_SQUARE_BRACKET;
import static com.huawei.ideacj.resource.CangjieResourceUtil.COMMA;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createInvalidNameErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createMissingNameErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createMissingSourceErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createMissingTypeErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createSourceEmptyErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createUnknownNameErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createUnknownSourceErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createUnknownTypeErrorInfo;
import static com.huawei.ideacj.resource.CangjieResourceUtil.isResourceDecl;
import static com.huawei.ideacj.resource.CangjieResourceUtil.createUnSupportedSource;

import com.huawei.ace.ohos.reference.EtsResourcesReferenceUtil;
import com.huawei.ace.ohos.reference.SysResourceManager;
import com.huawei.ace.rawfileref.CrossModuleResourceManager;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.res.common.FolderTypeRelationship;
import com.huawei.deveco.res.common.ResourceFolderType;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.common.item.HosResourceItem;
import com.huawei.deveco.res.ohos.inspector.quickfix.CreateValueResourceQuickFix;
import com.huawei.deveco.res.ohos.rrm.LocalResourceRepository;
import com.huawei.deveco.res.ohos.rrm.ResourceRepositoryManager;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.deveco.res.ohos.utils.ProjectUtils;
import com.huawei.deveco.res.utils.ResourceUtils;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.ideacj.language.visitor.CangjieBasePsiVisitor;
import com.huawei.ideacj.lsp.utils.LspConfigUtils;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CangjieResourceInspection
 *
 * @since 2024/10/21
 */
public class CangjieResourceInspection extends LocalInspectionTool {
    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new CangjieBasePsiVisitor() {
            @Override
            public void visitMacroTokens(CjMacroTokens cjElement) {
                if (!ProjectUtils.isHvigorProject(cjElement.getProject())) {
                    return;
                }
                ModuleModel module = ModuleUtils.findModuleModelByPsiElement(cjElement);
                if (!(module instanceof OhosModuleModel)
                        || !LspConfigUtils.isCangjieModule((OhosModuleModel) module)) {
                    return;
                }
                if (!isResourceDecl(cjElement, false)) {
                    return;
                }
                handleReferenceValue(holder, cjElement, module);
            }
        };
    }

    private void handleReferenceValue(@NotNull ProblemsHolder holder,
                                      @NotNull CjMacroTokens cjElement,
                                      @NotNull ModuleModel module) {
        PsiFile psiFile = cjElement.getContainingFile();
        if (psiFile == null) {
            return;
        }
        checkReferenceValue(holder, module, psiFile, cjElement);
    }

    private void checkReferenceValue(@NotNull ProblemsHolder holder,
                                     @NotNull ModuleModel module,
                                     @NotNull PsiFile psiFile,
                                     @NotNull CjMacroTokens cjElement) {
        String input = cjElement.getText();
        // comma as separator
        if (input.contains(COMMA)) {
            input = input.substring(0, input.indexOf(COMMA));
        }
        if (StringUtil.isEmpty(input)) {
            holder.registerProblem(cjElement.getParent(), createSourceEmptyErrorInfo());
            return;
        }
        String[] values;
        if (StringUtil.startsWith(input, LEFT_SQUARE_BRACKET) && StringUtil.contains(input, RIGHT_SQUARE_BRACKET)) {
            values = CrossModuleResourceManager.parseCrossHspResource(input);
        } else {
            values = input.split(CangjieResourceUtil.RESOURCE_SEPARATOR_REG_REGULAR);
        }
        // 如果资源的参数长度大于1，并且第二个参数为 profile，则告警
        if (values.length > 1 && Objects.equals("profile", values[1])) {
            holder.registerProblem(cjElement, createUnSupportedSource());
            return;
        }
        if (input.startsWith(CangjieResourceUtil.RESOURCE_SEPARATOR) || values.length == 0) {
            holder.registerProblem(cjElement, createMissingSourceErrorInfo());
            return;
        }
        String source = values[CrossModuleResourceManager.RES_SOURCE_INDEX];
        if (isInvalidSource(holder, module, cjElement, source)) {
            return;
        }
        if (values.length == 1) {
            holder.registerProblem(cjElement, createMissingTypeErrorInfo());
            return;
        }
        ResourceInputInfo info = new ResourceInputInfo(cjElement, module, psiFile, input, values);
        if (CangjieResourceUtil.APP.equals(source)) {
            checkAppReferenceValue(holder, info);
        } else if (CangjieResourceUtil.SYS.equals(source)) {
            checkSystemReferenceValue(holder, info);
        } else {
            return;
        }
    }

    private boolean isInvalidSource(@NotNull ProblemsHolder holder, @NotNull ModuleModel module,
                                    @NotNull CjMacroTokens cjElement, String source) {
        if (!CangjieResourceUtil.APP.equals(source) && !CangjieResourceUtil.SYS.equals(source)) {
            holder.registerProblem(cjElement, createUnknownSourceErrorInfo(source));
            return true;
        }
        return false;
    }

    private void checkAppReferenceValue(@NotNull ProblemsHolder holder, @NotNull ResourceInputInfo info) {
        String[] values = info.getValues();
        String type = values[CrossModuleResourceManager.RES_TYPE_INDEX];
        ResourceType resourceType = ResourceType.fromTypeName(type);
        if (resourceType == null) {
            holder.registerProblem(info.getCjMacroTokens(), createUnknownTypeErrorInfo(type));
            return;
        }
        if (values.length == 2) {
            holder.registerProblem(info.getCjMacroTokens(), createMissingNameErrorInfo());
        } else if (values.length == 3) {
            String resourceName = values[CrossModuleResourceManager.RES_NAME_INDEX];
            if (!checkAppResourceName(info.getModule(), resourceType, resourceName)) {
                LocalQuickFix[] quickFixes = getResourceQuickFix(info.getCjMacroTokens(), resourceType, holder,
                        resourceName);
                holder.registerProblem(info.getCjMacroTokens(), createUnknownNameErrorInfo(resourceName), quickFixes);
            } else if (info.getText().endsWith(CangjieResourceUtil.RESOURCE_SEPARATOR)) {
                // "app.media.icon."
                holder.registerProblem(info.getCjMacroTokens(), createInvalidNameErrorInfo());
            } else {
                return;
            }
        } else {
            // "app.media.icon.png", "app.media.icon.png.a.b.c"
            holder.registerProblem(info.getCjMacroTokens(), createInvalidNameErrorInfo());
        }
    }

    private boolean checkAppResourceName(@NotNull ModuleModel module, @NotNull ResourceType resourceType,
                                         @NotNull String resourceName) {
        LocalResourceRepository appResources = ResourceRepositoryManager.getAppResources(module);
        if (appResources == null) {
            return false;
        }
        return appResources.getResources(resourceType)
                .values()
                .stream()
                .map(HosResourceItem::getOriginalName)
                .anyMatch(resourceName::equals);
    }

    private void checkSystemReferenceValue(@NotNull ProblemsHolder holder, @NotNull ResourceInputInfo info) {
        String[] values = info.getValues();
        Project project = holder.getProject();
        String type = values[CrossModuleResourceManager.RES_TYPE_INDEX];
        Set<String> resourcesTypes = SysResourceManager.getInstance(project).getSysResourceTypes();
        if (!resourcesTypes.contains(type)) {
            holder.registerProblem(info.getCjMacroTokens(), createUnknownTypeErrorInfo(type));
            return;
        }
        if (values.length == 2) {
            holder.registerProblem(info.getCjMacroTokens(), createMissingNameErrorInfo());
        } else if (values.length == 3) {
            registerProblems(holder, info, values);
        } else {
            // "sys.media.ic_app.png", "sys.media.ic_app.png.a.b.c"
            holder.registerProblem(info.getCjMacroTokens(), createInvalidNameErrorInfo());
        }
    }

    private void registerProblems(@NotNull ProblemsHolder holder, @NotNull ResourceInputInfo info, String[] values) {
        Project project = holder.getProject();
        if (!checkSystemResourceName(project, values[1], values[2])) {
            holder.registerProblem(info.getCjMacroTokens(), createUnknownNameErrorInfo(values[2]));
        } else if (info.getText().endsWith(EtsResourcesReferenceUtil.RESOURCE_SEPARATOR)) {
            // "sys.media.ic_app."
            holder.registerProblem(info.getCjMacroTokens(), createInvalidNameErrorInfo());
        } else {
            return;
        }
    }

    private LocalQuickFix[] getResourceQuickFix(PsiElement psiElement, ResourceType resourceType, ProblemsHolder holder,
                                                @NotNull String resourceName) {
        LocalQuickFix[] quickFixes = LocalQuickFix.EMPTY_ARRAY;
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(psiElement);
        if (module == null) {
            return quickFixes;
        }
        boolean isOhosTest = ResourceUtils.isInOhosTestPsiElement(psiElement);
        if (isOhosTest) {
            return quickFixes;
        }
        LocalResourceRepository localResourceRepository = ResourceRepositoryManager.getAppResources(module, false);
        if (localResourceRepository == null) {
            return quickFixes;
        }
        List<ResourceFolderType> resourceFolderTypes = FolderTypeRelationship.getRelatedFolders(resourceType);
        if (resourceFolderTypes.size() == 1 && resourceFolderTypes.contains(ResourceFolderType.ELEMENT)) {
            quickFixes = new LocalQuickFix[] {
                    new CreateValueResourceQuickFix(module, resourceType, resourceName, holder.getFile())
            };
        }
        return quickFixes;
    }

    private boolean checkSystemResourceName(@NotNull Project project, @NotNull String resourceType,
                                            @NotNull String resourceName) {
        Set<String> sysResourceNames = SysResourceManager.getInstance(project).getSysResourceValues(resourceType);
        return sysResourceNames.contains(resourceName);
    }

    private class ResourceInputInfo {
        private final CjMacroTokens cjElement;

        private final ModuleModel module;

        private final PsiFile psiFile;

        private final String text;

        private final String[] values;

        public ResourceInputInfo(@NotNull CjMacroTokens cjElement, @NotNull ModuleModel module,
                                 @NotNull PsiFile psiFile, @NotNull String text, @NotNull String[] values) {
            this.cjElement = cjElement;
            this.module = module;
            this.psiFile = psiFile;
            this.text = text;
            this.values = values;
        }

        @NotNull
        public CjMacroTokens getCjMacroTokens() {
            return cjElement;
        }

        @NotNull
        public ModuleModel getModule() {
            return module;
        }

        @NotNull
        public PsiFile getPsiFile() {
            return psiFile;
        }

        @NotNull
        public String getText() {
            return text;
        }

        @NotNull
        public String[] getValues() {
            return values;
        }
    }
}
