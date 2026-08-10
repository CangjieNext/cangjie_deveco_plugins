/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.capabilities.lsp;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.isDynamicCombined;
import static com.huawei.capabilities.CangjiePackageUtils.extractPackageName;
import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;
import static org.wso2.lsp4intellij.requests.Timeouts.DEFINITION;

import com.huawei.capabilities.exports.ExportsItem;
import com.huawei.capabilities.exports.ExportsNameParam;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosDependency;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.idea.edit.CangjieEditorEventManager;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.idea.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.idea.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;
import com.huawei.idea.lsp.extend.ExtendRequestManager;
import com.huawei.idea.lsp.utils.LanguageManager;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.contributors.navigation.GotoDeclaration;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CangjieFindUsagesHandler
 *
 * @since 2022/10/19
 */
public class CangjieFindUsagesHandler extends FindUsagesHandler {
    private static final Logger LOG = Logger.getInstance(CangjieFindUsagesHandler.class);

    private final PsiElement originElement;

    private String packageName;

    /**
     * cangjie Find Usages Handler
     *
     * @param psiElement psiElement
     */
    protected CangjieFindUsagesHandler(@NotNull PsiElement psiElement) {
        super(psiElement);
        this.originElement = psiElement;
    }

    private static class DefaultReadActionProcessor extends ReadActionProcessor<PsiReference> {
        Processor<? super UsageInfo> myProcessor;

        DefaultReadActionProcessor(Processor<? super UsageInfo> processor) {
            this.myProcessor = processor;
        }

        /**
         * process in read action
         *
         * @param ref PsiReference
         * @return boolean
         */
        public boolean processInReadAction(PsiReference ref) {
            PsiElement el = ref.getElement();
            if (!el.isValid() || el.getProject().isDisposed()) {
                return false;
            }
            return myProcessor.process(new CangjieUsageInfo(ref));
        }
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element, @NotNull Processor<? super UsageInfo> processor,
                                        @NotNull FindUsagesOptions options) {
        List<PsiReference> psiReferences = new ArrayList<>(getReferences(originElement));
        Collection<PsiReference> searchResult = ReadAction.nonBlocking(() ->
                        ReferencesSearch.search(element, options.searchScope).findAll()
                ).executeSynchronously();
        psiReferences.addAll(searchResult);
        if (psiReferences.isEmpty()) {
            return false;
        }

        ReadActionProcessor<PsiReference> refProcessor = new DefaultReadActionProcessor(processor);
        boolean isSuccess = true;
        for (PsiReference psiReference : psiReferences) {
            boolean hasProcessResult = refProcessor.process(psiReference);
            if (!hasProcessResult) {
                isSuccess = false;
                break;
            }
        }

        return isSuccess;
    }

    /**
     * find reference by lsp
     *
     * @param element element to find reference
     *
     * @return PsiReference collection for references of element found
     */
    public Collection<PsiReference> getReferences(PsiElement element) {
        return ApplicationUtils.computableReadAction(() -> {
            if ("(".equals(element.getText())) {
                return new ArrayList<>();
            }

            Collection<PsiReference> result = new ArrayList<>(getReferencesByLsp(element));
            Collection<PsiReference> arkTsRefResult = ReadAction.computeBlocking(() ->
                getArkTsReferencesByLsp(element, element.getProject));
            result.addAll(arkTsRefResult);
            return result;
        });
    }

