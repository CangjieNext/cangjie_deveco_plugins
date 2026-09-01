/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract;

import static com.huawei.deveco.res.wizards.dialog.CreateResourceUtil.isExistenceSpecialFileInElement;

import com.huawei.ace.utils.AceEditorUtils;
import com.huawei.ace.utils.DotConstants;
import com.huawei.ace.utils.DotUtils;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.common.ResourceType;
import com.huawei.deveco.res.ohos.common.ResourceEditorBundle;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.deveco.res.ohos.wizards.fix.CreateResourceDialog;
import com.huawei.deveco.res.ohos.wizards.fix.ResourceParam;
import com.huawei.deveco.res.wizards.dialog.CreateResourceUtil;
import com.huawei.deveco.res.wizards.util.ResourceCreateContentUtil;
import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.othersnode.CjValueArgument;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjLambdaParam;
import com.huawei.ideacj.lsp.utils.CangjieBundle;
import com.huawei.ideacj.refactor.RefactorBaseHandler;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.containers.TreeTraversal;

import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * CangjieExtractResourceHandler
 *
 * @since 2026-03-28
 */
public class CangjieExtractResourceHandler extends RefactorBaseHandler {
    private static final String ELEMENT_RESOURCE_DIRECTORY = "element";
    private static final String CREATE_RESOURCE_FORMAT = "@r(app.%s.%s)";
    private static final Logger LOG = Logger.getInstance(CangjieExtractResourceHandler.class);

    private final ResourceInfo resourceInfo;
    private CreateResourceDialog refactorResourceDialog;

    public CangjieExtractResourceHandler(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
        // 设置父类的 tweak 标识，确保能匹配到 LSP 返回的 "Extract Resource" 操作
        this.setTweak(CangjieBundle.message("lsp.refactor.extract.resource.dialog.title"));
        this.setWarningMessage(CangjieBundle.message("lsp.refactor.extract.resource.dialog.invalid"));
    }

    @Override
    protected void executeCodeAction(Editor editor, CangjieCodeBlock codeBlock, int start, int end) {
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (!(manager instanceof CangjieEditorEventManager extendedManager)) {
            reportError(codeBlock, "Manager is null");
            return;
        }
        executeCommands(extendedManager, null);
    }

    @Override
    protected void executeCommands(CangjieEditorEventManager manager, Command command) {
        Project project = manager.getProject();
        if (resourceInfo == null) {
            LOG.debug("ResourceInfo is null, cannot proceed with extraction.");
            return;
        }

        PsiFile psiFile = resourceInfo.getResourceElement().getContainingFile();
        ModuleModel moduleModel = ModuleUtils.findModuleModelByPsiElement(psiFile);
        if (moduleModel == null) {
            return;
        }

        VirtualFile currentFile = psiFile.getVirtualFile();
        String defaultName = getResourceName(resourceInfo.getResourceElement());

        refactorResourceDialog = new CreateResourceDialog(
                new ResourceParam(project, moduleModel, resourceInfo.getResourceType(),
                        defaultName, resourceInfo.getResourceValue(), currentFile, true)
        );
        refactorResourceDialog.setTitle(CangjieBundle.message("lsp.refactor.extract.resource.dialog.title"));

        if (!refactorResourceDialog.showAndGet()) {
            return;
        }

        currentFile = refactorResourceDialog.getResourceDirectory();
        if (currentFile == null) {
            Messages.showErrorDialog(project, ResourceEditorBundle.message("check.resource.dir.error", moduleModel),
                    CommonBundle.getErrorTitle());
            return;
        }
        writeFile(project, psiFile, currentFile, resourceInfo.getResourceElement());
    }

