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
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkAllHeapUsageRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.vo.ArkHeapUsagesSummary;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjHeapUsageService} branch behaviors.
 *
 * @since 2026-08-14
 */
class CjHeapUsageServiceBranchTest {
    @Test
    void queryHeapUsage_whenLiveRecordServiceExists_usesLiveDataWithoutDa() {
        CommonQueryRequest request = commonRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjRecordService recordService = mock(CjRecordService.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        HeapUsages liveUsage = usage(4L);
        when(manager.getCjRecordService(7)).thenReturn(Optional.of(recordService));
        when(recordService.queryCjHeapUsages(request)).thenReturn(List.of(liveUsage));

        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(CjHeapUsageService.getInstance().queryHeapUsage(request)).containsExactly(liveUsage);
        }

        verify(dao, never()).selectCjHeapUsages(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void queryHeapUsage_whenRecordServiceMissing_readsDatabase() {
        CommonQueryRequest request = commonRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        HeapUsages storedUsage = usage(8L);
        when(manager.getCjRecordService(7)).thenReturn(Optional.empty());
        when(dao.selectCjHeapUsages("session", 7, 10L, 20L)).thenReturn(List.of(storedUsage));

        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(CjHeapUsageService.getInstance().queryHeapUsage(request)).containsExactly(storedUsage);
        }
    }

    @Test
    void queryAllHeapUsage_convertsRequestAndUsesPidAsTid() {
        ArkAllHeapUsageRequest request = allRequest();
        CjRecordManager manager = mock(CjRecordManager.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        HeapUsages usage = usage(3L);
        when(manager.getCjRecordService(99)).thenReturn(Optional.empty());
        when(dao.selectCjHeapUsages("session", 99, 10L, 20L)).thenReturn(List.of(usage));

        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class);
             MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(CjHeapUsageService.getInstance().queryAllHeapUsage(request)).containsExactly(usage);
        }

        verify(manager).getCjRecordService(99);
        verify(dao).selectCjHeapUsages("session", 99, 10L, 20L);
    }

    @Test
    void queryAllHeapUsagesDetail_whenDaoReturnsNull_returnsZeroSummary() {
        ArkAllHeapUsageRequest request = allRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.selectCjHeapUsages("session", 99, 10L, 20L)).thenReturn(null);

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            ArkHeapUsagesSummary summary = CjHeapUsageService.getInstance()
                .queryAllHeapUsagesDetail(request).get(0);

            assertThat(summary.getTid()).isEqualTo(99);
            assertThat(summary.getAvg()).isZero();
            assertThat(summary.getPeak()).isZero();
            assertThat(summary.getValley()).isZero();
        }
    }

    @Test
    void queryAllHeapUsagesDetail_whenDaoReturnsEmpty_returnsZeroSummary() {
        ArkAllHeapUsageRequest request = allRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.selectCjHeapUsages("session", 99, 10L, 20L)).thenReturn(List.of());

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            ArkHeapUsagesSummary summary = CjHeapUsageService.getInstance()
                .queryAllHeapUsagesDetail(request).get(0);

            assertThat(summary.getAvg()).isZero();
            assertThat(summary.getPeak()).isZero();
            assertThat(summary.getValley()).isZero();
        }
    }

    @Test
    void queryAllHeapUsagesDetail_calculatesAveragePeakAndValley() {
        ArkAllHeapUsageRequest request = allRequest();
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        when(dao.selectCjHeapUsages("session", 99, 10L, 20L))
            .thenReturn(List.of(usage(7L), usage(1L), usage(4L)));

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            ArkHeapUsagesSummary summary = CjHeapUsageService.getInstance()
                .queryAllHeapUsagesDetail(request).get(0);

            assertThat(summary.getTid()).isEqualTo(99);
            assertThat(summary.getAvg()).isEqualTo(4D);
            assertThat(summary.getPeak()).isEqualTo(7D);
            assertThat(summary.getValley()).isEqualTo(1D);
        }
    }

    private static CommonQueryRequest commonRequest() {
        CommonQueryRequest request = mock(CommonQueryRequest.class);
        when(request.getSessionId()).thenReturn("session");
        when(request.getTid()).thenReturn(7);
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(20L);
        return request;
    }

    private static ArkAllHeapUsageRequest allRequest() {
        ArkAllHeapUsageRequest request = mock(ArkAllHeapUsageRequest.class);
        when(request.getSessionId()).thenReturn("session");
        when(request.getPid()).thenReturn(99);
        when(request.getStartRecordTime()).thenReturn(1_000L);
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(20L);
        return request;
    }

    private static HeapUsages usage(long usedSize) {
        return new HeapUsages((double) usedSize, (double) usedSize * 2, 1L);
    }
}
