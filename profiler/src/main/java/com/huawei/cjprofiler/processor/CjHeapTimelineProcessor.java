/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.processor;

import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_PROFILER_PARSE;
import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_PROFILER_START;
import static com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType.HEAP_PROFILER_STOP;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.cjprofiler.model.dto.response.CjStackList;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapStats;
import com.huawei.deveco.insight.ohos.ability.device.IHarmonyDevice;
import com.huawei.deveco.insight.ohos.ability.device.OpenHarmonyDeviceManager;
import com.huawei.deveco.insight.ohos.common.ProfilerError;
import com.huawei.deveco.insight.ohos.common.Response;
import com.huawei.deveco.insight.ohos.common.constant.ArkExecuteType;
import com.huawei.deveco.insight.ohos.common.constant.CommonConstants;
import com.huawei.deveco.insight.ohos.common.enums.ArkMemoryTypeEnum;
import com.huawei.deveco.insight.ohos.common.enums.UnitKey;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkReleaseDataRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonExecuteRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapStatsLane;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkHeapTimelineDetail;
import com.huawei.deveco.insight.ohos.model.dto.response.ark.ArkInstanceMetadata;
import com.huawei.deveco.insight.ohos.processor.sessiondata.SessionDataCacheManager;
import com.huawei.deveco.insight.ohos.resourcehandler.controller.RequestMapping;
import com.huawei.cjprofiler.common.TraceConstant;
import com.huawei.cjprofiler.common.TraceUtils;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.deveco.insight.ohos.service.file.JsTraceFileService;
import com.huawei.deveco.insight.ohos.utils.JsonUtil;
import com.huawei.deveco.insight.ohos.utils.LogPrinter;
import com.huawei.deveco.insight.ohos.utils.ValidateUtil;
import com.huawei.deveco.insight.ohos.utils.annotations.ParsedJson;

import com.alibaba.fastjson2.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * cangjie heap timeline processor for handle service
 *
 * @since 2025/02/07/10:18
 */
