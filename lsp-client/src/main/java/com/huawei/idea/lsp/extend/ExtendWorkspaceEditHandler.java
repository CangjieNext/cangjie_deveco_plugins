/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.lsp.extend;

import static com.huawei.idea.lsp.extend.ExtendLanguageClient.implementMembersFormatCode;
import static com.huawei.idea.lsp.extend.ExtendLanguageClient.removeImportsFormatCode;

import com.huawei.idea.formatter.CangjieFormatCodeHandler;
import com.huawei.idea.formatter.LineRange;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.util.CommonRefactoringUtil;

import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.requests.WorkspaceEditHandler;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ExtendWorkspaceEditHandler extends the capability of base WorkspaceEditHandler
 * to support multi-file resource operations and customized text edits with undo support.
 *
 * @since 2026-05-28
 */
public class ExtendWorkspaceEditHandler extends WorkspaceEditHandler {
    private static final String ACTION_NAME = "LSP edits";
    private static final Logger LOG = Logger.getInstance(ExtendWorkspaceEditHandler.class);

    private static class CreateFileUndoInfo {
        final Project project;
        final VirtualFile createdFile;
        final VirtualFile parentDir;
        final String fileName;
        final File createdParentDir;
        final String initialContent;
        final Document[] activeDocument;

        CreateFileUndoInfo(Project project, VirtualFile createdFile, VirtualFile parentDir,
                           String fileName, File createdParentDir, String initialContent,
                           Document[] activeDocument) {
            this.project = project;
            this.createdFile = createdFile;
            this.parentDir = parentDir;
            this.fileName = fileName;
            this.createdParentDir = createdParentDir;
            this.initialContent = initialContent;
            this.activeDocument = activeDocument;
        }
    }
    /**
     * Applies a WorkspaceEdit that performs a remove-imports action and schedules code reformatting.
     *
     * @param edit The WorkspaceEdit object provided by the LSP server.
     * @param name The descriptive name for the refactoring action.
     */
    public static void removeImportsApplyEdit(WorkspaceEdit edit, String name) {
        applyEditWithFormat(edit, name, new ArrayList<>(), false, () -> removeImportsFormatCode(edit));
    }

    /**
     *
     * implement members applyEdit and format
     *
     * @param edit           edit
     * @param name           name
     */
    public static void implementMembersApplyEdit(WorkspaceEdit edit, String name) {
        applyEditWithFormat(edit, name, new ArrayList<>(), false, () -> implementMembersFormatCode(edit));
    }

    /**
     * Standard entry point to apply a basic WorkspaceEdit wrapper using default action tags.
     *
     * @param edit Target configuration maps payload.
     * @return Future pipeline tracking execution status.
     */
    public static CompletableFuture<Boolean> applyEdit(WorkspaceEdit edit) {
        return applyEdit(edit, ACTION_NAME, new ArrayList<>());
    }

    /**
     * Applies the workspace edits securely with specific trackable undo steps and post-cleanup triggers.
     *
     * @param edit    The workspace change tree collection.
     * @param name    Command title for refactoring identification.
     * @param toClose Explicit document descriptors targeted to refresh/close.
     * @return Future callback monitoring final layout mapping status.
     */
    public static CompletableFuture<Boolean> applyEdit(WorkspaceEdit edit, String name, List<VirtualFile> toClose) {
        return applyEditWithFormat(edit, name, toClose, true, null);
    }

