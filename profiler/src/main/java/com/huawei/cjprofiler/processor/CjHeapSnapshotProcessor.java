/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.deveco.insight.ohos.ability.arkmemory.instancefilters.MemoryObject;
import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.common.constant.ArkConstants;
import com.huawei.deveco.insight.ohos.common.constant.CommonConstants;
import com.huawei.deveco.insight.ohos.common.enums.ArkMemoryTypeEnum;
import com.huawei.deveco.insight.ohos.common.enums.GlobalSearchUnitType;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkGcRootPathExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeSearchRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotComparisonRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotParseRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.resourcehandler.controller.RequestMapping;
import com.huawei.cjprofiler.common.TraceConstant;
import com.huawei.cjprofiler.common.TraceUtils;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.cjprofiler.service.CjRecordService;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.deveco.insight.ohos.utils.AssertUtil;
import com.huawei.deveco.insight.ohos.utils.LogPrinter;
import com.huawei.deveco.insight.ohos.utils.annotations.ParsedJson;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteStopRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.SingleSearchResultCount;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapSnapshotGcRootPath;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapSnapshotsLane;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkSnapshotComparisonDetail;

import com.alibaba.fastjson2.JSONObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * cangjie heap snapshot processor for handle service
 *
 * @since 2025/02/07/10:17
 */
