/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.highlighting;

import com.huawei.ideacj.filetypes.CangjieCodeFile;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 过滤: 文件有error报错时  文件夹飘红显示
 *
 * @since 2024-01-27
 */
public class CangjieProblemFileHighlightFilter implements Condition<VirtualFile> {
    /**
     * 支持文件夹飘红的文件类型
     */
    public static final List<FileType> FILE_ERROR_SUPPORTED_TYPE = List.of(CangjieCodeFile.INSTANCE);

    private final Project myProject;

    CangjieProblemFileHighlightFilter(@NotNull Project project) {
        this.myProject = project;
    }

    /**
     * 判断文件类型 文件位置
     *
     * @param file 当前有error报错的文件
     * @return true:文件所在文件夹目录需飘红处理
     */
    public boolean value(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
            return false;
        }
        FileType fileType = file.getFileType();
        if (!FILE_ERROR_SUPPORTED_TYPE.contains(fileType)) {
            return false;
        }
        ProjectFileIndex index = ProjectFileIndex.getInstance(this.myProject);
        return !index.isExcluded(file) && !index.isInLibrary(file);
    }
}