/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor.extract.dialog;

import com.huawei.capabilities.CangjiePackageUtils;
import com.huawei.idea.lsp.utils.CangjieBundle;
import com.huawei.idea.refactor.extract.CangjieMemberInfo;
import com.huawei.idea.refactor.extract.CangjieMemberSelectionPanel;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.extractSuperclass.ExtractSuperBaseDialog;
import com.intellij.ui.EditorComboBox;
import com.intellij.ui.RecentsManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 仓颉提取接口对话框，用于交互式选择成员并设置提取后的接口名称与路径。
 *
 * @since 2026-05-28
 */
public class CangjieExtractInterfaceDialog extends ExtractSuperBaseDialog<PsiElement, CangjieMemberInfo> {
    private static final String INTERFACE_NAME_LABEL = "Interface name:";
    private static final String IMPLEMENTATION_CLASS_NAME_LABEL = "Rename implementation class to:";

    private JTextField myInterfaceNameField;
    private JTextField mySourceClassField;
    private final String mySourceClassName;
    private final Project myProject;

    private final Editor myEditor;
    private String myInterfaceName = "";
    private String myImplementationClassName = "";
    private boolean myRenameOriginalMode;

    /**
     * 仓颉提取接口对话框构造器。
     *
     * @param project         当前项目实例
     * @param editor          当前激活的编辑器对象
     * @param sourceClass     源 PSI 类元素
     * @param members         解析出的可见成员列表
     * @param sourceClassName 源结构体或类的原始名称
     */
    public CangjieExtractInterfaceDialog(Project project, Editor editor, PsiElement sourceClass,
                                         List<CangjieMemberInfo> members, String sourceClassName) {
        super(project, sourceClass, members, CangjieBundle.message("lsp.refactor.extract.interface.dialog.title"));
        this.myProject = project;
        this.mySourceClassName = (sourceClassName == null || sourceClassName.isEmpty()) ? "Unknown" : sourceClassName;
        this.myEditor = editor;
        init();
    }

    @Override
    @NotNull
    protected JTextField createSourceClassField() {
        mySourceClassField = new JTextField();
        mySourceClassField.setEditable(false);
        updateSourceClassText("<Default>");
        return mySourceClassField;
    }

    private void updateSourceClassText(String packageName) {
        if (mySourceClassField == null) {
            return;
        }
        String prefix = ("<Default>".equals(packageName) || packageName.isEmpty()) ? "" : packageName + ".";
        mySourceClassField.setText(prefix + mySourceClassName);
    }

    @Override
    @NotNull
    protected JTextField createExtractedSuperNameField() {
        myInterfaceNameField = new JTextField(mySourceClassName);
        myInterfaceNameField.selectAll();
        myInterfaceName = mySourceClassName;
        myImplementationClassName = mySourceClassName;
        return myInterfaceNameField;
    }

    @Override
    protected ComponentWithBrowseButton<EditorComboBox> createPackageNameField() {
        EditorComboBox comboBox = new EditorComboBox("");

        List<String> recentEntries = RecentsManager.getInstance(myProject)
            .getRecentEntries(getDestinationPackageRecentKey());
        if (recentEntries != null && !recentEntries.isEmpty()) {
            comboBox.setHistory(recentEntries.toArray(new String[0]));
        }

        return new ComponentWithBrowseButton<>(comboBox, e -> {
            VirtualFile currentFile = FileDocumentManager.getInstance().getFile(myEditor.getDocument());
            Module module = (currentFile != null) ? ModuleUtil.findModuleForFile(currentFile, myProject) : null;
            VirtualFile moduleRoot = (module != null) ? ProjectUtil.guessModuleDir(module) : myProject.getBaseDir();

            VirtualFile rootDir = (moduleRoot != null) ? moduleRoot.findFileByRelativePath("src/main/cangjie") : null;
            if (rootDir == null) {
                return;
            }

            String currentPackage = comboBox.getText().trim();
            String initialPath = currentPackage.isEmpty() ? null : currentPackage.replace('.', '/');

            CangjiePackageChooserDialog chooserDialog =
                new CangjiePackageChooserDialog(myProject, rootDir, initialPath);
            if (chooserDialog.showAndGet()) {
                String relativePath = chooserDialog.getSelectedRelativePath();
                String packageName = relativePath.replace('/', '.');
                comboBox.setText(packageName);
                updateSourceClassText(packageName);
            }
        });
    }

    @Override
    protected String getTopLabelText() {
        return CangjieBundle.message("lsp.refactor.extract.interface.dialog.text");
    }

    @Override
    protected String getClassNameLabelText() {
        return INTERFACE_NAME_LABEL;
    }

    @Override
    protected String getPackageNameLabelText() {
        return "Package for new interface:";
    }

    @Override
    protected String getEntityName() {
        return "interface";
    }

    @Override
    protected String getExtractedSuperNameNotSpecifiedMessage() {
        return CangjieBundle.message("lsp.refactor.extract.interface.dialog.empty");
    }

    @Nullable
    @Override
    protected String validateName(String interfaceName) {
        String trimmedName = interfaceName == null ? "" : interfaceName.trim();
        if (trimmedName.isEmpty()) {
            return CangjieBundle.message("lsp.refactor.extract.interface.dialog.empty");
        }

        if (!trimmedName.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            return "Invalid interface name. Interface name must be a valid Java identifier.";
        }

        if (isRenameOriginalClassAndUseInterfaceWherePossible()) {
            if (trimmedName.equals(mySourceClassName)) {
                return "Implementation class name must be different from the extracted interface name.";
            }
        }
        if (trimmedName.equals(mySourceClassName)) {
            return CangjieBundle.message("lsp.refactor.extract.interface.name.conflict");
        }

        return null;
    }

