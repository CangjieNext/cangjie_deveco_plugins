/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.move;

import static org.wso2.lsp4intellij.IntellijLanguageClient.getServerWrappersFor;

import com.huawei.cjlsp.capabilities.filerefactor.CangjieFileRefactorParam;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUpdateInfo;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUpdateInfo.UpdateInfo;
import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUsageInfo;
import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.res.ohos.utils.ModuleUtils;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.requests.Timeouts;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Locale;

/**
 * CangjieMoveFileHandler
 *
 * @since 2025/8/26
 */
public class CangjieMoveFileHandler extends MoveFileHandler {
    private static final Logger LOG = Logger.getInstance(CangjieMoveFileHandler.class);

    private static final int TIMEOUT = 50000;

    private static Set<String> needUpdates = new HashSet<>();

    private static Map<String, List<UpdateInfo>> updateFiles = new HashMap<>();

    private Project project;

    @Override
    public boolean canProcessElement(PsiFile psiFile) {
        return isCangjieFile(psiFile);
    }

    @Override
    public void prepareMovedFile(PsiFile psiFile, PsiDirectory psiDirectory, Map<PsiElement, PsiElement> map) {
    }

    @Override
    @Nullable
    public List<UsageInfo> findUsages(PsiFile psiFile, PsiDirectory psiDirectory, boolean b, boolean b1) {
        if (CangjieMoveChecker.getElements() == null || CangjieMoveChecker.getElements().length == 0
            || psiFile.getVirtualFile() == null) {
            return List.of();
        }
        if (needUpdates.isEmpty()) {
            for (PsiElement psiElement : CangjieMoveChecker.getElements()) {
                if (psiElement instanceof PsiDirectory directory) {
                    needUpdates.addAll(getAllMovedFiles(directory));
                } else if (psiElement instanceof PsiFile file && isCangjieFile(psiFile)) {
                    needUpdates.add(file.getVirtualFile().getUrl());
                } else {
                    continue;
                }
            }
        }
        needUpdates.remove(psiFile.getVirtualFile().getUrl());
        if (!checkSupport(psiFile, psiDirectory)) {
            return List.of();
        }
        String selectedFilePath = getSelectedFile(psiFile);
        if (selectedFilePath.isEmpty()) {
            return List.of();
        }
        List<UsageInfo> usages = new ArrayList<>();
        project = psiFile.getProject();
        doServerRequest(psiFile.getVirtualFile().getUrl(), psiDirectory, selectedFilePath).ifPresent(updateInfo -> {
            usages.add(new CangjieMoveUsageInfo(psiFile, updateInfo));
            mergeUpdates(updateInfo.getChanges());
            });

        return usages;
    }

    @Override
    public void retargetUsages(@Unmodifiable @NotNull List<? extends UsageInfo> list,
        @NotNull Map<PsiElement, PsiElement> map) {
        if (updateFiles.isEmpty()) {
            LOG.warn("updates already applied");
            return;
        }
        writeUpdates();
        updateFiles.clear();
    }

    @Override
    public void updateMovedFile(PsiFile psiFile) throws IncorrectOperationException {
    }

    private int getOffset(Position pos, Document document) {
        return document.getLineStartOffset(pos.getLine()) + pos.getCharacter();
    }

    private static boolean isCangjieFile(PsiFile psiFile) {
        return psiFile.getVirtualFile() != null
            && Objects.equals(psiFile.getVirtualFile().getExtension(), LanguageManager.CANGJIE_EXTENSION);
    }

