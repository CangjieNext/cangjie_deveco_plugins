/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.lsp.extend.params;

import org.eclipse.lsp4j.jsonrpc.util.Preconditions;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Overridable Methods
 *
 * @since 2025-07-01
 */
public class OverridableMethods {
    private final String fullPackageName;

    private final String identifier;

    private final String kind;

    private final List<OverridableMethodInfo> data;

    public OverridableMethods(String fullPackageName, String identifier, String kind,
                              List<OverridableMethodInfo> data) {
        this.fullPackageName = Preconditions.checkNotNull(fullPackageName, "fullPackageName");
        this.identifier = Preconditions.checkNotNull(identifier, "identifier");
        this.kind = Preconditions.checkNotNull(kind, "kind");
        this.data = Preconditions.checkNotNull(data, "data");
    }

    /**
     * Return data
     *
     * @return List<OverridableMethodInfo>
     */
    @NonNull
    public List<OverridableMethodInfo> getData() {
        List<OverridableMethodInfo> processedData = new ArrayList<>();
        for (OverridableMethodInfo info : data) {
            if (info.getSignatureWithRet() == null || info.getSignatureWithRet().isEmpty()) {
                // 从 insertText 提取签名并创建新的对象
                String extractedSignature = extractSignatureFromInsertText(info.getInsertText(), info.getIsProp());
                OverridableMethodInfo newInfo = new OverridableMethodInfo(
                    info.getDeprecated(),
                    info.getIsProp(),
                    extractedSignature,
                    info.getInsertText()
                );
                processedData.add(newInfo);
            } else {
                processedData.add(info);
            }
        }
        return processedData;
    }

    /**
     * 从 insertText 中提取方法或属性签名
     *
     * @param insertText 插入文本，如 "public func test1(x: Int64): Unit {\n    ... \n}"
     * @param isProp 是否为属性
     * @return 提取的签名，如 "test1(x: Int64): Unit" 或 "c: Int64"
     */
    private String extractSignatureFromInsertText(String insertText, boolean isProp) {
        // 获取第一行
        int newlineIndex = insertText.indexOf('\n');
        String firstLine;
        if (newlineIndex > 0) {
            firstLine = insertText.substring(0, newlineIndex).trim();
        } else {
            firstLine = insertText.trim();
        }

        if (isProp) {
            // 属性：提取 "prop 名称：类型" 部分
            // 例如："public mut prop c: Int64 {" -> "c: Int64"
            int propIndex = firstLine.indexOf("prop ");
            if (propIndex >= 0) {
                String afterProp = firstLine.substring(propIndex + 5).trim();  // 跳过 "prop "
                int braceIndex = afterProp.indexOf('{');
                if (braceIndex > 0) {
                    return afterProp.substring(0, braceIndex).trim();
                }
                return afterProp;
            }
        } else {
            // 方法：提取 "func 名称 (参数): 返回类型" 部分
            // 例如："public func test1(x: Int64): Unit {" -> "test1(x: Int64): Unit"
            int funcIndex = firstLine.indexOf("func ");
            if (funcIndex >= 0) {
                String afterFunc = firstLine.substring(funcIndex + 5).trim();  // 跳过 "func "
                int braceIndex = afterFunc.indexOf('{');
                if (braceIndex > 0) {
                    return afterFunc.substring(0, braceIndex).trim();
                }
                return afterFunc;
            }
        }

        return firstLine;
    }

    /**
     * Return full package name
     *
     * @return String
     */
    @NonNull
    public String getFullPackageName() {
        return fullPackageName;
    }

    /**
     * Return identifier
     *
     * @return String
     */
    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Return kind
     *
     * @return String
     */
    @NonNull
    public String getKind() {
        return kind;
    }
}
