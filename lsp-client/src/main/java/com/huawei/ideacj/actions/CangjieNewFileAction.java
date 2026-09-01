/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.actions;

import static com.huawei.deveco.projectmgmt.ohos.utils.CommonProjectUtil.getSelectFileOhosModuleModel;
import static com.huawei.ideacj.lsp.utils.LspConfigUtils.getPackageName;

import com.huawei.cangjie.projectmgmt.trace.TraceKind;
import com.huawei.cangjie.projectmgmt.trace.TraceUtils;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;

import com.intellij.ide.IdeView;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.EditorHelper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.utils.DocumentUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * the action to creat new Cangjie file.
 *
 * @since 2022-01-25
 */
public class CangjieNewFileAction extends DumbAwareAction {
    /**
     * the extension of Cangjie File
     */
    protected String extension = ".cj";

    /**
     * automatically import package declarations (e.g. package pkgName)
     */
    protected String packageStr = "package ";

    /**
     * whether to open the new file
     */
    protected boolean shouldOpenFile = true;
    private VirtualFile parentDir;

    @Override
    public void update(@NotNull AnActionEvent event) {
        if (!ActionsUtil.shouldShowNewCjFileButton(event)) {
            event.getPresentation().setVisible(false);
            return;
        }
        final DataContext dataContext = event.getDataContext();
        final Presentation presentation = event.getPresentation();
        final boolean isEnabled = isAvailable(dataContext);
        presentation.setVisible(isEnabled);
        presentation.setEnabled(isEnabled);
    }

    private static boolean isAvailable(DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final Object navigable = CommonDataKeys.NAVIGATABLE.getData(dataContext);
        // The path of new file is valid
        return project != null && (navigable instanceof PsiDirectory || navigable instanceof PsiFile);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if (view == null) {
            return;
        }
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        PsiDirectory directory = view.getOrChooseDirectory();
        if (directory == null || project == null) {
            notifyError(project, "Could not select directory to create file");
            return;
        }
        parentDir = directory.getVirtualFile();

        List<String> fileNames = new ArrayList<>();
        for (VirtualFile file : directory.getVirtualFile().getChildren()) {
            if (file == null || file.isDirectory() || StringUtil.isEmpty(file.getName())) {
                continue;
            }
            fileNames.add(file.getName().toLowerCase(Locale.ROOT));
        }
        DialogWithParameters dialog = new CangjieFileDialog(project, "New Cangjie File", this, fileNames);
        // If user cancelled action.
        if (dialog instanceof DialogWrapper && !((DialogWrapper) dialog).showAndGet()) {
            return;
        }
        if (!(dialog.getParameters().getOrDefault(CangjieFileDialog.DEFAULT_FILENAME, null) instanceof String)) {
            return;
        }
        String nameOfFile = (String) dialog.getParameters().getOrDefault(CangjieFileDialog.DEFAULT_FILENAME, null);
        if (nameOfFile == null) {
            return;
        }
        try {
            // create a new file , open it , and select in project view
            PsiFile createdFile = createNewFile(project, directory, nameOfFile, extension);
            if (createdFile != null) {
                view.selectElement(createdFile);
            }
            TraceUtils.trace(TraceKind.CJ_ADD_FILE);
            /**
             * This catches all creating file failure exceptions threw from inside blocks and notify user
             * even RuntimeExceptions as IllegalStateException and IncorrectOperationException
             */
        } catch (IOException | IllegalStateException | IncorrectOperationException ex) {
            notifyError(project, ex.getMessage());
        }
    }

    @Nullable
    private PsiFile createNewFile(Project project, PsiDirectory directory, String nameOfFile, String extension)
            throws IOException {
        String templateText = "";
        /**
         * add the creation time of file.
         * e.g. Created on 2021/7/16
         */
        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        String fileCreationTime = defaultProperties.getProperty("DATE", null);
        if (!StringUtil.isEmpty(fileCreationTime)) {
            templateText += "/**" + DocumentUtils.LINUX_SEPARATOR + " * Created on "
                    + fileCreationTime + DocumentUtils.LINUX_SEPARATOR + " */" + DocumentUtils.LINUX_SEPARATOR;
        }
        String packageName = "";
        OhosModuleModel module = getSelectFileOhosModuleModel(project, directory.getVirtualFile());
        if (module != null) {
            packageName = getPackageName(project, module, directory.getVirtualFile());
        }
        if (!StringUtil.isEmpty(packageName)) {
            templateText += packageStr + packageName;
        }
        String fullFileName = nameOfFile.endsWith(extension) ? nameOfFile : nameOfFile + extension;
        FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(fullFileName);
        PsiFile psiFileForTemplate = PsiFileFactory.getInstance(project).createFileFromText(fullFileName,
                type, templateText);
        PsiFile psiFile = WriteCommandAction.writeCommandAction(project)
                .compute(() -> directory.add(psiFileForTemplate)).getContainingFile();
        // The initial position of the cursor
        Editor editor = EditorHelper.openInEditor(psiFile);
        editor.getCaretModel().moveToOffset(templateText.length());

        VirtualFile newFile = psiFile.getVirtualFile();
        if (shouldOpenFile && newFile != null) {
            FileEditorManager.getInstance(project).openFile(newFile, true);
            return psiFile;
        }
        return null;
    }

    /**
     * display error information.
     *
     * @param project interface of Project
     * @param message String of message
     */
    protected void notifyError(Project project, String message) {
        Notifications.Bus.notify(
                new Notification("Cangjie Actions", "File Creation Error",
                        "Could not create a file: " + message, NotificationType.ERROR), project);
    }

    /**
     * check the validity of the file name.
     *
     * @param parameters parameters of dialog
     * @return String
     */
    @Nullable
    public String validateInput(Map<String, Object> parameters) {
        if (!(parameters.getOrDefault(CangjieFileDialog.DEFAULT_FILENAME, null) instanceof String)) {
            return null;
        }
        String inputFileName = (String) parameters.getOrDefault(CangjieFileDialog.DEFAULT_FILENAME, null);
        String fullFileName = inputFileName.endsWith(extension) ? inputFileName : inputFileName + extension;
        // Won't happen because we most likely can't reach it, as the dialog must not be created without a parent read
        if (parentDir == null) {
            return null;
        }
        VirtualFile child = parentDir.findChild(fullFileName);
        if (child != null && child.exists()) {
            return "File '" + fullFileName + "' already exists";
        }
        return null;
    }
}
