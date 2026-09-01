/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjquality.utils.trace;

import com.huawei.deveco.common.trace.HarmonyCustomTopic;
import com.huawei.deveco.common.trace.TraceDataBean;
import com.huawei.deveco.common.trace.TraceUtil;

import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;

/**
 * This class is used to trace.
 *
 * @since 2024-12
 */
public class TraceUtils {
    private static final String SID = "10014";
    private static final Logger LOG = Logger.getInstance(TraceUtils.class);

    /**
     * action
     */
    public static class Action {
        /**
         * format action
         */
        public static final String FORMAT = "cjLspFormat";

        /**
         * code linter action
         */
        public static final String CODE_LINTER = "cjLspStaticChecker";
    }

    /**
     * cause
     */
    public static class Cause {
        /**
         * default cause
         */
        public static final String DEFAULT = "normal operation";

        /**
         * timeoutException cause
         */
        public static final String TIMEOUT_EXCEPTION = "timeoutException";

        /**
         * crash cause
         */
        public static final String CRASH = "crash";
    }

    /**
     * make trace
     *
     * @param action  action
     */
    public static void trace(String action) {
        JsonObject properties = new JsonObject();
        trace(action, Cause.DEFAULT, -1, -1, properties.toString());
    }

    /**
     * make trace
     *
     * @param action      action
     * @param cause       triggering reason
     */
    public static void trace(String action, String cause) {
        JsonObject properties = new JsonObject();
        trace(action, cause, -1, -1, properties.toString());
    }

    /**
     * make trace
     *
     * @param action      action
     * @param cause       triggering reason
     * @param properties  other info
     */
    public static void trace(String action, String cause, String properties) {
        trace(action, cause, -1, -1, properties);
    }

    /**
     * make trace that use default properties
     *
     * @param action      action
     * @param cause       triggering reason
     * @param count       trigger times, no need to record use -1
     * @param duration    elapsed time, no need to record use -1
     */
    public static void trace(String action, String cause, int count, long duration) {
        JsonObject properties = new JsonObject();
        trace(action, cause, count, duration, properties.toString());
    }

    /**
     * make trace
     *
     * @param action      action
     * @param cause       triggering reason
     * @param count       trigger times, no need to record use -1
     * @param duration    elapsed time, no need to record use -1
     * @param properties  other info
     */
    public static void trace(String action, String cause, int count, long duration, String properties) {
        JsonObject details = new JsonObject();
        details.addProperty("cause", cause);
        details.addProperty("count", count);
        details.addProperty("duration", duration);
        details.addProperty("properties", properties);
        TraceDataBean data = new TraceDataBean(SID, "", action, details.toString());
        LOG.debug("Cangjie lsp trace: {", "action: ", data.getAction(),
                ", detail: ", data.getDetail() + "}");
        TraceUtil.trace(data, HarmonyCustomTopic.topic);
    }
}
