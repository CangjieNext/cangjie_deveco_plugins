/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.huawei.cjprofiler.service.common.CjCommonTraceService;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@code CjTraceProcessor} trace request handling.
 *
 * @since 2026-08-14
 */
class CjTraceProcessorTest {
    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjTraceProcessor();
    }

    @Test
    void deleteTraceSessionDelegatesAndReturnsServiceResponse() {
        JSONObject params = new JSONObject();
        CjCommonTraceService service = mock(CjCommonTraceService.class);
        Response<?> expected = Response.success();
        doReturn(expected).when(service).deleteTraceSession(params);
        try (MockedStatic<CjCommonTraceService> serviceStatic = mockStatic(CjCommonTraceService.class)) {
            serviceStatic.when(CjCommonTraceService::getInstance).thenReturn(service);

            Response<?> actual = CjTraceProcessor.deleteTraceSession(params);

            assertThat(actual).isSameAs(expected);
            verify(service).deleteTraceSession(params);
        }
    }

    @Test
    void deleteTraceSessionPreservesParameterErrorFromService() {
        JSONObject params = new JSONObject();
        CjCommonTraceService service = mock(CjCommonTraceService.class);
        Response<?> expected = Response.failure(ProfilerError.REQUEST_PARAMETER_ERROR);
        doReturn(expected).when(service).deleteTraceSession(params);
        try (MockedStatic<CjCommonTraceService> serviceStatic = mockStatic(CjCommonTraceService.class)) {
            serviceStatic.when(CjCommonTraceService::getInstance).thenReturn(service);

            Response<?> actual = CjTraceProcessor.deleteTraceSession(params);

            assertThat(actual.getIsSuccess()).isFalse();
            assertThat(actual.getProfilerError()).isEqualTo(ProfilerError.REQUEST_PARAMETER_ERROR);
            verify(service).deleteTraceSession(params);
        }
    }
}
