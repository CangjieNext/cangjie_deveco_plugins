/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteRequest;

import com.alibaba.fastjson2.JSONObject;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Unit tests for {@code CjRecordManager} service creation, task execution and resource release.
 *
 * @since 2026-08-14
 */
class CjRecordManagerTest {
    private final CjRecordManager manager = CjRecordManager.getInstance();

    @BeforeEach
    @AfterEach
    void clearManagerState() {
        manager.releaseAllMemoryResources();
        manager.setCurrentSession(null);
        manager.setCurrentMode(ArkExecuteType.UNKNOWN);
    }

    @Test
    void getInstance_returnsSingleton() {
        assertThat(CjRecordManager.getInstance()).isSameAs(CjRecordManager.getInstance());
    }

    @Test
    void getCjRecordService_whenProcessIsMissing_returnsEmpty() {
        Optional<CjRecordService> result = manager.getCjRecordService(404);

        assertThat(result).isEmpty();
    }

    @Test
    void createCjRecordService_whenProcessIsMissing_createsRetrievableSer() {
        manager.creatCjRecordService(12);

        assertThat(manager.getCjRecordService(12))
            .isPresent()
            .get()
            .extracting(CjRecordService::getTid)
            .isEqualTo(12);
    }

    @Test
    void createCjRecordService_whenProcessAlreadyExists_keepsExistingServ() {
        manager.creatCjRecordService(21);
        CjRecordService first = manager.getCjRecordService(21).orElseThrow();

        manager.creatCjRecordService(21);

        assertThat(manager.getCjRecordService(21)).containsSame(first);
        assertThat(manager.getCjRecordServiceMap()).hasSize(1);
    }

    @Test
    void createCjMemoryService_whenSessionIsMissing_createsAndCachesServi() {
        CjMemoryService created = manager.creatCjMemoryService("session-1");

        assertThat(created).isNotNull();
        assertThat(manager.getCjMemoryService("session-1")).isSameAs(created);
    }

    @Test
    void createCjMemoryService_whenSessionAlreadyExists_keepsExistingServ() {
        CjMemoryService first = manager.creatCjMemoryService("session-2");

        CjMemoryService second = manager.creatCjMemoryService("session-2");

        assertThat(second).isSameAs(first);
        assertThat(manager.getCjMemoryServiceMap()).hasSize(1);
    }

    @Test
    void releaseAllMemoryResources_releasesEveryMemoryServiceAndClearsBot() {
        CjMemoryService first = mock(CjMemoryService.class);
        CjMemoryService second = mock(CjMemoryService.class);
        manager.getCjMemoryServiceMap().put("first", first);
        manager.getCjMemoryServiceMap().put("second", second);
        manager.creatCjRecordService(31);
        manager.creatCjRecordService(32);

        boolean isReleased = manager.releaseAllMemoryResources();

        assertThat(isReleased).isTrue();
        verify(first, times(1)).releaseResources();
        verify(second, times(1)).releaseResources();
        assertThat(manager.getCjMemoryServiceMap()).isEmpty();
        assertThat(manager.getCjRecordServiceMap()).isEmpty();
        assertThat(manager.getCjRecordService(31)).isEmpty();
    }

    @Test
    void initData_updatesModeAndSession() {
        manager.initData(ArkExecuteType.HEAP_PROFILER, "session-init");

        assertThat(manager.getCurrentMode()).isEqualTo(ArkExecuteType.HEAP_PROFILER);
        assertThat(manager.getCurrentSession()).isEqualTo("session-init");
    }

    @Test
    void releaseCjMemoryService_whenServiceExists_releasesResources() {
        CjMemoryService service = mock(CjMemoryService.class);
        manager.getCjMemoryServiceMap().put("session-rel", service);

        manager.releaseCjMemoryService("session-rel");

        verify(service, times(1)).releaseResources();
        assertThat(manager.getCjMemoryServiceMap()).doesNotContainKey("session-rel");
    }

    @Test
    void releaseCjMemoryService_whenServiceMissing_doesNothing() {
        manager.releaseCjMemoryService("session-missing");

        assertThat(manager.getCjMemoryServiceMap()).isEmpty();
    }

