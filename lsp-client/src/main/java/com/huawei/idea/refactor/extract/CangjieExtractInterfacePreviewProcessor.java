/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.refactor.extract;

import com.huawei.capabilities.lsp.CangjieFindUsagesHandler;
import com.huawei.capabilities.lsp.CangjieFindUsagesHandlerFactory;
import com.huawei.idea.edit.CangjieEditorEventManager;
import com.huawei.idea.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.idea.language.CjPsiUtils;
import com.huawei.idea.language.psi.othersnode.CjIdentifier;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassBody;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumBody;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendBody;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceBody;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructBody;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructDefinition;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.ui.UsageViewDescriptorAdapter;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;

import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Shows IntelliJ's Refactoring Preview before executing the LSP
 * extract-interface command.
 * The actual edits are still produced by the C++ language server. This
 * processor only provides
 * the preview/confirmation step expected by the IDE refactoring flow.
 *
 * @since 2026-05-28
 */
public class CangjieExtractInterfacePreviewProcessor extends BaseRefactoringProcessor {
    private static final String REFACTORING_ID = "cangjie.extract.interface";

    private final Command command;
    private final CangjieEditorEventManager manager;
    private final JsonObject extraOptions;
    private final PsiElement sourceElement;
    private final String sourceTypeName;
    private final String interfaceName;

    /**
     * Constructor for CangjieExtractInterfacePreviewProcessor.
     *
     * @param project The current active project.
     * @param sourceElement The source class/struct element.
     * @param request The preview request content.
     */
    public CangjieExtractInterfacePreviewProcessor(@NotNull Project project, @NotNull PsiElement sourceElement,
            @NotNull PreviewRequest request) {
        super(project);
        this.manager = request.manager;
        this.command = request.command;
        this.extraOptions = request.extraOptions;
        this.sourceElement = sourceElement;
        this.sourceTypeName = request.sourceTypeName;
        this.interfaceName = request.interfaceName;
        setPreviewUsages(true);
    }

    /**
     * Preview request encapsulating the context of the extract interface action.
     */
    public static class PreviewRequest {
        private final CangjieEditorEventManager manager;
        private final Command command;
        private final JsonObject extraOptions;
        private final String sourceTypeName;
        private final String interfaceName;

