package com.huawei.cjprofiler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.alibaba.fastjson2.JSONObject;
import com.huawei.cjprofiler.service.common.CjCommonTraceService;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CjTraceProcessorTest {
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
