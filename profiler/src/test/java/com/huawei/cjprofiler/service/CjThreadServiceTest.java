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
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjThreadDao;
import com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo;
import com.huawei.cjprofiler.model.vo.CjLaneSliceVo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadSliceInfoVo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

/**
 * Unit tests for {@link CjThreadService} query and lane-slice merge logic.
 *
 * @since 2026-06-04
 */
public class CjThreadServiceTest {
    private static final String SESSION_ID = "svc-session";
    private static final long START = 100L;
    private static final long END = 200L;

    private CjThreadSliceInfoPo buildSlice(int threadId, int processId, int cjThreadId, String state,
        long... range) {
        CjThreadSliceInfoPo po = new CjThreadSliceInfoPo();
        po.setThreadId(threadId);
        po.setProcessId(processId);
        po.setCjThreadId(cjThreadId);
        po.setState(state);
        po.setStartTime(range[0]);
        po.setEndTime(range[1]);
        po.setDuration(range[1] - range[0]);
        return po;
    }

    @Test
    public void testGetInstanceReturnsSingleton() {
        assertThat(CjThreadService.getInstance()).isSameAs(CjThreadService.getInstance());
    }

    @Test
    public void queryCjStateMetadata_mapsValuesToKeysAndAddsTotal() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadState(SESSION_ID, START, END))
            .thenReturn(List.of("Execute RUNNING", "Exit IDLE", "Unknown"));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjThreadService.getInstance().queryCjStateMetadata(SESSION_ID, START, END))
                .containsExactly("Total", "Running", "Idle");
        }
    }

    @Test
    public void queryCjStateMetadata_returnsEmptyWhenNoStateMatched() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadState(SESSION_ID, START, END)).thenReturn(List.of("Unknown"));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjThreadService.getInstance().queryCjStateMetadata(SESSION_ID, START, END)).isEmpty();
        }
    }

    @Test
    public void queryCjThreadSliceInfo_mapsPoToVoWithThreadDetail() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadSliceInfo(SESSION_ID, 9, START, END))
            .thenReturn(List.of(buildSlice(11, 22, 9, "Execute RUNNING", START, END)));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<CjThreadSliceInfoVo> result = CjThreadService.getInstance()
                .queryCjThreadSliceInfo(SESSION_ID, 9, START, END);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getState()).isEqualTo("Running");
            assertThat(result.get(0).getRelateThreadDetail().getThreadId()).isEqualTo(11);
            assertThat(result.get(0).getRelateThreadDetail().getProcessId()).isEqualTo(22);
        }
    }

    @Test
    public void queryCjThreadSliceInfo_returnsEmptyForMissingThread() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadSliceInfo(SESSION_ID, null, START, END)).thenReturn(List.of());
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjThreadService.getInstance().queryCjThreadSliceInfo(SESSION_ID, null, START, END))
                .isEmpty();
        }
    }

    @Test
    public void queryCjThreadList_delegatesToDao() {
        CjThreadDao dao = mock(CjThreadDao.class);
        CjThreadInfoVo vo = mock(CjThreadInfoVo.class);
        when(dao.queryCjThreadList(SESSION_ID, START, END)).thenReturn(List.of(vo));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjThreadService.getInstance().queryCjThreadList(SESSION_ID, START, END))
                .containsExactly(vo);
        }
    }

    @Test
    public void queryCjThreadLaneList_returnsEmptyWhenNoProcessId() {
        CjThreadDao dao = mock(CjThreadDao.class);
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            assertThat(CjThreadService.getInstance()
                .queryCjThreadLaneList(SESSION_ID, "Running", START, END, List.of())).isEmpty();
        }
    }

    @Test
    public void queryCjThreadLaneList_mergesTotalLaneWithOverlappingSlices() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadTotalLaneList(SESSION_ID, START, END, List.of(1L, 2L))).thenReturn(List.of(
            buildSlice(1, 1, 1, "Execute RUNNING", 100L, 120L),
            buildSlice(1, 1, 1, "Park PENDING", 110L, 150L),
            buildSlice(2, 2, 2, "Exit IDLE", 130L, 140L)));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<CjLaneSliceVo> lane = CjThreadService.getInstance()
                .queryCjThreadLaneList(SESSION_ID, "Total", START, END, List.of(1L, 2L));
            assertThat(lane).hasSize(6);
            assertThat(lane.get(0).getValue()).isEqualTo(1);
            assertThat(lane.get(3).getValue()).isEqualTo(2);
            assertThat(lane.get(5).getValue()).isEqualTo(0);
        }
    }

    @Test
    public void queryCjThreadLaneList_resolvesStateValueForKey() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadLaneList(SESSION_ID, "New READY", START, END, List.of(3L)))
            .thenReturn(List.of(buildSlice(1, 1, 1, "New READY", 100L, 110L)));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<CjLaneSliceVo> lane = CjThreadService.getInstance()
                .queryCjThreadLaneList(SESSION_ID, "Ready", START, END, List.of(3L));
            assertThat(lane).hasSize(2);
        }
    }

    @Test
    public void queryCjThreadLaneList_fallsBackToEmptyValueForUnknownKey() {
        CjThreadDao dao = mock(CjThreadDao.class);
        when(dao.queryCjThreadLaneList(SESSION_ID, "", START, END, List.of(3L)))
            .thenReturn(List.of(buildSlice(1, 1, 1, "Park PENDING", 100L, 110L)));
        try (MockedStatic<CjThreadDao> daoStatic = mockStatic(CjThreadDao.class)) {
            daoStatic.when(CjThreadDao::getInstance).thenReturn(dao);

            List<CjLaneSliceVo> lane = CjThreadService.getInstance()
                .queryCjThreadLaneList(SESSION_ID, "Missing", START, END, List.of(3L));
            assertThat(lane).hasSize(2);
        }
    }
}
