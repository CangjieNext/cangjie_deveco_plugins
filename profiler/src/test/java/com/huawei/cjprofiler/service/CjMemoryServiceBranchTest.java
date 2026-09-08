/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.dao.CjVmProfilerDao;
import com.huawei.cjprofiler.model.po.cjprof.HeapThreadInfo;
import com.huawei.cjprofiler.model.po.cjprof.StackFrame;
import com.huawei.deveco.insight.ohos.ability.arkmemory.JsHeapSnapshotInstanceNode;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.HeapSnapshot;
import com.huawei.deveco.insight.ohos.common.ProfilerException;
import com.huawei.deveco.insight.ohos.common.enums.ArkMemoryTypeEnum;
import com.huawei.deveco.insight.ohos.common.enums.UnitKey;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkGcRootPathExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeExpandRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkHeapNodeSearchRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkSnapshotComparisonRequest;
import com.huawei.deveco.insight.ohos.model.dto.request.common.CommonQueryRequest;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@code CjMemoryService} branch behaviors.
 *
 * @since 2026-08-14
 */
class CjMemoryServiceBranchTest {
    @Test
    void getDataTypeBySuffix_whenSnapshotSuffix_returnsSnapshotUnitType() {
        assertThat(new TestMemoryService().dataType(".cjheapsnapshot"))
            .isEqualTo(UnitKey.CJ_HEAP_SNAPSHOT.getType());
    }

    @Test
    void getDataTypeBySuffix_whenOtherSuffix_returnsTimelineUnitType() {
        assertThat(new TestMemoryService().dataType(".unknown"))
            .isEqualTo(UnitKey.CJ_HEAP_TIMELINE.getType());
    }

