/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi.toplevel;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.psi.CangjieGetID;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * TypeParameters
 *
 * @since 2021-07-31
 */
public class CjTypeParameters extends CJPsiNode implements CangjieGetID {
    private ArrayList<String> typeParams = null;

    public CjTypeParameters(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Return typeParams
     *
     * @return List<String>
     */
    public ArrayList<String> getTypeParams() {
        initTypeParams();
        return typeParams;
    }

    private void initTypeParams() {
        ArrayList<String> typeParamsTemp = new ArrayList<>();
        Arrays.stream(this.getChildren())
            .filter(CjPsiUtils.INSTANCE::isIdentifier)
            .forEach(element -> typeParamsTemp.add(element.getText()));
        typeParams = typeParamsTemp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CjTypeParameters that = (obj instanceof CjTypeParameters) ? (CjTypeParameters) obj : null;
        return Objects.equals(typeParams, that == null ? null : that.typeParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeParams == null ? null : new ArrayList<>(typeParams));
    }
}
