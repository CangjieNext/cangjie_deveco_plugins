/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.trace;

import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;

/**
 * This class is used to trace.
 *
 * @since 2024-12
 */
public class TraceUtils {
    private static TraceAdapter traceAdapter;
    private static final Logger LOG = Logger.getInstance(TraceUtils.class);

    /**
     * action
     */
    public static class Action {
        /**
         * call hierarchy action
         */
        public static final String CALL_HIERARCHY = "cjLspCallDep";

        /**
         * type hierarchy action
         */
        public static final String TYPE_HIERARCHY = "cjLspTypeDep";

        /**
         * goto declaration or usages action
         */
        public static final String GOTO_DECLARE_OR_USAGES = "cjLspJumper";

        /**
         * completion action
         */
        public static final String COMPLETION = "cjLspCompletion";

        /**
         * syntax highlight action
         */
        public static final String SYNTAX_HIGHLIGHT = "cjLspSyntaxHL";

        /**
         * semantics highlight action
         */
        public static final String SEMANTICS_HIGHLIGHT = "cjLspSemanticsHL";

        /**
         * lsp start action
         */
        public static final String LSP_START = "cjLspStart";

        /**
         * lsp crash action
         */
        public static final String LSP_CRASH = "cjLspCrash";
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
     * 触发打点的间隔次数
     */
    public static class ActionThreshold {
        /**
         * SYNTAX_HIGHLIGHT
         */
        public static final int SYNTAX_HIGHLIGHT = 15;

        /**
         * SEMANTICS_HIGHLIGHT
         */
        public static final int SEMANTICS_HIGHLIGHT = 15;
    }


    /**
     * register trace adapter
     *
     * @param adapter trace adapter
     */
    public static void registerTraceAdapter(TraceAdapter adapter) {
        traceAdapter = adapter;
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
        LOG.debug("Cangjie lsp trace: {", "action: ", action, ", detail: ", details + "}");
        if (traceAdapter != null) {
            traceAdapter.trace(action, details.toString());
        }
    }
}
