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

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.cjprofiler.service.CjRecordService;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotParseRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapSnapshotDetail;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Unit tests for {@code CjHeapSnapshotProcessor} snapshot start flows.
 *
 * @since 2026-08-14
 */
class CjHeapSnapshotProcessorTest {
    @Test
    void startWhenMapperRegistrationFailsReturnsSqlErrorWithoutCreatingMe() {
        CommonExecuteRequest request = executeRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(dao.registerMapper("session")).thenReturn(false);
        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.startTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SQL_ERROR);
            verify(manager, never()).creatCjMemoryService("session");
        }
    }

    @Test
    void takeSnapshotWhenRecordServiceMissingReturnsInternalError() {
        CommonExecuteRequest request = executeRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.empty());
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.takeHeapSnapshot(request);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_INTERNAL_ERROR);
            verify(manager).getCjRecordService(123);
        }
    }

    @Test
    void takeSnapshotWhenServiceRejectsRequestReturnsInternalError() {
        CommonExecuteRequest request = executeRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService recordService = mock(CjRecordService.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));
        when(recordService.takeHeapSnapshot(request)).thenReturn(false);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.takeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_INTERNAL_ERROR);
            verify(recordService).takeHeapSnapshot(request);
        }
    }

    @Test
    void queryAllNonCjprofWhenMapperRegistrationFailsSkipsSnapshotQuery() {
        CommonQueryRequest request = CommonQueryRequest.builder().sessionId("session").build();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.isCjprof()).thenReturn(false);
        when(dao.registerMapper("session")).thenReturn(false);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            Response<?> response = CjHeapSnapshotProcessor.queryAllHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SQL_ERROR);
            verify(service, never()).getAllSnapshot(request);
        }
    }

    @Test
    void queryAllCjprofSkipsMapperAndQueriesSnapshots() {
        CommonQueryRequest request = CommonQueryRequest.builder().sessionId("session").build();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.isCjprof()).thenReturn(true);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.queryAllHeapSnapshot(request);

            assertThat(response.getIsSuccess()).isTrue();
            daoStatic.verifyNoInteractions();
            verify(service).getAllSnapshot(request);
        }
    }

    @Test
    void querySnapshotDetailReturnsMemoryServiceResult() {
        CommonQueryRequest request = CommonQueryRequest.builder().sessionId("session").build();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        ArkHeapSnapshotDetail detail = mock(ArkHeapSnapshotDetail.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.querySnapshotDetail(request)).thenReturn(detail);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.querySnapshotDetailById(request);

            assertThat(response.getBody()).isSameAs(detail);
            verify(service).querySnapshotDetail(request);
        }
    }

    @Test
    void parseSnapshotFileFailureReturnsFileParsingError() {
        ArkSnapshotParseRequest request = mock(ArkSnapshotParseRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.creatCjMemoryService("session")).thenReturn(service);
        when(service.parseCjprofHeapSnapshotFile(request)).thenReturn(false);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.parseHeapSnapshotFile(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.FILE_PARSING_ERROR);
            verify(service).parseCjprofHeapSnapshotFile(request);
        }
    }

    @Test
    void parseSnapshotFileSuccessReturnsSuccess() {
        ArkSnapshotParseRequest request = mock(ArkSnapshotParseRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.creatCjMemoryService("session")).thenReturn(service);
        when(service.parseCjprofHeapSnapshotFile(request)).thenReturn(true);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.parseHeapSnapshotFile(request);

            assertThat(response.getIsSuccess()).isTrue();
        }
    }

    @Test
    void executeGcWithoutPidReturnsValidationErrorWithoutManagerLookup() {
        JSONObject params = new JSONObject();
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            Response<?> response = CjHeapSnapshotProcessor.executeArkVMGC(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.OBJECT_VALIDATE_ERROR);
            managerStatic.verifyNoInteractions();
        }
    }

    @Test
    void executeGcWhenRecordServiceMissingReturnsServerError() {
        JSONObject params = new JSONObject();
        params.put("pid", 123);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.empty());
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.executeArkVMGC(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
        }
    }

    @Test
    void executeGcWhenFutureReturnsFalseReturnsServerError() {
        JSONObject params = new JSONObject();
        params.put("pid", 123);
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService service = mock(CjRecordService.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(service));
        when(service.collectGarbage()).thenReturn(CompletableFuture.completedFuture(false));
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.executeArkVMGC(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
            verify(service).collectGarbage();
        }
    }

    @Test
    void executeGcWhenFutureIsNullReturnsServerError() {
        JSONObject params = new JSONObject();
        params.put("pid", 123);
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService service = mock(CjRecordService.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(service));
        when(service.collectGarbage()).thenReturn(null);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.executeArkVMGC(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SERVER_INTERNAL_ERROR);
        }
    }

    @Test
    void executeGcSuccessfulFutureReturnsSuccess() {
        JSONObject params = new JSONObject();
        params.put("pid", 123);
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService recordService = mock(CjRecordService.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));
        when(recordService.collectGarbage()).thenReturn(CompletableFuture.completedFuture(true));
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.executeArkVMGC(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(recordService).collectGarbage();
        }
    }

    private static CommonExecuteRequest executeRequest() {
        CommonExecuteRequest request = new CommonExecuteRequest();
        request.setSessionId("session");
        request.setDeviceKey("device");
        request.setPid(123);
        return request;
    }
}