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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjThreadDao;
import com.huawei.cjprofiler.model.dto.request.CjThreadSliceRequest;
import com.huawei.cjprofiler.model.dto.request.cjthread.CjThreadListRequest;
import com.huawei.cjprofiler.model.dto.request.cjthread.CjThreadMetadataRequest;
import com.huawei.cjprofiler.model.dto.request.cjthread.CjThreadSliceInfoRequest;
import com.huawei.cjprofiler.model.dto.response.cjthread.CjLaneSliceData;
import com.huawei.cjprofiler.model.dto.response.cjthread.CjThreadInfoData;
import com.huawei.cjprofiler.model.dto.response.cjthread.CjThreadSliceInfoListData;
import com.huawei.cjprofiler.model.dto.response.cjthread.CjThreadStateMetadata;
import com.huawei.cjprofiler.model.vo.CjLaneSliceVo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadSliceInfoVo;
import com.huawei.cjprofiler.service.CjThreadService;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

/**
 * Unit tests for {@code CjThreadProcessor} thread request handling.
 *
 * @since 2026-08-14
 */
class CjThreadProcessorTest {
    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjThreadProcessor();
    }

    @Test
    void queryMetadataWhenMapperRegistrationFailsReturnsSqlErrorAndSkipsS() {
        CjThreadMetadataRequest request = metadataRequest();
        CjThreadDao dao = mock(CjThreadDao.class);
        CjThreadService service = mock(CjThreadService.class);
        when(dao.registerMapper("session")).thenReturn(false);
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class);
             MockedStatic<CjThreadService> serviceStatic = mockStatic(CjThreadService.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);
            serviceStatic.when(CjThreadService::getInstance).thenReturn(service);

            Response<?> response = CjThreadProcessor.queryCjStateMetadata(request);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SQL_ERROR);
            verify(service, never()).queryCjStateMetadata("session", 10L, 20L);
        }
    }

    @Test
    void queryMetadataReturnsWrappedServiceResult() {
        CjThreadMetadataRequest request = metadataRequest();
        CjThreadDao dao = mock(CjThreadDao.class);
        CjThreadService service = mock(CjThreadService.class);
        List<String> values = List.of("Total", "Running");
        when(dao.registerMapper("session")).thenReturn(true);
        when(service.queryCjStateMetadata("session", 10L, 20L)).thenReturn(values);
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class);
             MockedStatic<CjThreadService> serviceStatic = mockStatic(CjThreadService.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);
            serviceStatic.when(CjThreadService::getInstance).thenReturn(service);

            Response<?> response = CjThreadProcessor.queryCjStateMetadata(request);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(CjThreadStateMetadata.class);
            assertThat(CjThreadStateMetadata.class.cast(response.getBody()).metadataList()).isSameAs(values);
            verify(dao).registerMapper("session");
            verify(service).queryCjStateMetadata("session", 10L, 20L);
        }
    }

    @Test
    void queryThreadSliceInfoDelegatesAllRequestFields() {
        CjThreadSliceInfoRequest request = new CjThreadSliceInfoRequest();
        request.setSessionId("session");
        request.setCjThreadId(7);
        request.setStartTime(11L);
        request.setEndTime(22L);
        CjThreadService service = mock(CjThreadService.class);
        List<CjThreadSliceInfoVo> values = List.of(mock(CjThreadSliceInfoVo.class));
        when(service.queryCjThreadSliceInfo("session", 7, 11L, 22L)).thenReturn(values);
        try (MockedStatic<CjThreadService> serviceStatic = mockStatic(CjThreadService.class)) {
            serviceStatic.when(CjThreadService::getInstance).thenReturn(service);

            Response<?> response = CjThreadProcessor.queryCjThreadSliceInfo(request);

            assertThat(response.getBody()).isInstanceOf(CjThreadSliceInfoListData.class);
            assertThat(CjThreadSliceInfoListData.class.cast(response.getBody()).cjThreadStateList()).isSameAs(values);
            verify(service).queryCjThreadSliceInfo("session", 7, 11L, 22L);
        }
    }

    @Test
    void queryThreadListReturnsThreadInfoData() {
        CjThreadListRequest request = new CjThreadListRequest();
        request.setSessionId("s");
        request.setStartTime(1L);
        request.setEndTime(2L);
        CjThreadService service = mock(CjThreadService.class);
        List<CjThreadInfoVo> values = List.of(mock(CjThreadInfoVo.class));
        when(service.queryCjThreadList("s", 1L, 2L)).thenReturn(values);
        try (MockedStatic<CjThreadService> serviceStatic = mockStatic(CjThreadService.class)) {
            serviceStatic.when(CjThreadService::getInstance).thenReturn(service);

            Response<?> response = CjThreadProcessor.queryCjThreadList(request);

            assertThat(response.getBody()).isInstanceOf(CjThreadInfoData.class);
            assertThat(CjThreadInfoData.class.cast(response.getBody()).cjThreadInfoList()).isSameAs(values);
            verify(service).queryCjThreadList("s", 1L, 2L);
        }
    }

    @Test
    void queryLaneListDelegatesProcessIdsAndWrapsResult() {
        CjThreadSliceRequest request = new CjThreadSliceRequest();
        request.setSessionId("s");
        request.setStateName("Running");
        request.setStartTime(3L);
        request.setEndTime(9L);
        request.setProcessIdList(List.of(10L, 20L));
        CjThreadService service = mock(CjThreadService.class);
        List<CjLaneSliceVo> values = List.of(mock(CjLaneSliceVo.class));
        when(service.queryCjThreadLaneList("s", "Running", 3L, 9L, request.getProcessIdList())).thenReturn(values);
        try (MockedStatic<CjThreadService> serviceStatic = mockStatic(CjThreadService.class)) {
            serviceStatic.when(CjThreadService::getInstance).thenReturn(service);

            Response<?> response = CjThreadProcessor.queryCjThreadLaneList(request);

            assertThat(response.getBody()).isInstanceOf(CjLaneSliceData.class);
            assertThat(CjLaneSliceData.class.cast(response.getBody()).cjLaneSliceInfo()).isSameAs(values);
            verify(service).queryCjThreadLaneList("s", "Running", 3L, 9L, List.of(10L, 20L));
        }
    }

    private static CjThreadMetadataRequest metadataRequest() {
        CjThreadMetadataRequest request = new CjThreadMetadataRequest();
        request.setSessionId("session");
        request.setStartTime(10L);
        request.setEndTime(20L);
        return request;
    }
}
