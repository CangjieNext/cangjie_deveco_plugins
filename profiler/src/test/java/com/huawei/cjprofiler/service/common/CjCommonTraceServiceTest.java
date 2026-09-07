/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.service.launch.CjLaunchTraceService;
import com.huawei.deveco.insight.ohos.ability.database.TraceDatabase;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.utils.singleton.SingletonContainer;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@code CjCommonTraceService} trace session deletion logic.
 *
 * @since 2026-08-14
 */
class CjCommonTraceServiceTest {
    @Test
    void getInstance_returnsSingleton() {
        assertThat(CjCommonTraceService.getInstance()).isSameAs(CjCommonTraceService.getInstance());
    }

    @Test
    void deleteTraceSession_sessionIdMissing_returnsParameterError() {
        TraceDatabase database = mock(TraceDatabase.class);
        try (MockedStatic<TraceDatabase> databaseStatic = mockStatic(TraceDatabase.class)) {
            databaseStatic.when(TraceDatabase::getInstance).thenReturn(database);

            Response<?> response = CjCommonTraceService.getInstance().deleteTraceSession(new JSONObject());

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.REQUEST_PARAMETER_ERROR);
            verify(database, never()).releaseDatabase(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void deleteTraceSession_sessionIdBlank_returnsParameterError() {
        JSONObject params = new JSONObject();
        params.put("sessionId", "   ");
        TraceDatabase database = mock(TraceDatabase.class);
        try (MockedStatic<TraceDatabase> databaseStatic = mockStatic(TraceDatabase.class)) {
            databaseStatic.when(TraceDatabase::getInstance).thenReturn(database);

            Response<?> response = CjCommonTraceService.getInstance().deleteTraceSession(params);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.REQUEST_PARAMETER_ERROR);
            verify(database, never()).releaseDatabase(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void deleteTraceSession_releaseFails_returnsInternalError() {
        JSONObject params = new JSONObject();
        params.put("sessionId", "session-failed");
        TraceDatabase database = mock(TraceDatabase.class);
        CjLaunchTraceService launchTraceService = mock(CjLaunchTraceService.class);
        when(database.releaseDatabase("session-failed")).thenReturn(false);
        try (MockedStatic<TraceDatabase> databaseStatic = mockStatic(TraceDatabase.class);
             MockedStatic<SingletonContainer> singletonStatic = mockStatic(SingletonContainer.class)) {
            databaseStatic.when(TraceDatabase::getInstance).thenReturn(database);
            singletonStatic.when(() -> SingletonContainer.getInstance(CjLaunchTraceService.class))
                .thenReturn(launchTraceService);

            Response<?> response = CjCommonTraceService.getInstance().deleteTraceSession(params);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
            verify(launchTraceService, never()).cleanSessionCache(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void deleteTraceSession_releaseSucceeds_cleansCacheAndReturnsSuccess() {
        JSONObject params = new JSONObject();
        params.put("sessionId", "session-ok");
        TraceDatabase database = mock(TraceDatabase.class);
        CjLaunchTraceService launchTraceService = mock(CjLaunchTraceService.class);
        when(database.releaseDatabase("session-ok")).thenReturn(true);
        try (MockedStatic<TraceDatabase> databaseStatic = mockStatic(TraceDatabase.class);
             MockedStatic<SingletonContainer> singletonStatic = mockStatic(SingletonContainer.class)) {
            databaseStatic.when(TraceDatabase::getInstance).thenReturn(database);
            singletonStatic.when(() -> SingletonContainer.getInstance(CjLaunchTraceService.class))
                .thenReturn(launchTraceService);

            Response<?> response = CjCommonTraceService.getInstance().deleteTraceSession(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(database).releaseDatabase("session-ok");
            verify(launchTraceService).cleanSessionCache("session-ok");
        }
    }
}
