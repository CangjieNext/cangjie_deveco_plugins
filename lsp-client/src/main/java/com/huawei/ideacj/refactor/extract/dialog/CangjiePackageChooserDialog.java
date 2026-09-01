/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.refactor.extract.dialog;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Package chooser dialog for Cangjie, providing directory tree browsing and new folder creation.
 * Root node is fixed to the given rootDir (typically src/main/cangjie).
 * Returns the selected directory as a relative path.
 *
 * @since 2026-06-17
 */
public class CangjiePackageChooserDialog extends DialogWrapper {
    private final Project myProject;
    private final VirtualFile myRootDir;
    private final String myInitialPath;

    private JBTextField myPathField;
    private FileSystemTree myFileSystemTree;
    private final Disposable myListenerDisposable = Disposer.newDisposable();

    /**
     * Constructs the package chooser dialog.
     *
     * @param project current project
     * @param rootDir VirtualFile for the directory tree root
     * @param initialPath current relative path (package dots converted to /), may be null or empty
     */
    public CangjiePackageChooserDialog(@NotNull Project project,
                                       @NotNull VirtualFile rootDir,
                                       @Nullable String initialPath) {
        super(project, true);
        this.myProject = project;
        this.myRootDir = rootDir;
        this.myInitialPath = initialPath;
        setTitle("Select Target Package");
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: path text field + new folder button
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));

        JLabel pathLabel = new JLabel("Relative path:");
        myPathField = new JBTextField();
        myPathField.setEditable(false);
        myPathField.setPreferredSize(new Dimension(400, myPathField.getPreferredSize().height));

        JButton newFolderButton = new JButton("New Folder");
        newFolderButton.addActionListener(e -> createNewFolder());

        topPanel.add(pathLabel);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(myPathField);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(newFolderButton);

        // Bottom: directory tree
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setRoots(myRootDir);
        descriptor.setShowFileSystemRoots(false);
        descriptor.withTreeRootVisible(true);

        myFileSystemTree = new FileSystemTreeImpl(myProject, descriptor);
        JComponent treeComponent = myFileSystemTree.getTree();

        // Listen for selection changes to update the path field
        myFileSystemTree.addListener(new FileSystemTree.Listener() {
            @Override
            public void selectionChanged(@NotNull java.util.List<? extends VirtualFile> selection) {
                updatePathField(selection.isEmpty() ? null : selection.getFirst());
            }
        }, myListenerDisposable);

        JBScrollPane scrollPane = new JBScrollPane(treeComponent);
        scrollPane.setPreferredSize(new Dimension(500, 350));

        // Initialize selection state
        if (myInitialPath != null && !myInitialPath.isEmpty()) {
            VirtualFile initialFile = myRootDir.findFileByRelativePath(myInitialPath);
            if (initialFile != null) {
                myFileSystemTree.expand(initialFile, null);
                myFileSystemTree.select(initialFile, null);
            }
        } else {
            myFileSystemTree.expand(myRootDir, null);
            updatePathField(myRootDir);
        }

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void updatePathField(@Nullable VirtualFile selectedFile) {
        if (selectedFile == null) {
            myPathField.setText("");
            return;
        }
        String relativePath = VfsUtil.getRelativePath(selectedFile, myRootDir, '/');
        myPathField.setText(relativePath != null ? relativePath : "");
    }

    private void createNewFolder() {
        VirtualFile parentDir = myFileSystemTree.getSelectedFile();
        if (parentDir == null || !parentDir.isDirectory()) {
            parentDir = myRootDir;
        }
        final VirtualFile effectiveParent = parentDir;

        String folderName = com.intellij.openapi.ui.Messages.showInputDialog(
            myProject,
            "Enter new folder name:",
            "New Folder",
            null);

        if (folderName == null || folderName.trim().isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile newFolder = effectiveParent.createChildDirectory(this, folderName.trim());
                myFileSystemTree.updateTree();
                myFileSystemTree.select(newFolder, () -> updatePathField(newFolder));
            } catch (IOException ex) {
                com.intellij.openapi.ui.Messages.showErrorDialog(
                    myProject,
                    "Failed to create folder: " + ex.getMessage(),
                    "Error");
            }
        });
    }

    /**
     * Returns the selected relative path (relative to the rootDir) using / as separator.
     * Returns empty string if the root directory itself is selected.
     *
     * @return relative path e.g. "sub/dir", or "" for root
     */
    @NotNull
    public String getSelectedRelativePath() {
        VirtualFile selectedFile = myFileSystemTree.getSelectedFile();
        if (selectedFile == null) {
            return "";
        }
        String relativePath = VfsUtil.getRelativePath(selectedFile, myRootDir, '/');
        return relativePath != null ? relativePath : "";
    }

    @Override
    protected void doOKAction() {
        if (!myFileSystemTree.selectionExists()) {
            return;
        }
        super.doOKAction();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myListenerDisposable);
        myFileSystemTree.dispose();
        super.dispose();
    }
}