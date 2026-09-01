/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.deveco.cjlint;

import static com.huawei.cangjie.projectmgmt.utils.FileUtils.getCangjieModuleSrcDir;
import static com.huawei.deveco.cjlint.utils.CodeCheckConstantUtil.ENGINE_NAME;
import static com.huawei.deveco.cjlint.utils.CodeCheckConstantUtil.LANGUAGE_CANGJIE;
import static com.huawei.deveco.cjlint.utils.CodeCheckConstantUtil.REGEX;
import static com.huawei.deveco.cjlint.utils.CodeCheckConstantUtil.SEPARATOR;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.CANGJIE;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.HUMP_CANGJIE;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.MAIN;
import static com.huawei.deveco.cjquality.constants.CangjieConstants.SRC;
import static com.huawei.deveco.cjquality.utils.ModuleUtils.getModuleFromFile;
import static com.huawei.tools.idea.codecheck.utils.CodeCheckUtil.getProjectBasePath;
import static com.intellij.util.PathUtil.toSystemIndependentName;

import com.huawei.deveco.projectmodel.ohos.model.ModuleModel;
import com.huawei.deveco.projectmodel.ohos.model.impl.OhosModuleModel;
import com.huawei.deveco.cjquality.utils.LogPrinter;
import com.huawei.deveco.cjquality.utils.NotificationUtil;
import com.huawei.deveco.cjquality.utils.PathUtils;
import com.huawei.ideacj.language.psi.toplevel.CjTopLevelObject;
import com.huawei.tools.idea.codecheck.core.cmd.CodeCheckCmd;
import com.huawei.tools.idea.codecheck.extensions.CodeLinterEngineProvider;
import com.huawei.tools.idea.codecheck.extensions.window.panel.ResultContentPanel;
import com.huawei.tools.idea.codecheck.extensions.window.problem.DefectProblemDescriptor;
import com.huawei.tools.idea.codecheck.extensions.window.problem.DefectProblemsFactory;
import com.huawei.tools.idea.codecheck.support.constant.Constant;
import com.huawei.tools.idea.codecheck.support.model.CheckCodeParams;
import com.huawei.tools.idea.codecheck.support.model.CodeMarsResult;
import com.huawei.tools.idea.codecheck.utils.CodeCheckAgentUtil;
import com.huawei.tools.idea.codecheck.utils.EditorUtil;
import com.huawei.tools.idea.codecheck.utils.FileUtil;
import com.huawei.tools.idea.codecheck.utils.PsiUtil;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lint engine
 *
 * @since 2023-01-14
 */
public class CjlintEngine extends CodeLinterEngineProvider {
    static final String LIB_PATH = "lib";

    private static final LogPrinter LOGGER = LogPrinter.createLogger(CjlintEngine.class);

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(List.of(LANGUAGE_CANGJIE));

    private static final String NEW_LINE = "\n";

    private static final String OUTPUT_FILE_NAME = "cjoutput.json";

    private static final String TOOLS = "tools";

    private static final String BIN = "bin";

    private static final String CJLINT_WINDOWS = "cjlint.exe";

    private static final String CJLINT_MAC = "cjlint";

    private static final Pattern CJLINT_IGNORE_MATCHER =
        Pattern.compile("(// cjlint-ignore)(\\s(![A-Z.]{1,10}.\\d{1,5}))+");

    private static final String IGNORE_TITLE = "// cjlint-ignore";

    private static final int MIN_LENGTH = 13;

    private boolean isImport;

    private Project currentProject;

    private boolean isLastTopLevel;

