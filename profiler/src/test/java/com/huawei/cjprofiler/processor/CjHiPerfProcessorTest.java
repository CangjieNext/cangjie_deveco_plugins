/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.service.CjHiPerfService;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.model.dto.request.hiperf.HiPerfMetaDataRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.hiperf.HiPerfNodeStateRequest;
import com.huawei.deveco.insight.ohos.model.vo.HiPerfMetadata;
import com.huawei.deveco.insight.ohos.model.vo.hiperf.HiPerfNodeStateVo;
import com.huawei.deveco.insight.ohos.processor.HiPerfProcessor;
import com.huawei.deveco.insight.ohos.service.HiPerfService;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjHiPerfProcessor} high-performance processor queries.
 *
 * @since 2026-08-14
 */
class CjHiPerfProcessorTest {
    @Test
    void queryNodeStateWhenServiceMissingReturnsParseError() {
        JSONObject params = new JSONObject();
        HiPerfNodeStateRequest request = mock(HiPerfNodeStateRequest.class);
        when(request.getSessionId()).thenReturn("session");
        HiPerfProcessor processor = mock(HiPerfProcessor.class);
        when(processor.getHiPerfService("session")).thenReturn(Optional.empty());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<HiPerfProcessor> processorStatic = mockStatic(HiPerfProcessor.class)) {
            json.when(() -> JsonUtil.parseObject(params, HiPerfNodeStateRequest.class)).thenReturn(request);
            processorStatic.when(HiPerfProcessor::getInstance).thenReturn(processor);

            Response<?> response = CjHiPerfProcessor.queryCjHiPerfNodeState(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PARSE_HIPERF_ERROR);
            validate.verify(() -> ValidateUtil.validate(request));
        }
    }

    @Test
    void queryNodeStateFiltersServiceResultThroughCangjieService() {
        JSONObject params = new JSONObject();
        HiPerfNodeStateRequest request = mock(HiPerfNodeStateRequest.class);
        when(request.getSessionId()).thenReturn("session");
        HiPerfProcessor processor = mock(HiPerfProcessor.class);
        HiPerfService service = mock(HiPerfService.class);
        List<HiPerfNodeStateVo> raw = List.of(mock(HiPerfNodeStateVo.class));
        List<HiPerfNodeStateVo> filtered = List.of(mock(HiPerfNodeStateVo.class));
        when(processor.getHiPerfService("session")).thenReturn(Optional.of(service));
        when(service.queryHiPerfNodeState(request)).thenReturn(raw);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<HiPerfProcessor> processorStatic = mockStatic(HiPerfProcessor.class);
             MockedStatic<CjHiPerfService> cjService = mockStatic(CjHiPerfService.class)) {
            json.when(() -> JsonUtil.parseObject(params, HiPerfNodeStateRequest.class)).thenReturn(request);
            processorStatic.when(HiPerfProcessor::getInstance).thenReturn(processor);
            cjService.when(() -> CjHiPerfService.selectCangJieStateVo(raw)).thenReturn(filtered);

            Response<?> response = CjHiPerfProcessor.queryCjHiPerfNodeState(params);

            assertThat(response.getBody()).isSameAs(filtered);
            verify(service).queryHiPerfNodeState(request);
            cjService.verify(() -> CjHiPerfService.selectCangJieStateVo(raw));
        }
    }

    @Test
    void queryMetadataWhenServiceMissingReturnsParseError() {
        JSONObject params = new JSONObject();
        HiPerfMetaDataRequest request = mock(HiPerfMetaDataRequest.class);
        when(request.getSessionId()).thenReturn("session");
        HiPerfProcessor processor = mock(HiPerfProcessor.class);
        when(processor.getHiPerfService("session")).thenReturn(Optional.empty());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<HiPerfProcessor> processorStatic = mockStatic(HiPerfProcessor.class)) {
            json.when(() -> JsonUtil.parseObject(params, HiPerfMetaDataRequest.class)).thenReturn(request);
            processorStatic.when(HiPerfProcessor::getInstance).thenReturn(processor);

            Response<?> response = CjHiPerfProcessor.queryCjHiPerfMetadata(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PARSE_HIPERF_ERROR);
        }
    }

    @Test
    void queryMetadataWithNoCangjieTidReturnsSuccessfulNull() {
        JSONObject params = new JSONObject();
        HiPerfMetaDataRequest request = mock(HiPerfMetaDataRequest.class);
        when(request.getSessionId()).thenReturn("session");
        HiPerfProcessor processor = mock(HiPerfProcessor.class);
        HiPerfService service = mock(HiPerfService.class);
        HiPerfMetadata metadata = mock(HiPerfMetadata.class);
        when(processor.getHiPerfService("session")).thenReturn(Optional.of(service));
        when(service.getFileMetadata()).thenReturn(metadata);
        when(metadata.getCangJieTid()).thenReturn(List.of());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<HiPerfProcessor> processorStatic = mockStatic(HiPerfProcessor.class)) {
            json.when(() -> JsonUtil.parseObject(params, HiPerfMetaDataRequest.class)).thenReturn(request);
            processorStatic.when(HiPerfProcessor::getInstance).thenReturn(processor);

            Response<?> response = CjHiPerfProcessor.queryCjHiPerfMetadata(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isNull();
            verify(service).getFileMetadata();
        }
    }

    @Test
    void queryThreadCallStackWithMissingSessionReturnsParameterErrorWitho() {
        JSONObject params = new JSONObject();
        params.put("tidList", "[1]");
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<HiPerfProcessor> processorStatic = mockStatic(HiPerfProcessor.class)) {
            json.when(() -> JsonUtil.parseArray("[1]", Integer.class)).thenReturn(List.of(1));

            Response<?> response = CjHiPerfProcessor.queryCjHiPerfThreadCallStack(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.REQUEST_PARAMETER_ERROR);
            processorStatic.verifyNoInteractions();
        }
    }
}
