/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 * Tests constant values of {@link LaunchTraceConstants}.
 *
 * @since 2026-08-14
 */
class LaunchTraceConstantsTest {
    @Test
    void maxLifeCycleDurationConstant_isMinusOne() {
        assertEquals(Long.valueOf(-1L), LaunchTraceConstants.MAX_LIFE_CYCLE_DURATION);
    }

    @Test
    void launchMainPhaseNumConstant_isSix() {
        assertEquals(Integer.valueOf(6), LaunchTraceConstants.LAUNCH_MAIN_PHASE_NUM);
    }

    @Test
    void regularStringFrameTraceRelateConstant_isExpectedRegex() {
        assertEquals("\\[\\d{1,19}\\,\\d{1,19}\\]?",
            LaunchTraceConstants.REGULAR_STRING_FRAME_TRACE_RELATE);
    }

    @Test
    void patternFrameTraceRelateConstant_isCompiledPattern() {
        Pattern pattern = LaunchTraceConstants.PATTERN_FRAME_TRACE_RELATE;
        assertNotNull(pattern);
        assertTrue(pattern.matcher("[123,456]").matches());
        assertTrue(pattern.matcher("[1,2]").matches());
    }

    @Test
    void traceStartAbilityConstant_isExpectedValue() {
        assertEquals("H:virtual int OHOS::AAFwk::AbilityManagerService::StartAbility(",
            LaunchTraceConstants.TRACE_START_ABILITY);
    }

    @Test
    void traceStartAbilityLockedConstant_isExpectedValue() {
        assertEquals("H:int OHOS::AAFwk::MissionListManager::StartAbilityLocked",
            LaunchTraceConstants.TRACE_START_ABILITY_LOCKED);
    }

    @Test
    void traceLoadAbilityConstant_isExpectedValue() {
        assertEquals("virtual void OHOS::AppExecFwk::AppMgrServiceInner::LoadAbility",
            LaunchTraceConstants.TRACE_LOAD_ABILITY);
    }

    @Test
    void traceAttachApplicationConstant_isExpectedValue() {
        assertEquals("H:virtual void OHOS::AppExecFwk::AppMgrServiceInner::AttachApplication",
            LaunchTraceConstants.TRACE_ATTACH_APPLICATION);
    }

    @Test
    void traceLoadExtensionConstant_isExpectedValue() {
        assertEquals("H:void OHOS::AppExecFwk::MainThread::LoadAllExtensions",
            LaunchTraceConstants.TRACE_LOAD_EXTENSION);
    }

    @Test
    void traceHandleLaunchConstant_isExpectedValue() {
        assertEquals("H:void OHOS::AppExecFwk::MainThread::HandleLaunchApplication",
            LaunchTraceConstants.TRACE_HANDLE_LAUNCH);
    }

    @Test
    void traceLaunchAbilityConstant_isExpectedValue() {
        assertEquals("H:void OHOS::AppExecFwk::MainThread::HandleLaunchAbility",
            LaunchTraceConstants.TRACE_LAUNCH_ABILITY);
    }

    @Test
    void traceForegroundAbilityConstant_isExpectedValue() {
        assertEquals("HandleAbilityTransaction", LaunchTraceConstants.TRACE_FOREGROUND_ABILITY);
    }

    @Test
    void traceReceiveVsyncConstant_isExpectedValue() {
        assertEquals("H:ReceiveVsync", LaunchTraceConstants.TRACE_RECEIVE_VSYNC);
    }

    @Test
    void traceAppRenderingConstant_isExpectedValue() {
        assertEquals("transactionFlag:[", LaunchTraceConstants.TRACE_APP_RENDERING);
    }

    @Test
    void traceAppRenderingSendCommandsConstant_isExpectedValue() {
        assertEquals("SendCommands", LaunchTraceConstants.TRACE_APP_RENDERING_SEND_COMMANDS);
    }

    @Test
    void traceRsRenderingConstant_isExpectedValue() {
        assertEquals("H:RSMainThread::ProcessCommandUni", LaunchTraceConstants.TRACE_RS_RENDERING);
    }

