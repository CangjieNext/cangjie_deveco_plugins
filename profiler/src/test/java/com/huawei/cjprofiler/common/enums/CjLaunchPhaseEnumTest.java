/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests persisted values of {@link CjLaunchPhaseEnum}.
 *
 * @since 2026-08-14
 */
class CjLaunchPhaseEnumTest {
    @Test
    void getValue_returnsExpectedLifecycleNameForEachConstant() {
        assertEquals("Process Creating", CjLaunchPhaseEnum.LIFE_CYCLE_NAME_PROCESS_CREATING.getValue());
        assertEquals("Application Launching",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_APPLICATION_LAUNCHING.getValue());
        assertEquals("UI Ability Launching",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_UI_ABILITY_LAUNCHING.getValue());
        assertEquals("UI Ability OnForeground",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_UI_ABILITY_ON_FOREGROUND.getValue());
        assertEquals("First Frame - App Phase",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_FIRST_APP_FRAME_RENDERING.getValue());
        assertEquals("First Frame - Render Phase",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_FIRST_RS_FRAME_RENDERING.getValue());
        assertEquals("Runtime Initialization",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_RUNTIME_INITIALIZATION.getValue());
        assertEquals(".so File Loading", CjLaunchPhaseEnum.LIFE_CYCLE_NAME_LOADING_SO_FILE.getValue());
        assertEquals("Load Extension", CjLaunchPhaseEnum.LIFE_CYCLE_NAME_LOAD_EXTENSION.getValue());
        assertEquals("Module Information Acquisition",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_MODULE_INFORMATION_ACQUISITION.getValue());
        assertEquals("UI Ability Initialization",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_UI_ABILITY_INITIALIZATION.getValue());
        assertEquals("onCreate Execution",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_ON_CREATE_EXECUTION.getValue());
        assertEquals("Ability Lifecycle Callback",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_ABILITY_LIFECYCLE_CALLBACK.getValue());
        assertEquals("Content and Page Loading",
            CjLaunchPhaseEnum.LIFE_CYCLE_NAME_LOAD_CONTENT_AND_PAGE.getValue());
    }

    @ParameterizedTest
    @EnumSource(CjLaunchPhaseEnum.class)
    void enumValues_haveNonBlankValue(CjLaunchPhaseEnum phase) {
        assertEquals(phase.getValue(), CjLaunchPhaseEnum.valueOf(phase.name()).getValue());
    }

    @Test
    void enumConstantsCount_isFourteen() {
        assertEquals(14, CjLaunchPhaseEnum.values().length);
    }
}
