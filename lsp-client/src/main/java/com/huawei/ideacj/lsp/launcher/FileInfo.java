/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.launcher;

import java.util.Objects;

/**
 * The type File info.
 *
 * @since 2026-3-14
 */
public class FileInfo {
    private final long size;

    private final long lastModifiedTime;

    /**
     * Instantiates a new File info.
     *
     * @param size the size
     * @param lastModifiedTime the last modified time
     */
    public FileInfo(long size, long lastModifiedTime) {
        this.size = size;
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * Gets size.
     *
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets last modified time.
     *
     * @return the last modified time
     */
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) o;
        return size == fileInfo.size && lastModifiedTime == fileInfo.lastModifiedTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, lastModifiedTime);
    }

    @Override
    public String toString() {
        return "FileInfo{" + "size=" + size + ", lastModifiedTime=" + lastModifiedTime + '}';
    }
}