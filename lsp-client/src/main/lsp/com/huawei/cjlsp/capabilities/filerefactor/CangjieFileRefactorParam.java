/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.filerefactor;

import org.jetbrains.annotations.NotNull;

/**
 * CangjieFileRefactorParam
 *
 * @since 2025/9/17
 */
public class CangjieFileRefactorParam {
    @NotNull
    private File file = new File("");

    @NotNull
    private File targetPath = new File("");

    @NotNull
    private File selectedElement = new File("");

    public CangjieFileRefactorParam() {}

    public CangjieFileRefactorParam(@NotNull String uri, @NotNull String targetPath, @NotNull String selectedElement) {
        this.file = new File(uri);
        this.targetPath = new File(targetPath);
        this.selectedElement = new File(selectedElement);
    }

    /**
     * 用于查询文件移动时描述文件信息参数
     */
    public static class File {
        private String uri;

        public File(@NotNull String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @NotNull
    public File getFile() {
        return file;
    }

    public void setFile(@NotNull File file) {
        this.file = file;
    }

    @NotNull
    public File getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(@NotNull File targetPath) {
        this.targetPath = targetPath;
    }

    @NotNull
    public File getselectedElement() {
        return selectedElement;
    }

    public void selectedElement(@NotNull File selectedElement) {
        this.selectedElement = selectedElement;
    }
}
