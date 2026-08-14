/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.dao;

import com.huawei.cjprofiler.dao.mapper.CjVmProfilerMapper;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapSnapshot;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapStats;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapUsages;
import com.huawei.deveco.insight.ohos.utils.ListUtil;

import org.apache.ibatis.session.SqlSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * CjVmProfilerDao
 *
 * @since 2025/02/28/11:41
 */
public class CjVmProfilerDao extends AbstractDao {
    private static volatile CjVmProfilerDao cjVmProfilerDao = null;

    private static final int MAX_BATCH_SIZE = 10000;

    private CjVmProfilerDao() {
    }

    /**
     * ProfilerDao getInstance
     *
     * @return ProfilerDao
     */
    public static CjVmProfilerDao getInstance() {
        if (cjVmProfilerDao == null) {
            synchronized (CjVmProfilerDao.class) {
                if (cjVmProfilerDao == null) {
                    cjVmProfilerDao = new CjVmProfilerDao();
                }
            }
        }
        return cjVmProfilerDao;
    }

    @Override
    public Class<?> relatedMapper() {
        return CjVmProfilerMapper.class;
    }

    /**
     * selectCjHeapUsages
     *
     * @param sessionId String
     * @param tid tid
     * @param startTime long
     * @param endTime long
     * @return list List<HeapUsages>
     */
    public List<HeapUsages> selectCjHeapUsages(String sessionId, int tid, long startTime, long endTime) {
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            return cjVmProfilerMapper.selectCjHeapUsages(tid, startTime, endTime);
        }
    }

    /**
     * selectCjHeapStats
     *
     * @param sessionId String
     * @param tid tid
     * @param startTime long
     * @param endTime long
     * @return list List<HeapStats>
     */
    public List<HeapStats> selectCjHeapStats(String sessionId, int tid, long startTime, long endTime) {
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            return cjVmProfilerMapper.selectCjHeapStats(tid, startTime, endTime);
        }
    }

    /**
     * selectCjHeapDumps
     *
     * @param sessionId String
     * @param startTime long
     * @param endTime long
     * @return list List<HeapSnapshot>
     */
    public List<HeapSnapshot> selectCjHeapDumps(String sessionId, long startTime, long endTime) {
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            return cjVmProfilerMapper.selectCjHeapDumps(startTime, endTime);
        }
    }

    /**
     * insertCjHeapUsages
     *
     * @param sessionId String
     * @param tid tid
     * @param heapUsageList List<HeapUsages>
     * @param readWriteLock ReadWriteLock
     * @return isInsert boolean
     */
    public boolean insertCjHeapUsages(String sessionId, int tid, List<HeapUsages> heapUsageList,
        ReadWriteLock readWriteLock) {
        if (heapUsageList.isEmpty()) {
            return true;
        }
        readWriteLock.writeLock().lock();
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            List<List<HeapUsages>> batchList = ListUtil.splitList(heapUsageList, MAX_BATCH_SIZE);
            boolean isInsert = true;
            for (List<HeapUsages> list : batchList) {
                isInsert &= cjVmProfilerMapper.insertCjHeapUsages(tid, list);
                sqlSession.commit();
                sqlSession.clearCache();
            }
            return isInsert;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * insertCjHeapStats
     *
     * @param sessionId String
     * @param tid tid
     * @param heapStatMap LinkedHashMap<Long, HeapStats>
     * @param readWriteLock ReadWriteLock
     * @return isInsert boolean
     */
    public boolean insertCjHeapStats(String sessionId, int tid, LinkedHashMap<Long, HeapStats> heapStatMap,
        ReadWriteLock readWriteLock) {
        if (heapStatMap.isEmpty()) {
            return true;
        }
        readWriteLock.readLock().lock();
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            List<HeapStats> heapStatList = new ArrayList<>(heapStatMap.values());
            List<List<HeapStats>> batchList = ListUtil.splitList(heapStatList, MAX_BATCH_SIZE);
            boolean isInsert = true;
            for (List<HeapStats> list : batchList) {
                isInsert &= cjVmProfilerMapper.insertCjHeapStats(tid, list);
                sqlSession.commit();
                sqlSession.clearCache();
            }
            return isInsert;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * insertCjHeapDumps
     *
     * @param sessionId String
     * @param heapSnapshotList List<HeapSnapshot>
     * @return isInsert boolean
     */
    public boolean insertCjHeapDumps(String sessionId, List<HeapSnapshot> heapSnapshotList) {
        if (heapSnapshotList.isEmpty()) {
            return true;
        }
        try (SqlSession sqlSession = obtainSqlSession(sessionId)) {
            CjVmProfilerMapper cjVmProfilerMapper = sqlSession.getMapper(CjVmProfilerMapper.class);
            boolean isInsert = cjVmProfilerMapper.insertCjHeapDumps(heapSnapshotList);
            sqlSession.commit();
            return isInsert;
        }
    }
}