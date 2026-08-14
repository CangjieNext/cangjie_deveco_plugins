/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjprofiler.utils;

import com.huawei.cjprofiler.common.Constants;
import com.huawei.cjprofiler.service.CjMemoryService;
import com.huawei.cjprofiler.service.CjRecordManager;
import com.huawei.deveco.insight.ohos.ability.persistence.importconfig.TypeRule;
import com.huawei.deveco.insight.ohos.utils.BalloonNotification;
import com.huawei.deveco.insight.ohos.utils.CangjieExtension;

import com.intellij.notification.NotificationType;

import lombok.extern.slf4j.Slf4j;

import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * cjplugin 提供的 MyBatis 扩展实现。
 *
 * @since 2025-11-24
 */
@Slf4j
public class CjCommonImpl implements CangjieExtension {
    @Override
    public void register(Configuration configuration) {
        LOGGER.info("register start");
        // 注册 PO 类的别名
        configuration.getTypeAliasRegistry()
            .registerAlias("com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo",
                com.huawei.cjprofiler.model.po.cjthread.CjThreadSliceInfoPo.class);
        configuration.getTypeAliasRegistry()
            .registerAlias("com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo",
                com.huawei.cjprofiler.model.vo.cjthread.CjThreadInfoVo.class);
    }

    /**
     * parseCjHeapSnapshotFiles
     *
     * @param sessionId sessionId
     * @param startTime startTime
     * @param filePathList filePathList
     */
    public void parseCjHeapSnapshotFiles(String sessionId, long startTime, List<String> filePathList) {
        LOGGER.info("parseCjHeapSnapshotFiles start");
        CjMemoryService service = CjRecordManager.getInstance().creatCjMemoryService(sessionId);
        service.parseCjprofHeapSnapshotFiles(sessionId, startTime, filePathList, true);
    }

    /**
     * isNotifyCjFileNumExceedLimit
     *
     * @param typeRule typeRule
     * @return boolean
     */
    public boolean isNotifyCjFileNumExceedLimit(TypeRule typeRule) {
        LOGGER.info("isNotifyCjFileNumExceedLimit start");
        if (Constants.CJPROF_FILE_TYPE.equals(typeRule.getType())) {
            BalloonNotification.show(CjNotificationError.CJ_IMPORT_EXCEEDS_QUANTITY_LIMIT_ERROR.getContent(),
                    NotificationType.ERROR);
            return true;
        }
        return false;
    }
}
