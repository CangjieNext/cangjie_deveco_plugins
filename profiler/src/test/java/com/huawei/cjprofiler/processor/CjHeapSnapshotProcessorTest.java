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
import com.huawei.deveco.insight.ohos.ability.arkmemory.instancefilters.MemoryObject;
import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteStopRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkGcRootPathExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeSearchRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotComparisonRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotParseRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapSnapshotDetail;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapSnapshotGcRootPath;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkSnapshotComparisonDetail;
import com.huawei.deveco.insight.ohos.model.dto.response.SingleSearchResultCount;
import com.huawei.deveco.insight.ohos.common.enums.ArkMemoryTypeEnum;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
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

    @Test
    void startWhenDeviceMissingReturnsDeviceEmpty() {
        CommonExecuteRequest request = executeRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        when(dao.registerMapper("session")).thenReturn(true);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.empty());
        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);

            Response<?> response = CjHeapSnapshotProcessor.startTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
        }
    }

    @Test
    void startWhenAllStepsSucceedRegistersServiceAndInitsRecordService() {
        CommonExecuteRequest request = executeRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService memoryService = mock(CjMemoryService.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        when(dao.registerMapper("session")).thenReturn(true);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.creatCjMemoryService("session")).thenReturn(memoryService);
        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordService> recordServiceStatic = mockStatic(CjRecordService.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);

            Response<?> response = CjHeapSnapshotProcessor.startTakeHeapSnapshot(request);

            assertThat(response.getIsSuccess()).isTrue();
            verify(memoryService).setInsertHeapSnapshot(false);
            recordServiceStatic.verify(CjRecordService::init);
        }
    }

    @Test
    void stopWhenDeviceMissingReturnsDeviceEmpty() {
        ArkExecuteStopRequest request = mock(ArkExecuteStopRequest.class);
        when(request.getDeviceKey()).thenReturn("device");
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.empty());
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managerStatic.verifyNoInteractions();

            Response<?> response = CjHeapSnapshotProcessor.stopTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
        }
    }

    @Test
    void stopWhenHandleAllSnapshotFailsReturnsPluginStopError() {
        ArkExecuteStopRequest request = stopRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService memoryService = mock(CjMemoryService.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.getCjMemoryService("session")).thenReturn(memoryService);
        when(memoryService.handleAllSnapshot("session")).thenReturn(false);
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.stopTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_STOP_ERROR);
            verify(manager, never()).getCjRecordService(123);
        }
    }

    @Test
    void stopWhenRecordServiceMissingReturnsPluginStopError() {
        ArkExecuteStopRequest request = stopRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService memoryService = mock(CjMemoryService.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.getCjMemoryService("session")).thenReturn(memoryService);
        when(memoryService.handleAllSnapshot("session")).thenReturn(true);
        when(manager.getCjRecordService(123)).thenReturn(Optional.empty());
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.stopTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_STOP_ERROR);
        }
    }

    @Test
    void stopWhenRecordServiceRejectsStopReturnsPluginStopError() {
        ArkExecuteStopRequest request = stopRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService memoryService = mock(CjMemoryService.class);
        CjRecordService recordService = mock(CjRecordService.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.getCjMemoryService("session")).thenReturn(memoryService);
        when(memoryService.handleAllSnapshot("session")).thenReturn(true);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));
        when(recordService.stopTakeHeapSnapshot()).thenReturn(false);
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.stopTakeHeapSnapshot(request);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_STOP_ERROR);
            verify(recordService).stopTakeHeapSnapshot();
        }
    }

    @Test
    void stopWhenAllStepsSucceedReturnsSuccess() {
        ArkExecuteStopRequest request = stopRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService memoryService = mock(CjMemoryService.class);
        CjRecordService recordService = mock(CjRecordService.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.getCjMemoryService("session")).thenReturn(memoryService);
        when(memoryService.handleAllSnapshot("session")).thenReturn(true);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));
        when(recordService.stopTakeHeapSnapshot()).thenReturn(true);
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.stopTakeHeapSnapshot(request);

            assertThat(response.getIsSuccess()).isTrue();
        }
    }

    @Test
    void takeHeapSnapshotWhenServiceAcceptsRequestReturnsSuccess() {
        CommonExecuteRequest request = executeRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService recordService = mock(CjRecordService.class);
        when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));
        when(recordService.takeHeapSnapshot(request)).thenReturn(true);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.takeHeapSnapshot(request);

            assertThat(response.getIsSuccess()).isTrue();
            verify(recordService).takeHeapSnapshot(request);
        }
    }

    @Test
    void expandRetainerDelegatesToCalMemData() {
        ArkHeapNodeExpandRequest request = mock(ArkHeapNodeExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        MemoryObject memoryObject = mock(MemoryObject.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.expandHeapNode(request)).thenReturn(memoryObject);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.expandRetainer(request);

            assertThat(response.getBody()).isSameAs(memoryObject);
            verify(service).expandHeapNode(request);
        }
    }

    @Test
    void expandSnapshotRetainerSetsSnapshotType() {
        ArkHeapNodeExpandRequest request = mock(ArkHeapNodeExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        MemoryObject memoryObject = mock(MemoryObject.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.expandHeapNode(request)).thenReturn(memoryObject);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.expandSnapshotRetainer(request);

            assertThat(response.getBody()).isSameAs(memoryObject);
            verify(request).setType(ArkMemoryTypeEnum.SNAPSHOT.getValue());
        }
    }

    @Test
    void expandComparisonRetainerSetsComparisonType() {
        ArkHeapNodeExpandRequest request = mock(ArkHeapNodeExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        MemoryObject memoryObject = mock(MemoryObject.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.expandHeapNode(request)).thenReturn(memoryObject);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.expandComparisonRetainer(request);

            assertThat(response.getBody()).isSameAs(memoryObject);
            verify(request).setType(ArkMemoryTypeEnum.COMPARISON.getValue());
        }
    }

    @Test
    void querySnapshotComparisonWrapsDiffViewNodes() {
        ArkSnapshotComparisonRequest request = mock(ArkSnapshotComparisonRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.getDiffView(request)).thenReturn(List.of());
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.querySnapshotComparison(request);

            assertThat(response.getBody()).isInstanceOf(ArkSnapshotComparisonDetail.class);
            verify(service).getDiffView(request);
        }
    }

    @Test
    void jumpToInstanceNodeReturnsMemoryObjectWhenPresent() {
        ArkHeapNodeExpandRequest request = mock(ArkHeapNodeExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        MemoryObject memoryObject = mock(MemoryObject.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.jumpToInstanceNode(request)).thenReturn(Optional.of(memoryObject));
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.jumpToInstanceNode(request);

            assertThat(response.getBody()).isSameAs(memoryObject);
        }
    }

    @Test
    void parseRawHeapFileDelegatesAndReturnsSuccess() {
        ArkSnapshotParseRequest request = mock(ArkSnapshotParseRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.parseRawHeapFile(request);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).parseRawHeapFile(request);
        }
    }

    @Test
    void queryParseSnapshotTimeRangeReturnsServiceTime() {
        JSONObject params = new JSONObject();
        params.put("sessionId", "session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.queryParseSnapshotTimeRange(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).queryParseSnapshotTimeRange();
        }
    }

    @Test
    void queryCountOfResultsWrapsCountWithArkMemoryUnit() {
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.queryCountOfResults(request)).thenReturn(7);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.queryCountOfResults(request);

            assertThat(response.getIsSuccess()).isTrue();
            SingleSearchResultCount count = SingleSearchResultCount.class.cast(response.getBody());
            assertThat(count.getCount()).isEqualTo(7);
        }
    }

    @Test
    void queryResultByIndexReturnsServiceResult() {
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        MemoryObject memoryObject = mock(MemoryObject.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.queryResultByIndex(request)).thenReturn(Optional.of(memoryObject));
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.queryResultByIndex(request);

            assertThat(response.getBody()).isInstanceOf(Optional.class);
            assertThat(Optional.class.cast(response.getBody()).orElse(null)).isSameAs(memoryObject);
        }
    }

    @Test
    void expandGcRootPathUsesCjprofPathWhenServiceIsCjprof() {
        ArkGcRootPathExpandRequest request = mock(ArkGcRootPathExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.isCjprof()).thenReturn(true);
        when(service.cjprofExpandGcRootPath(request)).thenReturn(List.of());
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.expandGcRootPath(request);

            assertThat(response.getBody()).isInstanceOf(ArkHeapSnapshotGcRootPath.class);
            verify(service).cjprofExpandGcRootPath(request);
        }
    }

    @Test
    void expandGcRootPathUsesBasePathWhenServiceIsNotCjprof() {
        ArkGcRootPathExpandRequest request = mock(ArkGcRootPathExpandRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.isCjprof()).thenReturn(false);
        when(service.expandGcRootPath(request)).thenReturn(List.of());
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.expandGcRootPath(request);

            assertThat(response.getBody()).isInstanceOf(ArkHeapSnapshotGcRootPath.class);
            verify(service).expandGcRootPath(request);
        }
    }

    @Test
    void queryHeapThreadInfoDelegatesToMemoryService() {
        JSONObject params = new JSONObject();
        params.put("sessionId", "session");
        params.put("snapshotId", "snapshot-1");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapSnapshotProcessor.queryHeapThreadInfo(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).getHeapThreadInfo("session", "snapshot-1");
        }
    }

    private static ArkExecuteStopRequest stopRequest() {
        ArkExecuteStopRequest request = mock(ArkExecuteStopRequest.class);
        when(request.getDeviceKey()).thenReturn("device");
        when(request.getSessionId()).thenReturn("session");
        when(request.getPid()).thenReturn(123);
        return request;
    }
}