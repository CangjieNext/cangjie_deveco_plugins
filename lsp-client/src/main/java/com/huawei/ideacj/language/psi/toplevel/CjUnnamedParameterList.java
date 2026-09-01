/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.psi.PsiElementInfoHandler;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * UnnamedParameterList
 *
 * @since 2021-07-31
 */
public class CjUnnamedParameterList extends CJPsiNode {
    private final LinkedHashMap<String, String> unnamedParamMap = new LinkedHashMap<>();

    private final List<String> typeParams = new ArrayList<>();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    public CjUnnamedParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public LinkedHashMap<String, String> getUnnamedParamMap() {
        boolean isEmpty;
        readLock.lock();
        try {
            isEmpty = unnamedParamMap.isEmpty();
        } finally {
            readLock.unlock();
        }
        if (isEmpty) {
            initUnnamedParamMap();
        }
        readLock.lock();
        try {
            return new LinkedHashMap<>(unnamedParamMap);
        } finally {
            readLock.unlock();
        }
    }

    public List<String> getTypeParams() {
        initUnnamedParamMap();
        readLock.lock();
        try {
            return new ArrayList<>(typeParams);
        } finally {
            readLock.unlock();
        }
    }

    private void initUnnamedParamMap() {
        writeLock.lock();
        try {
            unnamedParamMap.clear();
            typeParams.clear();
            new CjUnnamedParameterListHandler().processAllElement(this.getChildren());
        } finally {
            writeLock.unlock();
        }
    }

    private class CjUnnamedParameterListHandler extends PsiElementInfoHandler {
        public CjUnnamedParameterListHandler() {
            initHandlers();
        }

        @Override
        public final void initHandlers() {
            handlers.put(
                    CjUnnamedParameter.class,
                    element -> {
                        if (element instanceof CjUnnamedParameter) {
                            unnamedParamMap.put(
                                    ((CjUnnamedParameter) element).getParameterName(),
                                    ((CjUnnamedParameter) element).getParameterType());
                            typeParams.add(((CjUnnamedParameter) element).getParameterType());
                        }
                    });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CjUnnamedParameterList that = (CjUnnamedParameterList) obj;
        readLock.lock();
        try {
            that.readLock.lock();
            try {
                return Objects.equals(unnamedParamMap, that.unnamedParamMap)
                        && Objects.equals(typeParams, that.typeParams);
            } finally {
                that.readLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return Objects.hash(new LinkedHashMap<>(unnamedParamMap), new ArrayList<>(typeParams));
        } finally {
            readLock.unlock();
        }
    }
}
