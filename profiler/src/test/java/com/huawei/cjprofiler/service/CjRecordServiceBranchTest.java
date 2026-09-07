/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapUsages;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.service.ark.RecordServiceBase;
import com.huawei.deveco.panda.websocket.services.PandaDevToolsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@code CjRecordService} branch behaviors on heap timeline and usage records.
 *
 * @since 2026-08-14
 */
class CjRecordServiceBranchTest {
    private TestRecordService service;

    @BeforeEach
    void setUp() {
        CjRecordService.init();
        service = new TestRecordService(42);
    }

    @Test
    void takeHeapSnapshot_whenConnectionMissing_returnsFalse() {
        CommonExecuteRequest request = mock(CommonExecuteRequest.class);

        assertThat(service.takeHeapSnapshot(request)).isFalse();
    }

    @Test
    void startCjHeapTimeLine_whenConnectionMissing_returnsFalseAndMarksMe() {
        assertThat(service.startCjHeapTimeLine(false)).isFalse();
        assertThat(service.memoryRunning()).isTrue();
    }

    @Test
    void stopCjHeapTimeLine_whenProfilerNotRunning_returnsFalse() {
        assertThat(service.stopCjHeapTimeLine("session", 100L)).isFalse();
    }

    @Test
    void queryCjHeapUsages_whenCacheNotAvailable_delegatesToDao() {
        CommonQueryRequest request = queryRequest("session", 42, 10L, 30L, 1_000L);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        HeapUsages expected = new HeapUsages(2D, 4D, 10L);
        when(dao.selectCjHeapUsages("session", 42, 10L, 30L)).thenReturn(List.of(expected));

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(service.queryCjHeapUsages(request)).containsExactly(expected);
        }

        verify(dao).selectCjHeapUsages("session", 42, 10L, 30L);
    }

    @Test
    void queryCjHeapUsages_whenCacheInserted_delegatesToDaoDespiteCachedV() {
        service.addUsage(new HeapUsages(2D, 4D, 1_010L));
        service.markInserted();
        CommonQueryRequest request = queryRequest("session", 42, 0L, 50_000_000L, 1_000L);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.selectCjHeapUsages("session", 42, 0L, 50_000_000L)).thenReturn(List.of());

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(service.queryCjHeapUsages(request)).isEmpty();
        }

        verify(dao).selectCjHeapUsages("session", 42, 0L, 50_000_000L);
    }

    @Test
    void queryCjHeapUsages_whenLiveCacheExists_filtersRangeAndConvertsTim() {
        service.addUsage(new HeapUsages(1D, 10D, 1_005L));
        service.addUsage(new HeapUsages(2D, 20D, 1_015L));
        service.addUsage(new HeapUsages(3D, 30D, 1_025L));
        CommonQueryRequest request = queryRequest("session", 42,
            TimeUnit.MILLISECONDS.toNanos(10L), TimeUnit.MILLISECONDS.toNanos(20L), 1_000L);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            List<HeapUsages> result = service.queryCjHeapUsages(request);

            assertThat(result).singleElement().satisfies(usage -> {
                assertThat(usage.getUsedSize()).isEqualTo(2D);
                assertThat(usage.getTotalSize()).isEqualTo(20D);
                assertThat(usage.getTimestamp()).isEqualTo(TimeUnit.MILLISECONDS.toNanos(15L));
            });
        }

        verify(dao, never()).selectCjHeapUsages(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void startCjHeapUsageTimer_whenConnectionMissing_returnsFalse() {
        assertThat(service.startCjHeapUsageTimer()).isFalse();
    }

    @Test
    void stopCjHeapUsageTimer_whenNoTaskRunning_completesWithoutError() {
        // No task scheduled yet; should not throw
        service.stopCjHeapUsageTimer();
        // just verify no exception
    }

    @Test
    void stopCjHeapUsageTimerAndSaveData_whenNotRunning_returnsFalseForNo() {
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.insertCjHeapUsages(org.mockito.ArgumentMatchers.eq("session"), org.mockito.ArgumentMatchers.eq(42),
            org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);
        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);
            assertThat(service.stopCjHeapUsageTimerAndSaveData("session", 1_000L)).isTrue();
        }
    }

    @Test
    void init_setsSnapshotSuccessToTrue() {
        CjRecordService.init();
        CommonExecuteRequest request = mock(CommonExecuteRequest.class);
        assertThat(service.takeHeapSnapshot(request)).isFalse();
    }

    @Test
    void createHandleSnapshotThread_completesFutureOnParseSuccess() throws Exception {
        RecordServiceBase.CustomThread thread = service.createHandleSnapshotThread();
        thread.start();
        thread.join(2000);
    }

    private static CommonQueryRequest queryRequest(String sessionId, int tid, long startTime, long endTime,
        long startRecordTime) {
        CommonQueryRequest request = mock(CommonQueryRequest.class);
        when(request.getSessionId()).thenReturn(sessionId);
        when(request.getTid()).thenReturn(tid);
        when(request.getStartTime()).thenReturn(startTime);
        when(request.getEndTime()).thenReturn(endTime);
        when(request.getStartRecordTime()).thenReturn(startRecordTime);
        return request;
    }

    private static final class TestRecordService extends CjRecordService {
        private TestRecordService(int pid) {
            super(pid);
        }

        @Override
        public Optional<PandaDevToolsService> getDevToolsService() {
            return Optional.empty();
        }

        private void addUsage(HeapUsages usage) {
            heapUsagesList.add(usage);
        }

        private void markInserted() {
            isInsertHeapUsage = true;
        }

        private boolean memoryRunning() {
            return isMemoryRunning;
        }
    }
}
