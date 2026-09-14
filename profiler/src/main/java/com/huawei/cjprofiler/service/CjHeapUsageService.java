/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapUsages;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkAllHeapUsageRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.vo.ArkHeapUsagesSummary;
import com.huawei.deveco.insight.ohos.service.ark.ArkHeapUsageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CjHeapUsageService
 *
 * @since 2025/02/25/20:05
 */
public class CjHeapUsageService {
    private static volatile CjHeapUsageService cjHeapUsageService = null;

    private CjHeapUsageService() {}

    /**
     * getInstance
     *
     * @return ArkHeapUsageService
     */
    public static CjHeapUsageService getInstance() {
        if (cjHeapUsageService == null) {
            synchronized (ArkHeapUsageService.class) {
                if (cjHeapUsageService == null) {
                    cjHeapUsageService = new CjHeapUsageService();
                }
            }
        }
        return cjHeapUsageService;
    }

    /**
     * Query ark single instance heap usages
     *
     * @param request CommonQueryRequest
     * @return List<HeapUsages>
     */
    public List<HeapUsages> queryHeapUsage(CommonQueryRequest request) {
        Optional<CjRecordService> cjRecordService = CjRecordManager.getInstance()
                .getCjRecordService(request.getTid());
        if (cjRecordService.isPresent()) {
            return cjRecordService.get().queryCjHeapUsages(request);
        }
        return CjVmProfilerDao.getInstance().selectCjHeapUsages(
                request.getSessionId(), request.getTid(), request.getStartTime(), request.getEndTime());
    }

    /**
     * Query ark all instance heap usages
     *
     * @param request ArkAllHeapUsageRequest
     * @return List<HeapUsages>
     */
    public List<HeapUsages> queryAllHeapUsage(ArkAllHeapUsageRequest request) {
        CommonQueryRequest queryRequest = convertCommonQueryRequest(request);
        queryRequest.setTid(request.getPid());
        return queryHeapUsage(queryRequest);
    }

    /**
     * Query ark all instance heap usages detail
     *
     * @param request ArkAllHeapUsageRequest
     * @return List<HeapUsages>
     */
    public List<ArkHeapUsagesSummary> queryAllHeapUsagesDetail(ArkAllHeapUsageRequest request) {
        CommonQueryRequest queryRequest = convertCommonQueryRequest(request);
        int pid = request.getPid();
        List<ArkHeapUsagesSummary> summaryList = new ArrayList<>();
        queryRequest.setTid(pid);
        ArkHeapUsagesSummary arkHeapUsagesSummary = calcSummary(CjVmProfilerDao.getInstance()
                .selectCjHeapUsages(request.getSessionId(), pid, request.getStartTime(), request.getEndTime()));
        arkHeapUsagesSummary.setTid(pid);
        summaryList.add(arkHeapUsagesSummary);
        return summaryList;
    }

    private ArkHeapUsagesSummary calcSummary(List<HeapUsages> heapUsagesList) {
        ArkHeapUsagesSummary arkHeapUsagesSummary = new ArkHeapUsagesSummary();
        if (heapUsagesList == null || heapUsagesList.isEmpty()) {
            return arkHeapUsagesSummary;
        }
        double total = 0D;
        double peak = 0D;
        double vally = Double.MAX_VALUE;
        for (HeapUsages data : heapUsagesList) {
            total += data.getUsedSize();
            peak = Math.max(peak, data.getUsedSize());
            vally = Math.min(vally, data.getUsedSize());
        }
        arkHeapUsagesSummary.setAvg(total / heapUsagesList.size());
        arkHeapUsagesSummary.setPeak(peak);
        arkHeapUsagesSummary.setValley(vally);
        return arkHeapUsagesSummary;
    }

    private CommonQueryRequest convertCommonQueryRequest(ArkAllHeapUsageRequest request) {
        return CommonQueryRequest.builder()
                .sessionId(request.getSessionId())
                .pid(request.getPid())
                .startRecordTime(request.getStartRecordTime())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }
}
