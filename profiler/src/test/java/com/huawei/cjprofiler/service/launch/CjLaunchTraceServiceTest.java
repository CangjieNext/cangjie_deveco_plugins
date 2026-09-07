/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service.launch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.common.constant.LaunchTraceConstants;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.dao.LaunchTraceDao;
import com.huawei.deveco.insight.ohos.model.dto.LaunchData;
import com.huawei.deveco.insight.ohos.model.dto.request.launch.LaunchLifeCycleRequest;
import com.huawei.deveco.insight.ohos.model.po.LaunchTracePo;
import com.huawei.deveco.insight.ohos.model.vo.LifeCycleEventVo;
import com.huawei.deveco.insight.ohos.service.event.LifeCycleEventCache;
import com.huawei.deveco.insight.ohos.service.event.LifeCycleEventCacheManager;
import com.huawei.deveco.insight.ohos.service.launch.LaunchDataCache;
import com.huawei.deveco.insight.ohos.service.launch.LaunchDataCacheManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjLaunchTraceService} launch trace caching and query logic.
 *
 * @since 2026-08-14
 */
class CjLaunchTraceServiceTest {
    @Test
    void cacheCjLaunchLifeCycleTrace_whenDaoReturnsNull_returnsFalse() {
        LaunchLifeCycleRequest request = branchRequest("null-session");
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        when(dao.queryLaunchTraceList(
            org.mockito.ArgumentMatchers.eq("null-session"),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(20L),
            org.mockito.ArgumentMatchers.anyList())).thenReturn(null);

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isFalse();
        }
    }

    @Test
    void cacheCjLaunchLifeCycleTrace_whenDaoReturnsEmptyList_returnsFalse() {
        LaunchLifeCycleRequest request = branchRequest("empty-session");
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        when(dao.queryLaunchTraceList(
            org.mockito.ArgumentMatchers.eq("empty-session"),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(20L),
            org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isFalse();
        }
    }

    @Test
    void cacheCjLaunchLifeCycleTrace_whenDaoThrowsProfilerException_retur() {
        LaunchLifeCycleRequest request = branchRequest("error-session");
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        when(dao.queryLaunchTraceList(
            org.mockito.ArgumentMatchers.eq("error-session"),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(20L),
            org.mockito.ArgumentMatchers.anyList()))
            .thenThrow(new ProfilerException(ProfilerError.SQL_ERROR));

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isFalse();
        }
    }

    @Test
    void syncCacheLaunchLifeCycle_afterSuccessfulCache_skipsDuplicateSess() {
        CjLaunchTraceService service = spy(new CjLaunchTraceService());
        LaunchLifeCycleRequest request = branchRequest("cached-session");
        doReturn(Boolean.TRUE).when(service).cacheCjLaunchLifeCycleTrace(request);

        service.syncCacheLaunchLifeCycle(request);
        service.syncCacheLaunchLifeCycle(request);

        verify(service, times(1)).cacheCjLaunchLifeCycleTrace(request);
    }

    @Test
    void syncCacheLaunchLifeCycle_afterFailedCache_retriesSameSession() {
        CjLaunchTraceService service = spy(new CjLaunchTraceService());
        LaunchLifeCycleRequest request = branchRequest("retry-session");
        doReturn(Boolean.FALSE).when(service).cacheCjLaunchLifeCycleTrace(request);

        service.syncCacheLaunchLifeCycle(request);
        service.syncCacheLaunchLifeCycle(request);

        verify(service, times(2)).cacheCjLaunchLifeCycleTrace(request);
    }

    @Test
    void cleanSessionCache_afterSuccessfulCache_allowsSessionToBeCachedAg() {
        CjLaunchTraceService service = spy(new CjLaunchTraceService());
        LaunchLifeCycleRequest request = branchRequest("clean-session");
        doReturn(Boolean.TRUE).when(service).cacheCjLaunchLifeCycleTrace(request);
        service.syncCacheLaunchLifeCycle(request);

        service.cleanSessionCache("clean-session");
        service.syncCacheLaunchLifeCycle(request);

        verify(service, times(2)).cacheCjLaunchLifeCycleTrace(request);
    }

    @Test
    void cacheTrace_withUnmatchedTrace_cachesNoMainPhasesAndStillSucceeds() {
        LaunchLifeCycleRequest request = pureLogicRequest();
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        LaunchTracePo unrelated = trace("unrelated", "worker", 9, 100L, 10L);
        LaunchDataCacheManager cacheManager = mock(LaunchDataCacheManager.class);
        LaunchDataCache cache = mock(LaunchDataCache.class);
        LifeCycleEventCacheManager eventManager = mock(LifeCycleEventCacheManager.class);
        when(dao.queryLaunchTraceList(eq("session"), eq(10L), eq(500L), anyList())).thenReturn(List.of(unrelated));
        when(cacheManager.getLaunchDataCache("session")).thenReturn(cache);
        when(cache.getLaunchDataLinkedList()).thenReturn(new LinkedList<>());
        when(eventManager.getLifeCycleEventCache("session")).thenReturn(Optional.empty());

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class);
             MockedStatic<LaunchDataCacheManager> cacheStatic = mockStatic(LaunchDataCacheManager.class);
             MockedStatic<LifeCycleEventCacheManager> eventStatic = mockStatic(LifeCycleEventCacheManager.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);
            cacheStatic.when(LaunchDataCacheManager::getInstance).thenReturn(cacheManager);
            eventStatic.when(LifeCycleEventCacheManager::getInstance).thenReturn(eventManager);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isTrue();
        }

        verify(cache).addLaunchDataList(List.of());
    }

    @Test
    void cacheTrace_startAbilityThenLockedSameThread_preservesInitialProc() {
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        LaunchTracePo start = trace(LaunchTraceConstants.TRACE_START_ABILITY, "ams", 3, 100L, 100L);
        LaunchTracePo locked = trace(LaunchTraceConstants.TRACE_START_ABILITY_LOCKED + " bundle", "ams", 3,
            120L, 10L);
        LaunchDataCacheManager cacheManager = mock(LaunchDataCacheManager.class);
        LaunchDataCache cache = mock(LaunchDataCache.class);
        LifeCycleEventCacheManager eventManager = mock(LifeCycleEventCacheManager.class);
        when(dao.queryLaunchTraceList(eq("session"), eq(10L), eq(500L), anyList())).thenReturn(List.of(start, locked));
        when(cacheManager.getLaunchDataCache("session")).thenReturn(cache);
        when(cache.getLaunchDataLinkedList()).thenReturn(new LinkedList<>());
        when(eventManager.getLifeCycleEventCache("session")).thenReturn(Optional.empty());

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class);
             MockedStatic<LaunchDataCacheManager> cacheStatic = mockStatic(LaunchDataCacheManager.class);
             MockedStatic<LifeCycleEventCacheManager> eventStatic = mockStatic(LifeCycleEventCacheManager.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);
            cacheStatic.when(LaunchDataCacheManager::getInstance).thenReturn(cacheManager);
            eventStatic.when(LifeCycleEventCacheManager::getInstance).thenReturn(eventManager);

            LaunchLifeCycleRequest request = pureLogicRequest();
            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isTrue();
        }

        org.mockito.ArgumentCaptor<List<LaunchData>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(cache).addLaunchDataList(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(data ->
            assertThat(data.getStartTime()).isEqualTo(100L));
    }

    @Test
    void cacheTrace_renderServiceTrace_recordsRenderTidEvenWhenNoPhaseMat() {
        LaunchLifeCycleRequest request = pureLogicRequest();
        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        LaunchTracePo render = trace("unrelated", LaunchTraceConstants.PROCESS_RENDER_SERVICE, 88, 100L, 10L);
        LaunchDataCacheManager cacheManager = mock(LaunchDataCacheManager.class);
        LaunchDataCache cache = mock(LaunchDataCache.class);
        LifeCycleEventCacheManager eventManager = mock(LifeCycleEventCacheManager.class);
        when(dao.queryLaunchTraceList(eq("session"), eq(10L), eq(500L), anyList())).thenReturn(List.of(render));
        when(cacheManager.getLaunchDataCache("session")).thenReturn(cache);
        when(cache.getLaunchDataLinkedList()).thenReturn(new LinkedList<>());
        when(eventManager.getLifeCycleEventCache("session")).thenReturn(Optional.empty());

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class);
             MockedStatic<LaunchDataCacheManager> cacheStatic = mockStatic(LaunchDataCacheManager.class);
             MockedStatic<LifeCycleEventCacheManager> eventStatic = mockStatic(LifeCycleEventCacheManager.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);
            cacheStatic.when(LaunchDataCacheManager::getInstance).thenReturn(cacheManager);
            eventStatic.when(LifeCycleEventCacheManager::getInstance).thenReturn(eventManager);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isTrue();
        }

        org.mockito.ArgumentCaptor<int[]> captor = org.mockito.ArgumentCaptor.forClass(int[].class);
        verify(cache).setThreadIdList(captor.capture());
        assertThat(captor.getValue()[2]).isEqualTo(42);
        assertThat(captor.getValue()[3]).isEqualTo(88);
    }

    @Test
    void cacheTrace_withRealAbilityFixture_appendsMappedAbilityLifeCycles() throws IOException {
        LaunchLifeCycleRequest request = pureLogicRequest();
        when(request.getStartRecordTime()).thenReturn(1_787_729_280_000L);
        when(request.getEndTime()).thenReturn(900_000_000L);

        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        String startAbilityName = LaunchTraceConstants.TRACE_START_ABILITY;
        LaunchTracePo startAbility = trace(startAbilityName, "ams", 42, 100_000_000L, 100_000_000L);
        when(dao.queryLaunchTraceList(eq("session"), eq(10L), eq(900_000_000L), anyList()))
            .thenReturn(List.of(startAbility));

        LaunchDataCacheManager cacheManager = mock(LaunchDataCacheManager.class);
        LaunchDataCache cache = mock(LaunchDataCache.class);
        when(cacheManager.getLaunchDataCache("session")).thenReturn(cache);
        when(cache.getLaunchDataLinkedList()).thenReturn(new LinkedList<>(List.of(
            new LaunchData(LaunchTraceConstants.LIFE_CYCLE_NAME_PROCESS_CREATING, 100_000_000L,
                100_000_000L, startAbilityName, 100_000_000L, 42, null))));

        LifeCycleEventCacheManager eventManager = mock(LifeCycleEventCacheManager.class);
        LifeCycleEventCache eventCache = mock(LifeCycleEventCache.class);
        when(eventManager.getLifeCycleEventCache("session")).thenReturn(Optional.of(eventCache));
        List<LifeCycleEventVo> fixtureEvents = realAbilityEvents();
        when(eventCache.queryData(eq(1_787_729_280_200L), eq(1_787_729_280_900L), eq("bundle")))
            .thenReturn(fixtureEvents);

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class);
             MockedStatic<LaunchDataCacheManager> cacheStatic = mockStatic(LaunchDataCacheManager.class);
             MockedStatic<LifeCycleEventCacheManager> eventStatic = mockStatic(LifeCycleEventCacheManager.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);
            cacheStatic.when(LaunchDataCacheManager::getInstance).thenReturn(cacheManager);
            eventStatic.when(LifeCycleEventCacheManager::getInstance).thenReturn(eventManager);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isTrue();
        }

        ArgumentCaptor<List<LaunchData>> captor = ArgumentCaptor.forClass(List.class);
        verify(cache, org.mockito.Mockito.times(2)).addLaunchDataList(captor.capture());
        List<LaunchData> abilityData = captor.getAllValues().get(1);
        assertThat(abilityData).hasSize(5);
        assertThat(abilityData).extracting(LaunchData::getLifeCycleName).contains(
            LaunchTraceConstants.LIFE_CYCLE_NAME_TERMINATE,
            LaunchTraceConstants.LIFE_CYCLE_NAME_START,
            "EntryAbility");
        assertThat(abilityData).allSatisfy(data -> assertThat(data.getThreadId()).isEqualTo(42));
    }

    @Test
    void frameRelationKey_extractsOnlyBracketedProcessAndIndex() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod(
            "getFrameRelateKey", String.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, "MarshRSTransactionData [42,7]")).isEqualTo(Optional.of("[42,7]"));
        assertThat(method.invoke(service, "MarshRSTransactionData without relation")).isEqualTo(Optional.empty());
    }

    @Test
    void sendCommands_returnsOnlyTraceContainingDataWithinCommandWindow() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod("getSendCommands",
            LaunchTracePo.class, List.class);
        method.setAccessible(true);

        LaunchTracePo data = trace("MarshRSTransactionData [42,7]", "main", 42, 120L, 20L);
        LaunchTracePo matching = trace(LaunchTraceConstants.TRACE_APP_RENDERING_SEND_COMMANDS, "main", 42, 100L, 60L);
        LaunchTracePo wrongThread = trace(LaunchTraceConstants.TRACE_APP_RENDERING_SEND_COMMANDS,
            "other", 7, 100L, 60L);

        assertThat(method.invoke(service, data, List.of(wrongThread, matching))).isEqualTo(Optional.of(matching));
        assertThat(method.invoke(service, data, List.of(wrongThread))).isEqualTo(Optional.empty());
    }

    @Test
    void msTimeConversion_usesStartRecordAndNanosecondOffset() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod("getMsTimeByNsWithStartRecord",
            Long.class, Long.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, 1_000L, 2_000_000L)).isEqualTo(1_002L);
    }

    @Test
    void searchRenderServiceTid_recordsTidWhenRenderServiceTracePresent() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod(
            "searchRenderServiceTid", LaunchTracePo.class, int[].class);
        method.setAccessible(true);

        int[] threadIdArray = new int[4];
        threadIdArray[2] = 42;

        LaunchTracePo renderService = trace("any", LaunchTraceConstants.PROCESS_RENDER_SERVICE, 99, 100L, 10L);
        boolean isRenderTidRecorded = (boolean) method.invoke(service, renderService, threadIdArray);
        assertThat(isRenderTidRecorded).isTrue();
        assertThat(threadIdArray[3]).isEqualTo(99);

        LaunchTracePo nonRender = trace("any", "otherProcess", 77, 100L, 10L);
        boolean isNonRenderTidRecorded = (boolean) method.invoke(service, nonRender, threadIdArray);
        assertThat(isNonRenderTidRecorded).isFalse();
    }

    @Test
    void isSameEvent_checksSameThreadAndTimeRange() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod(
            "isSameEvent", LaunchTracePo.class, LaunchData[].class);
        method.setAccessible(true);

        LaunchTracePo data = trace("test", "ams", 3, 120L, 10L);
        LaunchData[] launchDataArray = new LaunchData[6];
        launchDataArray[0] = new LaunchData("ProcessCreating", 100L, 50L, "startAbility", 50L, 3, null);

        boolean isSameEventFound = (boolean) method.invoke(service, data, launchDataArray);
        assertThat(isSameEventFound).isTrue();

        launchDataArray[0] = new LaunchData("ProcessCreating", 100L, 10L, "startAbility", 10L, 5, null);
        boolean isSameEventWithDiffTime = (boolean) method.invoke(service, data, launchDataArray);
        assertThat(isSameEventWithDiffTime).isFalse();

        boolean isSameEventWithEmptyArray = (boolean) method.invoke(service, data, new LaunchData[6]);
        assertThat(isSameEventWithEmptyArray).isFalse();
    }

    @Test
    void createLaunchData_mapsTraceFieldsToLaunchData() throws Exception {
        CjLaunchTraceService service = new CjLaunchTraceService();
        java.lang.reflect.Method method = CjLaunchTraceService.class.getDeclaredMethod(
            "createLaunchData", String.class, LaunchTracePo.class);
        method.setAccessible(true);

        LaunchTracePo data = trace("H:startAbility", "ams", 3, 100L, 50L);
        Object launchResult = method.invoke(service, "ProcessCreating", data);
        assertThat(launchResult).isInstanceOf(LaunchData.class);
        LaunchData result = LaunchData.class.cast(launchResult);

        assertThat(result.getLifeCycleName()).isEqualTo("ProcessCreating");
        assertThat(result.getStartTime()).isEqualTo(100L);
        assertThat(result.getThreadId()).isEqualTo(3);
        assertThat(result.getTraceName()).isEqualTo("H:startAbility");
    }

    @Test
    void cacheTrace_withRealLaunchTraceChain_cachesAllMainPhases() throws IOException {
        List<LaunchTracePo> traceChain = realLaunchTraceChain();
        LaunchLifeCycleRequest request = mock(LaunchLifeCycleRequest.class);
        when(request.getSessionId()).thenReturn("session");
        when(request.getStartTime()).thenReturn(0L);
        when(request.getEndTime()).thenReturn(10_000_000L);
        when(request.getStartRecordTime()).thenReturn(1_787_729_280_250L);
        when(request.getBundleName()).thenReturn("com.huawei.Cangjie");
        when(request.getAbilityName()).thenReturn("EntryAbility");
        when(request.getProcessId()).thenReturn(42665);

        LaunchTraceDao dao = mock(LaunchTraceDao.class);
        when(dao.queryLaunchTraceList(eq("session"), eq(0L), eq(10_000_000L), anyList()))
            .thenReturn(traceChain);
        when(dao.queryFrameSliceDuration(eq("session"), anyString(), anyInt(), anyLong()))
            .thenReturn(150_000L);

        LaunchDataCacheManager cacheManager = mock(LaunchDataCacheManager.class);
        LaunchDataCache cache = mock(LaunchDataCache.class);
        when(cacheManager.getLaunchDataCache("session")).thenReturn(cache);
        when(cache.getLaunchDataLinkedList()).thenReturn(new LinkedList<>());

        LifeCycleEventCacheManager eventManager = mock(LifeCycleEventCacheManager.class);
        when(eventManager.getLifeCycleEventCache("session")).thenReturn(Optional.empty());

        try (MockedStatic<LaunchTraceDao> daoStatic = mockStatic(LaunchTraceDao.class);
             MockedStatic<LaunchDataCacheManager> cacheStatic = mockStatic(LaunchDataCacheManager.class);
             MockedStatic<LifeCycleEventCacheManager> eventStatic = mockStatic(LifeCycleEventCacheManager.class)) {
            daoStatic.when(LaunchTraceDao::getInstance).thenReturn(dao);
            cacheStatic.when(LaunchDataCacheManager::getInstance).thenReturn(cacheManager);
            eventStatic.when(LifeCycleEventCacheManager::getInstance).thenReturn(eventManager);

            assertThat(new CjLaunchTraceService().cacheCjLaunchLifeCycleTrace(request)).isTrue();
        }

        ArgumentCaptor<List<LaunchData>> captor = ArgumentCaptor.forClass(List.class);
        verify(cache).addLaunchDataList(captor.capture());
        List<LaunchData> mainPhases = captor.getValue();
        assertThat(mainPhases).isNotEmpty();
        assertThat(mainPhases).extracting(LaunchData::getLifeCycleName).contains(
            LaunchTraceConstants.LIFE_CYCLE_NAME_PROCESS_CREATING,
            LaunchTraceConstants.LIFE_CYCLE_NAME_APPLICATION_LAUNCHING,
            LaunchTraceConstants.LIFE_CYCLE_NAME_UI_ABILITY_LAUNCHING,
            LaunchTraceConstants.LIFE_CYCLE_NAME_UI_ABILITY_ON_FOREGROUND);
    }

    private static List<LaunchTracePo> realLaunchTraceChain() throws IOException {
        try (InputStream input = CjLaunchTraceServiceTest.class.getClassLoader()
            .getResourceAsStream("fixtures/real-launch-trace-chain.json")) {
            assertThat(input).as("real launch trace chain fixture").isNotNull();
            JSONArray records = JSON.parseArray(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            List<LaunchTracePo> traces = new ArrayList<>();
            for (int index = 0; index < records.size(); index++) {
                JSONObject record = records.getJSONObject(index);
                traces.add(new LaunchTracePo(
                    record.getInteger("threadId"),
                    record.getInteger("threadId"),
                    record.getString("threadName"),
                    record.getString("name"),
                    record.getLong("startTime"),
                    record.getLong("duration")));
            }
            return traces;
        }
    }

    private static List<LifeCycleEventVo> realAbilityEvents() throws IOException {
        try (InputStream input = CjLaunchTraceServiceTest.class.getClassLoader()
            .getResourceAsStream("fixtures/real-launch-ability-data.json")) {
            assertThat(input).as("real launch ability fixture").isNotNull();
            JSONArray records = JSON.parseArray(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            List<LifeCycleEventVo> events = new ArrayList<>();
            for (int index = 0; index < records.size(); index++) {
                JSONObject record = records.getJSONObject(index);
                events.add(new LifeCycleEventVo(record.getLong("timestamp"), record.getString("name"),
                    record.getString("abilityName")));
            }
            return events;
        }
    }

    private static LaunchLifeCycleRequest branchRequest(String sessionId) {
        LaunchLifeCycleRequest request = mock(LaunchLifeCycleRequest.class);
        when(request.getSessionId()).thenReturn(sessionId);
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(20L);
        return request;
    }

    private static LaunchLifeCycleRequest pureLogicRequest() {
        LaunchLifeCycleRequest request = mock(LaunchLifeCycleRequest.class);
        when(request.getSessionId()).thenReturn("session");
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(500L);
        when(request.getStartRecordTime()).thenReturn(1_000L);
        when(request.getBundleName()).thenReturn("bundle");
        when(request.getAbilityName()).thenReturn("EntryAbility");
        when(request.getProcessId()).thenReturn(42);
        return request;
    }

    private static LaunchTracePo trace(String name, String threadName, int threadId, long start, long duration) {
        LaunchTracePo trace = mock(LaunchTracePo.class);
        when(trace.getName()).thenReturn(name);
        when(trace.getThreadName()).thenReturn(threadName);
        when(trace.getThreadId()).thenReturn(threadId);
        when(trace.getStartTime()).thenReturn(start);
        when(trace.getDuration()).thenReturn(duration);
        return trace;
    }
}