    private void writeFile(Project project, PsiFile psiFile, VirtualFile resourceDirVirtualFile,
                           @NotNull PsiElement psiElement) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (!createResource(project, psiFile, resourceDirVirtualFile)) {
                LOG.warn("Create Resource failed");
                return;
            }
            String replaceStr = String.format(CREATE_RESOURCE_FORMAT,
                    refactorResourceDialog.getResourceType().getName(), refactorResourceDialog.getResourceName());
            Editor editor = AceEditorUtils.getEditor(psiElement);
            if (editor == null) {
                return;
            }
            Document document = editor.getDocument();
            document.replaceString(resourceInfo.getResourceElement().getTextRange().getStartOffset(),
                    resourceInfo.getResourceElement().getTextRange().getEndOffset(), replaceStr);
            FileDocumentManager.getInstance().saveDocument(document);
            DotUtils.dotExtract(project, DotConstants.ACTION_SUCCESS, DotConstants.ExtractType.EXTRACT_RESOURCE, "");
        });
    }

    /**
     * 在json文件中创建对应的资源
     *
     * @param project project
     * @param psiFile psiFile
     * @param resourceDirVirtualFile resourceDirVirtualFile
     * @return boolean
     */
    private boolean createResource(Project project, PsiFile psiFile, VirtualFile resourceDirVirtualFile) {
        String baseResourceUrl = resourceDirVirtualFile.getUrl();
        List<String> dirNames = refactorResourceDialog.getDirNames();
        if (StringUtil.isEmpty(baseResourceUrl) || dirNames == null || dirNames.isEmpty()) {
            return false;
        }
        for (String dirFileName : dirNames) {
            VirtualFile targetBaseDirVirtualFile = VirtualFileManager.getInstance().findFileByUrl(baseResourceUrl);
            if (targetBaseDirVirtualFile == null) {
                LOG.warn(
                        String.format(Locale.ROOT,
                                "Refactor Resource failed: targetBaseDirVirtualFile is null under %s",
                                dirFileName));
                continue;
            }
            VirtualFile qualifierDir = CreateResourceUtil.getChildDirectory(targetBaseDirVirtualFile, dirFileName);
            if (qualifierDir == null) {
                LOG.warn(
                        String.format(Locale.ROOT,
                                "Refactor Resource failed: qualifierDir is null under %s",
                                dirFileName));
                continue;
            }
            VirtualFile elementDir = CreateResourceUtil.getChildDirectory(qualifierDir, ELEMENT_RESOURCE_DIRECTORY);
            if (elementDir == null) {
                LOG.warn(
                        String.format(Locale.ROOT,
                                "Refactor Resource failed: elementDir is null under %s",
                                dirFileName));
                continue;
            }
            PsiDirectory elementPsiDir = PsiManager.getInstance(project).findDirectory(elementDir);
            if (elementPsiDir == null) {
                LOG.warn(String.format(Locale.ROOT,
                        "Refactor Resource failed: elementPsiDir is null under %s",
                        dirFileName));
                continue;
            }
            String fileName = refactorResourceDialog.getFileName();
            try {
                if (isExistenceSpecialFileInElement(Collections.singleton(elementDir), fileName)) {
                    // 若名称为fileName的资源文件已存在，则在该文件新增条目
                    addKeyValueMethod(project, elementDir.getUrl() + "/" + fileName, elementPsiDir);
                } else {
                    // 不存在则新建资源文件
                    createTemFile(project, fileName, elementPsiDir);
                }
            } catch (Exception exception) {
                LOG.warn("Refactor Resource failed");
                return false;
            }
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        UndoUtil.markPsiFileForUndo(psiFile);
        return true;
    }

    private void createTemFile(Project project, String fileName, @NotNull PsiDirectory elementPsiDir) throws Exception {
        String value = refactorResourceDialog.getValue();
        PsiElement file = CreateResourceUtil.createFileResource(fileName, elementPsiDir,
                refactorResourceDialog.getResourceType(), refactorResourceDialog.getResourceName(), value.trim());
        if (file != null) {
            // 因为文件创建后 VFS 会刷新，原始的 'file' 引用可能瞬间变为 invalid
            var pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file);

            // 必须脱离当前的 WriteCommandAction，等 IDE 的缓存系统和 VFS 稳定后执行 UI 动作
            ApplicationManager.getApplication().invokeLater(() -> {
                PsiElement validFile = pointer.getElement();
                if (validFile != null && validFile.isValid()) {
                    PsiNavigateUtil.navigate(validFile);
                }
            });
        }
    }

    private String getResourceName(@NotNull PsiElement psiElement) {
        // 查找带()组件的方法名
        PsiElement lamdaParam = PsiTreeUtil.findFirstParent(psiElement,
                psiElement1 -> psiElement1 instanceof CjLambdaParam);
        CjIdentifier identifier = PsiTreeUtil.findChildOfType(lamdaParam, CjIdentifier.class);
        if (identifier != null) {
            return identifier.getText();
        }

        PsiElement firstParent = PsiTreeUtil.findFirstParent(psiElement,
                psiElement1 -> psiElement1 instanceof CjPostfixExpression);
        CjPostfixExpression childOfType = PsiTreeUtil.findChildOfType(firstParent, CjPostfixExpression.class);
        if (childOfType != null) {
            // 不带()的组件方法名
            CjIdentifier cjIdentifier = PsiTreeUtil.getChildOfType(childOfType, CjIdentifier.class);
            // 否则就返回组件名称
            return cjIdentifier != null ? cjIdentifier.getText() : childOfType.getText();
        }
        return "";
    }

    /**
     * 解析参数类型
     *
     * @param element element
     * @return 资源对象
     */
    @NotNull
    public static Optional<ResourceInfo> parsingParameterType(@NotNull PsiElement element) {
        // 1. 查找父级参数节点
        PsiElement parentArgument = PsiTreeUtil.findFirstParent(element,
                e -> e instanceof CjValueArgument);

        // 2. 基础过滤：如果不是参数，或者包含点号（成员访问），直接返回空 Optional
        if (!(parentArgument instanceof CjValueArgument) || hasDotNavigation(parentArgument)) {
            return Optional.empty();
        }

        String content = StringUtil.unquoteString(parentArgument.getText());

        // 3. 按照优先级匹配类型
        ResourceType type = resolveType(content);

        // 4. 返回封装好的 Optional 对象，永远不会是 null
        return Optional.of(new ResourceInfo(type, content, parentArgument));
    }

    private static ResourceType resolveType(String content) {
        if (ResourceCreateContentUtil.checkValueInteger(content)) {
            return ResourceType.INTEGER;
        }
        if (ResourceCreateContentUtil.checkValueFloat(content)) {
            return ResourceType.FLOAT;
        }
        if (ResourceCreateContentUtil.checkValueIntArray(content)) {
            return ResourceType.INTARRAY;
        }
        if (ResourceCreateContentUtil.checkValueStrArray(content)) {
            return ResourceType.STRARRAY;
        }
        if (ResourceCreateContentUtil.checkValueBoolean(content)) {
            return ResourceType.BOOLEAN;
        }
        if (ResourceCreateContentUtil.checkValueColor(content)) {
            return ResourceType.COLOR;
        }

        // 兜底默认类型
        return ResourceType.STRING;
    }

    private static boolean hasDotNavigation(PsiElement element) {
        return SyntaxTraverser.psiTraverser(element)
                .withTraversal(TreeTraversal.LEAVES_BFS)
                .traverse()
                .filter(node -> ".".equals(node.getText()))
                .first() != null;
    }

    private void addKeyValueMethod(@NotNull Project project, String targetFileUrl, @NotNull PsiDirectory psiDirectory) {
        if (StringUtil.isEmpty(targetFileUrl)) {
            return;
        }
        Pair<String, String> resourcePair = new Pair<>(refactorResourceDialog.getResourceName(),
                refactorResourceDialog.getValue());
        CreateResourceUtil.addKeyValueMethod(project, targetFileUrl, refactorResourceDialog.getResourceType().getName(),
                resourcePair, psiDirectory);
    }

    /**
     * 资源抽取详情
     */
    public static class ResourceInfo {
        private ResourceType resourceType;

        private String resourceValue;

        private PsiElement resourceElement;

        public ResourceInfo(ResourceType resourceType, String resourceValue, PsiElement resourceElement) {
            this.resourceType = resourceType;
            this.resourceValue = resourceValue;
            this.resourceElement = resourceElement;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public void setResourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceValue() {
            return resourceValue;
        }

        public void setResourceValue(String resourceValue) {
            this.resourceValue = resourceValue;
        }

        public PsiElement getResourceElement() {
            return resourceElement;
        }

        public void setResourceElement(PsiElement resourceElement) {
            this.resourceElement = resourceElement;
        }
    }
}
