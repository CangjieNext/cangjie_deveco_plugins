/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.listener;

import static org.wso2.lsp4intellij.utils.FileUtils.editorToURIString;

import com.huawei.ideacj.lsp.utils.LanguageManager;

import com.google.common.io.Files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.problems.ProblemListener;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.PsiManagerImpl;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.LanguageServerDefinition;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.listeners.LSPFileEventManager;
import org.wso2.lsp4intellij.listeners.VFSListener;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cangjie LSP File Listener
 *
 * @since 2024-1-19
 */
public class CangjieFileListener extends VFSListener {
    private static final Logger LOG = Logger.getInstance(CangjieFileListener.class);

    private static final Set<String> FILE_EXTENSIONS = Set.of(LanguageManager.CANGJIE_EXTENSION);

    /**
     * Fired when a virtual file is renamed from within IDEA, or its writable status is changed.
     * For files renamed externally, {@link #fileCreated} and {@link #fileDeleted} events will be fired.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        if (event.getOldValue() == null || !isCangjieFile(event)
                && !FILE_EXTENSIONS.contains(Files.getFileExtension(event.getOldValue().toString()))) {
            return;
        }

        if (!VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            return;
        }

        final String oldFileName = event.getOldValue().toString();
        final String newFileName = event.getNewValue().toString();
        final String url = FileUtils.VFSToURI(event.getFile());
        if (Strings.isEmpty(url)) {
            return;
        }
        // It is useless to rename a file with same name
        if (StringUtils.isEmpty(oldFileName) || StringUtils.isEmpty(newFileName) || newFileName.equals(oldFileName)) {
            return;
        }
        final String oldPath = url.substring(0, url.length() - newFileName.length()) + oldFileName;

        final Project project = FileUtils.findGuessProjectsFor(event.getFile());
        if (project == null) {
            return;
        }
        FileUtils.getAllOpenedEditors(project)
                .stream()
                .map(EditorEventManagerBase::forEditor)
                .filter(Objects::nonNull)
                .forEach(manager -> {
                    final TextDocumentIdentifier identifier = manager.getIdentifier();
                    final String oldUri = identifier.getUri();
                    if (oldUri.startsWith(oldPath)) {
                        identifier.setUri(editorToURIString(manager.editor));
                        manager.getChangesParams().getTextDocument().setUri(identifier.getUri());
                        manager.documentEventManager.getIdentifier().setUri(
                                editorToURIString(manager.editor));
                    }
                });
        fileRename(oldFileName, newFileName, event);
    }

    /**
     * Fired when the contents of a virtual file is changed.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        if (!isCangjieFile(event)) {
            return;
        }
        LSPFileEventManager.fileChanged(event.getFile());
    }

    /**
     * Fired when a virtual file is deleted.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        if (!isCangjieFile(event)) {
            return;
        }
        VirtualFile file = event.getFile();
        Project project = getProject(event, file);
        if (project != null) {
            LSPFileEventManager.fileDeleted(file, project);
        } else {
            LSPFileEventManager.fileDeleted(file);
        }
        if (Objects.nonNull(project)) {
            project.getMessageBus()
                    .syncPublisher(com.intellij.problems.ProblemListener.TOPIC)
                    .problemsDisappeared(event.getFile());
        }
    }

    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
        if (event.getFile().isDirectory()) {
            beforeDirectoryDeletion(event);
        }
    }

    private void beforeDirectoryDeletion(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        Project project = getProject(event, file);
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile f) {
                if (!f.isDirectory() && isCangjieFile(f)) {
                    if (Objects.nonNull(project)) {
                        project.getMessageBus()
                                .syncPublisher(ProblemListener.TOPIC)
                                .problemsDisappeared(f);
                    }
                }
                return true;
            }
        });
    }

    /**
     * Fired when a virtual file is moved from within IDEA.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        final Project project = FileUtils.findGuessProjectsFor(event.getFile());
        ProjectView.getInstance(project).refresh();
    }

    /**
     * Fired when a virtual file is copied from within IDEA.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        if (!isCangjieFile(event)) {
            return;
        }

        fileCreated(event);
    }

    /**
     * Fired when a virtual file is created. This event is not fired for files
     * discovered during initial VFS initialization.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        if (!isCangjieFile(event)) {
            return;
        }
        LSPFileEventManager.fileCreated(event.getFile());
    }

    /**
     * Fired before the movement of a file is processed.
     *
     * @param event the event object containing information about the change.
     */
    @Override
    public void beforeFileMovement(@NotNull VirtualFileMoveEvent event) {
        if (!isCangjieFile(event)) {
            return;
        }
        LSPFileEventManager.beforeFileMoved(event);
    }

