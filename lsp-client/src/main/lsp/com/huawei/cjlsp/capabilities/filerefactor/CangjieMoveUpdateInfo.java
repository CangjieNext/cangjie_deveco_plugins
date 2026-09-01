/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.filerefactor;

import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CangjieMoveUpdateInfo
 *
 * @since 2025/9/17
 */
public class CangjieMoveUpdateInfo {
    private Map<String, List<UpdateInfo>> changes;

    public CangjieMoveUpdateInfo(Map<String, List<UpdateInfo>> changes) {
        this.changes = changes;
    }

    /**
     * UpdateInfo
     *
     * @since 2025-10-08
     */
    public static class UpdateInfo implements Comparable<UpdateInfo> {
        private int type;
        private Range range;
        private String content;

        public UpdateInfo(int type, Range range, String content) {
            this.type = type;
            this.range = range;
            this.content = content;
        }

        public int getType() {
            return type;
        }

        public Range getRange() {
            return range;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setRange(Range range) {
            this.range = range;
        }

        public void setType(int type) {
            this.type = type;
        }

        @Override
        public int compareTo(@NotNull UpdateInfo updateInfo) {
            if (updateInfo.range.getStart().getLine() > this.getRange().getStart().getLine()) {
                return 1;
            }
            if (updateInfo.range.getStart().getLine() < this.getRange().getStart().getLine()) {
                return -1;
            }
            if (updateInfo.range.getStart().getCharacter() > this.getRange().getStart().getCharacter()) {
                return 1;
            }
            if (updateInfo.range.getStart().getCharacter() < this.getRange().getStart().getCharacter()) {
                return -1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof UpdateInfo other)) {
                return false;
            }
            if (this.getType() != other.getType()) {
                return false;
            }
            if (!Objects.equals(this.content, other.content)) {
                return false;
            }
            return this.getRange().equals(other.getRange());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.range, this.content);
        }
    }

    public Map<String, List<UpdateInfo>> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, List<UpdateInfo>> changes) {
        this.changes = changes;
    }
}
