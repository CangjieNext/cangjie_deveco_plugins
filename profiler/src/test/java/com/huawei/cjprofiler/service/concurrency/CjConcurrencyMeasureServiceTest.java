/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.common.constant.CjConcurrencyConstants;
import com.huawei.cjprofiler.dao.CjThreadDao;
import com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo;
import com.huawei.cjprofiler.model.vo.concurrency.MeasureDetailVo;
import com.huawei.cjprofiler.service.CjThreadState;
import com.huawei.deveco.insight.ohos.model.dto.request.concurrency.QueryConcurrencyMeasureRequest;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

/**
 * Unit tests for {@code CjConcurrencyMeasureService} concurrency measure queries.
 *
 * @since 2026-08-14
 */
class CjConcurrencyMeasureServiceTest {
    @Test
    void getInstance_returnsSingleton() {
        assertThat(CjConcurrencyMeasureService.getInstance()).isSameAs(CjConcurrencyMeasureService.getInstance());
    }

    @Test
    void queryConcurrencyMeasureList_processIdsEmpty_returnsEmpty() {
        QueryConcurrencyMeasureRequest request = request("CJThread", "Running", List.of());
        CjThreadDao dao = mock(CjThreadDao.class);
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjConcurrencyMeasureService.getInstance().queryConcurrencyMeasureList(request)).isEmpty();

            verify(dao, never()).queryCjThreadList("session", 10L, 30L);
        }
    }

    @Test
    void queryConcurrencyMeasureList_unitInvalid_returnsEmpty() {
        QueryConcurrencyMeasureRequest request = request("FFRT", "Running", List.of(1L));
        CjThreadDao dao = mock(CjThreadDao.class);
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjConcurrencyMeasureService.getInstance().queryConcurrencyMeasureList(request)).isEmpty();

            verify(dao, never()).queryCjThreadList("session", 10L, 30L);
        }
    }

    @Test
    void queryStateMeasure_slicesEmpty_returnsEmptyAndDelegatesState() {
        QueryConcurrencyMeasureRequest request = request(CjConcurrencyConstants.CONCURRENCY_CJ_THREAD,
            CjThreadState.RUNNING.getKey(), List.of(7L));
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadList("session", 10L, 30L)).thenReturn(List.of());
        when(dao.queryCjThreadLaneList("session", CjThreadState.RUNNING.getValue(), 10L, 30L, List.of(7L)))
            .thenReturn(List.of());
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjConcurrencyMeasureService.getInstance().queryConcurrencyMeasureList(request)).isEmpty();

            verify(dao).queryCjThreadLaneList("session", CjThreadState.RUNNING.getValue(), 10L, 30L, List.of(7L));
        }
    }

    @Test
    void queryStateMeasure_aggregatesDurationsAndBuildsJumpDetails() {
        QueryConcurrencyMeasureRequest request = request(CjConcurrencyConstants.CONCURRENCY_CJ_THREAD,
            CjThreadState.RUNNING.getKey(), List.of(7L));
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadList("session", 10L, 30L)).thenReturn(List.of(
            CjThreadInfoVo.builder().cjThreadId(1).cjThreadName("worker-1").build(),
            CjThreadInfoVo.builder().cjThreadId(2).cjThreadName("worker-2").build()));
        when(dao.queryCjThreadLaneList("session", CjThreadState.RUNNING.getValue(), 10L, 30L, List.of(7L)))
            .thenReturn(List.of(slice(1, 5), slice(1, 15), slice(2, 8)));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<MeasureDetailVo> result = CjConcurrencyMeasureService.getInstance()
                .queryConcurrencyMeasureList(request);

            assertThat(result).hasSize(2);
            assertThat(findByThreadId(result, 1)).satisfies(detail -> {
                assertThat(detail.getType()).isEqualTo(CjConcurrencyConstants.CONCURRENCY_NAME_TYPE_CJ_THREAD);
                assertThat(detail.getName()).isEqualTo("worker-1");
                assertThat(detail.getTotalDuration()).isEqualTo(20L);
                assertThat(detail.getAvgDuration()).isEqualTo(10L);
                assertThat(detail.getMinDuration()).isEqualTo(5L);
                assertThat(detail.getMaxDuration()).isEqualTo(15L);
                assertThat(detail.getRelatedTaskDetail().getId()).isEqualTo(1);
                assertThat(detail.getRelatedTaskDetail().getUnitName())
                    .isEqualTo(CjConcurrencyConstants.CONCURRENCY_CJ_THREAD);
            });
            assertThat(findByThreadId(result, 2).getTotalDuration()).isEqualTo(8L);
        }
    }

    @Test
    void queryTotalMeasure_groupsKnownStatesAndBuildsStateSummary() {
        QueryConcurrencyMeasureRequest request = request(CjConcurrencyConstants.CONCURRENCY_CJ_THREAD,
            CjThreadState.TOTAL.getKey(), List.of(9L));
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadList("session", 10L, 30L)).thenReturn(List.of(
            CjThreadInfoVo.builder().cjThreadId(3).cjThreadName("worker-3").build()));
        when(dao.queryCjThreadTotalLaneList("session", 10L, 30L, List.of(9L))).thenReturn(List.of(
            slice(3, 4, CjThreadState.READY.getValue()),
            slice(3, 10, CjThreadState.READY.getValue()),
            slice(3, 20, "Unknown state")));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<MeasureDetailVo> result = CjConcurrencyMeasureService.getInstance()
                .queryConcurrencyMeasureList(request);

            assertThat(result).singleElement().satisfies(detail -> {
                assertThat(detail.getType()).isEqualTo(CjConcurrencyConstants.CONCURRENCY_NAME_TYPE_STATE);
                assertThat(detail.getName()).isEqualTo(CjThreadState.READY.getKey());
                assertThat(detail.getTotalDuration()).isEqualTo(14L);
                assertThat(detail.getAvgDuration()).isEqualTo(7L);
                assertThat(detail.getMinDuration()).isEqualTo(4L);
                assertThat(detail.getMaxDuration()).isEqualTo(10L);
                assertThat(detail.getChildren()).hasSize(2);
            });
            verify(dao).queryCjThreadTotalLaneList("session", 10L, 30L, List.of(9L));
            verify(dao, never()).queryCjThreadLaneList("session", "", 10L, 30L, List.of(9L));
        }
    }

    private static QueryConcurrencyMeasureRequest request(String unit, String measure, List<Long> processIds) {
        QueryConcurrencyMeasureRequest request = mock(QueryConcurrencyMeasureRequest.class);
        when(request.getUnitName()).thenReturn(unit);
        when(request.getMeasureName()).thenReturn(measure);
        when(request.getProcessIdList()).thenReturn(processIds);
        when(request.getSessionId()).thenReturn("session");
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(30L);
        return request;
    }

    private static CjThreadSliceInfoPo slice(int threadId, long duration) {
        return slice(threadId, duration, CjThreadState.RUNNING.getValue());
    }

    private static CjThreadSliceInfoPo slice(int threadId, long duration, String state) {
        return CjThreadSliceInfoPo.builder().cjThreadId(threadId).duration(duration).state(state).build();
    }

    private static MeasureDetailVo findByThreadId(List<MeasureDetailVo> details, int threadId) {
        return details.stream().filter(detail -> detail.getCjThreadId() == threadId).findFirst().orElseThrow();
    }
}
