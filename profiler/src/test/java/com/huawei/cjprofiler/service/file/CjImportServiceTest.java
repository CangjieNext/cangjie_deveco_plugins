/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.service.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.deveco.insight.ohos.ability.arkcpu.parser.JSTraceParser;
import com.huawei.deveco.insight.ohos.ability.arkmemory.entity.RawHeapSnapshot;
import com.huawei.deveco.insight.ohos.ability.database.manager.DatabaseManager;
import com.huawei.deveco.insight.ohos.common.enums.DatabaseType;
import com.huawei.deveco.insight.ohos.common.enums.UnitKey;
import com.huawei.deveco.insight.ohos.model.dto.DataStructure;
import com.huawei.deveco.insight.ohos.model.dto.request.ImportDataDto;
import com.huawei.deveco.insight.ohos.service.file.JsTraceFileService;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@code CjImportService} covering timeline and snapshot import flows.
 *
 * @since 2026-08-14
 */
class CjImportServiceTest {
    private static final String SESSION_ID = "import-session";

    @Test
    void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new CjImportService();
    }

    @Test
    void importTimeline_whenDataStructureIsNull_returnsTrueWithoutParsing() {
        Fixture fixture = new Fixture(Collections.singletonList(null));

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager()) {
            assertThat(CjImportService.importCjHeapTimeLine(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(fixture.memoryService, never()).handlerTraceFile(org.mockito.ArgumentMatchers.anyString());
        verify(fixture.memoryService, never()).calculateTotalSize();
    }

    @Test
    void importTimeline_whenFileTypeDoesNotMatch_skipsFileAndCalculatesTo() {
        DataStructure data = dataWithFile("trace.json");
        Fixture fixture = new Fixture(List.of(data));

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager()) {
            assertThat(CjImportService.importCjHeapTimeLine(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(fixture.memoryService, never()).handlerTraceFile("trace.json");
        verify(fixture.memoryService).calculateTotalSize();
    }

    @Test
    void importTimeline_whenTraceParsingFails_returnsFalseWithoutSavingOr() {
        String path = "bad.cjheaptimeline";
        Fixture fixture = new Fixture(List.of(dataWithFile(path)));
        when(fixture.memoryService.handlerTraceFile(path)).thenReturn(null);
        JsTraceFileService traceFileService = mock(JsTraceFileService.class);

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager();
             MockedStatic<JsTraceFileService> traceStatic = mockStatic(JsTraceFileService.class)) {
            traceStatic.when(JsTraceFileService::getInstance).thenReturn(traceFileService);

            assertThat(CjImportService.importCjHeapTimeLine(fixture.dto, fixture.unitKey)).isFalse();
        }

        verify(traceFileService, never()).saveTraceFile(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong());
        verify(fixture.memoryService, never()).calculateTotalSize();
    }

    @Test
    void importTimeline_whenTidMissing_usesPidSavesParsesAndCalculatesSiz() {
        String path = "heap.cjheaptimeline";
        Fixture fixture = new Fixture(List.of(dataWithFile(path)));
        RawHeapSnapshot snapshot = mock(RawHeapSnapshot.class);
        JsTraceFileService traceFileService = mock(JsTraceFileService.class);
        when(snapshot.getStartTime()).thenReturn(123L);
        when(fixture.memoryService.handlerTraceFile(path)).thenReturn(snapshot);
        when(fixture.dto.getPid()).thenReturn(77);

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager();
             MockedStatic<JsTraceFileService> traceStatic = mockStatic(JsTraceFileService.class);
             MockedStatic<JSTraceParser> parserStatic = mockStatic(JSTraceParser.class)) {
            traceStatic.when(JsTraceFileService::getInstance).thenReturn(traceFileService);
            parserStatic.when(() -> JSTraceParser.getTidFromProfilePath(path)).thenReturn(0);

            assertThat(CjImportService.importCjHeapTimeLine(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(traceFileService).saveTraceFile(SESSION_ID, "heap", path, 123L);
        verify(fixture.memoryService).setStartRecordTimeMap(77, 123L);
        verify(fixture.memoryService).parseArkHeapTimeline(SESSION_ID, 77, snapshot, false);
        verify(fixture.memoryService).calculateTotalSize();
    }

    @Test
    void importSnapshot_whenFileIsValid_savesTraceAndSnapshotRoot() {
        String path = "snapshot.cjheapsnapshot";
        Fixture fixture = new Fixture(List.of(dataWithFile(path)));
        RawHeapSnapshot snapshot = mock(RawHeapSnapshot.class);
        JsTraceFileService traceFileService = mock(JsTraceFileService.class);
        when(snapshot.getStartTime()).thenReturn(456L);
        when(fixture.memoryService.handlerTraceFile(path)).thenReturn(snapshot);

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager();
             MockedStatic<JsTraceFileService> traceStatic = mockStatic(JsTraceFileService.class)) {
            traceStatic.when(JsTraceFileService::getInstance).thenReturn(traceFileService);

            assertThat(CjImportService.importCjHeapSnapshot(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(traceFileService).saveTraceFile(SESSION_ID, "heap", path, 456L);
        verify(fixture.memoryService).saveHeapSnapshotToRootMap(snapshot);
    }

    @Test
    void importSnapshot_whenDatabaseProvided_connectsMemoryDatabase() {
        DataStructure data = mock(DataStructure.class, RETURNS_DEEP_STUBS);
        when(data.getFile()).thenReturn(null);
        when(data.getDatabase().getPath()).thenReturn("memory.db");
        Fixture fixture = new Fixture(List.of(data));
        DatabaseManager databaseManager = mock(DatabaseManager.class);

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager();
             MockedStatic<DatabaseManager> databaseStatic = mockStatic(DatabaseManager.class)) {
            databaseStatic.when(DatabaseManager::getInstance).thenReturn(databaseManager);

            assertThat(CjImportService.importCjHeapSnapshot(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(databaseManager).connectDatabase(SESSION_ID, "memory.db", DatabaseType.MEMORY);
        verify(fixture.memoryService, never()).handlerTraceFile(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void importSnapshot_whenTraceParsingFails_returnsFalseWithoutSavingRo() {
        String path = "broken.cjheapsnapshot";
        Fixture fixture = new Fixture(List.of(dataWithFile(path)));
        when(fixture.memoryService.handlerTraceFile(path)).thenReturn(null);

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager()) {
            assertThat(CjImportService.importCjHeapSnapshot(fixture.dto, fixture.unitKey)).isFalse();
        }

        verify(fixture.memoryService, never()).saveHeapSnapshotToRootMap(
            org.mockito.ArgumentMatchers.any(RawHeapSnapshot.class));
    }

    @Test
    void importSnapshot_whenDataStructureIsNull_returnsTrueWithoutImport() {
        Fixture fixture = new Fixture(Collections.singletonList(null));

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager()) {
            assertThat(CjImportService.importCjHeapSnapshot(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(fixture.memoryService, never()).handlerTraceFile(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void importSnapshot_whenBothFileAndDatabaseAreNull_warnsAndContinues() {
        DataStructure data = mock(DataStructure.class, RETURNS_DEEP_STUBS);
        when(data.getFile()).thenReturn(null);
        when(data.getDatabase()).thenReturn(null);
        Fixture fixture = new Fixture(List.of(data));

        try (MockedStatic<CjRecordManager> managerStatic = fixture.mockRecordManager()) {
            assertThat(CjImportService.importCjHeapSnapshot(fixture.dto, fixture.unitKey)).isTrue();
        }

        verify(fixture.memoryService, never()).handlerTraceFile(org.mockito.ArgumentMatchers.anyString());
    }

    private static DataStructure dataWithFile(String path) {
        DataStructure data = mock(DataStructure.class, RETURNS_DEEP_STUBS);
        when(data.getFile().getPath()).thenReturn(path);
        return data;
    }

    private static final class Fixture {
        private final ImportDataDto dto = mock(ImportDataDto.class);
        private final UnitKey unitKey = mock(UnitKey.class);
        private final CjRecordManager recordManager = mock(CjRecordManager.class);
        private final CjMemoryService memoryService = mock(CjMemoryService.class);

        private Fixture(List<DataStructure> structures) {
            when(dto.getSessionId()).thenReturn(SESSION_ID);
            when(dto.getDataStructure()).thenReturn(structures);
            when(unitKey.getType()).thenReturn("heap");
            when(recordManager.creatCjMemoryService(SESSION_ID)).thenReturn(memoryService);
            when(recordManager.getCjMemoryService(SESSION_ID)).thenReturn(memoryService);
        }

        private MockedStatic<CjRecordManager> mockRecordManager() {
            MockedStatic<CjRecordManager> mocked = mockStatic(CjRecordManager.class);
            mocked.when(CjRecordManager::getInstance).thenReturn(recordManager);
            return mocked;
        }
    }
}
