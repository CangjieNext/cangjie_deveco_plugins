/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.sync;

import static com.huawei.ideacj.lsp.utils.PathConstants.CJPM_LOCK_FILE;

import com.huawei.cangjie.projectmgmt.utils.Constants;
import com.huawei.cangjie.projectmgmt.utils.FileUtils;
import com.huawei.cangjie.projectmgmt.utils.SdkUtils;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.ProjectModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.projectmodel.ohos.sync.SyncRequest;
import com.huawei.deveco.projectmodel.ohos.sync.syncinterface.PreSync;
import com.huawei.ideacj.lsp.utils.CangjieCommandExecutor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The type Cangjie module pre sync.
 *
 * @since 2025/10/27
 */
public class CangjieModulePreSyncImpl implements PreSync {
    @Override
    public void checkStatus(@NotNull ProjectModel projectModel, @NotNull SyncRequest syncRequest) {
        updateModule(projectModel);
    }

    private void updateModule(@NotNull ProjectModel projectModel) {
        String sdkPath = SdkUtils.getSdkPath(projectModel);
        if (!Paths.get(sdkPath).toFile().exists()) {
            return;
        }
        Project project = projectModel.getProject();
        List<ModuleModel> modelsList = projectModel.getModuleModelList();
        List<String> commands = CangjieCommandExecutor.confEnv(project, "cjpm update --skip-script");
        for (ModuleModel model : modelsList) {
            if (!(model instanceof OhosModuleModel) || !FileUtils.isCangjieModule(model)) {
                continue;
            }
            String cjpmTomlDir = FileUtils.getRealCjpmTomlDir(model, false);
            if (Paths.get(cjpmTomlDir, CJPM_LOCK_FILE).toFile().exists() || !Paths.get(cjpmTomlDir, Constants.CJPM_FILE)
                .toFile().exists()) {
                continue;
            }
            CangjieCommandExecutor.executeWithProgress(project, commands, Paths.get(cjpmTomlDir).toFile(),
                "cjpm update", result -> {
                    refreshFile(Paths.get(cjpmTomlDir, CJPM_LOCK_FILE));
                });
        }
    }

    private void refreshFile(Path filePath) {
        File lockFile = filePath.toFile();
        if (!lockFile.exists()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            VirtualFile virtualFile = VfsUtil.findFileByIoFile(lockFile, true);
            if (virtualFile == null) {
                return;
            }
            Document document = ReadAction.compute(() ->
                FileDocumentManager.getInstance().getDocument(virtualFile));
            if (document == null) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    FileDocumentManager.getInstance().saveDocument(document);
                    virtualFile.refresh(false, false);
                });
            });
        });
    }
}