        /**
         * Constructor for PreviewRequest.
         *
         * @param manager The editor event manager.
         * @param command The LSP command.
         * @param extraOptions Additional refactoring options.
         * @param sourceTypeName The original concrete type name.
         * @param interfaceName The targeted interface name.
         */
        public PreviewRequest(@NotNull CangjieEditorEventManager manager, @NotNull Command command,
                              @NotNull JsonObject extraOptions, @NotNull String sourceTypeName,
                              @NotNull String interfaceName) {
            this.manager = manager;
            this.command = command;
            this.extraOptions = extraOptions;
            this.sourceTypeName = sourceTypeName;
            this.interfaceName = interfaceName;
        }
    }

    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new UsageViewDescriptorAdapter() {
            @Override
            public PsiElement[] getElements() {
                return PsiElement.EMPTY_ARRAY;
            }

            @Override
            public String getProcessedElementsHeader() {
                return "";
            }

            @Override
            public String getCodeReferencesText(int usagesCount, int filesCount) {
                return usagesCount + " type reference" + (usagesCount == 1 ? "" : "s") + " to replace with "
                        + interfaceName + " in " + filesCount + " file" + (filesCount == 1 ? "" : "s");
            }
        };
    }

    @Override
    protected UsageInfo @NotNull [] findUsages() {
        if (sourceTypeName.isEmpty()) {
            return UsageInfo.EMPTY_ARRAY;
        }

        List<UsageInfo> result = new ArrayList<>();
        CjIdentifier cjIdentifier = PsiTreeUtil.findChildOfType(sourceElement, CjIdentifier.class);
        if (cjIdentifier == null) {
            return UsageInfo.EMPTY_ARRAY;
        }
        CJPsiLeafNode node = PsiTreeUtil.findChildOfType(cjIdentifier, CJPsiLeafNode.class);
        if (node == null) {
            return UsageInfo.EMPTY_ARRAY;
        }
        TextRange range = node.getTextRange();
        CangjieFindUsagesHandlerFactory findUsagesHandlerFactory = new CangjieFindUsagesHandlerFactory();
        FindUsagesHandler findUsagesHandler = findUsagesHandlerFactory
            .createFindUsagesHandler(node, false);
        if ((findUsagesHandler instanceof CangjieFindUsagesHandler cangjieFindUsagesHandler)) {
            Collection<PsiReference> references = cangjieFindUsagesHandler.getRefactorInterfaceReferences(node);
            references.forEach(reference -> {
                final PsiElement psiElement = CjPsiUtils.getOriginalPsiElement(reference.getElement());
                if (!psiElement.getTextRange().contains(range) && isValidUsage(psiElement)) {
                    result.add(new UsageInfo(reference));
                }
            });

            return result.toArray(UsageInfo[]::new);
        }
        return UsageInfo.EMPTY_ARRAY;
    }

    private boolean isValidUsage(PsiElement originalElement) {
        // 如果引用在{}中，说明是有效引用
        PsiElement bodyElement = PsiTreeUtil.findFirstParent(originalElement, this::isSupportedTypeBody);
        if (bodyElement != null) {
            return true;
        }
        // 否则的在definition中
        PsiElement parent = PsiTreeUtil.findFirstParent(originalElement, this::isSupportedTypeDefinition);
        if (parent == null) {
            return true;
        }
        // 查找子节点，文本内容为 "<" 的元素
        Collection<PsiElement> matchedElements = Arrays.stream(parent.getChildren())
                .filter(element -> "<:".equals(element.getText()))
                .toList();
        return matchedElements.isEmpty();
    }

    private boolean isSupportedTypeDefinition(PsiElement element) {
        if (element == null) {
            return false;
        }
        return element instanceof CjClassDefinition
                || element instanceof CjStructDefinition
                || element instanceof CjInterfaceDefinition
                || element instanceof CjEnumDefinition
                || element instanceof CjExtendDefinition;
    }

    private boolean isSupportedTypeBody(PsiElement element) {
        if (element == null) {
            return false;
        }
        return element instanceof CjClassBody
                || element instanceof CjStructBody
                || element instanceof CjInterfaceBody
                || element instanceof CjEnumBody
                || element instanceof CjExtendBody;
    }

    @Override
    protected void performRefactoring(UsageInfo @NotNull [] usages) {
        JsonObject payload = getCommandPayload(command);
        extraOptions.addProperty("hasSelectedTypeReferences", true);
        extraOptions.add("selectedTypeReferences", buildSelectedTypeReferences(usages));
        payload.add("extraOptions", extraOptions);

        Map<?, ?> finalMap = new Gson().fromJson(payload, Map.class);
        command.setArguments(new ArrayList<>(List.of(finalMap)));

        ApplicationManager.getApplication()
                .invokeLater(() -> manager.executeCommands4Refactor(Collections.singletonList(command)));
    }

    @Override
    protected void refreshElements(PsiElement @NotNull [] elements) {
    }

    @Override
    protected String getCommandName() {
        return "Extract Interface";
    }

    @Override
    protected String getRefactoringId() {
        return REFACTORING_ID;
    }

    /**
     * Collects and triggers the internal usage collection process for preview
     * purposes.
     *
     * @return An array of collected UsageInfo elements.
     */
    public UsageInfo @NotNull [] collectPreviewUsages() {
        return findUsages();
    }

    private JsonArray buildSelectedTypeReferences(UsageInfo[] usages) {
        JsonArray refs = new JsonArray();
        for (UsageInfo usage : usages) {
            PsiElement element = usage.getElement();
            if (element == null) {
                continue;
            }
            PsiFile psiFile = element.getContainingFile();
            if (psiFile == null) {
                continue;
            }
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                continue;
            }

            TextRange elementRange = element.getTextRange();
            if (elementRange == null) {
                continue;
            }
            TextRange rangeInElement = usage.getRangeInElement();
            int startOffset = rangeInElement != null
                    ? elementRange.getStartOffset() + rangeInElement.getStartOffset()
                    : elementRange.getStartOffset();
            int endOffset = rangeInElement != null
                    ? elementRange.getStartOffset() + rangeInElement.getEndOffset()
                    : elementRange.getEndOffset();
            if (startOffset < 0 || endOffset < startOffset || endOffset > document.getTextLength()) {
                continue;
            }

            JsonObject ref = new JsonObject();
            ref.addProperty("uri", Path.of(virtualFile.getPath()).toUri().toString());
            JsonObject start = new JsonObject();
            start.addProperty("line", document.getLineNumber(startOffset));
            start.addProperty("character",
                    startOffset - document.getLineStartOffset(document.getLineNumber(startOffset)));
            JsonObject end = new JsonObject();
            end.addProperty("line", document.getLineNumber(endOffset));
            end.addProperty("character", endOffset - document.getLineStartOffset(document.getLineNumber(endOffset)));
            ref.add("start", start);
            ref.add("end", end);
            refs.add(ref);
        }
        return refs;
    }

    private JsonObject getCommandPayload(Command command) {
        if (command.getArguments() == null || command.getArguments().isEmpty()
                || command.getArguments().getFirst() == null) {
            return new JsonObject();
        }

        Object arg0 = command.getArguments().getFirst();

        if (arg0 instanceof JsonObject) {
            return (JsonObject) arg0;
        }

        if (arg0 instanceof Map) {
            JsonElement jsonElement = new Gson().toJsonTree(arg0);
            if (jsonElement.isJsonObject()) {
                return jsonElement.getAsJsonObject();
            }
            return new JsonObject();
        }

        String rawArgsStr = arg0.toString().trim();
        return com.google.gson.JsonParser.parseString(rawArgsStr).getAsJsonObject();
    }
}