    @Test
    void traceAbilityTransactionConstant_isExpectedValue() {
        assertEquals("H:bool OHOS::AbilityRuntime::UIAbilityImpl::AbilityTransaction",
            LaunchTraceConstants.TRACE_ABILITY_TRANSACTION);
    }

    @Test
    void traceCangjieRuntimeConstant_isExpectedValue() {
        assertEquals("Initialize cangjie runtime and namespace",
            LaunchTraceConstants.TRACE_CANGJIE_RUNTIME);
    }

    @Test
    void traceLoadCangjieAppLibraryConstant_isExpectedValue() {
        assertEquals("OHOS::AbilityRuntime::CJRuntime::LoadCJAppLibrary",
            LaunchTraceConstants.TRACE_LOAD_CANGJIE_APP_LIBRARY);
    }

    @Test
    void traceUiStartConstant_isExpectedValue() {
        assertEquals("H:void OHOS::AbilityRuntime::UIAbilityImpl::Start("
                + "const AAFwk::Want &, sptr<AAFwk::SessionInfo>)",
            LaunchTraceConstants.TRACE_UI_START);
    }

    @Test
    void processRenderServiceConstant_isExpectedValue() {
        assertEquals("render_service", LaunchTraceConstants.PROCESS_RENDER_SERVICE);
    }

    @Test
    void forceLoadClass_initializesLaunchTraceConstants() {
        assertNotNull(new LaunchTraceConstants());
    }

    @Test
    void lifeCycleNameConstants_areExpectedValues() {
        assertEquals("Process Creating", LaunchTraceConstants.LIFE_CYCLE_NAME_PROCESS_CREATING);
        assertEquals("Application Launching",
            LaunchTraceConstants.LIFE_CYCLE_NAME_APPLICATION_LAUNCHING);
        assertEquals("Runtime Initialization",
            LaunchTraceConstants.LIFE_CYCLE_NAME_RUNTIME_INITIALIZATION);
        assertEquals("Load Extension", LaunchTraceConstants.LIFE_CYCLE_NAME_LOAD_EXTENSION);
        assertEquals("Module Information Acquisition",
            LaunchTraceConstants.LIFE_CYCLE_NAME_MODULE_INFORMATION_ACQUISITION);
        assertEquals("UI Ability Launching",
            LaunchTraceConstants.LIFE_CYCLE_NAME_UI_ABILITY_LAUNCHING);
        assertEquals(".so File Loading", LaunchTraceConstants.LIFE_CYCLE_NAME_LOADING_SO_FILE);
        assertEquals("UI Ability Initialization",
            LaunchTraceConstants.LIFE_CYCLE_NAME_UI_ABILITY_INITIALIZATION);
        assertEquals("onCreate Execution",
            LaunchTraceConstants.LIFE_CYCLE_NAME_ON_CREATE_EXECUTION);
        assertEquals("UI Ability OnForeground",
            LaunchTraceConstants.LIFE_CYCLE_NAME_UI_ABILITY_ON_FOREGROUND);
        assertEquals("Ability Lifecycle Callback",
            LaunchTraceConstants.LIFE_CYCLE_NAME_ABILITY_LIFECYCLE_CALLBACK);
        assertEquals("Content and Page Loading",
            LaunchTraceConstants.LIFE_CYCLE_NAME_LOAD_CONTENT_AND_PAGE);
        assertEquals("First Frame - App Phase",
            LaunchTraceConstants.LIFE_CYCLE_NAME_FIRST_APP_FRAME_RENDERING);
        assertEquals("First Frame - Render Phase",
            LaunchTraceConstants.LIFE_CYCLE_NAME_FIRST_RS_FRAME_RENDERING);
        assertEquals("Start", LaunchTraceConstants.LIFE_CYCLE_NAME_START);
        assertEquals("Background", LaunchTraceConstants.LIFE_CYCLE_NAME_BACKGROUND);
        assertEquals("Terminate", LaunchTraceConstants.LIFE_CYCLE_NAME_TERMINATE);
        assertEquals("Inactive", LaunchTraceConstants.LIFE_CYCLE_NAME_INACTIVE);
        assertEquals("Active", LaunchTraceConstants.LIFE_CYCLE_NAME_ACTIVE);
    }
}