    private static void mergeUpdates(Map<String, List<UpdateInfo>> newUpdates) {
        for (Map.Entry<String, List<UpdateInfo>> entry : newUpdates.entrySet()) {
            if (!updateFiles.containsKey(entry.getKey())) {
                updateFiles.put(entry.getKey(), entry.getValue());
            } else {
                mergeLists(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void mergeLists(String key, List<UpdateInfo> list) {
        for (UpdateInfo updateInfo : list) {
            if (!updateFiles.get(key).contains(updateInfo)) {
                updateFiles.get(key).add(updateInfo);
            }
        }
    }

    @NotNull
    private static String getSelectedFile(PsiFile psiFile) {
        String selectedFilePath = "";
        if (psiFile.getVirtualFile() == null) {
            return selectedFilePath;
        }
        for (PsiElement psiElement : CangjieMoveChecker.getElements()) {
            if (psiElement instanceof PsiDirectory directory
                && psiFile.getVirtualFile().getUrl().contains(directory.getVirtualFile().getUrl())) {
                selectedFilePath = directory.getVirtualFile().getUrl();
            } else if (psiElement instanceof PsiFile file
                && psiFile.getVirtualFile().getUrl().contains(file.getVirtualFile().getUrl())) {
                selectedFilePath = file.getVirtualFile().getUrl();
            } else {
                continue;
            }
        }
        return selectedFilePath;
    }

    @NotNull
    private static Set<String> getAllMovedFiles(PsiDirectory psiDirectory) {
        Set<String> files = new HashSet<>();
        for (PsiElement psiElement : psiDirectory.getChildren()) {
            if (psiElement instanceof PsiDirectory directory) {
                files.addAll(getAllMovedFiles(directory));
            } else if (psiElement instanceof PsiFile file && isCangjieFile(file)) {
                files.add(file.getVirtualFile().getUrl());
            } else {
                continue;
            }
        }
        return files;
    }

    private static Boolean checkSupport(PsiFile psiFile, PsiDirectory psiDirectory) {
        if (psiFile.getVirtualFile() == null) {
            LOG.warn("source file path doesn't exist");
            return false;
        }
        if (psiDirectory.getVirtualFile().getCanonicalPath() == null) {
            LOG.warn("target directory path doesn't exist");
            return false;
        }
        if (psiFile.getProject().getBasePath() == null) {
            LOG.warn("cannot find project base");
            return false;
        }
        if (!psiDirectory.getVirtualFile().getCanonicalPath().contains(psiFile.getProject().getBasePath())) {
            LOG.warn("target path not in project");
            return false;
        }
        ModuleModel srcMod = ModuleUtils.findModuleModelByVirtualFile(psiFile.getProject(), psiFile.getVirtualFile());
        ModuleModel dstMod = ModuleUtils.findModuleModelByVirtualFile(psiFile.getProject(), psiFile.getVirtualFile());
        if (srcMod == null || dstMod == null) {
            LOG.warn(String.format("cannot find src or des module"));
            return false;
        }
        String srcModuleRoot = srcMod.getModulePath();
        String dstModuleRoot = dstMod.getModulePath();
        if (srcModuleRoot == null || dstModuleRoot == null) {
            LOG.warn(String.format("module path doesn't exist, src: %s, dst: %s", srcModuleRoot, dstModuleRoot));
            return false;
        }
        return srcModuleRoot.equals(dstModuleRoot);
    }

    private Optional<Document> getDocument(String uri) {
        String decodedUri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(decodedUri);
        if (virtualFile == null) {
            LOG.warn(String.format("Virtual File Not Found: %s", uri));
            return Optional.empty();
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            LOG.warn(String.format("Psi File Not Found: %s", uri));
            return Optional.empty();
        }
        PsiDocumentManager pdm = PsiDocumentManager.getInstance(project);
        return Optional.ofNullable(pdm.getDocument(psiFile));
    }

    private Optional<CangjieMoveUpdateInfo> doServerRequest(String psiFileURI, PsiDirectory psiDirectory,
                                                        String selectedFilePath) {
        CangjieFileRefactorParam param =
                new CangjieFileRefactorParam(psiFileURI, psiDirectory.getVirtualFile().getUrl(), selectedFilePath);
        LanguageServerWrapper lspWrapper =
                getServerWrappersFor(LanguageManager.CANGJIE_EXTENSION,
                        FileUtils.projectToUri(project));
        if (lspWrapper == null || !(lspWrapper.getRequestManager() instanceof ExtendRequestManager requestManager)) {
            LOG.warn("LSP Server Not Ready");
            return Optional.empty();
        }
        CompletableFuture<CangjieMoveUpdateInfo> request = requestManager.fileRefactor(param);
        if (request == null) {
            LOG.warn("Find File Request Is Null");
            return Optional.empty();
        }
        CangjieMoveUpdateInfo myUsages = null;
        LOG.warn(String.format("Find File Request: %s", psiFileURI));
        try {
            myUsages = request.get(TIMEOUT, TimeUnit.MILLISECONDS); // 50s
            lspWrapper.notifySuccess(Timeouts.REFERENCES);
        } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
            requestManager.checkStatus();
            lspWrapper.notifyFailure(Timeouts.REFERENCES);
        }
        return myUsages == null ? Optional.empty() : Optional.of(myUsages);
    }

    private void writeUpdates() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiDocumentManager pdm = PsiDocumentManager.getInstance(project);
            for (String uri : updateFiles.keySet()) {
                Document doc = getDocument(uri).orElse(null);
                if (doc == null) {
                    LOG.warn(String.format("Doc of %s is NULL", uri));
                    continue;
                }
                List<UpdateInfo> updates = updateFiles.get(uri);
                // 按偏移量倒序应用更新
                Collections.sort(updates);
                performUpdates(updates, doc, pdm);
            }
        });
    }

    private void performUpdates(List<UpdateInfo> updates, Document doc, PsiDocumentManager pdm) {
        for (UpdateInfo update : updates) {
            if (doc.getLineCount() < update.getRange().getStart().getLine()
                || doc.getLineCount() < update.getRange().getEnd().getLine()) {
                LOG.warn(String.format(Locale.ROOT, "Start Line: %d, End Line: %d exceed doc length",
                    update.getRange().getStart().getLine(), update.getRange().getEnd().getLine()));
                continue;
            }
            int startOffset = getOffset(update.getRange().getStart(), doc);
            int endOffset = getOffset(update.getRange().getEnd(), doc);
            if (startOffset > doc.getTextLength() || endOffset > doc.getTextLength()) {
                LOG.warn(String.format(Locale.ROOT, "Start Offset: %d, End Offset: %d exceed doc length",
                    startOffset, endOffset));
                continue;
            }
            if (update.getType() == 3) {
                doc.deleteString(startOffset, endOffset);
            } else if (update.getType() == 1) {
                doc.insertString(startOffset, update.getContent());
            } else {
                doc.deleteString(startOffset, endOffset);
                doc.insertString(startOffset, update.getContent());
            }
            pdm.commitDocument(doc);
        }
    }
}
