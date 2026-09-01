/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjfmt.core;

import static com.huawei.deveco.cjfmt.utils.FormatConstant.REFORMAT_CODE;
import static com.huawei.deveco.cjfmt.utils.FormatConstant.REFORMAT_DIR;
import static com.huawei.deveco.cjfmt.utils.FormatUtils.getFormatToolPath;
import static com.huawei.deveco.cjfmt.utils.FormatUtils.notifyResult;
import static com.huawei.deveco.cjquality.utils.PathUtils.getExePath;
import static com.intellij.codeInsight.actions.TextRangeType.SELECTED_TEXT;
import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;

import com.huawei.deveco.cjfmt.settings.CangjieCodeStyleSettings;
import com.huawei.deveco.cjfmt.utils.ErrorInfo;
import com.huawei.deveco.cjfmt.utils.FormatUtils;
import com.huawei.deveco.cjquality.constants.CommonConstants;
import com.huawei.deveco.sdkmanager.core.util.StringUtil;
import com.huawei.deveco.cjquality.utils.ExecuteResult;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.deveco.cjquality.utils.NotificationUtil;
import com.huawei.deveco.cjquality.utils.ShellCommand;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.actions.LastRunReformatCodeOptionsProvider;
import com.intellij.codeInsight.actions.ReformatCodeRunOptions;
import com.intellij.codeInsight.actions.TextRangeType;
import com.intellij.codeInsight.actions.VcsFacade;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ReformatCodeService
 *
 * @since 2023 -01-14
 */
public class ReformatCodeService {
    private static final LogPrinter LOGGER = LogPrinter.createLogger(ReformatCodeService.class);

    private static final String REFORMAT_FILE = "-f";

    private static final String REFORMAT_LINE = "-l";

    /**
     * getInstance
     *
     * @param project project
     * @return ReformatCodeService instance
     */
    public static ReformatCodeService getInstance(Project project) {
        return project.getService(ReformatCodeService.class);
    }

    /**
     * reformatCode
     *
     * @param file file
     * @param project project
     * @return optional result
     */
    public Optional<ExecuteResult> reformatCode(VirtualFile file, Project project) {
        if (!file.exists()) {
            LOGGER.warn("File does not exist: " + file.getPath());
            return Optional.empty();
        }
        if (file.isDirectory()) {
            return reformatDirectoryCode(file, project, REFORMAT_DIR);
        }
        return getExecuteResult(file, project, REFORMAT_FILE);
    }

    /**
     * reformat directory code
     *
     * @param file directory file
     * @param project project obj
     * @param formatOption format option
     * @return optional result
     */
    public Optional<ExecuteResult> reformatDirectoryCode(VirtualFile file, Project project, String formatOption) {
        return getExecuteResult(file, project, formatOption);
    }

    private Optional<ExecuteResult> getExecuteResult(VirtualFile file, Project project, String formatOption) {
        Optional<String> exePathOptional = getExePath(project);
        if (exePathOptional.isEmpty()) {
            return Optional.of(new ExecuteResult(1, ErrorInfo.SDK_ERROR.getValue()));
        }
        String executePath = exePathOptional.get();
        List<String> processArgs = getFormatToolPath(executePath);
        processArgs.add(formatOption);
        processArgs.add(toSystemIndependentName(file.getPath()));
        FormatUtils.addCjfmtConfigParam(processArgs, project);
        return ShellCommand.executeCommand(processArgs, executePath, project);
    }