    /**
     * 命令行获取
     *
     * @param checkCodeParams 路径参数
     * @param project project obj
     * @return 完善后的命令行
     */
    @Override
    public GeneralCommandLine buildCommandLine(@NotNull CheckCodeParams checkCodeParams, @NotNull Project project) {
        String cjOutPath = "";
        try {
            String projectCachePath = createProjectCacheDir(project);
            String resultCacheDir = projectCachePath + File.separator + getEngineName();
            File resultCacheDirFile = new File(resultCacheDir);
            if (!resultCacheDirFile.isDirectory()) {
                boolean isCreated = resultCacheDirFile.mkdir();
                LOGGER.debug("createResultCacheDirectory: result cache folder create: {}", String.valueOf(isCreated));
            }
            cjOutPath = resultCacheDirFile.getCanonicalPath() + File.separator + OUTPUT_FILE_NAME;
        } catch (IOException e) {
            LOGGER.warn("Init output path error.");
        }
        GeneralCommandLine cmdline = new GeneralCommandLine();
        Optional<String> compilerPathOptional = PathUtils.getCompilerPath(project);
        if (compilerPathOptional.isEmpty() || !new File(compilerPathOptional.get()).exists()) {
            LOGGER.warn("Can't get Cangjie SDK build-tools path");
            return cmdline;
        }
        Optional<String> exePathOptional = getCjlintExe(project);
        if (exePathOptional.isEmpty() || !new File(exePathOptional.get()).exists()) {
            LOGGER.warn("Can't get cjlint path");
        } else {
            cmdline.setExePath(exePathOptional.get());
            cmdline.addParameter(getSrcPath(checkCodeParams, project));
            cmdline.addParameter(cjOutPath);
        }
        cmdline.setWorkDirectory(CodeCheckAgentUtil.getWorkPath());
        return cmdline;
    }

    @Override
    public Set<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    private static String createProjectCacheDir(@NotNull Project project) throws IOException {
        final String basePath = getProjectBasePath(project).orElse("");
        LOGGER.debug("createProjectCacheDirectory: project base path = {}", basePath);
        String projectCachePath = toSystemIndependentName(
            basePath + File.separator + Constant.DOT_IDEA + File.separator + Constant.PROJECT_CACHE_DIRECTORY);
        File projectCacheFolder = new File(projectCachePath);
        if (!projectCacheFolder.isDirectory()) {
            boolean isCreated = projectCacheFolder.mkdir();
            LOGGER.debug("createProjectCacheDirectory: project cache folder create: {}", String.valueOf(isCreated));
        }
        // Delete the last cache files
        if (StringUtils.isNotEmpty(projectCachePath)) {
            FileUtil.deleteChild(projectCacheFolder);
        }
        return projectCacheFolder.getCanonicalPath();
    }

    @Override
    public String getRuleDocPath(CodeMarsResult.@NotNull DefectInfo defectInfo) {
        String defectInfoRuleDocPath = defectInfo.getRuleDocPath();
        if (com.intellij.openapi.util.io.FileUtil.isAbsolute(defectInfoRuleDocPath)) {
            return defectInfoRuleDocPath;
        }
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.huawei.cangjie-support-plugin"));
        if (plugin == null) {
            LOGGER.warn("Install cangjie sdk failed.");
            return "";
        }
        return plugin.getPluginPath() + File.separator + LIB_PATH + File.separator + ENGINE_NAME + File.separator
            + defectInfoRuleDocPath;
    }

    @Override
    public void shieldDefect(CodeMarsResult.@NotNull DefectInfo defectInfo, @NotNull Project project) {
        String ruleId = defectInfo.getRuleId();
        this.currentProject = project;
        Matcher matcher = Pattern.compile(REGEX).matcher(ruleId);
        if (CANGJIE.equals(defectInfo.getFileName()) && matcher.find()) {
            String projectBasePath = project.getBasePath();
            DefectProblemDescriptor descriptor = projectBasePath != null
                ? DefectProblemsFactory.getDefectDescriptor(projectBasePath).get(defectInfo.getMergeKey()) : null;
            if (descriptor == null) {
                LOGGER.warn("cj ShieldDetect fail, descriptor is null");
                return;
            }
            PsiElement psiElement = descriptor.getPsiElement();
            if (psiElement == null) {
                LOGGER.warn("cj ShieldDetect fail, get null PsiElement from descriptor");
                return;
            }
            if (!isDocumentCommitted(psiElement, project)) {
                return;
            }
            changeOrAddComment(defectInfo, psiElement);
        }
    }

