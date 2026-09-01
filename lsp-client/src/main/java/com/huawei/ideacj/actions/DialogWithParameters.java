/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.actions;

import java.util.Map;

/**
 * Obtain parameters in the dialog box.
 *
 * @since 2022-01-25
 */
public interface DialogWithParameters {
    /**
     * Obtain parameters in the dialog box.
     *
     * @return Map<String, Object>
     */
    Map<String, Object> getParameters();
}