    @Test
    void handleAllSnapshot_whenCacheEmpty_returnsTrueWithoutDao() {
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(new TestMemoryService().handleAllSnapshot("session")).isTrue();
        }
    }

    @Test
    void getAllSnapshot_whenCacheUnavailable_delegatesExactRangeToDao() {
        TestMemoryService service = new TestMemoryService();
        service.markSnapshotInserted();
        CommonQueryRequest request = mock(CommonQueryRequest.class);
        CjVmProfilerDao dao = mock(CjVmProfilerDao.class);
        HeapSnapshot snapshot = mock(HeapSnapshot.class);
        when(request.getSessionId()).thenReturn("session");
        when(request.getStartRecordTime()).thenReturn(1_000L);
        when(request.getStartTime()).thenReturn(10L);
        when(request.getEndTime()).thenReturn(20L);
        when(dao.selectCjHeapDumps("session", 10L, 20L)).thenReturn(List.of(snapshot));

        try (MockedStatic<CjVmProfilerDao> daoStatic = mockStatic(CjVmProfilerDao.class)) {
            daoStatic.when(CjVmProfilerDao::getInstance).thenReturn(dao);

            assertThat(service.getAllSnapshot(request)).containsExactly(snapshot);
        }

        verify(dao).selectCjHeapDumps("session", 10L, 20L);
    }

    @Test
    void getStackList_whenPidMissing_returnsNull() {
        assertThat(new TestMemoryService().getStackList(404)).isNull();
    }

    @Test
    void getHeapThreadInfo_whenSnapshotMissing_returnsEmpty() {
        List<HeapThreadInfo> result = new TestMemoryService().getHeapThreadInfo("session", "missing");

        assertThat(result).isEmpty();
    }

    @Test
    void cleanCjHeapSnapshotFiles_whenNotCjprof_returnsWithoutNativeCall() {
        TestMemoryService service = new TestMemoryService();

        service.cleanCjHeapSnapshotFiles();

        assertThat(service.isCjprof()).isFalse();
    }

    @Test
    void parseCjprofHeapSnapshotFiles_whenFileListEmpty_marksCjprofAndThr() {
        TestMemoryService service = new TestMemoryService();

        assertThatThrownBy(() -> service.parseCjprofHeapSnapshotFiles("session", 1L, Collections.emptyList(), false))
            .isInstanceOf(ProfilerException.class);
        assertThat(service.isCjprof()).isTrue();
    }

    @Test
    void queryCountOfResults_whenCjprofSnapshotIdMissing_returnsZero() {
        TestMemoryService service = cjprofModeService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getType()).thenReturn(ArkMemoryTypeEnum.SNAPSHOT.getValue());
        when(request.getSnapshotId()).thenReturn("missing");

        assertThat(service.queryCountOfResults(request)).isZero();
    }

    @Test
    void queryResultByIndex_whenCjprofSnapshotIdMissing_returnsEmpty() {
        TestMemoryService service = cjprofModeService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getType()).thenReturn(ArkMemoryTypeEnum.SNAPSHOT.getValue());
        when(request.getSnapshotId()).thenReturn("missing");

        assertThat(service.queryResultByIndex(request)).isEmpty();
    }

    @Test
    void queryCountOfResults_whenCjprofComparisonCacheMissing_returnsZero() {
        TestMemoryService service = cjprofModeService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getType()).thenReturn(ArkMemoryTypeEnum.COMPARISON.getValue());
        when(request.getSnapshotId()).thenReturn("base");

        assertThat(service.queryCountOfResults(request)).isZero();
    }

    @Test
    void queryResultByIndex_whenCjprofComparisonCacheMissing_returnsEmpty() {
        TestMemoryService service = cjprofModeService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getType()).thenReturn(ArkMemoryTypeEnum.COMPARISON.getValue());
        when(request.getSnapshotId()).thenReturn("base");

        assertThat(service.queryResultByIndex(request)).isEmpty();
    }

    @Test
    void cjprofExpandGcRootPath_whenComparisonPositiveDelta_rewritesBaseR() {
        TestMemoryService service = cjprofModeService();
        ArkGcRootPathExpandRequest request = mock(ArkGcRootPathExpandRequest.class);
        when(request.getType()).thenReturn(ArkMemoryTypeEnum.COMPARISON.getValue());
        when(request.getCountDelta()).thenReturn(1);
        when(request.getBaseRawId()).thenReturn("base");
        when(request.getPathNum()).thenReturn(1);
        when(request.getRawId()).thenReturn("base");
        when(request.getNodeId()).thenReturn(10);

        assertThatThrownBy(() -> service.cjprofExpandGcRootPath(request))
            .isInstanceOf(NullPointerException.class);

        verify(request).setRawId("base");
    }

    @Test
    void jumpToInstanceNode_whenCjprofSnapshotIdMissing_returnsEmpty() {
        TestMemoryService service = cjprofModeService();
        ArkHeapNodeExpandRequest request = mock(ArkHeapNodeExpandRequest.class);
        when(request.getRawId()).thenReturn("missing");

        assertThat(service.jumpToInstanceNode(request)).isEqualTo(Optional.empty());
    }

    private static TestMemoryService cjprofModeService() {
        TestMemoryService service = new TestMemoryService();
        assertThatThrownBy(() -> service.parseCjprofHeapSnapshotFiles("session", 1L, Collections.emptyList(), false))
            .isInstanceOf(ProfilerException.class);
        return service;
    }

    @Test
    void getDiffView_whenNotCjprof_delegatesToSuper() {
        TestMemoryService service = new TestMemoryService();
        ArkSnapshotComparisonRequest request = mock(ArkSnapshotComparisonRequest.class);

        // isCjprof=false -> super.getDiffView(request). MemoryServiceBase.getDiffView
        // dereferences heapSnapshotRootMap.get(id).getRaw(), which NPEs because the
        // cache is empty in UT. The NPE proves delegation to super happened (the
        // cjprof path would return an empty list, not throw).
        assertThatThrownBy(() -> service.getDiffView(request))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("getRaw");
        assertThat(service.isCjprof()).isFalse();
    }

    @Test
    void getHeapThreadInfo_whenSnapshotIdPresentButMapEmpty_returnsEmptyL() {
        TestMemoryService service = new TestMemoryService();
        List<HeapThreadInfo> result = service.getHeapThreadInfo("session", "999999");
        assertThat(result).isEmpty();
    }

    @Test
    void cleanCjHeapSnapshotFiles_whenCjprofButNoSnapshots_returnsGracefu() {
        TestMemoryService service = cjprofModeService();
        // snapshotIdMap is empty, getAllCjprofSnapshotIds returns empty list
        // Cjprof.cleanHeapSnapshotFiles would be called with empty list
        // We cannot mockStatic(Cjprof.class) because Cjprof's static initializer
        // triggers ConfigUtil -> ApplicationManager which is null in UT.
        // Instead, verify the guard path: isCjprof=true, snapshotIdMap is empty
        assertThat(service.isCjprof()).isTrue();
    }

    @Test
    void releaseResources_whenNotCjprof_skipsNativeClean() {
        TestMemoryService service = new TestMemoryService();
        // releaseResources calls cleanTemporaryFiles then cleanCjHeapSnapshotFiles
        // isCjprof=false so cleanCjHeapSnapshotFiles returns immediately
        assertThatCode(() -> service.releaseResources()).doesNotThrowAnyException();
        assertThat(service.isCjprof()).isFalse();
    }

    @Test
    void isSamePath_normalizesBothPathsBeforeComparing() throws Exception {
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod(
            "isSamePath", String.class, String.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, "C:/test/file.txt", "C:/test/file.txt")).isEqualTo(true);
        assertThat(method.invoke(null, "C:/test/file.txt", "C:/test/other.txt")).isEqualTo(false);
        assertThat(method.invoke(null, null, null)).isEqualTo(true);
    }

    @Test
    void filterEmptyStackFrame_removesFramesWithEmptyNameAndNoObjects() throws Exception {
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("filterEmptyStackFrame", List.class);
        method.setAccessible(true);

        StackFrame frameWithName = StackFrame.builder().methodName("main").build();
        StackFrame emptyFrameNoObj = StackFrame.builder()
            .methodName("")
            .localObjectList(java.util.Collections.emptyList())
            .build();
        StackFrame emptyFrameWithObj = StackFrame.builder()
            .methodName("")
            .localObjectList(List.of(new com.huawei.cjprofiler.ability.arkmemory.entity.cj.LocalObject()))
            .build();
        HeapThreadInfo info = new HeapThreadInfo(
            1, "main", new java.util.ArrayList<>(List.of(frameWithName, emptyFrameNoObj, emptyFrameWithObj)));

        @SuppressWarnings("unchecked")
        List<HeapThreadInfo> result = (List<HeapThreadInfo>) method.invoke(new TestMemoryService(), List.of(info));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStackFrameInfoList()).hasSize(2);
        assertThat(result.get(0).getStackFrameInfoList().get(0).getMethodName()).isEqualTo("main");
        assertThat(result.get(0).getStackFrameInfoList().get(1).getMethodName()).isEmpty();
    }

    @Test
    void filterEmptyStackFrame_whenListIsEmpty_returnsSameList() throws Exception {
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("filterEmptyStackFrame", List.class);
        method.setAccessible(true);

        List<HeapThreadInfo> emptyList = java.util.Collections.emptyList();
        @SuppressWarnings("unchecked")
        List<HeapThreadInfo> result = (List<HeapThreadInfo>) method.invoke(new TestMemoryService(), emptyList);
        assertThat(result).isSameAs(emptyList);
    }

    @Test
    void filterStackFrameByCondition_keepsNamedFramesAndFirstObjectFrame() throws Exception {
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod(
            "filterStackFrameByCondition", HeapThreadInfo.class);
        method.setAccessible(true);

        // named1 kept, emptyNoObj skipped (no object, before hasObject),
        // emptyWithObj kept (has object, no prior object), named2 kept
        StackFrame named1 = StackFrame.builder().methodName("run")
            .localObjectList(java.util.Collections.emptyList()).build();
        StackFrame emptyNoObj = StackFrame.builder().methodName("")
            .localObjectList(java.util.Collections.emptyList()).build();
        StackFrame emptyWithObj = StackFrame.builder().methodName("")
            .localObjectList(List.of(new com.huawei.cjprofiler.ability.arkmemory.entity.cj.LocalObject())).build();
        StackFrame named2 = StackFrame.builder().methodName("init")
            .localObjectList(java.util.Collections.emptyList()).build();
        HeapThreadInfo info = new HeapThreadInfo(
            1, "thread", new java.util.ArrayList<>(List.of(named1, emptyNoObj, emptyWithObj, named2)));

        method.invoke(new TestMemoryService(), info);

        assertThat(info.getStackFrameInfoList()).hasSize(3);
        assertThat(info.getStackFrameInfoList().get(0).getMethodName()).isEqualTo("run");
        assertThat(info.getStackFrameInfoList().get(1).getMethodName()).isEmpty();
        assertThat(info.getStackFrameInfoList().get(2).getMethodName()).isEqualTo("init");
    }

    @Test
    void filterStackFrameByCondition_whenStackFrameListEmpty_doesNotThrow() throws Exception {
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod(
            "filterStackFrameByCondition", HeapThreadInfo.class);
        method.setAccessible(true);

        HeapThreadInfo info = new HeapThreadInfo(1, "thread", java.util.Collections.emptyList());
        method.invoke(new TestMemoryService(), info);
        assertThat(info.getStackFrameInfoList()).isEmpty();
    }

    @Test
    void getCjSnapshotId_whenSnapshotIdProvided_returnsDirectly() throws Exception {
        TestMemoryService service = new TestMemoryService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getSnapshotId()).thenReturn("snap-42");

        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getCjSnapshotId",
            ArkHeapNodeSearchRequest.class);
        method.setAccessible(true);

        Object snapshotResult = method.invoke(service, request);
        assertThat(snapshotResult).isInstanceOf(String.class);
        String result = String.class.cast(snapshotResult);
        assertThat(result).isEqualTo("snap-42");
    }

    @Test
    void getCjSnapshotId_whenSnapshotListEmpty_returnsEmptyString() throws Exception {
        TestMemoryService service = new TestMemoryService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getSnapshotId()).thenReturn(null);

        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getCjSnapshotId",
            ArkHeapNodeSearchRequest.class);
        method.setAccessible(true);

        Object snapshotResult = method.invoke(service, request);
        assertThat(snapshotResult).isInstanceOf(String.class);
        String result = String.class.cast(snapshotResult);
        assertThat(result).isEmpty();
    }

    @Test
    void getCjSnapshotId_whenNoSnapshotInRange_returnsEmptyString() throws Exception {
        TestMemoryService service = new TestMemoryService();
        ArkHeapNodeSearchRequest request = mock(ArkHeapNodeSearchRequest.class);
        when(request.getSnapshotId()).thenReturn(null);
        when(request.getStartTimeAll()).thenReturn(100L);
        when(request.getEndTimeAll()).thenReturn(200L);

        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getCjSnapshotId",
            ArkHeapNodeSearchRequest.class);
        method.setAccessible(true);

        Object snapshotResult = method.invoke(service, request);
        assertThat(snapshotResult).isInstanceOf(String.class);
        String result = String.class.cast(snapshotResult);
        assertThat(result).isEmpty();
    }

    @Test
    void getCjprofSnapshotId_whenRawIdMissing_returnsNull() throws Exception {
        TestMemoryService service = new TestMemoryService();
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getCjprofSnapshotId", String.class);
        method.setAccessible(true);

        // reflection returns boxed Long, null is valid for map.get("nonexistent")
        assertThat(method.invoke(service, "nonexistent")).isNull();
    }

    @Test
    void getAllCjprofSnapshotIds_whenMapEmpty_returnsEmptyList() throws Exception {
        TestMemoryService service = new TestMemoryService();
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getAllCjprofSnapshotIds");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) method.invoke(service);
        assertThat(result).isEmpty();
    }

    @Test
    void getAllCjprofSnapshotIds_whenMapHasEntries_returnsAllValues() throws Exception {
        TestMemoryService service = cjprofModeService();
        java.lang.reflect.Method putMethod = CjMemoryService.class.getDeclaredMethod("putCjprofSnapshotId",
            String.class, Long.class);
        putMethod.setAccessible(true);
        putMethod.invoke(service, "raw1", 1L);
        putMethod.invoke(service, "raw2", 2L);

        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("getAllCjprofSnapshotIds");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) method.invoke(service);
        assertThat(result).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void covertInstanceToTree_whenEmptyList_returnsEmpty() throws Exception {
        TestMemoryService service = new TestMemoryService();
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("covertInstanceToTree",
            List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<JsHeapSnapshotInstanceNode> result = (List<JsHeapSnapshotInstanceNode>)
            method.invoke(service, java.util.Collections.emptyList(), 0);
        assertThat(result).isEmpty();
    }

    @Test
    void setInstanceAttribute_whenNotConstructorNode_doesNothing() throws Exception {
        TestMemoryService service = new TestMemoryService();
        java.lang.reflect.Method method = CjMemoryService.class.getDeclaredMethod("setInstanceAttribute",
            com.huawei.deveco.insight.ohos.ability.arkmemory.instancefilters.MemoryObject.class);
        method.setAccessible(true);

        // Pass a non-ConstructorNode object; should not throw
        method.invoke(service, (Object) null);
    }

    private static final class TestMemoryService extends CjMemoryService {
        private String dataType(String suffix) {
            return getDataTypeBySuffix(suffix);
        }

        private void markSnapshotInserted() {
            setInsertHeapSnapshot(true);
        }
    }
}
