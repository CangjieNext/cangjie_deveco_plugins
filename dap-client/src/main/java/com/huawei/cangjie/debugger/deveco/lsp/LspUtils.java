/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cangjie.debugger.deveco.lsp;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * utils class for calling LSP related code
 *
 * @since 2022-11-21
 */
public class LspUtils {
    /**
     * lsp line comment attrs key
     */
    public static final String LSP_LINE_COMMENT_ATTRS_KEY = "LINE_COMMENT";

    private static final String CANGJIE_FILE_TYPE = "Cangjie";

    private static final List<String> LSP_SUPPORTED_FILE_TYPES =
            List.of(CANGJIE_FILE_TYPE);

    private static final String LSP_EDITORS_PROVIDER_CLASS_NAME =
            "com.huawei.cjlsp.capabilities.completion.CangjieDebuggerEditorsProvider";

    /**
     * check is virtual file supported by lsp
     *
     * @param file file
     * @return boolean is virtual file supported by Lsp
     */
    public static boolean isVirtualFileSupportedByLsp(VirtualFile file) {
        return LSP_SUPPORTED_FILE_TYPES.stream().anyMatch(name -> name.equals(file.getFileType().getName()));
    }

    /**
     * get xdebugger editor provider
     *
     * @return com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
     */
    public static XDebuggerEditorsProvider getXDebuggerEditorsProvider() {
        try {
            Class<?> clazz = Class.forName(LSP_EDITORS_PROVIDER_CLASS_NAME);
            Constructor<?> constructor = clazz.getConstructor();
            Object object = constructor.newInstance();
            if (object instanceof XDebuggerEditorsProvider) {
                return (XDebuggerEditorsProvider) object;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                | IllegalAccessException | InvocationTargetException e) {
            return DumbXDebuggerEditorsProvider.INSTANCE;
        }
        return DumbXDebuggerEditorsProvider.INSTANCE;
    }
}
