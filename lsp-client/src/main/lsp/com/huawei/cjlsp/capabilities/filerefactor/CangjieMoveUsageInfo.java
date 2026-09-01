/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.filerefactor;

import com.huawei.cjlsp.capabilities.filerefactor.CangjieMoveUpdateInfo.UpdateInfo;

import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;

import java.util.List;
import java.util.Map;

/**
 * CangjieMoveUsageInfo
 *
 * @since 2025/10/8
 */
public class CangjieMoveUsageInfo extends UsageInfo {
    private final Map<String, List<UpdateInfo>> changes;

    public CangjieMoveUsageInfo(PsiFile psiFile, CangjieMoveUpdateInfo updateInfo) {
        super(psiFile);
        this.changes = updateInfo.getChanges();
    }

    public Map<String, List<UpdateInfo>> getChanges() {
        return changes;
    }
}
