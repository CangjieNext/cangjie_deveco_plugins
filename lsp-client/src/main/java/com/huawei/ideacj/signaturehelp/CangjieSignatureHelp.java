/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.signaturehelp;

import com.huawei.ideacj.edit.CangjieEditorEventManager;
import com.huawei.ideacj.language.psi.othersnode.CjAdCallSuffix;
import com.huawei.ideacj.language.psi.othersnode.CjCallSuffix;
import com.huawei.ideacj.lsp.utils.LSPThreadPoolManager;

import com.intellij.codeInsight.hint.ShowParameterInfoContext;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * CangjieSignatureHelpBase
 *
 * @since 2022-11-14
 */
public class CangjieSignatureHelp implements ParameterInfoHandler<PsiElement, ParameterInformation> {
    private static final Logger LOG = Logger.getInstance(CangjieSignatureHelp.class);
    private static final String COMMA = ",";

    private HashMap<ParameterInformation, SignatureInformation> signatureHelpMap = new HashMap<>();

    private PsiElement currentFuncPsi;

    @Override
    @Nullable
    public PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        int offset = context.getFile().getText().charAt(context.getOffset() - 1) == ')' ? context.getOffset()
                : context.getOffset() - 1;
        PsiElement element = context.getFile().findElementAt(offset == -1 ? 0 : offset);
        CjAdCallSuffix adCallSuffix = PsiTreeUtil.getParentOfType(element, CjAdCallSuffix.class);
        if (adCallSuffix != null) {
            currentFuncPsi = adCallSuffix;
            return adCallSuffix;
        }
        CjCallSuffix cjCallSuffix = PsiTreeUtil.getParentOfType(element, CjCallSuffix.class);
        currentFuncPsi = cjCallSuffix;
        return cjCallSuffix;
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        LSPThreadPoolManager.pool(() -> {
            SignatureHelp response = getSignature(element);
            if (response == null) {
                return;
            }
            List<ParameterInformation> parameterInformationList = new ArrayList<>(response.getSignatures().size());
            List<SignatureInformation> signatures = response.getSignatures();
            signatureHelpMap.clear();
            for (SignatureInformation info : signatures) {
                ParameterInformation parameterInformation = new ParameterInformation(info.getLabel());
                parameterInformationList.add(parameterInformation);
                signatureHelpMap.put(parameterInformation, info);
            }
            if (parameterInformationList.isEmpty()) {
                return;
            }
            context.setItemsToShow(parameterInformationList.toArray(new ParameterInformation[0]));
            context.showHint(element, element.getTextOffset() + 1, this);
        });
    }

    @Nullable
    @Override
    public PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        PsiElement owner = context.getParameterOwner();
        int offset = context.getFile().getText().charAt(context.getOffset() - 1) == ')' ? context.getOffset()
                : context.getOffset() - 1;
        PsiElement currentFuncCall = PsiTreeUtil.getParentOfType(
                context.getFile().findElementAt(offset == -1 ? 0 : offset), CjAdCallSuffix.class, CjCallSuffix.class);
        if (currentFuncCall != null && owner != currentFuncCall && currentFuncCall.isValid()) {
            context.setParameterOwner(currentFuncCall);
            return currentFuncCall;
        }
        return owner.getTextRange().contains(offset) ? owner : null;
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement owner, @NotNull UpdateParameterInfoContext context) {
        int offset = context.getEditor().getCaretModel().getOffset();
        if (currentFuncPsi != owner) {
            final ShowParameterInfoContext newContext =
                    new ShowParameterInfoContext(context.getEditor(), owner.getProject(), context.getFile(),
                            offset, -1, false, false);
            showParameterInfo(owner, newContext);
            currentFuncPsi = owner;
            context.removeHint();
            return;
        }
        PsiFile psiFile = context.getFile();
        if (psiFile == null) {
            return;
        }
        int parameter = 0;
        for (PsiElement element : owner.getChildren()) {
            if (!COMMA.equals(element.getText())) {
                continue;
            }
            int endOffset = element.getTextRange().getEndOffset();
            if (endOffset <= offset) {
                parameter++;
            }
            if (endOffset > offset) {
                break;
            }
        }
        context.setCurrentParameter(parameter);
    }

    @Override
    public void updateUI(ParameterInformation parameterInformation, @NotNull ParameterInfoUIContext context) {
        // put parameter information into UI component
        int active = 0;
        if (context.getCurrentParameterIndex() != -1) {
            active = context.getCurrentParameterIndex();
        }
        SignatureInformation info = signatureHelpMap.get(parameterInformation);
        int highlightStart = -1;
        int highlightEnd = -1;
        List<ParameterInformation> piList = info.getParameters();
        if (piList != null && active <= piList.size() - 1) {
            String highlightLabel = info.getParameters().get(active).getLabel().getLeft();
            highlightStart = parameterInformation.getLabel().getLeft().indexOf(highlightLabel);
            highlightEnd = highlightStart + highlightLabel.length();
        }
        context.setUIComponentEnabled(true);
        context.setupUIComponentPresentation(parameterInformation.getLabel().getLeft(), highlightStart, highlightEnd,
                true, false, false, context.getDefaultParameterColor());
    }

    @Nullable
    private SignatureHelp getSignature(@NotNull PsiElement element) {
        CangjieEditorEventManager cjEditorEventManager =
                ApplicationManager.getApplication().runReadAction((Computable<CangjieEditorEventManager>) () -> {
                    Editor editor = FileUtils.editorFromPsiFile(element.getContainingFile());
                    if (editor == null || editor.isDisposed() || editor.getProject() == null) {
                        return null;
                    }
                    EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
                    if (!(eventManager instanceof CangjieEditorEventManager cangjieEditorEventManager)) {
                        return null;
                    }
                    return cangjieEditorEventManager;
                });
        return cjEditorEventManager.cjSignatureHelp();
    }

    @Override
    public String toString() {
        return "CangjieSignatureHelp {}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatureHelpMap);
    }
}
