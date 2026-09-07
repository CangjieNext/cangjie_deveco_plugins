/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.resourcehandler.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.resourcehandler.CangjieMappingRegistyManager;
import com.huawei.cjprofiler.utils.ArgumentsUtil;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.resourcehandler.controller.HandlerMethod;

import com.alibaba.fastjson2.JSONObject;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code RequestDispatcher} request mapping and dispatch.
 *
 * @since 2026-08-14
 */
class RequestDispatcherTest {
    private static final JSONObject PARAMS = new JSONObject();

    @Test
    void getInstance_returnsSingleton() {
        assertThat(RequestDispatcher.getInstance()).isSameAs(RequestDispatcher.getInstance());
    }

    @Test
    void dispatch_whenMappingIsMissing_returnsNonCallbackResponse() {
        CangjieMappingRegistyManager manager = mock(CangjieMappingRegistyManager.class);
        when(manager.getHandlerMethod(List.of("memory", "missing"))).thenReturn(Optional.empty());

        try (MockedStatic<CangjieMappingRegistyManager> managerStatic =
                mockStatic(CangjieMappingRegistyManager.class)) {
            managerStatic.when(CangjieMappingRegistyManager::getInstance).thenReturn(manager);

            Response<?> response = RequestDispatcher.getInstance().dispatch("memory", "missing", PARAMS);

            assertThat(response.isNeedCallback()).isFalse();
        }
    }

    @Test
    void dispatch_whenHandlerReturnsResponse_returnsSameResponse() throws Exception {
        Response<?> expected = Response.success("payload");
        HandlerTargets.response = expected;

        Response<?> actual = dispatchTo("success", HandlerTargets.class.getMethod("success", JSONObject.class));

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void dispatch_whenHandlerReturnsOtherType_returnsNonCallbackResponse() throws Exception {
        Response<?> response = dispatchTo("plain", HandlerTargets.class.getMethod("plain", JSONObject.class));

        assertThat(response.isNeedCallback()).isFalse();
    }

    @Test
    void dispatch_whenArgumentTypeDoesNotMatch_returnsInternalServerError() throws Exception {
        Response<?> response = dispatchTo("wrongArgument",
            HandlerTargets.class.getMethod("wrongArgument", String.class));

        assertThat(response.getIsSuccess()).isFalse();
        assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
    }

    @Test
    void dispatch_whenHandlerThrowsPersistenceException_returnsPersistenc() throws Exception {
        Response<?> response = dispatchTo("persistence",
            HandlerTargets.class.getMethod("persistence", JSONObject.class));

        assertThat(response.getIsSuccess()).isFalse();
        assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PERSISTENCE_ERROR);
    }

    @Test
    void dispatch_whenHandlerThrowsRuntimeException_returnsInternalServer() throws Exception {
        Response<?> response = dispatchTo("runtime", HandlerTargets.class.getMethod("runtime", JSONObject.class));

        assertThat(response.getIsSuccess()).isFalse();
        assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
    }

    private static Response<?> dispatchTo(String requestMethod, Method method) {
        CangjieMappingRegistyManager manager = mock(CangjieMappingRegistyManager.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);
        when(handlerMethod.getMethod()).thenReturn(method);
        when(manager.getHandlerMethod(List.of("test", requestMethod))).thenReturn(Optional.of(handlerMethod));

        try (MockedStatic<CangjieMappingRegistyManager> managerStatic = mockStatic(CangjieMappingRegistyManager.class);
             MockedStatic<ArgumentsUtil> argumentsStatic = mockStatic(ArgumentsUtil.class)) {
            managerStatic.when(CangjieMappingRegistyManager::getInstance).thenReturn(manager);
            argumentsStatic.when(() -> ArgumentsUtil.parseJsonObject(any(Method.class), any(Object[].class)))
                .thenAnswer(invocation -> null);
            return RequestDispatcher.getInstance().dispatch("test", requestMethod, PARAMS);
        }
    }

    /**
     * Test handler targets used by the dispatcher to resolve method mapping.
     */
    public static final class HandlerTargets {
        private static Response<?> response;

        private HandlerTargets() {
        }

        public static Response<?> success(JSONObject params) {
            return response;
        }

        public static String plain(JSONObject params) {
            return "plain";
        }

        public static String wrongArgument(String params) {
            return params;
        }

        /**
         * Throws a persistence exception to simulate database failures.
         *
         * @param params request parameters
         * @return never returns
         */
        public static Response<?> persistence(JSONObject params) {
            throw new PersistenceException("database unavailable");
        }

        /**
         * Throws a runtime exception to simulate unexpected failures.
         *
         * @param params request parameters
         * @return never returns
         */
        public static Response<?> runtime(JSONObject params) {
            throw new IllegalStateException("unexpected");
        }
    }
}
