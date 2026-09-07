/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.ability.database.manager.CjMapperManager;
import com.huawei.cjprofiler.dao.mapper.CjThreadMapper;
import com.huawei.deveco.insight.ohos.ability.database.TraceDatabase;
import com.huawei.deveco.insight.ohos.ability.database.manager.DatabaseManager;
import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.common.enums.DatabaseType;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

/**
 * Unit tests for {@code AbstractDao} sql session and mapper handling.
 *
 * @since 2026-08-14
 */
class AbstractDaoTest {
    private static final String SESSION_ID = "session-1";

    private static class TestDao extends AbstractDao {
        private final Class<?> mapperClass;

        TestDao(Class<?> mapperClass) {
            this.mapperClass = mapperClass;
        }

        @Override
        Class<?> relatedMapper() {
            return mapperClass;
        }

        SqlSession obtain(String sessionId) {
            return obtainSqlSession(sessionId);
        }

        Long offset(String sessionId) {
            return getAbsoluteOffsetBySessionId(sessionId);
        }
    }

    @Test
    void getAbsoluteOffsetBySessionId_delegatesToTraceDatabase() {
        TraceDatabase traceDatabase = mock(TraceDatabase.class);
        when(traceDatabase.getAbsoluteOffsetBySessionId(SESSION_ID)).thenReturn(987L);
        try (MockedStatic<TraceDatabase> mocked = mockStatic(TraceDatabase.class)) {
            mocked.when(TraceDatabase::getInstance).thenReturn(traceDatabase);

            assertThat(new TestDao(CjThreadMapper.class).offset(SESSION_ID)).isEqualTo(987L);
            verify(traceDatabase).getAbsoluteOffsetBySessionId(SESSION_ID);
        }
    }

    @Test
    void obtainSqlSession_whenSessionExists_returnsConfiguredSession() {
        CjMapperManager mapperManager = mock(CjMapperManager.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        SqlSession session = mock(SqlSession.class);
        when(mapperManager.getDatabaseTypeByMapper(CjThreadMapper.class)).thenReturn(DatabaseType.TRACE);
        when(databaseManager.getSqlSession(SESSION_ID, DatabaseType.TRACE)).thenReturn(Optional.of(session));
        try (MockedStatic<CjMapperManager> mapperStatic = mockStatic(CjMapperManager.class);
             MockedStatic<DatabaseManager> databaseStatic = mockStatic(DatabaseManager.class)) {
            mapperStatic.when(CjMapperManager::getInstance).thenReturn(mapperManager);
            databaseStatic.when(DatabaseManager::getInstance).thenReturn(databaseManager);

            assertThat(new TestDao(CjThreadMapper.class).obtain(SESSION_ID)).isSameAs(session);
        }
    }

    @Test
    void obtainSqlSession_whenMapperUnregistered_throwsProfilerExceptionW() {
        CjMapperManager mapperManager = mock(CjMapperManager.class);
        when(mapperManager.getDatabaseTypeByMapper(String.class)).thenReturn(null);
        try (MockedStatic<CjMapperManager> mapperStatic = mockStatic(CjMapperManager.class)) {
            mapperStatic.when(CjMapperManager::getInstance).thenReturn(mapperManager);

            assertThatThrownBy(() -> new TestDao(String.class).obtain(SESSION_ID))
                .isInstanceOf(ProfilerException.class)
                .hasMessageContaining("has not been registered");
        }
    }

    @Test
    void obtainSqlSession_whenDatabaseHasNoSession_throwsProfilerExceptio() {
        CjMapperManager mapperManager = mock(CjMapperManager.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(mapperManager.getDatabaseTypeByMapper(CjThreadMapper.class)).thenReturn(DatabaseType.TRACE);
        when(databaseManager.getSqlSession(SESSION_ID, DatabaseType.TRACE)).thenReturn(Optional.empty());
        try (MockedStatic<CjMapperManager> mapperStatic = mockStatic(CjMapperManager.class);
             MockedStatic<DatabaseManager> databaseStatic = mockStatic(DatabaseManager.class)) {
            mapperStatic.when(CjMapperManager::getInstance).thenReturn(mapperManager);
            databaseStatic.when(DatabaseManager::getInstance).thenReturn(databaseManager);

            assertThatThrownBy(() -> new TestDao(CjThreadMapper.class).obtain(SESSION_ID))
                .isInstanceOf(ProfilerException.class)
                .hasMessageContaining("SqlSession is null");
        }
    }

    @Test
    void registerMapper_delegatesToDatabaseManagerAndReturnsItsResult() {
        CjMapperManager mapperManager = mock(CjMapperManager.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(mapperManager.getDatabaseTypeByMapper(CjThreadMapper.class)).thenReturn(DatabaseType.TRACE);
        when(databaseManager.addMapper(SESSION_ID, DatabaseType.TRACE, CjThreadMapper.class)).thenReturn(true);
        try (MockedStatic<CjMapperManager> mapperStatic = mockStatic(CjMapperManager.class);
             MockedStatic<DatabaseManager> databaseStatic = mockStatic(DatabaseManager.class)) {
            mapperStatic.when(CjMapperManager::getInstance).thenReturn(mapperManager);
            databaseStatic.when(DatabaseManager::getInstance).thenReturn(databaseManager);

            assertThat(new TestDao(CjThreadMapper.class).registerMapper(SESSION_ID)).isTrue();
            verify(databaseManager).addMapper(SESSION_ID, DatabaseType.TRACE, CjThreadMapper.class);
        }
    }
}
