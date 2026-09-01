/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language;

import com.huawei.ideacj.filetypes.CangjieCodeFile;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;

import org.jetbrains.annotations.NotNull;

/**
 * definition Cangjie file type
 *
 * @author rice
 * @since 2020-06-08
 */
public class CangjieFile extends PsiFileBase {
    public CangjieFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CangJieLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return CangjieCodeFile.INSTANCE;
    }

    @Override
    public String toString() {
        return "Cangjie file";
    }
}