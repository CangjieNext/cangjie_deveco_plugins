/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.exports;

/**
 * ExportsItem
 *
 * @since 2025/09/03
 */
public class ExportsItem {
    private String exportName;
    private String containerName;

    public String getExportName() {
        return this.exportName;
    }

    public String getContainerName() {
        return this.containerName;
    }
}