/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.extend.params;

/**
 * LSP Indexing progress update params
 *
 * @since 2026-05-06
 */
public class IndexingProgressUpdateParams {
    private int total;
    private int current;

    public IndexingProgressUpdateParams() {
    }

    public int getTotal() {
        return this.total;
    }

    public int getCurrent() {
        return this.current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
