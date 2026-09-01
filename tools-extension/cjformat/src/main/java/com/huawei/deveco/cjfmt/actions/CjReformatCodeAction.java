/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.actions;

import static com.huawei.deveco.cjfmt.utils.FormatConstant.REFORMAT_CODE;
import static com.huawei.deveco.cjfmt.utils.FormatConstant.REFORMAT_DIR;
import static com.huawei.deveco.cjfmt.utils.FormatUtils.checkFile;
import static com.huawei.deveco.cjfmt.utils.FormatUtils.notifyResult;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.HUMP_CANGJIE;

import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.deveco.cjfmt.core.ReformatCodeService;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.deveco.cjquality.utils.trace.TraceUtils;
import com.huawei.deveco.cjquality.utils.CangjieCompileArg;
import com.huawei.deveco.cjquality.utils.ExecuteResult;
import com.huawei.deveco.cjquality.utils.LanguageProperties;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.deveco.cjquality.utils.NotificationUtil;

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.lang.LanguageFormatting;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

/**
 * CJReformatCodeAction
 *
 * @since 2023-02-01
 */
public class CjReformatCodeAction extends ReformatCodeAction {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjReformatCodeAction.class);

    private boolean shouldExecuteOldReformatAction = true;

    private boolean hasSelectionIncludedDir = false;

    /**
     * CJReformatCodeAction
     */
    public CjReformatCodeAction() {
        setEnabledInModalContext(true);
        getTemplatePresentation().setText(LanguageProperties.message("title.cjfmt"));
        getTemplatePresentation().setDescription(LanguageProperties.message("title.cjfmt"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        long startTime = System.currentTimeMillis();
        boolean shouldExecuteCJFormatAction = shouldExecuteCJFormatAction(event);
        // 根据update运行的结果,分别执行仓颉格式化代码动作和intellij底座格式化代码的动作
        if (shouldExecuteOldReformatAction) {
            super.actionPerformed(event);
            return;
        }
        if (shouldExecuteCJFormatAction) {
            executeCJReformatAction(event);
            TraceUtils.trace(TraceUtils.Action.FORMAT, TraceUtils.Cause.DEFAULT,
                    -1, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 执行格式化仓颉代码的动作
     *
     * @param event event
     */
    private void executeCJReformatAction(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        final Project project = event.getProject();
        if (isProjectAbnormal(project)) {
            return;
        }
        if ("".equals(CangjieCompileArg.getCjSdkPath())) {
            CangjieCompileArg.initCjSdkPath(project);
        }
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        FileDocumentManager.getInstance().saveAllDocuments(); // Save all unsaved files
        final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
        if (files == null || files.length == 0) {
            return;
        }
        extractCangjieCodeReformat(event, project, editor, files, dataContext);
    }

    private void extractCangjieCodeReformat(@NotNull AnActionEvent event, Project project, Editor editor,
        VirtualFile[] files, DataContext dataContext) {
        PsiFile file = null;
        PsiDirectory dir;
        boolean hasSelection = false;
        final ReformatCodeService reformatCodeService = ReformatCodeService.getInstance(project);
        ActionManager actionManager = ActionManager.getInstance();
        if (editor != null) { // editor 不为空时取要格式化的文件或者目录
            file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }
            dir = file.getContainingDirectory();
            hasSelection = editor.getSelectionModel().hasSelection();
        } else if (containsOnlyFiles(files)) { // 当格式化多选文件时走的分支
            reformatFilesCode(files, project, reformatCodeService);
            actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event);
            return;
        } else if (PlatformCoreDataKeys.PROJECT_CONTEXT.getData(dataContext) != null
            || LangDataKeys.MODULE_CONTEXT.getData(dataContext) != null) { // 格式化module分支
            reformatFilesCode(files, project, reformatCodeService);
            actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event);
            return;
        } else { // editor为空且不是多选文件
            PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
            if (element == null) {
                return;
            }
            if (element instanceof PsiDirectoryContainer) {
                dir = ArrayUtil.getFirstElement(((PsiDirectoryContainer) element).getDirectories());
            } else if (element instanceof PsiDirectory) {
                dir = (PsiDirectory) element;
            } else {
                file = element.getContainingFile();
                if (file == null) {
                    return;
                }
                dir = file.getContainingDirectory();
            }
        }
        if (file == null && dir != null) { // 格式化目录
            executeCJReformatDirectoryCode(dir, project, actionManager, reformatCodeService, event);
            return;
        }
        if (file == null || editor == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        reformatCodeService.reformatFileCode(file, hasSelection, project, editor);
        actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event); // 重新加载格式化后的文件
    }

    private void executeCJReformatDirectoryCode(PsiDirectory dir, Project project, ActionManager actionManager,
        ReformatCodeService reformatCodeService, @NotNull AnActionEvent event) {
        Optional<ExecuteResult> resultOptional =
            reformatCodeService.reformatDirectoryCode(dir.getVirtualFile(), project, REFORMAT_DIR);
        if (resultOptional.isEmpty()) {
            LOGGER.warn("Reformat directory code is empty.");
            notifyResult(false, project);
            return;
        }
        if (resultOptional.get().exitCode() != 0) {
            LOGGER.warn("ExecuteCJReformatDirectoryCode, result is {}", resultOptional.get().executeOut());
            NotificationUtil.showWithProject(resultOptional.get().executeOut(), REFORMAT_CODE,
                NotificationUtil.Type.INFO, project);
        }
        actionManager.getAction("SynchronizeCurrentFile").actionPerformed(event); // 重新加载格式化后的文件
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
    }

    private boolean shouldExecuteCJFormatAction(@NotNull AnActionEvent event) {
        shouldExecuteOldReformatAction = false;
        // 动作启用条件:1.选中单个支持格式化的文件 2.选中多个文件且其中包含支持格式化的文件 3.选中单个文件夹且文件夹中包含支持格式化的文件
        final Project project = event.getProject();
        // 判断是否符合执行仓颉格式化代码动作的条件
        if (isProjectAbnormal(project)) {
            return false;
        }
        final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
        if (virtualFiles == null || virtualFiles.length == 0) {
            return false;
        } else {
            if (!isSelectedValid(project, virtualFiles)) {
                // 不符合执行仓颉格式化代码动作的情况下继续判断是否符合执行intellij底座格式化代码动作的条件
                updateOldFormatAction(event);
                return false;
            }
        }
        // 符合执行仓颉格式化代码动作的情况下继续判断是否符合执行intellij底座格式化代码动作的条件
        // 选择单个文件夹或者多个文件时进行判断
        if ((hasSelectionIncludedDir && virtualFiles.length == 1) || virtualFiles.length > 1) {
            shouldExecuteOldReformatAction = isOldReformatActionAvailable(event);
            event.getPlace();
        } else {
            shouldExecuteOldReformatAction = false;
        }
        return true;
    }

    private void updateOldFormatAction(@NotNull AnActionEvent event) {
        shouldExecuteOldReformatAction = isOldReformatActionAvailable(event);
        Presentation presentation = event.getPresentation();
        presentation.setEnabledAndVisible(shouldExecuteOldReformatAction);
    }

    private boolean isProjectAbnormal(Project project) {
        boolean isAbnormal = project == null;
        isAbnormal = isAbnormal || project.isDefault();
        isAbnormal = isAbnormal || !project.isInitialized();
        isAbnormal = isAbnormal || project.isDisposed();
        isAbnormal = isAbnormal || !project.isOpen();
        return isAbnormal;
    }

    /**
     * 查找是否有满足要求的文件
     *
     * @param project project
     * @param virtualFiles virtualFiles
     * @return boolean
     */
    private boolean isSelectedValid(Project project, VirtualFile[] virtualFiles) {
        Queue<VirtualFile> files = new ArrayDeque<>();
        hasSelectionIncludedDir = false;
        boolean hasSelectionIncludedFiles = false;
        int dirCount = 0;
        for (VirtualFile virtualFile : virtualFiles) {
            ModuleModel module = ApplicationManager.getApplication().runReadAction((Computable<ModuleModel>) () -> {
                return ModuleUtils.findModuleModelByVirtualFile(project, virtualFile);
            });
            if (!FileUtils.isCangjieModule(module)) {
                continue;
            }
            files.offer(virtualFile);
            if (virtualFile.isDirectory()) {
                hasSelectionIncludedDir = true;
                dirCount++;
                if (dirCount > 1) {
                    break;
                }
            } else {
                hasSelectionIncludedFiles = true;
                if (hasSelectionIncludedDir) {
                    break;
                }
            }
        }
        // 同时选中了文件夹和文件则动作禁用,选中多个文件夹则动作禁用
        if ((hasSelectionIncludedDir && hasSelectionIncludedFiles) || dirCount > 1) {
            return false;
        }
        while (!files.isEmpty()) {
            VirtualFile file = files.poll();
            if (file == null) {
                continue;
            }
            // 文件夹则取子文件入队列
            if (file.isDirectory()) {
                offerVirtualFile(files, file);
                continue;
            }
            // 文件则判断是否满足要求
            if (checkFile(file)) {
                return true;
            }
        }
        return false;
    }

    private void offerVirtualFile(Queue<VirtualFile> files, VirtualFile file) {
        if (file.getChildren() == null || file.getChildren().length == 0) {
            return;
        }
        for (VirtualFile virtualFile : file.getChildren()) {
            boolean isSuccess = files.offer(virtualFile);
            if (!isSuccess) {
                LOGGER.warn("add child file failed");
            }
        }
    }

    /**
     * 判断intellij底座的格式化代码action是否可用
     *
     * @param event event
     * @return boolean
     */
    private synchronized boolean isOldReformatActionAvailable(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return false;
        }
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        if (virtualFiles == null) {
            LOGGER.warn("virtualFiles is null");
            return false;
        }
        Queue<VirtualFile> files = new ArrayDeque<>();
        for (VirtualFile virtualFile : virtualFiles) {
            boolean isSuccess = files.offer(virtualFile);
            if (!isSuccess) {
                LOGGER.warn("add virtualFile failed");
            }
        }
        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null || file.getVirtualFile() == null) {
                return false;
            }

            if (LanguageFormatting.INSTANCE.forContext(file) != null) {
                return true;
            }
        } else if (virtualFiles.length == 1 || containsOnlyFiles(virtualFiles)) {
            return isSelectedValidForOldAction(project, files);
        } else if (LangDataKeys.MODULE_CONTEXT.getData(dataContext) == null
            && PlatformDataKeys.PROJECT_CONTEXT.getData(dataContext) == null) {
            PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
            if (element == null) {
                return false;
            }
            if (!(element instanceof PsiDirectory)) {
                PsiFile file = element.getContainingFile();
                return file != null && LanguageFormatting.INSTANCE.forContext(file) != null;
            }
        } else {
            LOGGER.info("skip old reformat action.");
        }
        return true;
    }

    private boolean isSelectedValidForOldAction(Project project, Queue<VirtualFile> files) {
        try {
            while (!files.isEmpty()) {
                VirtualFile virtualFile = files.poll();
                if (virtualFile == null) {
                    continue;
                }
                // 文件夹则取子文件入队列
                if (virtualFile.isDirectory()) {
                    offerVirtualFile(files, virtualFile);
                    continue;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    virtualFile.refresh(false, false);
                }, ModalityState.defaultModalityState());
                final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null || !psiFile.isValid()) {
                    return false;
                }
                if (!psiFile.getVirtualFile().getParent().isValid()) {
                    return false;
                }
                final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(psiFile);
                if (builder != null) {
                    return true;
                }
            }
            return false;
        } catch (InvalidVirtualFileAccessException e) {
            LOGGER.warn("invalid virtual file.");
            Notification formatterNotification = new Notification("Cangjie Formatter Notification", HUMP_CANGJIE,
                "Invalid virtual File, please sync project.", NotificationType.WARNING);
            formatterNotification.notify(project);
            return false;
        }
    }

    /**
     * 检查选中的选项是否只包含文件
     *
     * @param files files
     * @return boolean
     */
    static boolean containsOnlyFiles(VirtualFile @NotNull [] files) {
        if (files.length < 1) {
            return false;
        }
        for (VirtualFile virtualFile : files) {
            if (virtualFile.isDirectory()) {
                return false;
            }
        }
        return true;
    }

    private void reformatFilesCode(VirtualFile[] files, Project project, ReformatCodeService reformatCodeService) {
        boolean isInitConfig = false;
        for (VirtualFile virtualFile : files) {
            if (!checkFile(virtualFile) && !virtualFile.isDirectory()) {
                continue;
            }
            if (!isInitConfig) {
                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                reformatCodeService.initCodeStyleConfig(project, file);
                isInitConfig = true;
            }
            Optional<ExecuteResult> resultOptional = reformatCodeService.reformatCode(virtualFile, project);
            if (resultOptional.isEmpty()) {
                LOGGER.warn("reformat file failed.");
                notifyResult(false, project);
                break;
            }
            if (resultOptional.get().exitCode() != 0) {
                LOGGER.warn("reformat file failed, result is {}", resultOptional.get().executeOut());
                NotificationUtil.showWithProject(resultOptional.get().executeOut(), REFORMAT_CODE,
                    NotificationUtil.Type.INFO, project);
                break;
            }
        }
    }
}