    /**
     * Called when a file is renamed. Notifies the server if this file was watched.
     *
     * @param oldFileName The old file name
     * @param newFileName The new file name
     * @param event       The file event
     */
    void fileRename(String oldFileName, String newFileName, VirtualFilePropertyEvent event) {
        Module module = FileUtils.getModule(event.getFile());
        if (module == null) {
            LOG.warn("file rename get module is null, just return");
            return;
        }
        final Project project = module.getProject();
        project.getMessageBus()
                .syncPublisher(com.intellij.problems.ProblemListener.TOPIC)
                .problemsDisappeared(event.getFile());
        ApplicationUtils.invokeAfterPsiEvents(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                ApplicationUtils.readAction(() -> {
                    processRename(oldFileName, newFileName, event, project);
                });
            });
        });
    }

    /**
     * before property change
     *
     * @param event file event
     */
    @Override
    public void beforePropertyChange(@NotNull VirtualFilePropertyEvent event) {
        super.beforePropertyChange(event);
        if (event.getOldValue() == null || !isCangjieFile(event)) {
            return;
        }
    }

    private void processRename(String oldFileName, String newFileName, VirtualFilePropertyEvent event,
                               Project project) {
        // Getting the right file is not trivial here since we only have the file name.
        // Since we have to iterate over all opened projects and filter based on the file name.
        boolean isDirectory = event.getFile().isDirectory();
        Set<VirtualFile> files = Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                .flatMap(proj -> Arrays.stream(FileUtils.searchFiles(newFileName, proj, isDirectory)))
                .map(PsiFileSystemItem::getVirtualFile)
                .collect(Collectors.toSet());

        for (VirtualFile file : files) {
            if (process(oldFileName, event, project, file)) {
                continue;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            ApplicationManager.getApplication().invokeLater(() -> {
                fileEditorManager.openFile(file, true);
            });
            String oldExtension = FileUtilRt.getExtension(oldFileName);
            if (oldExtension.equals(LanguageManager.CANGJIE_EXTENSION)) {
                continue;
            }
            String newPathUri = FileUtils.VFSToURI(file);
            if (StringUtils.isEmpty(newPathUri)) {
                continue;
            }
            processNoCjExtension(file, event, project);
        }
    }

    /**
     * need reopen file when rename non-cj file to cj file
     *
     * @param file VirtualFile
     * @param event VirtualFilePropertyEvent
     * @param project Project
     */
    private void processNoCjExtension(VirtualFile file, VirtualFilePropertyEvent event, Project project) {
        var projects = FileUtils.findProjectsFor(file);
        for (Project p : projects) {
            Set<LanguageServerWrapper> wrappers = IntellijLanguageClient.getAllServerWrappers(
                    FileUtils.projectToUri(p));
            for (LanguageServerWrapper wrapper : wrappers) {
                processDidOpen(wrapper, event, project);
            }
        }
    }

    private void processDidOpen(LanguageServerWrapper wrapper, VirtualFilePropertyEvent event, Project project) {
        String uri = FileUtils.VFSToURI(event.getFile());
        List<Editor> editors = FileUtils.getAllOpenedEditorsForUri(project, uri);
        Iterator var3 = editors.iterator();
        while (var3.hasNext()) {
            var next = var3.next();
            if (!(next instanceof Editor)) {
                continue;
            }
            Editor editor = (Editor) next;
            if (editor == null) {
                continue;
            }
            String[] extensions = wrapper.serverDefinition.ext.split(
                    LanguageServerDefinition.SPLIT_CHAR);
            if (!FileUtils.isEditorSupported(editor, extensions)) {
                continue;
            }
            wrapper.disconnect(editor, true);
        }
        wrapper.connect(FileUtils.VFSToURI(event.getFile()));
    }

    private boolean process(String oldFileName, VirtualFilePropertyEvent event, Project project, VirtualFile file) {
        if (!FileUtils.isFileSupported(file)) {
            return true;
        }
        VirtualFile virtualFile = event.getParent();
        if (virtualFile != null) {
            String dirPath = virtualFile.getCanonicalPath();
            String fileDirPath = file.getParent().getCanonicalPath();
            if (!Objects.equals(fileDirPath, dirPath)) {
                return true;
            }
        }
        String newPathUri = FileUtils.VFSToURI(file);
        String oldPathUri = newPathUri.replace(file.getName(), oldFileName);

        // Notifies the language server.
        FileUtils.findProjectsFor(file)
                .forEach(
                        proj -> LSPFileEventManager.changedConfiguration(oldPathUri,
                                FileUtils.projectToUri(proj), FileChangeType.Deleted));
        FileUtils.findProjectsFor(file)
                .forEach(
                        proj -> LSPFileEventManager.changedConfiguration(newPathUri,
                                FileUtils.projectToUri(proj), FileChangeType.Created));

        if (!file.isDirectory()) {
            // Detaches old file from the wrappers.
            FileUtils.findProjectsFor(file).forEach(projectUri -> {
                Set<LanguageServerWrapper> wrappers = IntellijLanguageClient.getAllServerWrappers(
                        FileUtils.projectToUri(projectUri));
                if (wrappers != null) {
                    wrappers.forEach(wrapper -> wrapper.refreshConnectInfo(oldPathUri, newPathUri));
                }
            });
            return false;
        }
        FileUtils.getAllOpenedEditors(project).stream().filter(Objects::nonNull).forEach(editor -> {
            final String newFileUri = editorToURIString(editor);
            if (!LSPFileEventManager.isFileUnderDirectory(newFileUri, newPathUri)) {
                return;
            }
            final String oldFileUri = oldPathUri + newFileUri.substring(newPathUri.length());
            // Detaches old file from the wrappers.
            FileUtils.findProjectsFor(file).forEach(proj -> {
                Set<LanguageServerWrapper> wrappers = IntellijLanguageClient.getAllServerWrappers(
                    FileUtils.projectToUri(proj));
                if (wrappers != null) {
                    wrappers.forEach(wrapper -> wrapper.refreshConnectInfo(oldFileUri, newFileUri));
                }
            });
        });
        return false;
    }

    private Project getProject(@NotNull VirtualFileEvent event, @NotNull VirtualFile file) {
        Object requestor = event.getRequestor();
        Project project;
        if (requestor instanceof PsiManagerImpl psiManagerImpl) {
            project = psiManagerImpl.getProject();
        } else {
            // could find project by path when file was deleted, use in ctrl+z action
            project = getProjectForFile(file);
        }
        return project;
    }

    private Project getProjectForFile(@NotNull VirtualFile file) {
        String fileCanonicalPath = file.getCanonicalPath();
        Project project = null;
        if (Strings.isEmpty(fileCanonicalPath)) {
            return project;
        }
        ProjectManager projectManager = ProjectManager.getInstanceIfCreated();
        if (projectManager == null) {
            return project;
        }
        for (Project openProject : projectManager.getOpenProjects()) {
            if (VfsUtil.isUnder(fileCanonicalPath, Collections.singleton(openProject.getBasePath()))) {
                project = openProject;
            }
        }
        return project;
    }

    private boolean isCangjieFile(VirtualFileEvent event) {
        return isCangjieFile(event.getFile());
    }

    private boolean isCangjieFile(VirtualFile file) {
        if (file.getExtension() == null) {
            return false;
        }
        return FILE_EXTENSIONS.contains(file.getExtension());
    }
}

