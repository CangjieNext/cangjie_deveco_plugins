/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_PROFILER_START;
import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_PROFILER_STOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapStats;
import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapStatsLane;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkInstanceMetadata;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjHeapTimelineProcessor} heap timeline queries.
 *
 * @since 2026-08-14
 */
class CjHeapTimelineProcessorTest {
    @Test
    void startWhenDeviceMissingReturnsDeviceEmptyWithoutManagerUse() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.empty());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<OpenHarmonyDeviceManager> devices = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managers = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            devices.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);

            Response<?> response = CjHeapTimelineProcessor.startHeapTimeLine(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
            managers.verifyNoInteractions();
        }
    }

    @Test
    void startWhenTaskFailsReturnsPluginStartError() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.executeCJTask(request, HEAP_PROFILER_START, params)).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<OpenHarmonyDeviceManager> devices = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managers = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            devices.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managers.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapTimelineProcessor.startHeapTimeLine(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_START_ERROR);
            verify(manager).initData(ArkExecuteType.HEAP_PROFILER, "session");
            verify(manager).creatCjRecordService(123);
        }
    }

    @Test
    void stopWhenTaskFailsReturnsPluginStopError() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(deviceManager.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.executeCJTask(request, HEAP_PROFILER_STOP, params)).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<OpenHarmonyDeviceManager> devices = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managers = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            devices.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            managers.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapTimelineProcessor.stopHeapTimeLine(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_STOP_ERROR);
            verify(manager).executeCJTask(request, HEAP_PROFILER_STOP, params);
        }
    }

    @Test
    void parseWhenTaskFailsReturnsFileParsingErrorAndSkipsCalculation() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_PARSE, params))
            .thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapTimelineProcessor.parseHeapTimeLine(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.FILE_PARSING_ERROR);
            verify(manager, never()).getCjMemoryService("session");
            verify(service, never()).calculateTotalSize();
            validate.verify(() -> ValidateUtil.validate(request));
        }
    }

    @Test
    void parseWhenTaskSucceedsCalculatesTotalSize() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_PARSE, params))
            .thenReturn(true);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapTimelineProcessor.parseHeapTimeLine(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).calculateTotalSize();
        }
    }

    @Test
    void queryHeapStatsDelegatesAllFieldsAndWrapsDaoResult() {
        JSONObject params = new JSONObject();
        CommonQueryRequest request = CommonQueryRequest.builder()
            .sessionId("session").tid(4).startTime(10L).endTime(20L).build();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        List<HeapStats> stats = List.of(new HeapStats(1L, 2, 3, 4, 5L));
        when(dao.selectCjHeapStats("session", 4, 10L, 20L)).thenReturn(stats);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            json.when(() -> JsonUtil.parseObject(params, CommonQueryRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            Response<?> response = CjHeapTimelineProcessor.queryHeapStats(params);

            assertThat(response.getBody()).isInstanceOf(ArkHeapStatsLane.class);
            assertThat(ArkHeapStatsLane.class.cast(response.getBody()).getHeapStatsList()).isSameAs(stats);
            verify(dao).selectCjHeapStats("session", 4, 10L, 20L);
            validate.verify(() -> ValidateUtil.validate(request));
        }
    }

    @Test
    void queryTimelineDetailDelegatesToMemoryService() {
        JSONObject params = new JSONObject();
        ArkQueryRequest request = mock(ArkQueryRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjRecordManager manager = mock(CjRecordManager.class);
        CjMemoryService service = mock(CjMemoryService.class);
        when(manager.getCjMemoryService("session")).thenReturn(service);
        when(service.queryHeapTimelineDetail(request)).thenReturn(null);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjRecordManager> managers = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkQueryRequest.class)).thenReturn(request);
            managers.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapTimelineProcessor.queryHeapTimelineDetail(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).queryHeapTimelineDetail(request);
            validate.verify(() -> ValidateUtil.validate(request));
        }
    }

    @Test
    void queryInstanceMetadataReturnsSingletonPid() {
        JSONObject params = new JSONObject();
        CommonExecuteRequest request = new CommonExecuteRequest();
        request.setPid(99);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class)) {
            json.when(() -> JsonUtil.parseObject(params, CommonExecuteRequest.class)).thenReturn(request);

            Response<?> response = CjHeapTimelineProcessor.queryInstanceMetadata(params);

            assertThat(response.getBody()).isInstanceOf(ArkInstanceMetadata.class);
            assertThat(ArkInstanceMetadata.class.cast(response.getBody()).getTidList()).containsExactly(99);
            validate.verify(() -> ValidateUtil.validate(request));
        }
    }

    private static ArkExecuteRequest executeRequest() {
        ArkExecuteRequest request = new ArkExecuteRequest();
        request.setSessionId("session");
        request.setDeviceKey("device");
        request.setPid(123);
        return request;
    }
}