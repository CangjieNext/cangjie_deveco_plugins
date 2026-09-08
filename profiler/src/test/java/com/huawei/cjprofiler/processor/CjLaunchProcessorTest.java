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

import com.huawei.cjprofiler.service.launch.CjLaunchTraceService;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.model.dto.request.launch.LaunchLifeCycleRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.launch.LaunchLifeCycleLane;
import com.huawei.deveco.insight.ohos.model.dto.response.launch.LaunchThreadMetaData;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;
import com.huawei.deveco.insight.ohos.utils.singleton.SingletonContainer;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@code CjLaunchProcessor} launch request handling.
 *
 * @since 2026-08-14
 */
class CjLaunchProcessorTest {
    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjLaunchProcessor();
    }

    @Test
    void queryLifeCycleLaneValidatesDelegatesAndWrapsResult() {
        JSONObject params = new JSONObject();
        LaunchLifeCycleRequest request = mock(LaunchLifeCycleRequest.class);
        CjLaunchTraceService service = mock(CjLaunchTraceService.class);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<SingletonContainer> singleton = mockStatic(SingletonContainer.class)) {
            json.when(() -> JsonUtil.parseObject(params, LaunchLifeCycleRequest.class)).thenReturn(request);
            singleton.when(() -> SingletonContainer.getInstance(CjLaunchTraceService.class)).thenReturn(service);
            when(service.queryLaunchLifeCycleLane(request)).thenReturn(java.util.Collections.emptyList());

            Response<?> response = CjLaunchProcessor.queryLaunchLifeCycleLane(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(LaunchLifeCycleLane.class);
            validate.verify(() -> ValidateUtil.validate(request));
            verify(service).queryLaunchLifeCycleLane(request);
        }
    }

    @Test
    void queryThreadMetadataValidatesDelegatesAndWrapsResult() {
        JSONObject params = new JSONObject();
        LaunchLifeCycleRequest request = mock(LaunchLifeCycleRequest.class);
        CjLaunchTraceService service = mock(CjLaunchTraceService.class);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<SingletonContainer> singleton = mockStatic(SingletonContainer.class)) {
            json.when(() -> JsonUtil.parseObject(params, LaunchLifeCycleRequest.class)).thenReturn(request);
            singleton.when(() -> SingletonContainer.getInstance(CjLaunchTraceService.class)).thenReturn(service);

            Response<?> response = CjLaunchProcessor.queryLaunchThreadMetadata(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(LaunchThreadMetaData.class);
            validate.verify(() -> ValidateUtil.validate(request));
            verify(service).queryLaunchThreadMetadata(request);
        }
    }
}