    @Override
    public String getTargetPackageName() {
        if (myPackageNameField.getChildComponent() instanceof EditorComboBox comboBox) {
            return comboBox.getText();
        }
        return "";
    }

    @Override
    protected String validateQualifiedName(String packageName, @NotNull String extractedSuperName) {
        String trimmedPackage = packageName == null ? "" : packageName.trim();
        if (trimmedPackage.isEmpty() || "<Default>".equals(trimmedPackage)) {
            return null;
        }
        if (!CangjiePackageUtils.isValidCangjieName(trimmedPackage)) {
            return "Invalid package name: " + trimmedPackage;
        }
        return null;
    }

    @Override
    protected void preparePackage() {
    }

    @Override
    protected String getDestinationPackageRecentKey() {
        return "CangjieExtractInterface";
    }

    @Override
    protected BaseRefactoringProcessor createProcessor() {
        return null;
    }

    @Override
    protected void executeRefactoring() {
    }

    @Override
    protected int getDocCommentPolicySetting() {
        return 0;
    }

    @Override
    protected void setDocCommentPolicySetting(int policy) {
    }

    @Override
    @NotNull
    protected String getDocCommentPanelName() {
        return "";
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        final CangjieMemberSelectionPanel memberSelectionPanel =
                new CangjieMemberSelectionPanel(CangjieBundle.message("title.MembersToFormInterface"), myMemberInfos);
        panel.add(memberSelectionPanel, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void updateDialog() {
        cacheCurrentName();
        super.updateDialog();
        getPreviewAction().setEnabled(false);
        syncNameFieldForCurrentMode();
        updateClassNameLabel();
    }

    /**
     * 判断当前重构模式是否为重命名原实现类并尽可能替换为接口。
     *
     * @return 满足条件返回 true
     */
    public boolean isRenameOriginalClassAndUseInterfaceWherePossible() {
        return !isExtractSuperclass();
    }

    /**
     * 获取对应的接口名称。
     *
     * @return 接口名称字符串
     */
    public String getInterfaceName() {
        return isRenameOriginalClassAndUseInterfaceWherePossible() ? mySourceClassName : getExtractedSuperName();
    }

    /**
     * 获取目标类的实现类重命名名称。
     *
     * @return 重命名的实现类名
     */
    public String getImplementationClassName() {
        return isRenameOriginalClassAndUseInterfaceWherePossible() ? getExtractedSuperName() : "";
    }

    private void cacheCurrentName() {
        if (myInterfaceNameField == null) {
            return;
        }
        String currentName = myInterfaceNameField.getText().trim();
        myInterfaceName = currentName;
        myImplementationClassName = currentName;
    }

    private void syncNameFieldForCurrentMode() {
        if (myInterfaceNameField == null) {
            return;
        }

        boolean renameOriginalMode = isRenameOriginalClassAndUseInterfaceWherePossible();
        String nextName;
        if (renameOriginalMode) {
            nextName = myImplementationClassName == null || myImplementationClassName.isEmpty()
                    ? mySourceClassName + "Impl"
                    : myImplementationClassName;
        } else {
            nextName = myInterfaceName == null || myInterfaceName.isEmpty()
                    ? mySourceClassName
                    : myInterfaceName;
        }

        if (!nextName.equals(myInterfaceNameField.getText())) {
            myInterfaceNameField.setText(nextName);
            myInterfaceNameField.selectAll();
        }
        myRenameOriginalMode = renameOriginalMode;
    }

    private void updateClassNameLabel() {
        JLabel classNameLabel = getClassNameLabel();
        if (classNameLabel != null) {
            classNameLabel.setText(isRenameOriginalClassAndUseInterfaceWherePossible()
                    ? IMPLEMENTATION_CLASS_NAME_LABEL
                    : INTERFACE_NAME_LABEL);
        }
    }

    private JLabel getClassNameLabel() {
        try {
            Field field = ExtractSuperBaseDialog.class.getDeclaredField("myClassNameLabel");
            field.setAccessible(true);
            Object labelObject = field.get(this);
            if (labelObject instanceof JLabel jLabel) {
                return jLabel;
            }
            return new JLabel();
        } catch (ReflectiveOperationException ignored) {
            return new JLabel();
        }
    }

    @Override
    @Nullable
    public PsiDirectory getTargetDirectory() {
        String packageName = getTargetPackageName();
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(myEditor.getDocument());
        Module module = (currentFile != null) ? ModuleUtil.findModuleForFile(currentFile, myProject) : null;
        VirtualFile moduleRoot = (module != null) ? ProjectUtil.guessModuleDir(module) : myProject.getBaseDir();

        VirtualFile rootDir = (moduleRoot != null) ? moduleRoot.findFileByRelativePath("src/main/cangjie") : null;
        if (rootDir == null) {
            return null;
        }

        VirtualFile targetVF;
        if (packageName == null || packageName.isEmpty() || "<Default>".equals(packageName)) {
            targetVF = rootDir;
        } else {
            String relativePath = packageName.replace('.', '/');
            targetVF = rootDir.findFileByRelativePath(relativePath);
        }

        return (targetVF != null) ? PsiManager.getInstance(myProject).findDirectory(targetVF) : null;
    }
}