    /**
     * Apply workspace edit with optional formatting
     *
     * @param edit              The workspace change tree collection.
     * @param name              Command title for refactoring identification.
     * @param toClose           Explicit document descriptors targeted to refresh/close.
     * @param doFormat          Whether to format code after applying edits.
     * @param formatCallback    Optional callback executed inside CommandProcessor command scope for undo grouping.
     * @return Future callback monitoring final layout mapping status.
     */
    public static CompletableFuture<Boolean> applyEditWithFormat(WorkspaceEdit edit, String name,
                                                                  List<VirtualFile> toClose, boolean doFormat,
                                                                  @Nullable Runnable formatCallback) {
        if (edit == null) {
            return CompletableFuture.completedFuture(false);
        }

        String newName = name != null ? name : ACTION_NAME;
        Project[] curProject = new Project[]{null};
        List<VirtualFile> openedEditors = new ArrayList<>();
        List<VirtualFile> affectedFiles = new ArrayList<>();
        List<Runnable> toApply = new ArrayList<>();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Document[] activeDocument = new Document[]{null};

        if (edit.getDocumentChanges() != null) {
            handleDocumentChangesManual(edit.getDocumentChanges(), curProject, toApply, openedEditors, newName,
                activeDocument, affectedFiles);
        }

        if (edit.getChanges() != null && !edit.getChanges().isEmpty()) {
            openClosedFiles(edit.getChanges(), curProject, openedEditors);
            WorkspaceEditHandler.handleChanges(edit.getChanges(), curProject, toApply, openedEditors, newName);
        }

        ApplicationUtils.invokeLater(() -> {
            if (toApply.isEmpty()) {
                future.complete(true);
                return;
            }
            Project project = resolveProject(curProject);
            if (activeDocument[0] == null && project != null) {
                Editor editor = getSelectedEditor(project);
                if (editor != null) {
                    activeDocument[0] = editor.getDocument();
                }
            }
            final Project finalProject = project;
            ApplicationUtils.writeAction(() -> {
                try {
                    CommandProcessor.getInstance().executeCommand(finalProject, () -> {
                        for (Runnable runnable : toApply) {
                            if (runnable != null) {
                                runnable.run();
                            }
                        }
                        if (finalProject != null) {
                            markAffectedFilesForUndo(finalProject, affectedFiles);
                        }
                        if (doFormat && finalProject != null) {
                            performFormatInCommand(edit, finalProject);
                        }
                        if (formatCallback != null) {
                            formatCallback.run();
                        }
                    }, newName, "LSPPlugin", UndoConfirmationPolicy.DEFAULT, false);

                    if (finalProject != null) {
                        FileEditorManager manager = FileEditorManager.getInstance(finalProject);
                        closeFilesSafely(manager, openedEditors);
                        closeFilesSafely(manager, toClose);
                    }
                    future.complete(true);
                } catch (ProcessCanceledException e) {
                    future.completeExceptionally(e);
                    LOG.warn("Command apply edit failed.");
                    throw e;
                }
            });
        });
        return future;
    }

