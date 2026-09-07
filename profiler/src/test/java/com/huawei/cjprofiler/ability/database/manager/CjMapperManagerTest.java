/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.ability.database.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.huawei.cjprofiler.dao.mapper.CjThreadMapper;
import com.huawei.cjprofiler.dao.mapper.CjVmProfilerMapper;
import com.huawei.deveco.insight.ohos.common.enums.DatabaseType;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@code CjMapperManager} mapper type resolution.
 *
 * @since 2026-08-14
 */
class CjMapperManagerTest {
    @Test
    void getDatabaseTypeByMapper_returnsTraceForThreadMapper() {
        assertThat(CjMapperManager.getInstance().getDatabaseTypeByMapper(CjThreadMapper.class))
            .isEqualTo(DatabaseType.TRACE);
    }

    @Test
    void getDatabaseTypeByMapper_returnsTimeSnapshotForVmProfilerMapper() {
        assertThat(CjMapperManager.getInstance().getDatabaseTypeByMapper(CjVmProfilerMapper.class))
            .isEqualTo(DatabaseType.TIME_SNAPSHOT);
    }

    @Test
    void getDatabaseTypeByMapper_returnsNullForUnregisteredMapper() {
        assertThat(CjMapperManager.getInstance().getDatabaseTypeByMapper(String.class)).isNull();
    }

    @Test
    void getDatabaseTypeByMapper_whenMapperIsNull_throwsNullPointerExcept() {
        assertThatThrownBy(() -> CjMapperManager.getInstance().getDatabaseTypeByMapper(null))
            .isInstanceOf(NullPointerException.class);
    }
}
