/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.lsp.utils;

import com.huawei.ideacj.lsp.extend.ExtendRequestManager;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.swing.JLabel;

/**
 * CommonUtils
 *
 * @since 2024-11-04
 */
public class CommonUtils {
    private static final Logger LOG = Logger.getInstance(CommonUtils.class);

    private static final String MACRO_EXPANDING_TIP = "waiting macro expand...";

    private static final int RAW_IDENT_MIN_LEN = 3;

    private static final String CANGJIE_CODE_STYLE = "com.huawei.deveco.cjfmt.settings.CangjieCodeStyleSettings";

    private static final String INDENT_WIDTH = "indentWidth";

    /**
     * deleteTempLspBuildDir
     *
     * @param path Path
     */
    public static void tryDeleteDirectory(Path path) {
        if (!path.toFile().exists()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            int retryCount = 20;
            while (retryCount > 0) {
                deleteDirectory(path);
                if (!path.toFile().exists()) {
                    return;
                }
                retryCount--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    /**
     * deleteDirectory
     *
     * @param path Path
     */
    public static void deleteDirectory(Path path) {
        if (!java.nio.file.Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = java.nio.file.Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    java.nio.file.Files.delete(p);
                } catch (IOException e) {
                    LOG.warn("Cangjie related LSP: retry to delete the file failed: " + p);
                }
            });
        } catch (IOException e) {
            LOG.warn("Cangjie related LSP: retry to delete the dir failed " + path);
        }
    }


    /**
     * subLetterFromString
     *
     * @param string input
     * @return subbed string
     */
    public static String subLetterFromString(String string) {
        if (!Character.isUnicodeIdentifierStart(string.charAt(0))) {
            return string;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (!Character.isUnicodeIdentifierPart(c)) {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * renameLspBuildPath
     *
     * @param project Project
     * @return Path
     */
    public static Path renameLspBuildPath(Project project) {
        Path lspBuildPath = Paths.get(project.getBasePath(), ".idea", ".deveco", "cangjie", ".cache", "lsp");
        Path lspBuildTempPath = Paths.get(project.getBasePath(),
                ".idea", ".deveco", "cangjie", ".cache", "lsp_temp");
        if (lspBuildPath.toFile().exists()) {
            if (!lspBuildPath.toFile().renameTo(lspBuildTempPath.toFile())) {
                LOG.warn(".cache/lsp dir rename failed");
            }
        }
        return lspBuildTempPath;
    }

    /**
     * close current displaying ToolTip
     */
    public static void closeCompletionToolTip() {
        IdeTooltipManager tooltipManager = IdeTooltipManager.getInstance();
        if (!tooltipManager.hasCurrent()) {
            return;
        }
        try {
            Field field = IdeTooltipManager.class.getDeclaredField("currentTooltip");
            field.setAccessible(true);
            if (!(field.get(tooltipManager) instanceof IdeTooltip tooltip)
                    || !(tooltip.getTipComponent() instanceof JLabel tipComponent)) {
                return;
            }
            if (!MACRO_EXPANDING_TIP.equals(tipComponent.getText())) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(tooltip::hide);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.warn("Get current ToolTip exception.");
        }
    }

    /**
     * check if identifier is valid
     *
     * @param input identifier
     * @return bool
     */
    public static boolean isValidIdentifier(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        boolean isRawIdentifier = input.length() >= RAW_IDENT_MIN_LEN && input.startsWith("`") && input.endsWith("`");
        String relIdent = input;
        if (isRawIdentifier) {
            relIdent = input.substring(1, input.length() - 1);
        }
        char firstChar = relIdent.charAt(0);
        // start with _ or UnicodeIdentifierStart
        if (!(firstChar == '_' || Character.isUnicodeIdentifierStart(firstChar))) {
            return false;
        }

        for (int i = 1; i < relIdent.length(); i++) {
            char ch = relIdent.charAt(i);
            if (!Character.isUnicodeIdentifierPart(ch)) {
                return false;
            }
        }

        return true;
    }

    /**
     * get cangjie CodeStyle indent
     *
     * @param file file
     * @return indent
     */
    public static int getCodeStyleIndent(@NotNull PsiFile file) {
        int indentSize = 4;
        CodeStyleSettings settings = CodeStyle.getSettings(file);
        try {
            Class<?> cjCodeStyleSettingsClass = Class.forName(CANGJIE_CODE_STYLE);
            Method getCustomSettingsMethod = CodeStyleSettings.class.getMethod("getCustomSettings", Class.class);
            Object customSettings = getCustomSettingsMethod.invoke(settings, cjCodeStyleSettingsClass);
            if (customSettings == null) {
                return indentSize;
            }
            Field indentWidthField = cjCodeStyleSettingsClass.getField(INDENT_WIDTH);
            indentSize = (int) indentWidthField.get(customSettings);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException e) {
            LOG.warn("Get CangjieCodeStyleSettings indent fail.");
        }
        return indentSize;
    }

    /**
     * 请求超时检测是否需要重启语言服务
     *
     * @param requestManager RequestManager
     */
    public static void timeoutCrashCheck(RequestManager requestManager) {
        if (requestManager instanceof ExtendRequestManager extendRequestManager) {
            extendRequestManager.checkStatus();
        }
    }
}
