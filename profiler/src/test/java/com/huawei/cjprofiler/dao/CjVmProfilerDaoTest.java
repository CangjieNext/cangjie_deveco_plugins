/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.ability.database.manager.CjMapperManager;
import com.huawei.cjprofiler.dao.mapper.CjVmProfilerMapper;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapSnapshot;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapStats;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapUsages;
import com.huawei.deveco.insight.ohos.ability.database.manager.DatabaseManager;
import com.huawei.deveco.insight.ohos.common.enums.DatabaseType;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Unit tests for {@code CjVmProfilerDao} heap stats and usages persistence.
 *
 * @since 2026-08-14
 */
class CjVmProfilerDaoTest {
    private static final String SESSION_ID = "vm-session";

    @Test
    void getInstanceAndRelatedMapper_returnExpectedValues() {
        assertThat(CjVmProfilerDao.getInstance()).isSameAs(CjVmProfilerDao.getInstance());
        assertThat(CjVmProfilerDao.getInstance().relatedMapper()).isEqualTo(CjVmProfilerMapper.class);
    }

    @Test
    void selectMethods_delegateAndCloseSession() {
        Fixture fixture = new Fixture();
        HeapUsages usage = mock(HeapUsages.class);
        HeapStats stats = mock(HeapStats.class);
        HeapSnapshot dump = mock(HeapSnapshot.class);
        when(fixture.mapper.selectCjHeapUsages(9, 10L, 20L)).thenReturn(List.of(usage));
        when(fixture.mapper.selectCjHeapStats(9, 10L, 20L)).thenReturn(List.of(stats));
        when(fixture.mapper.selectCjHeapDumps(10L, 20L)).thenReturn(List.of(dump));

        try (MockedStatic<CjMapperManager> mapperStatic = fixture.mockMapperManager();
             MockedStatic<DatabaseManager> databaseStatic = fixture.mockDatabaseManager()) {
            CjVmProfilerDao dao = CjVmProfilerDao.getInstance();
            assertThat(dao.selectCjHeapUsages(SESSION_ID, 9, 10L, 20L)).containsExactly(usage);
            assertThat(dao.selectCjHeapStats(SESSION_ID, 9, 10L, 20L)).containsExactly(stats);
            assertThat(dao.selectCjHeapDumps(SESSION_ID, 10L, 20L)).containsExactly(dump);
        }

        verify(fixture.session, org.mockito.Mockito.times(3)).close();
    }

    @Test
    void insertEmptyCollections_returnsTrueWithoutDatabaseOrLockInteracti() {
        ReadWriteLock lock = mock(ReadWriteLock.class);

        assertThat(CjVmProfilerDao.getInstance().insertCjHeapUsages(SESSION_ID, 1, List.of(), lock)).isTrue();
        assertThat(CjVmProfilerDao.getInstance().insertCjHeapStats(
            SESSION_ID, 1, new LinkedHashMap<>(), lock)).isTrue();
        assertThat(CjVmProfilerDao.getInstance().insertCjHeapDumps(SESSION_ID, List.of())).isTrue();

        verify(lock, never()).readLock();
        verify(lock, never()).writeLock();
    }

    @Test
    void insertHeapUsages_aggregatesBatchResultsCommitsClearsCacheAndUnlo() {
        Fixture fixture = new Fixture();
        ReadWriteLock readWriteLock = mock(ReadWriteLock.class);
        Lock writeLock = mock(Lock.class);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        List<HeapUsages> usages = java.util.stream.IntStream.range(0, 10_001)
            .mapToObj(index -> mock(HeapUsages.class))
            .toList();
        when(fixture.mapper.insertCjHeapUsages(5, usages.subList(0, 10_000))).thenReturn(true);
        when(fixture.mapper.insertCjHeapUsages(5, usages.subList(10_000, 10_001))).thenReturn(false);

        try (MockedStatic<CjMapperManager> mapperStatic = fixture.mockMapperManager();
             MockedStatic<DatabaseManager> databaseStatic = fixture.mockDatabaseManager()) {
            assertThat(CjVmProfilerDao.getInstance().insertCjHeapUsages(
                SESSION_ID, 5, usages, readWriteLock)).isFalse();
        }

        verify(fixture.mapper).insertCjHeapUsages(5, usages.subList(0, 10_000));
        verify(fixture.mapper).insertCjHeapUsages(5, usages.subList(10_000, 10_001));
        verify(fixture.session, org.mockito.Mockito.times(2)).commit();
        verify(fixture.session, org.mockito.Mockito.times(2)).clearCache();
        InOrder order = inOrder(writeLock, fixture.session);
        order.verify(writeLock).lock();
        order.verify(fixture.session).close();
        order.verify(writeLock).unlock();
    }