    @Override
    public void shieldSelectedDefect(CodeMarsResult.@NotNull DefectFile defectFile, @NotNull Project project) {
        this.currentProject = project;
        if (defectFile.isAllChecked()) {
            // 屏蔽整个文件的检查
            PsiFile psiFile = EditorUtil.getPsiFile(defectFile.getFilePath(), currentProject);
            if (psiFile == null) {
                LOGGER.warn("cj handleSingleFile: shield fail, get null psiFile from filePath");
                return;
            }
            PsiElement firstChild = psiFile.getFirstChild();
            PsiElement lastChild = psiFile.getLastChild();
            if (firstChild == null || lastChild == null) {
                LOGGER.warn("cj handleSingleFile: the first or last child of PsiFile is null, shield fail");
                return;
            }
            Document document = ApplicationManager.getApplication()
                .runReadAction((Computable<Document>) () -> psiFile.getViewProvider().getDocument());
            // 需要判断当前文件第一行代码上是否以// cjlint-ignore -start的注释开头
            if (PsiHandler.alreadyDisabledWholeFile(document)) {
                LOGGER.info(
                    String.format(Locale.ROOT, "cj handleSingleFile: %s already been disabled", psiFile.getName()));
                return;
            }
            PsiElement ignoreStartComment = PsiHandler.createIgnoreStartComment(currentProject, defectFile);
            if (ignoreStartComment == null) {
                LOGGER.warn("cj handleSingleFile: create PsiComment failed, shield fail");
                return;
            }
            PsiElement ignoreEndComment = PsiHandler.createIgnoreEndComment(currentProject);
            if (ignoreEndComment == null) {
                LOGGER.warn("cj handleSingleFile: create PsiComment failed, shield fail");
                return;
            }
            try {
                performWriteActionWithFormatterDisabled(PsiHandler.getAddCommentAtTopFileComputable(currentProject,
                    psiFile, firstChild, ignoreStartComment), currentProject);
                performWriteActionWithFormatterDisabled(PsiHandler.getAddCommentAtBottomFileComputable(currentProject,
                    psiFile, lastChild, ignoreEndComment), currentProject);
            } catch (IncorrectOperationException exception) {
                LOGGER.warn("cj handleSingleFile: add comment fail, shield fail");
            }
        } else {
            // 屏蔽部分检查结果
            List<CodeMarsResult.DefectInfo> defects = defectFile.getDefects();
            if (defects == null || defects.isEmpty()) {
                LOGGER.warn("cj handleSingleFile: defects is null or empty, no need to shield");
                return;
            }
            defects.stream().filter(Objects::nonNull).forEach(defectInfo -> shieldDefect(defectInfo, currentProject));
        }
    }

    private void performWriteActionWithFormatterDisabled(Computable<PsiElement> computable, @NotNull Project project) {
        WriteCommandAction.runWriteCommandAction(project,
            (Computable<PsiElement>) () -> CodeStyleManager.getInstance(project)
                .performActionWithFormatterDisabled(computable));
    }

    private static Optional<String> getCjlintExe(@NotNull Project project) {
        Optional<String> compilerPathOptional = PathUtils.getCompilerPath(project);
        if (SystemInfo.isWindows) {
            return compilerPathOptional.map(s -> s + SEPARATOR + TOOLS + SEPARATOR + BIN + SEPARATOR + CJLINT_WINDOWS);
        } else {
            return compilerPathOptional.map(s -> s + SEPARATOR + TOOLS + SEPARATOR + BIN + SEPARATOR + CJLINT_MAC);
        }
    }

    private String getSrcPath(@NotNull CheckCodeParams checkCodeParams, @NotNull Project project) {
        List<String> checkedFilePaths = new Gson().fromJson(checkCodeParams.getCustomCheckPathJsonStr(),
            new TypeToken<List<String>>() {}.getType());
        for (String checkedFilePath : checkedFilePaths) {
            String filePath;
            try {
                filePath = new File(checkedFilePath).getCanonicalPath();
            } catch (IOException e) {
                String message = String.format("File %s was skipped because exception occurred.", checkedFilePath);
                LOGGER.warn(message);
                NotificationUtil.showWithProject(message, HUMP_CANGJIE, NotificationUtil.Type.WARN, project);
                continue;
            }
            ModuleModel moduleModel = getModuleFromFile(filePath, project);
            if (!(moduleModel instanceof OhosModuleModel)) {
                return "";
            }
            String cangjieModuleSrcDir = getCangjieModuleSrcDir((OhosModuleModel) moduleModel, false);
            if (StringUtil.isEmpty(cangjieModuleSrcDir)) {
                cangjieModuleSrcDir = SRC;
            }
            try {
                String cjpmDirPath = Path.of(moduleModel.getModulePath()).toString();
                if (!Path.of(cjpmDirPath, "cjpm.toml").toFile().exists()) {
                    cjpmDirPath = Path.of(moduleModel.getModulePath(), "src", "main", "cangjie").toString();
                }
                return Path.of(cjpmDirPath, cangjieModuleSrcDir)
                        .normalize().toString();
            } catch (InvalidPathException e) {
                LOGGER.warn("Invalid custom combination src-dir file path.");
                return Path.of(moduleModel.getModulePath(), SRC, MAIN, CANGJIE, SRC)
                        .normalize().toString();
            }
        }
        return "";
    }