    private Collection<PsiReference> getArkTsReferencesByLsp(@NotNull PsiElement element, Project project) {
        Collection<PsiReference> result = Collections.emptyList();
        Editor editor = getEditor(element);
        Optional<String> libPath = getLibPath(element, project);
        PsiElement definition = new GotoDeclaration().getGotoDeclarationTarget(element, editor);
        if (!(definition instanceof CjFunctionDefinition) && !(definition instanceof CjInterfaceDefinition)
                && !(definition instanceof CjVariableDeclaration) || libPath.isEmpty()) {
            return result;
        }

        Optional<ExportsItem> exportsItem = getExportsItem(element, project);
        String className = (exportsItem.isEmpty() || "".equals(exportsItem.get().getContainerName()))
                ? null : exportsItem.get().getContainerName();
        className = className != null ? className : findEnclosingClassName(element).orElse("");
        String arkTSName = getArkTSName(definition, exportsItem);
        String functionType = getFunctionType(definition, className);
        Pair<String, String> arkTsFuncName = new Pair<>(functionType, arkTSName);
        try {
            Class<?> arkFindCpp = Class.forName("com.huawei.ace.crosslanguage.FindUsageCalledInCpp");
            Method method = arkFindCpp.getDeclaredMethod("findCppUsagesInArkTs", String.class, String.class,
                    Project.class, String.class);
            method.setAccessible(true);
            result = (List<PsiReference>) method.invoke(null, libPath.get(),
                    StringUtil.unquoteString(arkTsFuncName.second), project,
                    "globalFunction".equals(arkTsFuncName.first) ? null : arkTsFuncName.first);
        } catch (ClassNotFoundException | NoSuchMethodException
                 | InvocationTargetException | IllegalAccessException e) {
            LOG.warn("getArkTsReferencesByLsp error");
        }
        return result;
    }

    private String getArkTSName(PsiElement definition, Optional<ExportsItem> exportsItem) {
        String arkTSName = "";
        if (!(definition instanceof CjFunctionDefinition) && !(definition instanceof CjInterfaceDefinition)
            && !(definition instanceof CjVariableDeclaration)) {
            return arkTSName;
        }
        if (definition instanceof CjInterfaceDefinition) {
            arkTSName = ((CjInterfaceDefinition) definition).getName();
        } else if (definition instanceof CjVariableDeclaration) {
            arkTSName = (exportsItem.isEmpty() || "".equals(exportsItem.get().getExportName()))
                ? ((CjVariableDeclaration) definition).getName()
                : exportsItem.get().getExportName();
        } else {
            arkTSName = (exportsItem.isEmpty() || "".equals(exportsItem.get().getExportName()))
                ? ((CjFunctionDefinition) definition).getName()
                : exportsItem.get().getExportName();
        }
        return arkTSName;
    }

    private String getFunctionType(PsiElement definition, String className) {
        String functionType;
        if (definition instanceof CjInterfaceDefinition) {
            functionType = "globalFunction";
        } else if (definition instanceof CjVariableDeclaration) {
            functionType = (className.equals("")) ? "globalFunction" : className;
        } else {
            functionType = (className.equals("")) ? "globalFunction" : className;
        }
        return functionType;
    }

    private Optional<String> getLibPath(@NotNull PsiElement element, Project project) {
        ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
        if (projectModel == null) {
            return Optional.empty();
        }
        List<ModuleModel> moduleModels = projectModel.getModuleModelList();
        for (ModuleModel moduleModel : moduleModels) {
            String modulePath = moduleModel.getModulePath().replace("\\", "/");
            if (element.getContainingFile().getVirtualFile().getPath().contains(modulePath)
                    && moduleModel instanceof OhosModuleModel ohosModuleModel) {
                List<OhosDependency> dependencies = ohosModuleModel.getFinalDependencies();
                dependencies.addAll(ohosModuleModel.getFinalDevDependencies());
                dependencies.addAll(ohosModuleModel.getFinalDynamicDependencies());
                packageName = extractPackageNameFromElement(element, moduleModel).orElse("");
                String soName = String.format("lib%s.so", packageName);
                Optional<OhosDependency> targetDependency = dependencies.stream()
                        .filter(custom -> soName.equals(custom.getName()))
                        .findFirst();
                return targetDependency.map(ohosDependency -> ohosDependency.getDependencyPath().toString());
            }
        }
        return Optional.empty();
    }

