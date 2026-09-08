/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.cjprofiler.common.Constants;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.deveco.insight.ohos.ability.persistence.importconfig.TypeRule;
import com.huawei.deveco.insight.ohos.utils.BalloonNotification;

import com.intellij.notification.NotificationType;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;

/**
 * CjCommonImplTest
 */
public class CjCommonImplTest {

    private CjCommonImpl cjCommonImpl;

    @BeforeEach
    public void setUp() {
        cjCommonImpl = new CjCommonImpl();
    }

    @Test
    public void testInstanceCreation() {
        CjCommonImpl instance = new CjCommonImpl();
        assertNotNull(instance);
    }

    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeNotMatches() {
        TypeRule typeRule = new TypeRule();
        typeRule.setType("other_type");

        boolean isNotifyExceedLimit = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(isNotifyExceedLimit);
    }

    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeIsNull() {
        TypeRule typeRule = new TypeRule();
        typeRule.setType(null);

        boolean isNotifyExceedLimit = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(isNotifyExceedLimit);
    }

    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeIsEmpty() {
        TypeRule typeRule = new TypeRule();
        typeRule.setType("");

        boolean isNotifyExceedLimit = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

        assertFalse(isNotifyExceedLimit);
    }

    @Test
    public void testIsNotifyCjFileNumExceedLimit_TypeMatches() {
        TypeRule typeRule = new TypeRule();
        typeRule.setType(Constants.CJPROF_FILE_TYPE);

        try (MockedStatic<BalloonNotification> notificationStatic =
            mockStatic(BalloonNotification.class)) {
            boolean isNotifyExceedLimit = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

            assertTrue(isNotifyExceedLimit);
            notificationStatic.verify(() -> BalloonNotification.show(
                eq(CjNotificationError.CJ_IMPORT_EXCEEDS_QUANTITY_LIMIT_ERROR.getContent()),
                eq(NotificationType.ERROR)));
        }
    }

    @Test
    public void testIsNotifyCjFileNumExceedLimit_NoNotificationWhenNotMatch() {
        TypeRule typeRule = new TypeRule();
        typeRule.setType("other_type");

        try (MockedStatic<BalloonNotification> notificationStatic =
            mockStatic(BalloonNotification.class)) {
            boolean isNotifyExceedLimit = cjCommonImpl.isNotifyCjFileNumExceedLimit(typeRule);

            assertFalse(isNotifyExceedLimit);
            notificationStatic.verify(() -> BalloonNotification.show(anyString(), any(NotificationType.class)),
                never());
        }
    }

    @Test
    public void testRegister() {
        Configuration configuration = mock(Configuration.class);
        TypeAliasRegistry registry = mock(TypeAliasRegistry.class);
        when(configuration.getTypeAliasRegistry()).thenReturn(registry);

        cjCommonImpl.register(configuration);

        verify(registry, times(1)).registerAlias(
            eq("com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo"),
            eq(com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo.class));
        verify(registry, times(1)).registerAlias(
            eq("com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo"),
            eq(com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo.class));
    }

    @Test
    public void testParseCjHeapSnapshotFiles() {
        CjMemoryService memoryService = mock(CjMemoryService.class);
        try (MockedStatic<CjRecordManager> managerStatic = mockStatic(CjRecordManager.class)) {
            CjRecordManager manager = mock(CjRecordManager.class);
            when(manager.creatCjMemoryService("session-parse")).thenReturn(memoryService);
            managerStatic.when(CjRecordManager::getInstance).thenReturn(manager);

            cjCommonImpl.parseCjHeapSnapshotFiles("session-parse", 1234L,
                Arrays.asList("/tmp/a.cjheapdump", "/tmp/b.cjheapdump"));

            verify(memoryService, times(1)).parseCjprofHeapSnapshotFiles(
                eq("session-parse"), eq(1234L),
                eq(Arrays.asList("/tmp/a.cjheapdump", "/tmp/b.cjheapdump")), eq(true));
        }
    }
}