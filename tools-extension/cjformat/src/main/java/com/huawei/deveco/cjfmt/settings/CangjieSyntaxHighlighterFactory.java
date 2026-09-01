/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.settings;

import com.huawei.ideacj.syntaxhighlighter.CangjieSyntaxHighlighter;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * The type Cangjie syntax highlighter factory.
 *
 * @since 2024 -11-7
 */
public class CangjieSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return new CangjieSyntaxHighlighter();
    }

    /**
     * Create cangjie highlighter editor highlighter.
     *
     * @param settings the settings
     * @return the editor highlighter
     */
    public static EditorHighlighter createCangjieHighlighter(EditorColorsScheme settings) {
        return HighlighterFactory.createHighlighter(new CangjieSyntaxHighlighter(), settings);
    }
}