    private Optional<ExecuteResult> reformatSelectedCode(VirtualFile file, @NotNull Editor editor, Project project) {
        Optional<String> exePathOptional = getExePath(project);
        if (exePathOptional.isEmpty()) {
            LOGGER.warn("Reformat Selected Code error, file path is {}", file.getPath());
            return Optional.of(new ExecuteResult(1, ErrorInfo.SDK_ERROR.getValue()));
        }
        @NotNull
        Collection<LineRange> ranges = getRangesToFormat(editor);
        ranges.forEach(lineRange -> {
            List<String> processArgs = getFormatToolPath(exePathOptional.get());
            processArgs.add(REFORMAT_FILE);
            processArgs.add(toSystemIndependentName(file.getPath()));
            processArgs.add(REFORMAT_LINE);
            processArgs.add(lineRange.startLine() + CommonConstants.COLON + lineRange.endLine());
            FormatUtils.addCjfmtConfigParam(processArgs, project);
            Optional<ExecuteResult> executeResult =
                ShellCommand.executeCommand(processArgs, exePathOptional.get(), project);
            if (executeResult.isEmpty()) {
                LOGGER.warn("Reformat Selected Code error, file path is {}", file.getPath());
            }
        });
        return Optional.of(new ExecuteResult(0, StringUtil.EMPTY));
    }

    @NotNull
    private Collection<LineRange> getRangesToFormat(@NotNull Editor editor) {
        final List<LineRange> ranges = new SmartList<>();
        SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.hasSelection()) {
            int startLine = editor.getDocument().getLineNumber(selectionModel.getSelectionStart());
            int endLine = editor.getDocument().getLineNumber(selectionModel.getSelectionEnd());
            LineRange lineRange = LineRange.create(startLine + 1, endLine + 1);
            ranges.add(lineRange);
        }
        return ranges;
    }

    /**
     * reformat file
     *
     * @param file format file
     * @param hasSelection has selection code
     * @param project project
     * @param editor editor
     */
    public void reformatFileCode(PsiFile file, boolean hasSelection, Project project, Editor editor) {
        boolean isProcessSelectedText = getProcessSelectedText(file, hasSelection, project);
        initCodeStyleConfig(project, file);
        Optional<ExecuteResult> resultOptional;
        if (isProcessSelectedText) {
            resultOptional = reformatSelectedCode(file.getVirtualFile(), editor, project);
        } else {
            resultOptional = reformatCode(file.getVirtualFile(), project);
        }
        if (resultOptional.isEmpty()) {
            LOGGER.warn("Reformat Selected Code error, file path is {}", file.getVirtualFile().toString());
            notifyResult(false, project);
            return;
        }
        if (resultOptional.get().exitCode() != 0) {
            LOGGER.warn("reformat code failed, exit code is {}", String.valueOf(resultOptional.get().exitCode()));
            NotificationUtil.showWithProject(resultOptional.get().executeOut(), REFORMAT_CODE,
                NotificationUtil.Type.INFO, project);
        }
    }

    /**
     * Init code style config.
     *
     * @param project the project
     * @param file the file
     */
    public void initCodeStyleConfig(Project project, PsiFile file) {
        CangjieCodeStyleSettings settings = CodeStyle.getCustomSettings(file, CangjieCodeStyleSettings.class);
        try {
            CangjieCodeStyleSettings.writeConfigToFile(settings,
                FormatUtils.getCjfmtConfigPath(project));
        } catch (IOException e) {
            LOGGER.error("Cangjie cjfmt write codeStyle config file failed.", e);
        }
    }

    private boolean getProcessSelectedText(PsiFile file, boolean hasSelection, Project project) {
        LastRunReformatCodeOptionsProvider provider =
            new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
        ReformatCodeRunOptions currentRunOptions = provider.getLastRunOptions(file);
        TextRangeType processingScope = getProcessingScope(currentRunOptions, hasSelection, project, file);
        currentRunOptions.setProcessingScope(processingScope);
        return currentRunOptions.getTextRangeType() == SELECTED_TEXT;
    }

    private TextRangeType getProcessingScope(ReformatCodeRunOptions currentRunOptions, boolean hasSelection,
        Project project, PsiFile file) {
        TextRangeType processingScope = currentRunOptions.getTextRangeType();
        if (hasSelection) {
            processingScope = TextRangeType.SELECTED_TEXT;
        } else if (processingScope == TextRangeType.VCS_CHANGED_TEXT) {
            if (VcsFacade.getInstance().isChangeNotTrackedForFile(project, file)) {
                processingScope = TextRangeType.WHOLE_FILE;
            }
        } else {
            processingScope = TextRangeType.WHOLE_FILE;
        }
        return processingScope;
    }
}