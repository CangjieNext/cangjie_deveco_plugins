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
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.huawei.cangjie.sdkconfig.support.SdkConfig;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieComponent;
import com.huawei.cangjie.sdkmanager.idea.api.CangjieIdeaSdkInfoHandler;
import com.huawei.cangjie.sdkmanager.idea.constants.CangjieComponentPath;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@code ConfigUtil}.
 *
 * @since 2026-09-08
 */
public class ConfigUtilTest {
    @Test
    public void instantiateClass_coversConstructor() {
        // Instantiate to cover class declaration line
        new ConfigUtil();
    }

    @Test
    public void testGetCangjieHomePath_fileExists() throws Exception {
        Path tempDir = Files.createTempDirectory("cangjie-test-");
        try {
            try (MockedConstruction<CangjieIdeaSdkInfoHandler> mocked =
                mockConstruction(CangjieIdeaSdkInfoHandler.class, (mock, context) -> {
                    SdkConfig config = mock(SdkConfig.class);
                    when(config.getBuildToolsRootPath()).thenReturn(tempDir.toString());
                    CangjieComponent component = mock(CangjieComponent.class);
                    when(component.getSdkConfig()).thenReturn(config);
                    Map<String, CangjieComponent> map = new HashMap<>();
                    map.put(CangjieComponentPath.CANGJIE.value(), component);
                    when(mock.getLocalSdks(true, 20)).thenReturn(map);
                })) {
                String result = ConfigUtil.getCangjieHomePath();

                assertThat(result).isEqualTo(tempDir.toRealPath().toString());
            }
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    public void testGetCangjieHomePath_fileNotExists_returnsEmpty() {
        try (MockedConstruction<CangjieIdeaSdkInfoHandler> mocked =
            mockConstruction(CangjieIdeaSdkInfoHandler.class, (mock, context) -> {
                SdkConfig config = mock(SdkConfig.class);
                when(config.getBuildToolsRootPath()).thenReturn("/nonexistent/path/xyz");
                CangjieComponent component = mock(CangjieComponent.class);
                when(component.getSdkConfig()).thenReturn(config);
                Map<String, CangjieComponent> map = new HashMap<>();
                map.put(CangjieComponentPath.CANGJIE.value(), component);
                when(mock.getLocalSdks(true, 20)).thenReturn(map);
            })) {
            String result = ConfigUtil.getCangjieHomePath();

            assertThat(result).isEmpty();
        }
    }
}
