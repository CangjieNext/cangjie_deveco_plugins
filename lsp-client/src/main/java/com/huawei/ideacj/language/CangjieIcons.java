/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * definition Cangjie icon
 *
 * @author rice
 * @since 2020-06-08
 */
public class CangjieIcons {
    /**
     * definition Cangjie language icon
     */
    public static final Icon CANGJIE_FILE = IconLoader.getIcon("/icons/Cangjie_logo_icon.svg",
            CangjieIcons.class);

    /**
     * definition Cangjie language icon
     */
    public static final Icon CANGJIE_EXTEND = IconLoader.getIcon("/icons/extend.svg", CangjieIcons.class);

    /**
     * definition Cangjie enum icon
     */
    public static final Icon CANGJIE_ENUM = AllIcons.Nodes.Enum;

    /**
     * definition Cangjie class icon
     */
    public static final Icon CANGJIE_CLASS = AllIcons.Nodes.Class;

    /**
     * definition Cangjie interface icon
     */
    public static final Icon CANGJIE_INTERFACE = AllIcons.Nodes.Interface;

    /**
     * definition Cangjie function icon
     */
    public static final Icon CANGJIE_FUNCTION = AllIcons.Nodes.Function;

    /**
     * definition Cangjie variable icon
     */
    public static final Icon CANGJIE_VARIABLE = AllIcons.Nodes.Variable;

    /**
     * definition Cangjie record icon
     */
    public static final Icon CANGJIE_STRUCT = IconLoader.getIcon("/icons/struct.svg", CangjieIcons.class);

    /**
     * definition Cangjie Alias icon
     */
    public static final Icon CANGJIE_TYPEALIAS = AllIcons.Nodes.Type;

    /**
     * definition Cangjie function icon
     */
    public static final Icon CANGJIE_PROPERTY = AllIcons.Nodes.Property;

    /**
     * definition Cangjie function icon
     */
    public static final Icon CANGJIE_MACRO = IconLoader.getIcon("/icons/macro.svg", CangjieIcons.class);
}
