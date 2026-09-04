package com.huawei.cjprofiler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSONObject;
import com.huawei.cjprofiler.model.dto.response.concurrency.ConcurrencyMeasureInfo;
import com.huawei.cjprofiler.model.vo.concurrency.MeasureDetailVo;
import com.huawei.cjprofiler.service.concurrency.CjConcurrencyMeasureService;
import com.huawei.cjprofiler.service.concurrency.CjConcurrencyTaskService;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.model.dto.request.concurrency.QueryConcurrencyMeasureRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.concurrency.QueryConcurrencyProcessList;
import com.huawei.deveco.insight.ohos.model.dto.response.concurrency.ConcurrencyProcessListInfo;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

class CjConcurrencyProcessorTest {
    @Test
    void queryMeasureValidatesDelegatesAndWrapsExactResult() {
        JSONObject params = new JSONObject();
        QueryConcurrencyMeasureRequest request = mock(QueryConcurrencyMeasureRequest.class);
        CjConcurrencyMeasureService service = mock(CjConcurrencyMeasureService.class);
        List<MeasureDetailVo> details = List.of(mock(MeasureDetailVo.class));
        when(service.queryConcurrencyMeasureList(request)).thenReturn(details);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjConcurrencyMeasureService> serviceStatic =
                 mockStatic(CjConcurrencyMeasureService.class)) {
            json.when(() -> JsonUtil.parseObject(params, QueryConcurrencyMeasureRequest.class)).thenReturn(request);
            serviceStatic.when(CjConcurrencyMeasureService::getInstance).thenReturn(service);

            Response<?> response = CjConcurrencyProcessor.queryConcurrencyMeasureList(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getProfilerError()).isNull();
            assertThat(((ConcurrencyMeasureInfo) response.getBody()).getMeasureInfoDetail()).isSameAs(details);
            validate.verify(() -> ValidateUtil.validate(request));
            verify(service).queryConcurrencyMeasureList(request);
        }
    }

    @Test
    void queryMeasurePreservesEmptyServiceResult() {
        JSONObject params = new JSONObject();
        QueryConcurrencyMeasureRequest request = mock(QueryConcurrencyMeasureRequest.class);
        CjConcurrencyMeasureService service = mock(CjConcurrencyMeasureService.class);
        when(service.queryConcurrencyMeasureList(request)).thenReturn(List.of());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjConcurrencyMeasureService> serviceStatic =
                 mockStatic(CjConcurrencyMeasureService.class)) {
            json.when(() -> JsonUtil.parseObject(params, QueryConcurrencyMeasureRequest.class)).thenReturn(request);
            serviceStatic.when(CjConcurrencyMeasureService::getInstance).thenReturn(service);

            Response<?> response = CjConcurrencyProcessor.queryConcurrencyMeasureList(params);

            assertThat(((ConcurrencyMeasureInfo) response.getBody()).getMeasureInfoDetail()).isEmpty();
            verify(service).queryConcurrencyMeasureList(request);
        }
    }

    @Test
    void getProcessListValidatesDelegatesAndWrapsResult() {
        JSONObject params = new JSONObject();
        QueryConcurrencyProcessList request = mock(QueryConcurrencyProcessList.class);
        CjConcurrencyTaskService service = mock(CjConcurrencyTaskService.class);
        when(service.getConcurrencyProcessList(request)).thenReturn(List.of());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjConcurrencyTaskService> serviceStatic = mockStatic(CjConcurrencyTaskService.class)) {
            json.when(() -> JsonUtil.parseObject(params, QueryConcurrencyProcessList.class)).thenReturn(request);
            serviceStatic.when(CjConcurrencyTaskService::getInstance).thenReturn(service);

            Response<?> response = CjConcurrencyProcessor.getConcurrencyProcessList(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(ConcurrencyProcessListInfo.class);
            validate.verify(() -> ValidateUtil.validate(request));
            verify(service).getConcurrencyProcessList(request);
        }
    }
}