    @Nullable
    private Editor getEditor(@NotNull PsiElement element) {
        PsiFile psiFile;
        try {
            psiFile = element.getContainingFile();
        } catch (PsiInvalidElementAccessException e) {
            LOG.warn("Get psiElement's containingFile error: {}", e);
            return null;
        }
        VirtualFile virtualFile = Optional.ofNullable(psiFile).map(PsiFile::getVirtualFile)
                .orElse(null);
        if (virtualFile == null) {
            return null;
        }
        FileEditor selectedEditor = FileEditorManager.getInstance(element.getProject()).getSelectedEditor(virtualFile);
        if (selectedEditor != null) {
            return selectedEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
        }
        return Arrays.stream(FileEditorManager.getInstance(element.getProject()).getAllEditors())
                .filter(fileEditor -> virtualFile.equals(fileEditor.getFile())).findFirst()
                .filter(fileEditor -> fileEditor instanceof TextEditor).map(fileEditor -> (TextEditor) fileEditor)
                .map(TextEditor::getEditor).orElse(null);
    }

    private Collection<PsiReference> getReferencesByLsp(PsiElement element) {
        Editor editor = getEditor(element);
        if (editor == null || editor.isDisposed()) {
            return Collections.emptyList();
        }
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager)) {
            return Collections.emptyList();
        }
        CangjieEditorEventManager cangjieManager = (CangjieEditorEventManager) manager;
        Pair<List<PsiElement>, List<VirtualFile>> references = cangjieManager
                .referencesForFindUsages(editor.getCaretModel().getCurrentCaret().getOffset(), false);
        List<PsiElement> elements = new ArrayList<>();
        if (references.first != null && references.second != null) {
            elements.addAll(references.first);
        }
        return elements.stream().map(PsiElement::getReference).collect(Collectors.toList());
    }

    /**
     * toString
     *
     * @return {@link String}
     */
    @Override
    public String toString() {
        return "CangjieFindUsagesHandler";
    }

    private Optional<String> extractPackageNameFromElement(PsiElement element, ModuleModel moduleModel) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return Optional.empty();
        }
        String fileContent = file.getText();
        if (isDynamicCombined(moduleModel)) {
            Optional<String> curPackageName = extractPackageName(fileContent);
            if (curPackageName.isEmpty()) {
                return Optional.empty();
            }
            int firstDotIndex = curPackageName.get().indexOf('.');
            if (firstDotIndex == -1) {
                return curPackageName;
            }
            return Optional.of(curPackageName.get().substring(0, firstDotIndex));
        }
        return extractPackageName(fileContent);
    }

    private Optional<String> findEnclosingClassName(PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof CjClassDefinition) {
                return Optional.ofNullable(((CjClassDefinition) current).getName());
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private Optional<ExportsItem> getExportsItem(PsiElement element, Project project) {
        Editor editor = getEditor(element);
        if (editor == null || editor.isDisposed()) {
            return Optional.empty();
        }
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager cangjieManager)) {
            return Optional.empty();
        }
        int offset = element.getTextRange().getStartOffset();
        Position lspPos = DocumentUtils.offsetToLSPPos(editor, offset);
        ExportsNameParam param = new ExportsNameParam(cangjieManager.getIdentifier(), lspPos, packageName);
        LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION, FileUtils.projectToUri(project));
        if (lspWrapper == null || !(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            return Optional.empty();
        }
        CompletableFuture<ExportsItem> request = requestManager.exportsName(param);
        if (request == null) {
            return Optional.empty();
        }
        ExportsItem exportsItem = null;
        try {
            exportsItem = request.get(50000, TimeUnit.MILLISECONDS);
            lspWrapper.notifySuccess(DEFINITION);
            if (Objects.equals(exportsItem, "")) {
                return Optional.empty();
            }
        } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
            requestManager.checkStatus();
            lspWrapper.notifyFailure(DEFINITION);
            return Optional.empty();
        }
        return Optional.ofNullable(exportsItem);
    }
}