    @Test
    void insertHeapStats_usesReadLockAndPersistsMapValuesInInsertionOrder() {
        Fixture fixture = new Fixture();
        ReadWriteLock readWriteLock = mock(ReadWriteLock.class);
        Lock readLock = mock(Lock.class);
        when(readWriteLock.readLock()).thenReturn(readLock);
        HeapStats first = mock(HeapStats.class);
        HeapStats second = mock(HeapStats.class);
        LinkedHashMap<Long, HeapStats> stats = new LinkedHashMap<>();
        stats.put(2L, first);
        stats.put(1L, second);
        when(fixture.mapper.insertCjHeapStats(6, List.of(first, second))).thenReturn(true);

        try (MockedStatic<CjMapperManager> mapperStatic = fixture.mockMapperManager();
             MockedStatic<DatabaseManager> databaseStatic = fixture.mockDatabaseManager()) {
            assertThat(CjVmProfilerDao.getInstance().insertCjHeapStats(SESSION_ID, 6, stats, readWriteLock)).isTrue();
        }

        verify(fixture.mapper).insertCjHeapStats(6, List.of(first, second));
        verify(fixture.session).commit();
        verify(fixture.session).clearCache();
        verify(readLock).lock();
        verify(readLock).unlock();
    }

    @Test
    void insertHeapDumps_delegatesCommitsAndReturnsMapperResult() {
        Fixture fixture = new Fixture();
        HeapSnapshot dump = mock(HeapSnapshot.class);
        when(fixture.mapper.insertCjHeapDumps(List.of(dump))).thenReturn(false);

        try (MockedStatic<CjMapperManager> mapperStatic = fixture.mockMapperManager();
             MockedStatic<DatabaseManager> databaseStatic = fixture.mockDatabaseManager()) {
            assertThat(CjVmProfilerDao.getInstance().insertCjHeapDumps(SESSION_ID, List.of(dump))).isFalse();
        }

        verify(fixture.mapper).insertCjHeapDumps(List.of(dump));
        verify(fixture.session).commit();
        verify(fixture.session).close();
    }

    private static final class Fixture {
        private final CjVmProfilerMapper mapper = mock(CjVmProfilerMapper.class);
        private final SqlSession session = mock(SqlSession.class);
        private final CjMapperManager mapperManager = mock(CjMapperManager.class);
        private final DatabaseManager databaseManager = mock(DatabaseManager.class);

        private Fixture() {
            when(mapperManager.getDatabaseTypeByMapper(CjVmProfilerMapper.class)).thenReturn(DatabaseType.TRACE);
            when(databaseManager.getSqlSession(SESSION_ID, DatabaseType.TRACE)).thenReturn(Optional.of(session));
            when(session.getMapper(CjVmProfilerMapper.class)).thenReturn(mapper);
        }

        private MockedStatic<CjMapperManager> mockMapperManager() {
            MockedStatic<CjMapperManager> mocked = mockStatic(CjMapperManager.class);
            mocked.when(CjMapperManager::getInstance).thenReturn(mapperManager);
            return mocked;
        }

        private MockedStatic<DatabaseManager> mockDatabaseManager() {
            MockedStatic<DatabaseManager> mocked = mockStatic(DatabaseManager.class);
            mocked.when(DatabaseManager::getInstance).thenReturn(databaseManager);
            return mocked;
        }
    }
}
