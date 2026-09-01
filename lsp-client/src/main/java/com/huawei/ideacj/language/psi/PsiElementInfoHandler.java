/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.language.psi;

import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Handler for get PsiElement info, using table-driven
 *
 * @since 2020-08-09
 */
public abstract class PsiElementInfoHandler {
    /**
     * Map to store Info handlers for each type of PsiElement
     */
    public final HashMap<Class<? extends PsiElement>, InfoHandler> handlers = new HashMap<>();

    /**
     * Call initHandlers function
     */
    public PsiElementInfoHandler() {
        initWrapper();
    }

    /**
     * Handler for get PsiElement info
     *
     * @since 2020-08-09
     */
    public interface InfoHandler {
        /**
         * Process.
         *
         * @param element the element
         */
        void process(PsiElement element);
    }

    private void initWrapper() {
        initHandlers();
    }

    /**
     * Init handlers for different PsiElement
     */
    public abstract void initHandlers();

    /**
     * Process each element using handlers map
     *
     * @param elements elements to be handled
     */
    public void processAllElement(PsiElement[] elements) {
        Arrays.stream(elements).forEach(this::handle);
    }

    private void handle(PsiElement element) {
        InfoHandler handler = handlers.get(element.getClass());
        if (handler != null) {
            handler.process(element);
        }
    }
}