@RequestMapping(path = "cangjieProfiler.heapSnapshot")
public class CjHeapSnapshotProcessor {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjHeapSnapshotProcessor.class);

    /**
     * startTakeHeapSnapshot
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.startHeapSnapshot")
    public static Response<?> startTakeHeapSnapshot(@ParsedJson CommonExecuteRequest request) {
        if (!CjVmProfilerDao.getInstance().registerMapper(request.getSessionId())) {
            LOGGER.warn("Failed to register CjVmProfilerMapper");
            return Response.failure(ProfilerError.SQL_ERROR);
        }
        Optional<IHarmonyDevice> device = OpenHarmonyDeviceManager.getInstance().getDeviceByKey(request.getDeviceKey());
        if (device.isEmpty() || CommonConstants.OFFLINE.equals(device.get().getStatus().getValue())) {
            LOGGER.warn("Failed to start takeHeapSnapshot, cause device is empty or offline.");
            return Response.failure(ProfilerError.DEVICE_EMPTY);
        }

        CjMemoryService service = CjRecordManager.getInstance().creatCjMemoryService(request.getSessionId());
        service.setInsertHeapSnapshot(false);
        CjRecordService.init();
        return Response.success();
    }

    /**
     * stopTakeHeapSnapshot
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.stopHeapSnapshot")
    public static Response<?> stopTakeHeapSnapshot(@ParsedJson ArkExecuteStopRequest request) {
        Optional<IHarmonyDevice> device = OpenHarmonyDeviceManager.getInstance().getDeviceByKey(request.getDeviceKey());
        if (device.isEmpty() || CommonConstants.OFFLINE.equals(device.get().getStatus().getValue())) {
            LOGGER.warn("Failed to stop takeHeapSnapshot, cause device is empty or offline.");
            return Response.failure(ProfilerError.DEVICE_EMPTY);
        }

        CjMemoryService cjMemoryService = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        if (!cjMemoryService.handleAllSnapshot(request.getSessionId())) {
            LOGGER.warn("Failed to stop take snapshot, cause handle all snapshot is error.");
            return Response.failure(ProfilerError.PLUGIN_STOP_ERROR);
        }
        Optional<CjRecordService> cjRecordService = CjRecordManager.getInstance().getCjRecordService(request.getPid());
        if (cjRecordService.isEmpty() || !cjRecordService.get().stopTakeHeapSnapshot()) {
            LOGGER.warn("Failed to stop take snapshot, cause stop error.");
            return Response.failure(ProfilerError.PLUGIN_STOP_ERROR);
        }
        return Response.success();
    }

    /**
     * takeHeapSnapshot
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.heap.snapshot")
    public static Response<?> takeHeapSnapshot(@ParsedJson CommonExecuteRequest request) {
        TraceUtils.trace("cangjieGetArkSnapshot", new JSONObject());
        Optional<CjRecordService> cjRecordService = CjRecordManager.getInstance().getCjRecordService(request.getPid());
        if (cjRecordService.isEmpty()) {
            LOGGER.warn("Failed to task heapSnapshot, cause ark vm is error.");
            return Response.failure(ProfilerError.PLUGIN_INTERNAL_ERROR);
        }
        if (!cjRecordService.get().takeHeapSnapshot(request)) {
            LOGGER.warn("Failed to task heapSnapshot, cause ark vm is error.");
            return Response.failure(ProfilerError.PLUGIN_INTERNAL_ERROR);
        }
        return Response.success();
    }

    /**
     * queryAllHeapSnapshot
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryAllHeapSnapshot")
    public static Response<?> queryAllHeapSnapshot(@ParsedJson CommonQueryRequest request) {
        CjMemoryService memoryService = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        if (!memoryService.isCjprof()) {
            if (!CjVmProfilerDao.getInstance().registerMapper(request.getSessionId())) {
                LOGGER.warn("Failed to register CjVmProfilerMapper");
                return Response.failure(ProfilerError.SQL_ERROR);
            }
        }
        return Response.success(new ArkHeapSnapshotsLane(
            memoryService.getAllSnapshot(request)));
    }

    /**
     * querySnapshotDetailById
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.querySnapshotDetailById")
    public static Response<?> querySnapshotDetailById(@ParsedJson CommonQueryRequest request) {
        return Response.success(
            CjRecordManager.getInstance().getCjMemoryService(request.getSessionId()).querySnapshotDetail(request));
    }

    /**
     * calMemData
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.calculateMemoryData")
    public static Response<?> calMemData(@ParsedJson ArkHeapNodeExpandRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        MemoryObject memoryObject = service.expandHeapNode(request);
        return Response.success(memoryObject);
    }

    /**
     * 展开节点.
     *
     * @param request JSONObject
     * @return Optional<JSONObject>
     */
    @RequestMapping(path = "cj.expandRetainer")
    public static Response<?> expandRetainer(@ParsedJson ArkHeapNodeExpandRequest request) {
        return calMemData(request);
    }

    /**
     * expand Snapshot Retainer
     *
     * @param request JSONObject
     * @return Optional<JSONObject>
     */
    @RequestMapping(path = "cj.expandSnapshotRetainer")
    public static Response<?> expandSnapshotRetainer(@ParsedJson ArkHeapNodeExpandRequest request) {
        request.setType(ArkMemoryTypeEnum.SNAPSHOT.getValue());
        return calMemData(request);
    }

    /**
     * expand Comparison Retainer
     *
     * @param request JSONObject
     * @return Optional<JSONObject>
     */
    @RequestMapping(path = "cj.expandComparisonRetainer")
    public static Response<?> expandComparisonRetainer(@ParsedJson ArkHeapNodeExpandRequest request) {
        request.setType(ArkMemoryTypeEnum.COMPARISON.getValue());
        return calMemData(request);
    }

    /**
     * Query snapshot comparison detail
     *
     * @param request JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.querySnapshotComparison")
    public static Response<?> querySnapshotComparison(@ParsedJson ArkSnapshotComparisonRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(new ArkSnapshotComparisonDetail(service.getDiffView(request)));
    }

    /**
     * executeArkVMGC
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.heap.gc")
    public static Response<?> executeArkVMGC(JSONObject params) {
        TraceUtils.trace("cangjieGc", new JSONObject());
        if (!params.containsKey("pid")) {
            LOGGER.warn("Failed to execute ark gc, cause params is error.");
            return Response.failure(ProfilerError.OBJECT_VALIDATE_ERROR);
        }
        Optional<CjRecordService> cjRecordService = CjRecordManager.getInstance()
            .getCjRecordService(params.getInteger("pid"));
        if (cjRecordService.isEmpty()) {
            LOGGER.warn("Failed to execute ark gc command, cause ark recorder is error.");
            return Response.failure(ProfilerError.SERVER_INTERNAL_ERROR);
        }
        CompletableFuture<Boolean> future = cjRecordService.get().collectGarbage();
        try {
            if (future != null && future.get(ArkConstants.HDC_TIMEOUT, TimeUnit.MILLISECONDS)) {
                return Response.success();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Failed to execute ark gc, exception is {}", e.getMessage());
        }
        return Response.failure(ProfilerError.SERVER_INTERNAL_ERROR);
    }

    /**
     * jumpToInstanceNode
     *
     * @param request request
     * @return Response<?>
     */
    @RequestMapping(path = "cj.jumpToInstanceNode")
    public static Response<?> jumpToInstanceNode(@ParsedJson ArkHeapNodeExpandRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        MemoryObject memoryObject = service.jumpToInstanceNode(request).orElse(null);
        return Response.success(memoryObject);
    }

    /**
     * parseHeapSnapshotFile
     *
     * @param request JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.parseHeapSnapshotFile")
    public static Response<?> parseHeapSnapshotFile(@ParsedJson ArkSnapshotParseRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().creatCjMemoryService(request.getSessionId());
        if (service.parseCjprofHeapSnapshotFile(request)) {
            return Response.success();
        }
        TraceUtils.trace("cangjieImportFailed",
            new JSONObject().fluentPut(TraceConstant.REASON, "Failed to parse CJ heap snapshot file."));
        return Response.failure(ProfilerError.FILE_PARSING_ERROR);
    }

    /**
     * parse raw heap file
     *
     * @param request JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.parseRawHeapFile")
    public static Response<?> parseRawHeapFile(@ParsedJson ArkSnapshotParseRequest request) {
        CjRecordManager.getInstance().getCjMemoryService(request.getSessionId()).parseRawHeapFile(request);
        return Response.success();
    }

    /**
     * queryParseSnapshotTimeRange
     *
     * @param params JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryParseSnapshotTimeRange")
    public static Response<?> queryParseSnapshotTimeRange(JSONObject params) {
        String sessionId = params.getString("sessionId");
        AssertUtil.notNull(sessionId, "Failed to query snapshot time range, cause: sessionId is null");
        return Response.success(
            CjRecordManager.getInstance().getCjMemoryService(sessionId).queryParseSnapshotTimeRange());
    }

    /**
     * queryCountOfResults
     *
     * @param request JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryCountOfResults")
    public static Response<?> queryCountOfResults(@ParsedJson ArkHeapNodeSearchRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(new SingleSearchResultCount(GlobalSearchUnitType.ARK_MEMORY.getValue(),
                service.queryCountOfResults(request)));
    }

    /**
     * queryResultByIndex
     *
     * @param request JSONObject
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryResultByIndex")
    public static Response<?> queryResultByIndex(@ParsedJson ArkHeapNodeSearchRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(service.queryResultByIndex(request));
    }

    /**
     *
     * expandGcRootPath
     *
     * @param request ArkHeapNodeExpandRequest
     * @return Response<?>
     */
    @RequestMapping(path = "cj.expandGcRootPath")
    public static Response<?> expandGcRootPath(@ParsedJson ArkGcRootPathExpandRequest request) {
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        if (service.isCjprof()) {
            return Response.success(new ArkHeapSnapshotGcRootPath(service.cjprofExpandGcRootPath(request)));
        }
        return Response.success(new ArkHeapSnapshotGcRootPath(service.expandGcRootPath(request)));
    }

    /**
     * queryHeapThreadInfo
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryHeapThreadInfo")
    public static Response<?> queryHeapThreadInfo(JSONObject params) {
        String sessionId = params.getString("sessionId");
        String snapshotId = params.getString("snapshotId");
        AssertUtil.notNull(sessionId, "Failed to query heap thread info, cause: sessionId is null");
        AssertUtil.notNull(snapshotId, "Failed to query heap thread info, cause: snapshotId is null");

        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(sessionId);
        return Response.success(service.getHeapThreadInfo(sessionId, snapshotId));
    }
}