    private boolean isDocumentCommitted(@NotNull PsiElement psiElement, @NotNull Project project) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile containingFile =
            ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) psiElement::getContainingFile);
        if (containingFile == null) {
            LOGGER.warn("cj ShieldDetect fail, containingFile is null");
            return false;
        }
        Document cachedDocument = documentManager.getCachedDocument(containingFile);
        if (cachedDocument != null && documentManager.isUncommited(cachedDocument)) {
            documentManager.commitDocument(cachedDocument);
        }
        return true;
    }

    private void changeOrAddComment(@NotNull CodeMarsResult.DefectInfo defectInfo, @NotNull PsiElement psiElement) {
        Document document = Optional
            .ofNullable(
                ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) psiElement::getContainingFile))
            .map(file -> ApplicationManager.getApplication()
                .runReadAction((Computable<Document>) () -> file.getViewProvider().getDocument()))
            .orElse(null);
        isImport = ApplicationManager.getApplication()
            .runReadAction((Computable<Boolean>) () -> getIsImport(document, psiElement));
        PsiElement anchor = ApplicationManager.getApplication()
            .runReadAction((Computable<PsiElement>) () -> calculateAnchor(psiElement, document));
        isLastTopLevel = false;
        Optional<PsiElement> insertIndexElementOptional = ApplicationManager.getApplication()
            .runReadAction((Computable<Optional<PsiElement>>) () -> getInsertPsiElement(psiElement, anchor, document));
        if (insertIndexElementOptional.isEmpty()) {
            LOGGER.warn("cj ShieldDetect fail, insertIndexElementOptional is null");
            return;
        }
        String ignoreRule = CjLintManager.getIgnoreRule(defectInfo.getRuleId());
        if (ignoreRule == null) {
            LOGGER.warn("cj ShieldDetect fail, ruleId is null");
            return;
        }
        CommentMatchResult matchResult = ApplicationManager.getApplication()
            .runReadAction(
                (Computable<CommentMatchResult>) () -> getMatchResult(anchor, ignoreRule, psiElement, document));
        PsiElement comment = null;
        boolean shouldDeleteOldComment = false;
        if (matchResult.isMatched()) {
            @NotNull
            Optional<PsiElement> commentOptional = addRuleInOldComment(ignoreRule, currentProject, matchResult);
            if (commentOptional.isPresent()) {
                comment = commentOptional.get();
                shouldDeleteOldComment = true;
            }
        } else {
            comment = PsiHandler.createNewComment(ignoreRule, currentProject);
        }
        if (comment == null) {
            LOGGER.warn("cj ShieldDetect fail, comment is null");
            return;
        }
        try {
            // Must not change PSI outside command or undo-transparent action.
            // See com.intellij.openapi.command.WriteCommandAction or com.intellij.openapi.command.CommandProcessor
            Project project =
                ApplicationManager.getApplication().runReadAction((Computable<Project>) psiElement::getProject);
            WriteCommandAction.runWriteCommandAction(project, getAddElementComputable(anchor, comment,
                insertIndexElementOptional.get(), shouldDeleteOldComment, matchResult));
            if (isLastTopLevel) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    PsiElement whiteSpace = ApplicationManager.getApplication()
                            .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil
                                    .findChildOfType(PsiHandler.createDummyFile(" ", currentProject),
                                            PsiWhiteSpace.class, true));
                    PsiElement parent = insertIndexElementOptional.get().getParent();
                    parent.addAfter(whiteSpace, insertIndexElementOptional.get());
                });
            }
        } catch (IncorrectOperationException exception) {
            LOGGER.warn("cj ShieldDetect fail, add or delete fail");
        }
    }

    private Optional<PsiElement> getInsertPsiElement(PsiElement elementAt, PsiElement anchor, Document document) {
        if (isImport) {
            if (anchor.getLastChild() == null) {
                return Optional.empty();
            }
            PsiElement prevElement = anchor.getLastChild();
            while (prevElement != null) {
                int lineStartNumber = PsiUtil.getLineStartNumber(document, prevElement);
                int psiStartNumber = PsiUtil.getLineStartNumber(document, elementAt);
                if (!(prevElement instanceof PsiComment) && !(prevElement instanceof PsiWhiteSpace)
                    && lineStartNumber == psiStartNumber) {
                    return Optional.of(prevElement);
                }
                prevElement = prevElement.getPrevSibling();
            }
            return Optional.empty();
        }
        int targetNumber = PsiUtil.getLineStartNumber(document, elementAt);
        if (targetNumber < 0) {
            LOGGER.warn("cj getParentElement is null: line number should be bigger than 0");
            return Optional.empty();
        }
        int lineEndOffset = document.getLineEndOffset(targetNumber);
        @Nullable
        PsiFile psiFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(document);
        if (psiFile == null) {
            return Optional.empty();
        }
        @Nullable
        PsiElement psiElement = psiFile.findElementAt(lineEndOffset);
        if (psiElement == null) {
            if (anchor instanceof CjTopLevelObject
                    && anchor.getParent() != null && anchor.equals(anchor.getParent().getLastChild())) {
                isLastTopLevel = true;
                return Optional.of(anchor);
            }
            return Optional.empty();
        }
        return Optional.of(psiElement);
    }

    private boolean getIsImport(Document document, PsiElement elementAt) {
        int targetNumber = PsiUtil.getLineStartNumber(document, elementAt);
        if (targetNumber < 0) {
            LOGGER.warn("cj getParentElement is null: line number should be bigger than 0");
            return false;
        }
        int lineStartOffset = document.getLineStartOffset(targetNumber);
        int lineEndOffset = document.getLineEndOffset(targetNumber);
        @Nullable
        PsiFile psiFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(document);
        if (psiFile == null) {
            return false;
        }
        @NotNull
        String text = document.getText(TextRange.create(lineStartOffset, lineEndOffset));
        return text.contains("import");
    }

    @NotNull
    private static Optional<PsiElement> addRuleInOldComment(@NotNull String ruleId, @NotNull Project project,
        @NotNull CommentMatchResult matchResult) {
        String text = matchResult.getCommentText();
        if (text == null) {
            LOGGER.warn("cj addRuleInOldComment: get null text from backward");
            return Optional.empty();
        }
        int length = text.length();
        // 逻辑走到这里，说明comment包含cjlint-ignore
        // cjlint-ignore的长度是13
        if (length < MIN_LENGTH) {
            LOGGER.warn("cj addRuleInOldComment: backward is not a valid comment");
            return Optional.empty();
        }
        boolean isRuleIdExist = matchResult.getCommentText().contains(ruleId);
        if (isRuleIdExist) {
            LOGGER.info("cj addRuleInOldComment: comment already contains target rule, no need to change comment");
            return Optional.empty();
        }
        String finalRule = ruleId;
        if (text.endsWith("*/")) {
            // 去掉末尾的"*/"
            text = text.substring(0, length - 2);
            finalRule = String.format(Locale.ROOT, "%s */", finalRule);
        }
        String commentFormat = "%s %s";
        String content = String.format(Locale.ROOT, commentFormat, text, finalRule);
        return Optional.of(ApplicationManager.getApplication()
            .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil
                .findChildOfType(PsiHandler.createDummyFile(content, project), PsiComment.class, true)));
    }

    private CommentMatchResult getMatchResult(@Nullable PsiElement anchor, String ignoreRule,
        @NotNull PsiElement psiElement, Document document) {
        CommentMatchResult result = new CommentMatchResult();
        if (isImport) {
            if (anchor == null) {
                return result;
            }
            String commentText = ApplicationManager.getApplication()
                .runReadAction((Computable<String>) () -> Optional.of(anchor).map(PsiElement::getText).orElse(""));
            if (StringUtil.isEmpty(commentText)) {
                return result;
            } else {
                return getImportCommentMatchResult(anchor, result);
            }
        }
        int targetNumber = PsiUtil.getLineStartNumber(document, psiElement);
        int lineStartOffset = document.getLineStartOffset(targetNumber);
        int lineEndOffset = document.getLineEndOffset(targetNumber);
        @NotNull
        String text = document.getText(TextRange.create(lineStartOffset, lineEndOffset));
        if (text.contains(IGNORE_TITLE)) {
            int index = text.indexOf(IGNORE_TITLE);
            @Nullable
            PsiFile psiFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(document);
            if (psiFile == null) {
                return result;
            }
            @Nullable
            PsiElement findElement = psiFile.findElementAt(lineStartOffset + index);
            if (findElement == null) {
                return result;
            }
            result.setMatched(true);
            result.setIgnorePsiElement(findElement);
            result.setCommentText(findElement.getText());
            return result;
        }
        return result;
    }

    private static CommentMatchResult getImportCommentMatchResult(PsiElement anchor, CommentMatchResult result) {
        Matcher macher;
        @NotNull
        PsiElement @NotNull [] children = anchor.getChildren();
        for (PsiElement child : children) {
            macher = CJLINT_IGNORE_MATCHER.matcher(child.getText());
            if (macher.matches()) {
                result.setMatched(true);
                result.setIgnorePsiElement(child);
                result.setCommentText(macher.group());
                return result;
            }
            PsiElement nextElement = child.getNextSibling();
            while (nextElement != null) {
                macher = CJLINT_IGNORE_MATCHER.matcher(nextElement.getText());
                if (macher.matches()) {
                    result.setMatched(true);
                    result.setIgnorePsiElement(nextElement);
                    result.setCommentText(macher.group());
                    return result;
                }
                nextElement = nextElement.getNextSibling();
            }
        }
        return result;
    }

    private Computable<PsiElement> getAddElementComputable(@NotNull PsiElement anchor, @NotNull PsiElement comment,
        PsiElement insertIndexElement, boolean shouldDeleteOldComment, CommentMatchResult matchResult) {
        return () -> {
            PsiElement psiElement = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiElement>) () -> PsiTreeUtil
                    .findChildOfType(PsiHandler.createDummyFile(" ", currentProject), PsiWhiteSpace.class, true));
            if (isImport) {
                ASTNode node = anchor.getNode();
                if (!shouldDeleteOldComment) {
                    node.addChild(psiElement.getNode(), insertIndexElement.getNode());
                }
                node.addChild(comment.getNode(), insertIndexElement.getNode());
            } else if (!isLastTopLevel) {
                PsiElement parentNode = insertIndexElement.getParent();
                parentNode.addBefore(psiElement, insertIndexElement);
                parentNode.addBefore(comment, insertIndexElement);
            } else {
                PsiElement parentNode = insertIndexElement.getParent();
                parentNode.addAfter(comment, insertIndexElement);
            }
            // 合并注释的场景下需要删除原来的注释
            // 删除注释的场景下不需要格式化，因为在插入的时候已经格式化过了
            if (shouldDeleteOldComment && matchResult.getIgnorePsiElement() != null) {
                CodeStyleManager.getInstance(currentProject)
                    .performActionWithFormatterDisabled((Runnable) () -> matchResult.getIgnorePsiElement().delete());
            }
            return comment;
        };
    }

    private PsiElement calculateAnchor(@NotNull PsiElement elementAt, Document document) {
        if (document == null) {
            return elementAt;
        }
        int targetNumber = PsiUtil.getLineStartNumber(document, elementAt);
        if (targetNumber < 0) {
            LOGGER.warn("cj getParentElement is null: line number should be bigger than 0");
            return elementAt;
        }
        @Nullable
        PsiFile psiFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(document);
        if (psiFile == null) {
            return elementAt;
        }
        PsiElement parent = elementAt.getParent();
        PsiElement targetParent = elementAt;
        if (isImport) {
            while (parent != null) {
                if (targetParent.toString().startsWith("CjImportList")) {
                    return targetParent;
                }
                targetParent = parent;
                parent = parent.getParent();
            }
        } else {
            return formatPsiElement(parent, targetParent);
        }
        return targetParent;
    }

    private PsiElement formatPsiElement(PsiElement parent, PsiElement targetParent) {
        PsiElement target = targetParent;
        PsiElement tempParent = parent;
        while (tempParent != null) {
            PsiElement nextElement = tempParent.getNextSibling();
            if (tempParent instanceof CjTopLevelObject && nextElement == null) {
                return tempParent;
            }
            while (nextElement != null) {
                if (nextElement.getText().contains(NEW_LINE)) {
                    return nextElement;
                }
                nextElement = nextElement.getNextSibling();
            }
            target = tempParent;
            tempParent = tempParent.getParent();
        }
        return target;
    }

    @Override
    public CodeCheckCmd getCodeCheckCmd(@NotNull Project project, @NotNull ProgressIndicator indicator,
        ResultContentPanel panel, @NotNull CheckCodeParams checkCodeParams) throws ExecutionException {
        return new CjlintCodeCheckCmd(project, indicator, panel, buildCommandLine(checkCodeParams, project),
            checkCodeParams);
    }
}
