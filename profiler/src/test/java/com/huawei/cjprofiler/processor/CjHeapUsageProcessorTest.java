/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_USAGE_START;
import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_USAGE_STOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.cjprofiler.service.CjHeapUsageService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapUsages;
import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkAllHeapUsageRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapUsagesDetail;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapUsagesLane;
import com.huawei.deveco.insight.ohos.model.vo.ArkHeapUsagesSummary;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;

import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjHeapUsageProcessor} heap usage queries.
 *
 * @since 2026-08-14
 */
class CjHeapUsageProcessorTest {
    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjHeapUsageProcessor();
    }

    @Test
    void startWhenMapperRegistrationFailsReturnsSqlErrorAndSkipsRecordMan() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.registerMapper("session")).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            Response<?> response = CjHeapUsageProcessor.startHeapUsage(params);

            assertThat(response.getIsSuccess()).isFalse();
            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SQL_ERROR);
            validate.verify(() -> ValidateUtil.validate(request));
            managerStatic.verifyNoInteractions();
        }
    }

    @Test
    void startWhenDeviceMissingReturnsDeviceErrorAndSkipsTask() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager devices = mock(OpenHarmonyDeviceManager.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(dao.registerMapper("session")).thenReturn(true);
        when(devices.getDeviceByKey("device")).thenReturn(Optional.empty());
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(devices);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapUsageProcessor.startHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
            verify(manager, never()).executeCJTask(request, HEAP_USAGE_START, params);
        }
    }

    @Test
    void startWhenDeviceOfflineReturnsDeviceErrorWithoutManagerUse() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager devices = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(dao.registerMapper("session")).thenReturn(true);
        when(devices.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.OFFLINE);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(devices);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapUsageProcessor.startHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
            verify(manager, never()).initData(ArkExecuteType.HEAP_PROFILER, "session");
            verify(manager, never()).executeCJTask(request, HEAP_USAGE_START, params);
        }
    }

    @Test
    void startWhenTaskFailsReturnsPluginStartError() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager devices = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(dao.registerMapper("session")).thenReturn(true);
        when(devices.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.executeCJTask(request, HEAP_USAGE_START, params)).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(devices);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapUsageProcessor.startHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_START_ERROR);
            verify(manager).initData(ArkExecuteType.HEAP_PROFILER, "session");
            verify(manager).creatCjRecordService(123);
        }
    }

    @Test
    void stopWhenDeviceOfflineReturnsDeviceErrorWithoutTaskExecution() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager devices = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(devices.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.OFFLINE);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(devices);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapUsageProcessor.stopHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.DEVICE_EMPTY);
            verify(manager, never()).executeCJTask(request, HEAP_USAGE_STOP, params);
        }
    }

    @Test
    void stopWhenTaskFailsReturnsPluginStopError() {
        JSONObject params = new JSONObject();
        ArkExecuteRequest request = executeRequest();
        OpenHarmonyDeviceManager devices = mock(OpenHarmonyDeviceManager.class);
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        CjRecordManager manager = mock(CjRecordManager.class);
        when(devices.getDeviceByKey("device")).thenReturn(Optional.of(device));
        when(device.getStatus()).thenReturn(IHarmonyDevice.Status.ONLINE);
        when(manager.executeCJTask(request, HEAP_USAGE_STOP, params)).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkExecuteRequest.class)).thenReturn(request);
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(devices);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            Response<?> response = CjHeapUsageProcessor.stopHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.PLUGIN_STOP_ERROR);
            verify(manager).executeCJTask(request, HEAP_USAGE_STOP, params);
        }
    }

    @Test
    void queryHeapUsageValidatesAndWrapsServiceResult() {
        JSONObject params = new JSONObject();
        CommonQueryRequest request = CommonQueryRequest.builder().sessionId("session").tid(7).build();
        CjHeapUsageService service = mock(CjHeapUsageService.class);
        List<HeapUsages> usages = List.of(new HeapUsages(1.0, 2.0, 3L));
        when(service.queryHeapUsage(request)).thenReturn(usages);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjHeapUsageService> serviceStatic = mockStatic(CjHeapUsageService.class)) {
            json.when(() -> JsonUtil.parseObject(params, CommonQueryRequest.class)).thenReturn(request);
            serviceStatic.when(CjHeapUsageService::getInstance).thenReturn(service);

            Response<?> response = CjHeapUsageProcessor.queryHeapUsage(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(ArkHeapUsagesLane.class);
            assertThat(ArkHeapUsagesLane.class.cast(response.getBody()).getHeapUsagesList()).isSameAs(usages);
            validate.verify(() -> ValidateUtil.validate(request));
            verify(service).queryHeapUsage(request);
        }
    }

    @Test
    void queryAllWhenMapperRegistrationFailsReturnsSqlErrorWithoutValidat() {
        JSONObject params = new JSONObject();
        ArkAllHeapUsageRequest request = mock(ArkAllHeapUsageRequest.class);
        when(request.getSessionId()).thenReturn("session");
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjHeapUsageService service = mock(CjHeapUsageService.class);
        when(dao.registerMapper("session")).thenReturn(false);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validate = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjHeapUsageService> serviceStatic = mockStatic(CjHeapUsageService.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkAllHeapUsageRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            serviceStatic.when(CjHeapUsageService::getInstance).thenReturn(service);

            Response<?> response = CjHeapUsageProcessor.queryAllHeapUsage(params);

            assertThat(response.getProfilerError()).isEqualTo(ProfilerError.SQL_ERROR);
            validate.verifyNoInteractions();
            verify(service, never()).queryAllHeapUsage(request);
        }
    }

    @Test
    void queryAllSuccessValidatesAndWrapsServiceResult() {
        JSONObject params = new JSONObject();
        ArkAllHeapUsageRequest request = mock(ArkAllHeapUsageRequest.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        CjHeapUsageService service = mock(CjHeapUsageService.class);
        List<HeapUsages> values = List.of();
        when(request.getSessionId()).thenReturn("session");
        when(dao.registerMapper("session")).thenReturn(true);
        when(service.queryAllHeapUsage(request)).thenReturn(values);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class);
             MockedStatic<CjHeapUsageService> serviceStatic = mockStatic(CjHeapUsageService.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkAllHeapUsageRequest.class)).thenReturn(request);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            serviceStatic.when(CjHeapUsageService::getInstance).thenReturn(service);

            Response<?> response = CjHeapUsageProcessor.queryAllHeapUsage(params);

            assertThat(response.getIsSuccess()).isTrue();
            verify(service).queryAllHeapUsage(request);
            validation.verify(() -> ValidateUtil.validate(request));
        }
    }

    @Test
    void queryAllDetailWrapsServiceSummary() {
        JSONObject params = new JSONObject();
        ArkAllHeapUsageRequest request = mock(ArkAllHeapUsageRequest.class);
        CjHeapUsageService service = mock(CjHeapUsageService.class);
        List<ArkHeapUsagesSummary> values = List.of();
        when(service.queryAllHeapUsagesDetail(request)).thenReturn(values);
        try (MockedStatic<JsonUtil> json = mockStatic(JsonUtil.class);
             MockedStatic<ValidateUtil> validation = mockStatic(ValidateUtil.class);
             MockedStatic<CjHeapUsageService> serviceStatic = mockStatic(CjHeapUsageService.class)) {
            json.when(() -> JsonUtil.parseObject(params, ArkAllHeapUsageRequest.class)).thenReturn(request);
            serviceStatic.when(CjHeapUsageService::getInstance).thenReturn(service);

            Response<?> response = CjHeapUsageProcessor.queryAllHeapUsagesDetail(params);

            assertThat(response.getIsSuccess()).isTrue();
            assertThat(response.getBody()).isInstanceOf(ArkHeapUsagesDetail.class);
            assertThat(ArkHeapUsagesDetail.class.cast(response.getBody()).getHeapUsagesSummaryList()).isSameAs(values);
            verify(service).queryAllHeapUsagesDetail(request);
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