/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.resource;

import static com.huawei.ideacj.resource.CangjieResourceUtil.getResourceInput;

import com.huawei.ace.ohos.reference.EtsResourcesReferenceUtil;
import com.huawei.ace.ohos.reference.SysResourceManager;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.rrm.LocalResourceRepository;
import com.huawei.deveco.res.ohos.rrm.ResourceRepositoryManager;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;

import com.google.common.collect.ListMultimap;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CangjieResourceCompletionProvider
 *
 * @since 2024/10/6
 */
public class CangjieResourceCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters,
                                  @NotNull ProcessingContext processingContext,
                                  @NotNull CompletionResultSet completionResultSet) {
        PsiFile psiFile = completionParameters.getOriginalFile();
        PsiElement element = completionParameters.getPosition();
        String input = getResourceInput(element, false);
        if (StringUtils.isEmpty(input)) {
            return;
        }
        ModuleModel module = ModuleUtils.findModuleModelByPsiElement(psiFile);
        MyProvider myProvider = new MyProvider(psiFile, input, module);
        Set<LookupElement> resultSet = new HashSet<>();
        myProvider.addResourceElements(resultSet, completionParameters);
        handleResourceCompletion(completionResultSet, resultSet, input);
    }

    private static void handleResourceCompletion(@NotNull CompletionResultSet result, Set<LookupElement> resultSet,
                                                 @NotNull String userInput) {
        String matchPre;
        if (userInput.contains(CangjieResourceUtil.RESOURCE_SEPARATOR)) {
            matchPre = userInput.substring(userInput.lastIndexOf(CangjieResourceUtil.RESOURCE_SEPARATOR) + 1);
        } else {
            matchPre = userInput;
        }
        result.withPrefixMatcher(new CamelHumpMatcher(matchPre)).addAllElements(resultSet);
    }

    private static class MyProvider {
        private final PsiFile myPsiFile;

        private final String userInput;

        private final ModuleModel myModule;

        private MyProvider(PsiFile myPsiFile, String userInput, ModuleModel myModule) {
            this.myPsiFile = myPsiFile;
            this.userInput = userInput;
            this.myModule = myModule;
        }

        private void addResourceElements(@NotNull Set<LookupElement> result, @NotNull CompletionParameters parameters) {
            if (hasSameDotCount(0)) {
                // 第一段补全
                String myPrefix;
                if (ApplicationManager.getApplication().isUnitTestMode()
                        || !EtsResourcesReferenceUtil.isSysResourceJsExist(myPsiFile.getProject())) {
                    myPrefix = CangjieResourceUtil.PREFIX_APP;
                } else {
                    myPrefix = this.userInput.startsWith(EtsResourcesReferenceUtil.PREFIX_SYS_PREFIX)
                            ? CangjieResourceUtil.PREFIX_SYS : CangjieResourceUtil.PREFIX_APP;
                }
                result.addAll(addResourceSourcePrefix(myPrefix, parameters, this.userInput));
            } else if (hasSameDotCount(1)) {
                // 第二段补全
                result.addAll(addResourcesTypes());
            } else if (hasSameDotCount(2)) {
                // 第三段补全
                String resourceStr = this.userInput
                            .substring(this.userInput.indexOf(CangjieResourceUtil.RESOURCE_SEPARATOR) + 1);
                String typeName =
                        resourceStr.substring(0, resourceStr.indexOf(CangjieResourceUtil.RESOURCE_SEPARATOR));
                result.addAll(addResourcesValues(typeName));
            } else {
                return;
            }
        }

        private Collection<LookupElement> addResourceSourcePrefix(@NotNull String systemResourceType,
                                                                  @NotNull CompletionParameters parameters,
                                                                  @NotNull String inputText) {
            List<LookupElement> lookupElementList = new ArrayList<>();

            LookupElement builtInResTypes = getCompletionItemName(
                    systemResourceType.substring(0, systemResourceType.length() - 1));
            lookupElementList.add(builtInResTypes);
            return lookupElementList;
        }

        private Collection<LookupElement> addJsSystemResourcesValue(@NotNull String type) {
            Set<String> values = SysResourceManager.getInstance(this.myModule.getProjectModel().getProject())
                    .getSysResourceValues(type);
            return addJsSysResourcesLookupElement(values);
        }

        private Collection<LookupElement> addJsSystemResourcesTypes(@NotNull Project project) {
            Set<String> sysResourceTypes = SysResourceManager.getInstance(project).getSysResourceTypes();
            return addJsSysResourcesLookupElement(sysResourceTypes);
        }

        private Collection<LookupElement> addJsSysResourcesLookupElement(@NotNull Set<String> sysResourceTypes) {
            return sysResourceTypes.stream().map(this::getCompletionItemName).collect(Collectors.toSet());
        }

        private LookupElement getCompletionItemName(@NotNull String resName) {
            return LookupElementBuilder.create(resName).withIcon(AllIcons.Nodes.Field);
        }

        /**
         * check dot count
         *
         * @param dotCount dotCount
         * @return dot count
         */
        private boolean hasSameDotCount(int dotCount) {
            if (!this.userInput.contains(CangjieResourceUtil.RESOURCE_SEPARATOR)) {
                return dotCount == 0;
            }
            String newText = this.userInput.replace(CangjieResourceUtil.RESOURCE_SEPARATOR, "");
            return dotCount == this.userInput.length() - newText.length();
        }

        /**
         * resource type completion
         *
         * @return type completion
         */
        private Collection<LookupElement> addResourcesTypes() {
            if (this.userInput.startsWith(CangjieResourceUtil.PREFIX_APP)) {
                // 工程资源
                return getAppResourceTypes(this.myModule);
            } else if (this.userInput.startsWith(CangjieResourceUtil.PREFIX_SYS)) {
                // 系统资源
                return addJsSystemResourcesTypes(this.myModule.getProjectModel().getProject());
            } else {
                return new ArrayList<>();
            }
        }

        @NotNull
        private Collection<LookupElement> getAppResourceTypes(@NotNull ModuleModel moduleModel) {
            LocalResourceRepository localResourceRepository = ResourceRepositoryManager.getAppResources(moduleModel);
            if (localResourceRepository == null) {
                return new ArrayList<>();
            }
            Set<ResourceType> resourceTypes = localResourceRepository.getResourceTypes();
            return resourceTypes.stream()
                    .map(resourceType -> getCompletionItemName(resourceType.getName()))
                    .collect(Collectors.toSet());
        }

        /**
         * get resource name completion
         *
         * @param typeName typeName
         * @return resource name completion
         */
        private Collection<LookupElement> addResourcesValues(@NotNull String typeName) {
            if (this.userInput.startsWith(CangjieResourceUtil.PREFIX_APP)) {
                // 工程资源
                ResourceType resourceType = ResourceType.fromTypeName(typeName);
                return getAppResourcesOfModule(this.myModule, resourceType);
            } else if (this.userInput.startsWith(CangjieResourceUtil.PREFIX_SYS)) {
                // 系统资源
                return addJsSystemResourcesValue(typeName);
            } else {
                return new ArrayList<>();
            }
        }

        /**
         * get project resource
         *
         * @param module module
         * @param type type
         * @return project resource collection
         */
        private Collection<LookupElement> getAppResourcesOfModule(@NotNull ModuleModel module,
                                                                  @Nullable ResourceType type) {
            if (type == null) {
                return new ArrayList<>();
            }
            return Optional.of(module)
                    .map(ResourceRepositoryManager::getAppResources)
                    .map(localResourceRepository -> localResourceRepository.getResources(type))
                    .map(ListMultimap::keySet)
                    .orElse(new HashSet<>())
                    .stream()
                    .filter(StringUtil::isNotEmpty)
                    .filter(resName -> resName.length() > type.getName().length())
                    .map(resName -> {
                        resName = resName.substring(type.getName().length() + 1);
                        return getCompletionItemName(resName);
                    })
                    .collect(Collectors.toSet());
        }
    }
}