@RequestMapping(path = "cangjieProfiler.heapTimeline")
public class CjHeapTimelineProcessor {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjHeapTimelineProcessor.class);

    /**
     * startHeapTimeLine
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.startHeapTimeline")
    public static Response<?> startHeapTimeLine(JSONObject params) {
        ArkExecuteRequest request = JsonUtil.parseObject(params, ArkExecuteRequest.class);
        ValidateUtil.validate(request);
        Optional<IHarmonyDevice> device = OpenHarmonyDeviceManager.getInstance().getDeviceByKey(request.getDeviceKey());
        if (device.isEmpty() || CommonConstants.OFFLINE.equals(device.get().getStatus().getValue())) {
            LOGGER.warn("Failed to startHeapTimeLine, cause device is empty or offline.");
            return Response.failure(ProfilerError.DEVICE_EMPTY);
        }

        CjRecordManager cjRecordManager = CjRecordManager.getInstance();
        cjRecordManager.initData(ArkExecuteType.HEAP_PROFILER, request.getSessionId());
        cjRecordManager.creatCjRecordService(request.getPid());
        if (!cjRecordManager.executeCJTask(request, HEAP_PROFILER_START, params)) {
            LOGGER.warn("Failed to start ark heapTimeline, cause ark vm is error.");
            return Response.failure(ProfilerError.PLUGIN_START_ERROR);
        }
        return Response.success();
    }

    /**
     * stopHeapTimeLine
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.stopHeapTimeline")
    public static Response<?> stopHeapTimeLine(JSONObject params) {
        ArkExecuteRequest request = JsonUtil.parseObject(params, ArkExecuteRequest.class);
        ValidateUtil.validate(request);
        TraceUtils.trace("cangjieGetTimeline", new JSONObject());
        Optional<IHarmonyDevice> device = OpenHarmonyDeviceManager.getInstance().getDeviceByKey(request.getDeviceKey());
        if (device.isEmpty() || CommonConstants.OFFLINE.equals(device.get().getStatus().getValue())) {
            LOGGER.warn("Failed to stopHeapTimeLine, cause device is empty or offline.");
            return Response.failure(ProfilerError.DEVICE_EMPTY);
        }

        CjRecordManager cjRecordManager = CjRecordManager.getInstance();
        if (!cjRecordManager.executeCJTask(request, HEAP_PROFILER_STOP, params)) {
            LOGGER.warn("Failed to stop ark heapTimeline, cause ark vm is error.");
            return Response.failure(ProfilerError.PLUGIN_STOP_ERROR);
        }
        return Response.success();
    }

    /**
     * parseHeapTimeLine
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.parseHeapTimeline")
    public static Response<?> parseHeapTimeLine(JSONObject params) {
        ArkExecuteRequest request = JsonUtil.parseObject(params, ArkExecuteRequest.class);
        ValidateUtil.validate(request);
        CjRecordManager cjRecordManager = CjRecordManager.getInstance();
        if (!cjRecordManager.executeCJTask(request, HEAP_PROFILER_PARSE, params)) {
            LOGGER.warn("Failed to parse ark heapTimeline, cause cangjie vm is error.");
            TraceUtils.trace("cangjieImportFailed",
                new JSONObject().fluentPut(TraceConstant.REASON, "Failed to parse CJ heap timeline."));
            return Response.failure(ProfilerError.FILE_PARSING_ERROR);
        }
        cjRecordManager.getCjMemoryService(request.getSessionId()).calculateTotalSize();
        return Response.success();
    }

    /**
     * queryHeapTimelineDetail
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryHeapTimelineDetail")
    public static Response<?> queryHeapTimelineDetail(JSONObject params) {
        ArkQueryRequest request = JsonUtil.parseObject(params, ArkQueryRequest.class);
        ValidateUtil.validate(request);
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(new ArkHeapTimelineDetail(service.queryHeapTimelineDetail(request)));
    }

    /**
     * expand comparison detail
     *
     * @param request JSONObject
     * @return Optional<JSONObject>
     */
    @RequestMapping(path = "ark.expandHeapTimelineDetail")
    public static Response<?> expandHeapTimelineDetail(@ParsedJson ArkHeapNodeExpandRequest request) {
        request.setType(ArkMemoryTypeEnum.ALLOCATION.getValue());
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(service.expandHeapNode(request));
    }

    /**
     * queryHeapStats
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryHeapStatsUpdate")
    public static Response<?> queryHeapStats(JSONObject params) {
        CommonQueryRequest request = JsonUtil.parseObject(params, CommonQueryRequest.class);
        ValidateUtil.validate(request);
        List<HeapStats> heapStats = CjVmProfilerDao.getInstance()
            .selectCjHeapStats(request.getSessionId(), request.getTid(), request.getStartTime(), request.getEndTime());
        return Response.success(new ArkHeapStatsLane(heapStats));
    }

    /**
     * queryStackList
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryStackList")
    public static Response<?> queryStackList(JSONObject params) {
        ArkExecuteRequest request = JsonUtil.parseObject(params, ArkExecuteRequest.class);
        ValidateUtil.validate(request);
        CjMemoryService service = CjRecordManager.getInstance().getCjMemoryService(request.getSessionId());
        return Response.success(new CjStackList(service.getStackList(request.getPid())));
    }

    /**
     * queryInstanceMetadata
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.queryInstanceMetadata")
    public static Response<?> queryInstanceMetadata(JSONObject params) {
        CommonExecuteRequest request = JsonUtil.parseObject(params, CommonExecuteRequest.class);
        ValidateUtil.validate(request);
        return Response.success(new ArkInstanceMetadata(Collections.singletonList(request.getPid())));
    }

    /**
     * releaseDataForArkMemory
     *
     * @param params params
     * @return Response<?>
     */
    @RequestMapping(path = "cj.deleteSessionForHeapTimeline")
    public static Response<?> releaseDataForArkMemory(JSONObject params) {
        ArkReleaseDataRequest request = JsonUtil.parseObject(params, ArkReleaseDataRequest.class);
        ValidateUtil.validate(request);
        SessionDataCacheManager.getInstance().clearCache(request.getSessionId(), UnitKey.HEAP_SNAPSHOT);
        CjRecordManager.getInstance().releaseCjMemoryService(request.getSessionId());
        JsTraceFileService.getInstance().cleanCache(request.getSessionId());
        return Response.success();
    }
}
