/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjlint;

import com.huawei.ace.language.psi.lang.ExtendTypeScriptLanguage;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.tools.idea.codecheck.support.model.CodeMarsResult;
import com.huawei.tools.idea.codecheck.utils.PsiUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * psi handler class
 *
 * @since 2024-04-02
 */
public class PsiHandler {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(PsiHandler.class);

    private static final String IGNORE_FILE_START = "// cjlint-ignore -start ";

    private static final String IGNORE_FILE_END = "// cjlint-ignore -end";

    /**
     * 创建屏蔽整个文件的注释
     *
     * @param project 当前文件
     * @param defectFile defect file
     * @return PsiComment
     */
    public static PsiElement createIgnoreStartComment(@NotNull Project project,
        CodeMarsResult.@NotNull DefectFile defectFile) {
        List<CodeMarsResult.DefectInfo> defects = defectFile.getDefects();
        StringBuilder stringBuilder = new StringBuilder(IGNORE_FILE_START);
        defects.forEach(
            defectInfo -> stringBuilder.append(" ").append(CjLintManager.getIgnoreRule(defectInfo.getRuleId())));
        return ApplicationManager.getApplication()
            .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil
                .findChildOfType(createDummyFile(stringBuilder.toString(), project), PsiComment.class, true));
    }

    /**
     * create ignore end comment
     *
     * @param project project
     * @return psi element
     */
    public static PsiElement createIgnoreEndComment(@NotNull Project project) {
        return ApplicationManager.getApplication()
            .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil
                .findChildOfType(createDummyFile(IGNORE_FILE_END, project), PsiComment.class, true));
    }

    /**
     * create new comment
     *
     * @param ignoreRule ignore rule
     * @param project project
     * @return psi element
     */
    public static PsiElement createNewComment(@NotNull String ignoreRule, @NotNull Project project) {
        String content = String.format(Locale.ROOT, "// cjlint-ignore " + ignoreRule);
        return ApplicationManager.getApplication()
            .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil.findChildOfType(createDummyFile(content, project),
                PsiComment.class, true));
    }

    /**
     * create dummy file
     *
     * @param content content
     * @param project project
     * @return psi file
     */
    @Nullable
    public static PsiFile createDummyFile(@NotNull String content, @NotNull Project project) {
        PsiFile file;
        try {
            file = PsiFileFactory.getInstance(project).createFileFromText(ExtendTypeScriptLanguage.INSTANCE, content);
        } catch (IncorrectOperationException exception) {
            LOGGER.warn("IncorrectOperationException occurred, createDummyFile fail");
            return null;
        }
        return file;
    }

    /**
     * already disabled whole file
     *
     * @param document document
     * @return true or false
     */
    public static boolean alreadyDisabledWholeFile(Document document) {
        int lineStartOffset = document.getLineStartOffset(0);
        int lineEndOffset = document.getLineEndOffset(0);
        @NotNull
        String text = document.getText(TextRange.create(lineStartOffset, lineEndOffset));
        return text.startsWith(IGNORE_FILE_START);
    }

    /**
     * get add comment at bottom file
     *
     * @param project project
     * @param psiFile psi file
     * @param lastChild last child
     * @param ignoreEndComment ignore end comment
     * @return psi element
     */
    public static Computable<PsiElement> getAddCommentAtBottomFileComputable(Project project, PsiFile psiFile,
        PsiElement lastChild, PsiElement ignoreEndComment) {
        return () -> {
            PsiElement lineBreak = PsiUtil.createLineBreak(project);
            psiFile.addAfter(lineBreak, lastChild);
            PsiElement result = psiFile.addAfter(ignoreEndComment, lineBreak);
            CodeStyleManager.getInstance(project)
                .reformatNewlyAddedElement(result.getParent().getNode(), result.getNode());
            return result;
        };
    }

    /**
     * add comment at top file
     *
     * @param project project
     * @param psiFile psi file
     * @param firstChild first child
     * @param comment comment
     * @return psi element
     */
    @NotNull
    public static Computable<PsiElement> getAddCommentAtTopFileComputable(@NotNull Project project,
        @NotNull PsiFile psiFile, @NotNull PsiElement firstChild, @NotNull PsiElement comment) {
        return () -> {
            PsiElement result = psiFile.addBefore(comment, firstChild);
            CodeStyleManager.getInstance(project)
                .reformatNewlyAddedElement(result.getParent().getNode(), result.getNode());
            psiFile.addBefore(PsiUtil.createLineBreak(project), firstChild);
            return result;
        };
    }
}