    private static Project resolveProject(Project[] curProject) {
        Project project = curProject[0];
        if (project != null) {
            return project;
        }
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            Editor selectedEditor = getSelectedEditor(openProject);
            if (selectedEditor != null) {
                return openProject;
            }
        }
        return project;
    }

    /**
     * Performs code formatting within the CommandProcessor command scope.
     *
     * @param edit      The WorkspaceEdit containing the changes to format.
     * @param project   The current project.
     */
    private static void performFormatInCommand(WorkspaceEdit edit, Project project) {
        Map<Editor, Collection<LineRange>> formatMap = new HashMap<>();
        if (edit.getDocumentChanges() != null) {
            for (Either<TextDocumentEdit, ResourceOperation> either: edit.getDocumentChanges()) {
                if (either.isLeft()) {
                    TextDocumentEdit docEdit = either.getLeft();
                    String uri = docEdit.getTextDocument().getUri();
                    collectFileForFormat(uri, project, formatMap);
                } else if (either.isRight() && either.getRight() instanceof CreateFile) {
                    String uri = ((CreateFile) either.getRight()).getUri();
                    collectFileForFormat(uri, project, formatMap);
                } else {
                    LOG.warn("Enter the else block");
                }
            }
        }

        Map<String, List<TextEdit>> changes = edit.getChanges();
        if (changes != null && !changes.isEmpty()) {
            for (String uri: changes.keySet()) {
                collectFileForFormat(uri, project, formatMap);
            }
        }

        if (formatMap.isEmpty()) {
            return;
        }
        formatMap.forEach((editor, lines) -> {
            CangjieFormatCodeHandler.executeCodeFormat(project, editor, lines);
        });
    }

    private static void collectFileForFormat(String uri, Project project,
                                             Map<Editor, Collection<LineRange>> formatMap) {
        VirtualFile vFile = FileUtils.uriToVfs(uri);
        if (vFile == null || !vFile.isValid()) {
            return;
        }
        Editor editor = FileUtils.editorForFile(vFile);
        if (editor == null || editor.isDisposed()) {
            FileEditorManager fem = FileEditorManager.getInstance(project);
            if (fem != null) {
                editor = fem.openTextEditor(new OpenFileDescriptor(project, vFile), false);
            }
        }
        if (editor == null || editor.isDisposed()) {
            return;
        }
        if (formatMap.containsKey(editor)) {
            return;
        }
        Document document = editor.getDocument();
        int lineCount = document.getLineCount();
        if (lineCount <= 0) {
            return;
        }
        formatMap.put(editor, Collections.singleton(LineRange.create(1, lineCount)));
    }

    /**
     * Sorts text edits by start line and character position.
     *
     * @param edits The text edits to sort.
     */
    private static void sortEdits(List<TextEdit> edits) {
        edits.sort(Comparator
                .comparingInt((TextEdit edit) -> edit.getRange().getStart().getLine())
                .thenComparingInt((TextEdit edit) -> edit.getRange().getStart().getCharacter()));
    }

    private static void handleDocumentChangesManual(List<Either<TextDocumentEdit, ResourceOperation>> docChanges,
                                                    Project[] curProject, List<Runnable> toApply,
                                                    List<VirtualFile> openedEditors, String name,
                                                    Document[] activeDocument, List<VirtualFile> affectedFiles) {
        for (Either<TextDocumentEdit, ResourceOperation> either : docChanges) {
            if (either.isRight()) {
                ResourceOperation op = either.getRight();
                if (op instanceof CreateFile) {
                    String uri = ((CreateFile) op).getUri();
                    CreateFileUndoInfo undoInfo = performCreateFileVFS(curProject, uri, activeDocument, affectedFiles);
                    if (undoInfo != null) {
                        // Register undo action inside CommandProcessor scope, not now
                        toApply.add(() -> registerCreateFileUndoAction(undoInfo));
                    }
                }
            }
        }

        for (Either<TextDocumentEdit, ResourceOperation> either : docChanges) {
            if (either.isLeft()) {
                TextDocumentEdit docEdit = either.getLeft();
                collectTextEditRunnable(docEdit, curProject, toApply, openedEditors, name, affectedFiles);
            }
        }
    }

    private static void collectTextEditRunnable(TextDocumentEdit docEdit, Project[] curProject,
                                                List<Runnable> toApply, List<VirtualFile> openedEditors,
                                                String name, List<VirtualFile> affectedFiles) {
        String uri = docEdit.getTextDocument().getUri();
        VirtualFile file = FileUtils.virtualFileFromURI(uri);
        if (file == null) {
            return;
        }
        if (!affectedFiles.contains(file)) {
            affectedFiles.add(file);
        }

        // Try to find an already-open editorfor this file
        Editor[] editors = FileUtils.editorsForFile(file);
        EditorEventManager eventManager = null;
        if (editors.length > 0) {
            eventManager = EditorEventManagerBase.forEditor(editors[0]);
            if (eventManager != null && curProject[0] == null) {
                curProject[0] = eventManager.editor.getProject();
            }
        }

        if (eventManager != null) {
            Integer docVersion = docEdit.getTextDocument().getVersion();
            int version = docVersion == null ? Integer.MAX_VALUE : docVersion;
            Runnable editsRunnable = eventManager.getEditsRunnable(version, docEdit.getEdits(), name, false);
            if (editsRunnable != null) {
                toApply.add(editsRunnable);
            }
        } else {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                if (curProject[0] == null) {
                    curProject[0] = ProjectUtil.guessProjectForFile(file);
                }
                if (curProject[0] == null) {
                    LOG.warn("Cannot determine project for file: " + uri);
                    return;
                }
                FileEditorManager manager = FileEditorManager.getInstance(curProject[0]);
                // 检查该文件再应用 Edit 之前是否已经打开在编辑区
                boolean isAlreadyOpen = manager.isFileOpen(file);

                Editor editor = manager.openTextEditor(new OpenFileDescriptor(curProject[0], file), false);
                if (editor == null) {
                    LOG.warn("Failed to open editor for file: " + uri);
                    return;
                }
                EditorEventManager em = EditorEventManagerBase.forEditor(editor);
                if (em != null) {
                    Integer docVersion = docEdit.getTextDocument().getVersion();
                    int version = docVersion == null ? Integer.MAX_VALUE : docVersion;
                    Runnable editsRunnable = em.getEditsRunnable(version, docEdit.getEdits(), name, false);
                    if (editsRunnable != null) {
                        toApply.add(editsRunnable);
                    }
                }
                if (!isAlreadyOpen) {
                    openedEditors.add(file);
                }
            });
        }
    }

    @Nullable
    private static CreateFileUndoInfo performCreateFileVFS(Project[] curProject, String fileUri,
                                                           Document[] activeDocument,
                                                           List<VirtualFile> affectedFiles) {
        final CreateFileUndoInfo[] result = new CreateFileUndoInfo[]{null};
        ApplicationManager.getApplication().invokeAndWait(() -> {
            ApplicationUtils.writeAction(() -> {
                try {
                    URI uri = new URI(FileUtils.sanitizeURI(fileUri));
                    File ioFile = new File(uri);
                    File parentDirFile = ioFile.getParentFile();
                    if (parentDirFile == null) {
                        LOG.error("Cannot get parent directory for: " + fileUri);
                        return;
                    }
                    boolean createdDirs = false;
                    if (!parentDirFile.exists()) {
                        createdDirs = parentDirFile.mkdirs();
                        if (!createdDirs) {
                            LOG.error("Failed to create parent directory: " + parentDirFile.getCanonicalPath());
                            return;
                        }
                    }
                    VirtualFile parentDir = LocalFileSystem.getInstance()
                            .refreshAndFindFileByPath(parentDirFile.getCanonicalPath());
                    if (parentDir == null) {
                        LOG.error("Parent directory not found after creation: " + parentDirFile.getCanonicalPath());
                        return;
                    }
                    VirtualFile newFile = parentDir.createChildData(null, ioFile.getName());
                    if (curProject[0] == null) {
                        curProject[0] = ProjectUtil.guessProjectForFile(newFile);
                    }
                    newFile.refresh(false, false);
                    if (!affectedFiles.contains(newFile)) {
                        affectedFiles.add(newFile);
                    }

                    String initialContent = readFileContent(newFile);
                    result[0] = new CreateFileUndoInfo(curProject[0], newFile, parentDir, ioFile.getName(),
                        createdDirs ? parentDirFile : null, initialContent, activeDocument);
                } catch (URISyntaxException | IOException e) {
                    LOG.error("VFS file creation failed: " + fileUri);
                }
            });
        });
        return result[0];
    }

    private static void registerCreateFileUndoAction(CreateFileUndoInfo info) {
        if (info.project == null || info.createdFile == null) {
            return;
        }
        Document createdDoc = FileDocumentManager.getInstance().getDocument(info.createdFile);
        List<Document> documents = new ArrayList<>();
        if (createdDoc != null) {
            documents.add(createdDoc);
        }
        if (info.activeDocument[0] != null && !documents.contains(info.activeDocument[0])) {
            documents.add(info.activeDocument[0]);
        }
        BasicUndoableAction undoableAction;
        if (!documents.isEmpty()) {
            undoableAction = new BasicUndoableAction(documents.toArray(new Document[0])) {
                @Override
                public void undo() {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            VirtualFile fileToDelete = info.parentDir.findChild(info.fileName);
                            if (fileToDelete != null && fileToDelete.exists()) {
                                fileToDelete.delete(null);
                            }
                            if (info.createdParentDir != null && info.createdParentDir.exists()
                                && info.createdParentDir.listFiles() != null
                                && info.createdParentDir.listFiles().length == 0) {
                                VirtualFile dirToDelete = LocalFileSystem.getInstance()
                                    .findFileByPath(info.createdParentDir.getCanonicalPath());
                                if (dirToDelete != null && dirToDelete.exists()) {
                                    dirToDelete.delete(null);
                                }
                            }
                        } catch (IOException e) {
                            LOG.warn("Undo create file failed: " + info.fileName, e);
                        }
                    });
                }

                @Override
                public void redo() {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            if (info.createdParentDir != null && !info.createdParentDir.exists()) {
                                info.createdParentDir.mkdirs();
                                LocalFileSystem
                                    .getInstance()
                                    .refreshAndFindFileByPath(info.createdParentDir.getCanonicalPath());
                            }
                            VirtualFile parent = LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(info.parentDir.getPath());
                            if (parent != null && parent.findChild(info.fileName) == null) {
                                VirtualFile restored = parent.createChildData(null, info.fileName);
                                if (info.initialContent != null) {
                                    Document doc = FileDocumentManager.getInstance().getDocument(restored);
                                    if (doc != null) {
                                        doc.setText(info.initialContent);
                                        FileDocumentManager.getInstance().saveDocument(doc);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            LOG.warn("Redo create file failed: " + info.fileName, e);
                        }
                    });
                }
            };
        } else {
            undoableAction = new BasicUndoableAction() {
                @Override
                public void undo() {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            VirtualFile fileToDelete = info.parentDir.findChild(info.fileName);
                            if (fileToDelete != null && fileToDelete.exists()) {
                                fileToDelete.delete(null);
                            }
                            if (info.createdParentDir != null && info.createdParentDir.exists()
                                    && info.createdParentDir.listFiles() != null
                                    && info.createdParentDir.listFiles().length == 0) {
                                VirtualFile dirToDelete = LocalFileSystem.getInstance()
                                    .findFileByPath(info.createdParentDir.getCanonicalPath());
                                if (dirToDelete != null && dirToDelete.exists()) {
                                    dirToDelete.delete(null);
                                }
                            }
                        } catch (IOException e) {
                            LOG.warn("Undo create file failed: " + info.fileName, e);
                        }
                    });
                }

                @Override
                public void redo() {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            if (info.createdParentDir != null && !info.createdParentDir.exists()) {
                                info.createdParentDir.mkdirs();
                                LocalFileSystem
                                    .getInstance()
                                    .refreshAndFindFileByPath(info.createdParentDir.getCanonicalPath());
                            }
                            VirtualFile parent = LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(info.parentDir.getPath());
                            if (parent != null && parent.findChild(info.fileName) == null) {
                                VirtualFile restored = parent.createChildData(null, info.fileName);
                                if (info.initialContent != null) {
                                    Document doc = FileDocumentManager.getInstance().getDocument(restored);
                                    if (doc != null) {
                                        doc.setText(info.initialContent);
                                        FileDocumentManager.getInstance().saveDocument(doc);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            LOG.warn("Redo create file failed: " + info.fileName, e);
                        }
                    });
                }
            };
        }
        UndoManager.getInstance(info.project).undoableActionPerformed(undoableAction);
    }

    private static String readFileContent(VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), file.getCharset());
        } catch (IOException e) {
            return "";
        }
    }

    private static void markAffectedFilesForUndo(Project project, List<VirtualFile> affectedFiles) {
        if (project == null || affectedFiles == null || affectedFiles.isEmpty()) {
            return;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        for (VirtualFile virtualFile: affectedFiles) {
            if (virtualFile == null || !virtualFile.isValid()) {
                continue;
            }
            PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile == null) {
                continue;
            }
            Document document = psiDocumentManager.getDocument(psiFile);
            if (document != null) {
                UndoUtil.markPsiFileForUndo(psiFile);
            }
        }
    }

    private static void openClosedFiles(Map<String, List<TextEdit>> changes,
                                        Project[] curProject, List<VirtualFile> openedEditors) {
        for (String key: changes.keySet()) {
            URI uri = URI.create(key);
            String realUri;
            try {
                realUri = Path.of(uri).toRealPath().toUri().toString();
            } catch (IOException exception) {
                continue;
            }

            realUri = FileUtils.sanitizeURI(realUri);
            EditorEventManager manager = EditorEventManagerBase.forUri(realUri);
            if (manager != null) {
                if (manager.editor != null && curProject[0] == null) {
                    curProject[0] = manager.editor.getProject();
                }
                continue;
            }

            VirtualFile vFile = FileUtils.virtualFileFromURI(realUri);
            if (vFile == null || !vFile.isValid()) {
                continue;
            }
            if (curProject[0] == null) {
                curProject[0] = ProjectUtil.guessProjectForFile(vFile);
            }
            if (curProject[0] == null) {
                continue;
            }

            ApplicationManager.getApplication().invokeAndWait(() -> {
                FileEditorManager fem = FileEditorManager.getInstance(curProject[0]);
                Editor editor = fem.openTextEditor(new OpenFileDescriptor(curProject[0], vFile), false);
                if (editor != null) {
                    openedEditors.add(vFile);
                }
            });
        }
    }

    private static boolean checkClosed(Map<String, List<TextEdit>> changes) {
        Project curProj = null;
        Editor curEditor = null;
        boolean hasClosed = false;
        for (String key : changes.keySet()) {
            URI uri = URI.create(key);
            String realUri;
            try {
                realUri = Path.of(uri).toRealPath().toUri().toString();
            } catch (IOException exception) {
                return false;
            }

            realUri = FileUtils.sanitizeURI(realUri);
            EditorEventManager manager = EditorEventManagerBase.forUri(realUri);
            if (manager == null) {
                hasClosed = true;
                continue;
            }
            if (manager.editor != null && curEditor == null) {
                curProj = manager.editor.getProject();
                curEditor = getSelectedEditor(curProj);
            }
        }
        if (hasClosed) {
            if (curProj != null) {
                CommonRefactoringUtil.showErrorHint(curProj,
                        curEditor,
                        "Cannot perform this operation while the file is closed."
                                + " Open the file first and try again.", "Refactoring Failed",
                        null);
            }
            return false;
        }
        return true;
    }

    private static void closeFilesSafely(FileEditorManager manager, Collection<VirtualFile> files) {
        for (VirtualFile f : files) {
            if (f != null && f.isValid()) {
                manager.closeFile(f);
            }
        }
    }

    @Nullable
    private static Editor getSelectedEditor(Project project) {
        if (project == null) {
            return null;
        }
        FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        return (selectedEditor instanceof TextEditor textEditor) ? textEditor.getEditor() : null;
    }
}