    @Test
    void executeCJTask_whenApplicationIsNull_returnsTrueWithEmptyFutures() {
        manager.creatCjRecordService(99);
        ArkExecuteRequest request = buildRequest(99);
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(null);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.PROFILER_PARSE, new JSONObject());

            assertThat(isSubmitted).isTrue();
        }
    }

    @Test
    void executeCJTask_whenServiceMissing_returnsFalse() throws Exception {
        ArkExecuteRequest request = buildRequest(404);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.PROFILER_PARSE, new JSONObject());

            assertThat(isSubmitted).isFalse();
        }
    }

    @Test
    void executeCJTask_whenApplicationNotNull_executesTaskInline() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.startCjHeapUsageTimer()).thenReturn(true);
        manager.getCjRecordServiceMap().put(55, service);
        ArkExecuteRequest request = buildRequest(55);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_USAGE_START, new JSONObject());

            assertThat(isSubmitted).isTrue();
        }
    }

    @Test
    void executeCJTask_whenFutureThrowsTimeout_returnsFalse() throws Exception {
        manager.creatCjRecordService(56);
        ArkExecuteRequest request = buildRequest(56);
        Application app = mock(Application.class);
        Future<Boolean> future = mock(Future.class);
        when(future.get(anyLong(), any())).thenThrow(new TimeoutException("timeout"));
        doReturn(future).when(app).executeOnPooledThread(any(Callable.class));
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.PROFILER_PARSE, new JSONObject());

            assertThat(isSubmitted).isFalse();
        }
    }

    @Test
    void executeCJTask_whenHeapProfilerStop_detachesAppAndSetsModeUnknown() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.getTid()).thenReturn(77);
        when(service.stopCjHeapTimeLine(anyString(), anyLong())).thenReturn(false);
        manager.getCjRecordServiceMap().put(77, service);
        manager.initData(ArkExecuteType.HEAP_PROFILER, "session-stop");
        ArkExecuteRequest request = buildRequest(77);
        request.setSessionId("session-stop");
        IHarmonyDevice device = mock(IHarmonyDevice.class);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        when(deviceManager.getDeviceByKey(request.getDeviceKey())).thenReturn(Optional.of(device));
        JSONObject params = new JSONObject();
        params.put("startRecordTime", 123L);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_STOP, params);

            assertThat(isSubmitted).isFalse();
            verify(device, times(1)).wakeUpProcess(77);
            verify(device, times(1)).detachProcess(request.getBundleName());
            assertThat(manager.getCurrentMode()).isEqualTo(ArkExecuteType.UNKNOWN);
        }
    }

    @Test
    void executeCJTask_whenProfilerStopDeviceOffline_logsAndReturns() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.startCjHeapUsageTimer()).thenReturn(true);
        manager.getCjRecordServiceMap().put(78, service);
        ArkExecuteRequest request = buildRequest(78);
        OpenHarmonyDeviceManager deviceManager = mock(OpenHarmonyDeviceManager.class);
        when(deviceManager.getDeviceByKey(request.getDeviceKey())).thenReturn(Optional.empty());
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<OpenHarmonyDeviceManager> deviceStatic = mockStatic(OpenHarmonyDeviceManager.class);
             MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            deviceStatic.when(OpenHarmonyDeviceManager::getInstance).thenReturn(deviceManager);
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.PROFILER_STOP, new JSONObject());

            assertThat(isSubmitted).isFalse();
            assertThat(manager.getCurrentMode()).isEqualTo(ArkExecuteType.UNKNOWN);
        }
    }

    @Test
    void executeTask_whenHeapProfilerStart_usesTrackAllocations() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.getTid()).thenReturn(88);
        when(service.startCjHeapTimeLine(anyBoolean())).thenReturn(true);
        manager.getCjRecordServiceMap().put(88, service);
        ArkExecuteRequest request = buildRequest(88);
        JSONObject params = new JSONObject();
        params.put("trackAllocations", true);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_START, params);

            assertThat(isSubmitted).isTrue();
        }
    }

    @Test
    void executeTask_whenHeapUsageStart_callsService() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.startCjHeapUsageTimer()).thenReturn(true);
        manager.getCjRecordServiceMap().put(89, service);
        ArkExecuteRequest request = buildRequest(89);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_USAGE_START, new JSONObject());

            assertThat(isSubmitted).isTrue();
        }
    }

    @Test
    void executeTask_whenHeapUsageStop_callsService() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.stopCjHeapUsageTimerAndSaveData(anyString(), anyLong())).thenReturn(true);
        manager.getCjRecordServiceMap().put(90, service);
        manager.initData(ArkExecuteType.HEAP_PROFILER, "session-usage-stop");
        ArkExecuteRequest request = buildRequest(90);
        request.setSessionId("session-usage-stop");
        JSONObject params = new JSONObject();
        params.put("startRecordTime", 999L);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_USAGE_STOP, params);

            assertThat(isSubmitted).isTrue();
        }
    }

    @Test
    void executeTask_whenUnmatchedAction_returnsFalse() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        manager.getCjRecordServiceMap().put(91, service);
        ArkExecuteRequest request = buildRequest(91);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.UNKNOWN, new JSONObject());

            assertThat(isSubmitted).isFalse();
        }
    }

    @Test
    void executeTask_whenHeapProfilerParse_noSnapshot_returnsFalse() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.getTid()).thenReturn(92);
        when(service.getRawHeapSnapshot()).thenReturn(Optional.empty());
        manager.getCjRecordServiceMap().put(92, service);
        ArkExecuteRequest request = buildRequest(92);
        request.setSessionId("session-parse");
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_PARSE, new JSONObject());

            assertThat(isSubmitted).isFalse();
        }
    }

    @Test
    void executeCJTask_whenServiceThrowsPersistenceException_returnsFalse() throws Exception {
        CjRecordService service = mock(CjRecordService.class);
        when(service.getTid()).thenReturn(93);
        when(service.startCjHeapTimeLine(anyBoolean()))
            .thenThrow(new org.apache.ibatis.exceptions.PersistenceException("db error"));
        manager.getCjRecordServiceMap().put(93, service);
        ArkExecuteRequest request = buildRequest(93);
        Application app = mock(Application.class);
        when(app.executeOnPooledThread(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        try (MockedStatic<ApplicationManager> appStatic = mockStatic(ApplicationManager.class)) {
            appStatic.when(ApplicationManager::getApplication).thenReturn(app);

            boolean isSubmitted = manager.executeCJTask(request, ArkExecuteType.HEAP_PROFILER_START, new JSONObject());

            assertThat(isSubmitted).isFalse();
        }
    }

    @Test
    void getCjMemoryService_whenCacheMiss_recoversFromTraceFiles() {
        try (MockedStatic<com.huawei.deveco.insight.ohos.service.file.JsTraceFileService> traceStatic =
            mockStatic(com.huawei.deveco.insight.ohos.service.file.JsTraceFileService.class)) {
            com.huawei.deveco.insight.ohos.service.file.JsTraceFileService fileService =
                mock(com.huawei.deveco.insight.ohos.service.file.JsTraceFileService.class);
            when(fileService.getTraceFile(anyString())).thenReturn(null);
            traceStatic.when(com.huawei.deveco.insight.ohos.service.file.JsTraceFileService::getInstance)
                .thenReturn(fileService);

            CjMemoryService result = manager.getCjMemoryService("session-miss");

            assertThat(result).isNotNull();
            assertThat(manager.getCjMemoryServiceMap()).containsKey("session-miss");
        }
    }

    @Test
    void getCjMemoryService_whenCacheHit_returnsCached() {
        CjMemoryService cached = mock(CjMemoryService.class);
        manager.getCjMemoryServiceMap().put("session-hit", cached);

        CjMemoryService result = manager.getCjMemoryService("session-hit");

        assertThat(result).isSameAs(cached);
    }

    private ArkExecuteRequest buildRequest(int pid) {
        ArkExecuteRequest request = new ArkExecuteRequest();
        request.setPid(pid);
        request.setBundleName("com.example.app");
        request.setDeviceKey("device-key-1");
        request.setSessionId("session-" + pid);
        return request;
    }
}
