package com.huawei.cjprofiler.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CjRecordManagerTest {
    private final CjRecordManager manager = CjRecordManager.getInstance();

    @BeforeEach
    @AfterEach
    void clearManagerState() {
        manager.releaseAllMemoryResources();
        manager.setCurrentSession(null);
    }

    @Test
    void getInstance_returnsSingleton() {
        assertThat(CjRecordManager.getInstance()).isSameAs(CjRecordManager.getInstance());
    }

    @Test
    void getCjRecordService_whenProcessIsMissing_returnsEmpty() {
        Optional<CjRecordService> result = manager.getCjRecordService(404);

        assertThat(result).isEmpty();
    }

    @Test
    void createCjRecordService_whenProcessIsMissing_createsRetrievableService() {
        manager.creatCjRecordService(12);

        assertThat(manager.getCjRecordService(12))
            .isPresent()
            .get()
            .extracting(CjRecordService::getTid)
            .isEqualTo(12);
    }

    @Test
    void createCjRecordService_whenProcessAlreadyExists_keepsExistingService() {
        manager.creatCjRecordService(21);
        CjRecordService first = manager.getCjRecordService(21).orElseThrow();

        manager.creatCjRecordService(21);

        assertThat(manager.getCjRecordService(21)).containsSame(first);
        assertThat(manager.getCjRecordServiceMap()).hasSize(1);
    }

    @Test
    void createCjMemoryService_whenSessionIsMissing_createsAndCachesService() {
        CjMemoryService created = manager.creatCjMemoryService("session-1");

        assertThat(created).isNotNull();
        assertThat(manager.getCjMemoryService("session-1")).isSameAs(created);
    }

    @Test
    void createCjMemoryService_whenSessionAlreadyExists_keepsExistingService() {
        CjMemoryService first = manager.creatCjMemoryService("session-2");

        CjMemoryService second = manager.creatCjMemoryService("session-2");

        assertThat(second).isSameAs(first);
        assertThat(manager.getCjMemoryServiceMap()).hasSize(1);
    }

    @Test
    void releaseAllMemoryResources_releasesEveryMemoryServiceAndClearsBothCaches() {
        CjMemoryService first = mock(CjMemoryService.class);
        CjMemoryService second = mock(CjMemoryService.class);
        manager.getCjMemoryServiceMap().put("first", first);
        manager.getCjMemoryServiceMap().put("second", second);
        manager.creatCjRecordService(31);
        manager.creatCjRecordService(32);

        boolean result = manager.releaseAllMemoryResources();

        assertThat(result).isTrue();
        verify(first, times(1)).releaseResources();
        verify(second, times(1)).releaseResources();
        assertThat(manager.getCjMemoryServiceMap()).isEmpty();
        assertThat(manager.getCjRecordServiceMap()).isEmpty();
        assertThat(manager.getCjRecordService(31)).isEmpty();
    }
}
