/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.trace;

import com.huawei.deveco.common.trace.HarmonyCustomTopic;
import com.huawei.deveco.common.trace.TraceDataBean;
import com.huawei.deveco.common.trace.TraceUtil;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;

/**
 * TraceAdapterImpl
 *
 * @since 2024-12
 */
public class TraceAdapterImpl implements TraceAdapter {
    private static final Logger LOG = Logger.getInstance(TraceAdapterImpl.class);
    private static final String SID = "10014";

    /**
     * record the info we are interested in.
     *
     * @param action action
     * @param detail detail
     */
    @Override
    public void trace(String action, String detail) {
        ApplicationInfo info = ApplicationInfo.getInstance();
        if (info == null) {
            LOG.error("Failed to get an instance of ApplicationInfo.");
            return;
        }
        TraceDataBean data = new TraceDataBean(SID, info.getFullVersion(), action, detail);
        TraceUtil.trace(data, HarmonyCustomTopic.topic);
    }
}
