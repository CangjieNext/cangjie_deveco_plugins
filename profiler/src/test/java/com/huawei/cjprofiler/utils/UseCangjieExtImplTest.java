/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.cjprofiler.service.CjRecordService;
import com.huawei.cjprofiler.service.file.CjImportService;
import com.huawei.deveco.insight.ohos.common.enums.UnitKey;
import com.huawei.deveco.insight.ohos.model.dto.request.ImportDataDto;
import com.huawei.deveco.insight.ohos.model.dto.request.ark.ArkConnectRequest;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

/**
 * Unit tests for {@code UseCangjieExtImpl} extension actions.
 *
 * @since 2026-09-08
 */
public class UseCangjieExtImplTest {
    @Test
    public void testCreateCjConnect_createsAndInitializesService() {
        UseCangjieExtImpl extImpl = new UseCangjieExtImpl();
        ArkConnectRequest request = new ArkConnectRequest();
        request.setPid(123);

        CjRecordService recordService = mock(CjRecordService.class);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            CjRecordManager manager = mock(CjRecordManager.class);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            when(manager.getCjRecordService(123)).thenReturn(Optional.of(recordService));

            extImpl.createCjConnect(request);

            verify(manager, times(1)).creatCjRecordService(123);
            verify(manager, times(1)).getCjRecordService(123);
            verify(recordService, times(1)).initFlag();
        }
    }

    @Test
    public void testCreateCjConnect_whenServiceMissing_doesNothing() {
        UseCangjieExtImpl extImpl = new UseCangjieExtImpl();
        ArkConnectRequest request = new ArkConnectRequest();
        request.setPid(456);

        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            CjRecordManager manager = mock(CjRecordManager.class);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);
            when(manager.getCjRecordService(456)).thenReturn(Optional.empty());

            extImpl.createCjConnect(request);

            verify(manager, times(1)).creatCjRecordService(456);
            verify(manager, times(1)).getCjRecordService(456);
        }
    }

    @Test
    public void testReleaseAllMemoryResources() {
        UseCangjieExtImpl extImpl = new UseCangjieExtImpl();

        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            CjRecordManager manager = mock(CjRecordManager.class);
            when(manager.releaseAllMemoryResources()).thenReturn(true);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            extImpl.releaseAllMemoryResources();

            verify(manager, times(1)).releaseAllMemoryResources();
        }
    }

    @Test
    public void testImportCjHeapSnapshot_delegatesToService() {
        UseCangjieExtImpl extImpl = new UseCangjieExtImpl();
        ImportDataDto importDataDto = new ImportDataDto();

        try (MockedStatic<CjImportService> importStatic = mockStatic(CjImportService.class)) {
            importStatic.when(() -> CjImportService.importCjHeapSnapshot(importDataDto, UnitKey.HEAP_SNAPSHOT))
                .thenReturn(true);

            boolean isImported = extImpl.importCjHeapSnapshot(importDataDto, UnitKey.HEAP_SNAPSHOT);

            assertThat(isImported).isTrue();
            importStatic.verify(() -> CjImportService.importCjHeapSnapshot(importDataDto, UnitKey.HEAP_SNAPSHOT),
                times(1));
        }
    }

    @Test
    public void testImportCjHeapTimeLine_delegatesToService() {
        UseCangjieExtImpl extImpl = new UseCangjieExtImpl();
        ImportDataDto importDataDto = new ImportDataDto();

        try (MockedStatic<CjImportService> importStatic = mockStatic(CjImportService.class)) {
            importStatic.when(() -> CjImportService.importCjHeapTimeLine(importDataDto, UnitKey.HEAP_TIMELINE))
                .thenReturn(false);

            boolean isImported = extImpl.importCjHeapTimeLine(importDataDto, UnitKey.HEAP_TIMELINE);

            assertThat(isImported).isFalse();
            importStatic.verify(() -> CjImportService.importCjHeapTimeLine(importDataDto, UnitKey.HEAP_TIMELINE),
                times(1));
        }
    }
}