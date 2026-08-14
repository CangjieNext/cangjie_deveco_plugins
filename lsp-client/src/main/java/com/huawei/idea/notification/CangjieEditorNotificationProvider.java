/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.notification;

import static com.huawei.idea.lsp.utils.CommonUtils.renameLspBuildPath;
import static com.huawei.idea.lsp.utils.LspConfigUtils.removeEditorListeners;
import static org.wso2.lsp4intellij.utils.FileUtils.getAllOpenedEditors;

import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModelManager;
import com.huawei.idea.lsp.extend.ExtendRequestManager;
import com.huawei.idea.lsp.launcher.CangjieCommandsServerDefinition;
import com.huawei.idea.lsp.launcher.CangjieCompileBuildListener;
import com.huawei.idea.lsp.utils.CangjieCommandExecutor;
import com.huawei.idea.lsp.utils.CommonUtils;
import com.huawei.idea.lsp.utils.LSPThreadPoolManager;
import com.huawei.idea.lsp.utils.PathConstants;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.swing.JComponent;

/**
 * Cangjie Editor Notification Provider
 * notify user restart lsp when cjpm.toml was edited
 *
 * @since 2023-12-28
 */
public class CangjieEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
    /**
     * fileChangeFlag
     */
    private static boolean isFileChange = false;

    private static VirtualFile changedFile = null;

    private Project project;

    public static void setIsFileChange(boolean isFileChange) {
        CangjieEditorNotificationProvider.isFileChange = isFileChange;
    }

    public static boolean getIsFileChange() {
        return CangjieEditorNotificationProvider.isFileChange;
    }

    public static void setChangedFile(VirtualFile file) {
        CangjieEditorNotificationProvider.changedFile = file;
    }

    public static VirtualFile getChangedFile() {
        return CangjieEditorNotificationProvider.changedFile;
    }

    @Override
    @Nullable
    public Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(
            @NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        return fileEditor -> {
            if (!(fileEditor instanceof TextEditor)) {
                return null;
            }
            if (!PathConstants.CJPM_FILE.equals(file.getName())
                    || !isFileChange || !file.equals(changedFile)) {
                return null;
            }
            return new MyNotificationPanel();
        };
    }

    private class MyNotificationPanel extends EditorNotificationPanel {
        /**
         * MyNotificationPanel
         * create notificationPanel info
         */
        public MyNotificationPanel() {
            setText("The cjpm.toml file has been modified, "
                    + "a project sync may be necessary for the IDE to work properly.");
            createActionLabel("Sync Now", () -> {
                Document document = FileDocumentManager.getInstance()
                        .getDocument(CangjieEditorNotificationProvider.changedFile);
                if (document == null) {
                    return;
                }
                FileDocumentManager.getInstance()
                        .saveDocument(document);
                CangjieEditorNotificationProvider.changedFile.refresh(false, false);
                CangjieEditorNotificationProvider.isFileChange = false;
                CangjieEditorNotificationProvider.changedFile = null;
                EditorNotifications.getInstance(project).updateAllNotifications();
                ProjectModel projectModel = ProjectModelManager.getInstance().getTargetProjectModel(project);
                if (projectModel != null) {
                    com.huawei.cangjie.projectmgmt.utils.FileUtils.removeStdxAttribute(projectModel);
                }
                CangjieCommandExecutor.executeCommand(project, "cjpm update",
                true, "cjpm update", result -> {
                    Set<LanguageServerWrapper> lspWrappers = IntellijLanguageClient.getProjectToLanguageWrappers()
                        .get(FileUtils.projectToUri(project));
                    if (lspWrappers == null) {
                        return;
                    }
                    checkLspStatus(lspWrappers);
                });
            });
        }

        private void checkLspStatus(Set<LanguageServerWrapper> lspWrappers) {
            boolean isStopLsp = false;
            ExtendRequestManager requestManager = null;
            for (LanguageServerWrapper lspWrapper : lspWrappers) {
                if (!(lspWrapper.serverDefinition instanceof CangjieCommandsServerDefinition)) {
                    continue;
                }
                if (!ServerStatus.INITIALIZED.equals(lspWrapper.getStatus())) {
                    continue;
                }
                if (lspWrapper.getRequestManager() instanceof ExtendRequestManager) {
                    requestManager = ((ExtendRequestManager) lspWrapper.getRequestManager());
                    requestManager.setRestarting(true);
                }
                removeEditorListeners(lspWrapper);
                lspWrapper.stop(true);
                List<Editor> allOpenedEditors = getAllOpenedEditors(project);
                allOpenedEditors.forEach(IntellijLanguageClient::editorClosed);
                isStopLsp = true;
                break;
            }
            if (isStopLsp) {
                Path lspBuildTempPath = renameLspBuildPath(project);
                CangjieCompileBuildListener.preStartLsp(project);
                ExtendRequestManager finalRequestManager = requestManager;
                LSPThreadPoolManager.pool(() -> {
                    IntellijLanguageClient.initProjectConnections(project);
                    if (finalRequestManager != null) {
                        finalRequestManager.setRestarting(false);
                    }
                });
                CommonUtils.tryDeleteDirectory(lspBuildTempPath);
            }
        }
    }
}
