/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.icon;

import com.intellij.icons.AllIcons.Nodes;
import com.intellij.openapi.util.IconLoader;

import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.SymbolKind;
import org.wso2.lsp4intellij.contributors.icon.LSPDefaultIconProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

/**
 * Cangjie Icon Provider
 *
 * @author xiewen
 * @since 2022-04-02
 */
public class CangjieIconProvider extends LSPDefaultIconProvider {
    static final Icon STRUCT = IconLoader.getIcon("/icons/struct.svg", CangjieIconProvider.class);

    private static final Map<CompletionItemKind, Icon> CANGJIE_ICON_MAP = new ConcurrentHashMap();

    static {
        CANGJIE_ICON_MAP.put(CompletionItemKind.Struct, STRUCT);
        CANGJIE_ICON_MAP.put(CompletionItemKind.Method, Nodes.Function);
    }

    @Override
    public String toString() {
        return "CangjieIconProvider{}";
    }

    @Override
    public Icon getSymbolIcon(SymbolKind kind) {
        if (kind == null) {
            return null;
        }

        return switch (kind) {
            case Field, EnumMember -> Nodes.Field;
            case Function -> Nodes.Function;
            case Object -> Nodes.Type;
            case Method -> Nodes.Method;
            case Variable -> Nodes.Variable;
            case Class -> Nodes.Class;
            case Constructor -> Nodes.ClassInitializer;
            case Enum -> Nodes.Enum;
            case Struct -> STRUCT;
            case Property -> Nodes.Property;
            case Interface -> Nodes.Interface;
            default -> Nodes.Tag;
        };
    }

    @Override
    public Icon getCompletionIcon(CompletionItemKind kind) {
        Icon icon = super.getCompletionIcon(kind);
        if (kind == CompletionItemKind.Struct || kind == CompletionItemKind.Method) {
            icon = CANGJIE_ICON_MAP.get(kind);
        }
        return icon;
    }
}
