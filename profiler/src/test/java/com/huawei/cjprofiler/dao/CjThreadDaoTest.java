/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.ability.database.manager.CjMapperManager;
import com.huawei.cjprofiler.dao.mapper.CjThreadMapper;
import com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo;
import com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo;
import com.huawei.deveco.insight.ohos.ability.database.TraceDatabase;
import com.huawei.deveco.insight.ohos.ability.database.manager.DatabaseManager;
import com.huawei.deveco.insight.ohos.common.enums.DatabaseType;
import com.huawei.deveco.insight.ohos.model.vo.concurrency.ProcessListInfoVo;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjThreadDao} thread query methods.
 *
 * @since 2026-08-14
 */
class CjThreadDaoTest {
    private static final String SESSION_ID = "thread-session";
    private static final long OFFSET = 100L;
    private static final long START = 10L;
    private static final long END = 20L;

    @Test
    void getInstanceAndRelatedMapper_returnExpectedValues() {
        assertThat(CjThreadDao.getInstance()).isSameAs(CjThreadDao.getInstance());
        assertThat(CjThreadDao.getInstance().relatedMapper()).isEqualTo(CjThreadMapper.class);
    }

    @Test
    void queryMethods_delegateAllArgumentsAndCloseEachSession() {
        SqlSession sqlSession = mock(SqlSession.class);
        CjMapperManager mapperManager = mock(CjMapperManager.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        TraceDatabase traceDatabase = mock(TraceDatabase.class);
        CjThreadSliceInfoPo slice = mock(CjThreadSliceInfoPo.class);
        CjThreadInfoVo thread = mock(CjThreadInfoVo.class);
        List<Long> processIds = List.of(7L, 8L);
        CjThreadMapper mapper = mock(CjThreadMapper.class);
        when(mapperManager.getDatabaseTypeByMapper(CjThreadMapper.class)).thenReturn(DatabaseType.TRACE);
        when(databaseManager.getSqlSession(SESSION_ID, DatabaseType.TRACE)).thenReturn(Optional.of(sqlSession));
        when(traceDatabase.getAbsoluteOffsetBySessionId(SESSION_ID)).thenReturn(OFFSET);
        when(sqlSession.getMapper(CjThreadMapper.class)).thenReturn(mapper);
        when(mapper.queryCjThreadState(OFFSET, START, END)).thenReturn(List.of("RUNNING"));
        when(mapper.queryCjThreadSliceInfo(3, OFFSET, START, END)).thenReturn(List.of(slice));
        when(mapper.queryCjThreadList(OFFSET, START, END)).thenReturn(List.of(thread));
        when(mapper.queryCjThreadLaneList(OFFSET, "WAITING", START, END, processIds)).thenReturn(List.of(slice));
        when(mapper.queryCjThreadTotalLaneList(OFFSET, START, END, processIds)).thenReturn(List.of(slice));
        ProcessListInfoVo process = mock(ProcessListInfoVo.class);
        when(mapper.queryCjThreadProcessList(START, END, OFFSET)).thenReturn(List.of(process));

        try (MockedStatic<CjMapperManager> mapperStatic = mockStatic(CjMapperManager.class);
             MockedStatic<DatabaseManager> databaseStatic = mockStatic(DatabaseManager.class);
             MockedStatic<TraceDatabase> traceStatic = mockStatic(TraceDatabase.class)) {
            mapperStatic.when(CjMapperManager::getInstance).thenReturn(mapperManager);
            databaseStatic.when(DatabaseManager::getInstance).thenReturn(databaseManager);
            traceStatic.when(TraceDatabase::getInstance).thenReturn(traceDatabase);
            CjThreadDao dao = CjThreadDao.getInstance();

            assertThat(dao.queryCjThreadState(SESSION_ID, START, END)).containsExactly("RUNNING");
            assertThat(dao.queryCjThreadSliceInfo(SESSION_ID, 3, START, END)).containsExactly(slice);
            assertThat(dao.queryCjThreadList(SESSION_ID, START, END)).containsExactly(thread);
            assertThat(dao.queryCjThreadLaneList(SESSION_ID, "WAITING", START, END, processIds))
                .containsExactly(slice);
            assertThat(dao.queryCjThreadTotalLaneList(SESSION_ID, START, END, processIds)).containsExactly(slice);
            assertThat(dao.queryCjThreadProcessList(SESSION_ID, START, END)).containsExactly(process);
        }

        verify(mapper).queryCjThreadState(OFFSET, START, END);
        verify(mapper).queryCjThreadSliceInfo(3, OFFSET, START, END);
        verify(mapper).queryCjThreadList(OFFSET, START, END);
        verify(mapper).queryCjThreadLaneList(OFFSET, "WAITING", START, END, processIds);
        verify(mapper).queryCjThreadTotalLaneList(OFFSET, START, END, processIds);
        verify(mapper).queryCjThreadProcessList(START, END, OFFSET);
        verify(sqlSession, org.mockito.Mockito.times(6)).close();
    }
}
