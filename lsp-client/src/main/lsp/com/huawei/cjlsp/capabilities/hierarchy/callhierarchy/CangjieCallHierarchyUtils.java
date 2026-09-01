/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import static com.huawei.ideacj.lsp.utils.CommonUtils.timeoutCrashCheck;
import static org.wso2.lsp4intellij.utils.ApplicationUtils.computableReadAction;

import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.DocumentUtil;

import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.psi.LSPPsiElement;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.requests.Timeout;
import org.wso2.lsp4intellij.requests.Timeouts;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CangjieCallHierarchyUtils
 *
 * @since 2024/04/17
 */
public class CangjieCallHierarchyUtils {
    private static final Logger LOG = Logger.getInstance(CangjieCallHierarchyUtils.class);

    /**
     * sendPrepareCallHierarchy
     *
     * @param targetElement targetElement
     * @param myProject myProject
     * @return CallHierarchyItem
     */
    @Nullable
    public static CallHierarchyItem sendPrepareCallHierarchy(PsiElement targetElement, Project myProject) {
        String uri = targetElement.getContainingFile().getVirtualFile().getUrl();
        EditorEventManager eventManager = EditorEventManagerBase.forUri(FileUtils.sanitizeURI(uri));
        if (eventManager == null || eventManager.wrapper.getServer() == null) {
            return null;
        }
        PsiFile psiFile = PsiUtilCore.getPsiFile(myProject, targetElement.getContainingFile().getVirtualFile());
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
        if (isTextOffsetInvalidInDoc(document, targetElement.getTextOffset())) {
            return null;
        }
        Position position = DocumentUtils.offsetToLSPPos(document, targetElement.getTextOffset());
        CallHierarchyPrepareParams params = new CallHierarchyPrepareParams();
        params.setTextDocument(eventManager.getIdentifier());
        params.setPosition(position);
        CompletableFuture<List<CallHierarchyItem>> request =
                eventManager.getRequestManager().prepareCallHierarchy(params);
        if (request == null) {
            return null;
        }
        // check response message from lsp server
        List<CallHierarchyItem> callHierarchyItemList;
        try {
            TraceUtils.trace(TraceUtils.Action.CALL_HIERARCHY);
            callHierarchyItemList = request
                    .get(Timeout.getTimeout(Timeouts.CJ_PREPARE_HIERARCHY), TimeUnit.MILLISECONDS);
            eventManager.wrapper.notifySuccess(Timeouts.CJ_PREPARE_HIERARCHY);
            if (callHierarchyItemList != null && !callHierarchyItemList.isEmpty()) {
                return callHierarchyItemList.get(0);
            }
        } catch (TimeoutException e) {
            timeoutCrashCheck(eventManager.getRequestManager());
            TraceUtils.trace(TraceUtils.Action.CALL_HIERARCHY, TraceUtils.Cause.TIMEOUT_EXCEPTION);
            eventManager.wrapper.notifyFailure(Timeouts.CJ_PREPARE_HIERARCHY);
        } catch (JsonRpcException | ExecutionException | InterruptedException e) {
            TraceUtils.trace(TraceUtils.Action.CALL_HIERARCHY, TraceUtils.Cause.CRASH);
            eventManager.wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return null;
    }

    /**
     * getTargetPsiFile
     *
     * @param uri uri
     * @param myProject myProject
     * @return PsiFile
     */
    @Nullable
    public static PsiFile getTargetPsiFile(String uri, Project myProject) {
        VirtualFile file = FileUtils.uriToVfs(uri);
        if (file != null) {
            return PsiManager.getInstance(myProject).findFile(file);
        }
        return null;
    }

    /**
     * isTextOffsetInvalidInDoc
     *
     * @param document document
     * @param textOffset textOffset
     * @return isTextOffsetInvalidInDoc
     */
    public static boolean isTextOffsetInvalidInDoc(Document document, int textOffset) {
        return document == null || textOffset < 0 || textOffset > document.getTextLength();
    }

    /**
     * check is same hierarchy node descriptor
     *
     * @param targetObj targetObj
     * @param targetNameText targetNameText
     * @param sourceObj sourceObj
     * @param sourceNameText sourceNameText
     * @return is same hierarchy node descriptor
     */
    public static boolean isSameHierarchyNodeDescriptor(HierarchyNodeDescriptor targetObj,
                                                        String targetNameText,
                                                        HierarchyNodeDescriptor sourceObj,
                                                        String sourceNameText) {
        if (StringUtil.isEmpty(sourceNameText) || StringUtil.isEmpty(targetNameText)) {
            return false;
        }
        if (sourceObj.getContainingFile() == null || sourceObj.getContainingFile().getVirtualFile() == null) {
            return false;
        }
        if (targetObj.getContainingFile() == null || targetObj.getContainingFile().getVirtualFile() == null) {
            return false;
        }
        String sourceUri = sourceObj.getContainingFile().getVirtualFile().toString();
        String targetUri = targetObj.getContainingFile().getVirtualFile().toString();
        return sourceNameText.equals(targetNameText) && sourceUri.equals(targetUri);
    }

    /**
     * positionToOffset
     *
     * @param document The document
     * @param pos The LSPPos
     * @return The offset
     */
    public static int positionToOffset(Document document, Position pos) {
        return computableReadAction(() -> {
            int posLine = pos.getLine();
            int docLineCount = document.getLineCount();
            if (posLine >= docLineCount) {
                LOG.warn(String.format(Locale.ROOT, "Pos line (%s) >= docLineCount (%s)", posLine, docLineCount));
            }
            int line = Math.max(0, Math.min(posLine, docLineCount - 1));
            String lineText = document.getText(DocumentUtil.getLineTextRange(document, line));
            int character = pos.getCharacter();
            int offset = document.getLineStartOffset(line) + character;
            if (character > lineText.length()) {
                LOG.warn("Pos character greater than line length : " + character + " > " + lineText.length());
            }
            int docLength = document.getTextLength();
            if (offset > docLength) {
                LOG.warn("Offset greater than text length : " + offset + " > " + docLength);
            }
            return Math.min(Math.max(offset, 0), docLength);
        });
    }

    /**
     * openFile
     *
     * @param uri  uri
     * @param project  project
     * @return  Editor
     */
    public static Editor openFile(String uri, Project project) {
        return FileUtils.editorFromUri(uri, project);
    }

    /**
     * newLSPPsiElement
     *
     * @param startPos startPos
     * @param endPos startPos
     * @param targetPsifile targetPsifile
     * @param project project
     * @return newLSPPsiElement
     */
    @Nullable
    public static LSPPsiElement newLSPPsiElement(Position startPos,
                                                 Position endPos,
                                                 PsiFile targetPsifile,
                                                 Project project) {
        Document document = FileDocumentManager.getInstance().getDocument(targetPsifile.getVirtualFile());
        if (document != null) {
            int startOffset = positionToOffset(document, startPos);
            int endOffset = positionToOffset(document, endPos);
            String name = document.getText(new TextRange(startOffset, endOffset));
            return new LSPPsiElement(name, project, startOffset, endOffset, targetPsifile);
        }
        return null;
    }

    /**
     * descriptorNavigate
     *
     * @param element element
     * @param isRequestFocus isRequestFocus
     */
    public static void descriptorNavigate(PsiElement element, boolean isRequestFocus) {
        OpenFileDescriptor descriptor = new OpenFileDescriptor(element.getProject(),
                element.getContainingFile().getVirtualFile(), element.getTextOffset());
        ApplicationUtils.invokeLater(() -> {
            ApplicationUtils.writeAction(() -> {
                FileEditorManager.getInstance(element.getProject()).openEditor(descriptor, isRequestFocus);
            });
        });
    }

    /**
     * isFunctionDefinition
     *
     * @param element target element
     * @return isFunctionDefinition
     */
    public static boolean isFunctionDefinition(PsiElement element) {
        if (element == null) {
            return false;
        }
        PsiElement parent = element.getParent();
        if (parent == null) {
            return false;
        }
        return parent.getParent() instanceof CjFunctionDefinition;
    }

    /**
     * isPropertyDefinition
     *
     * @param element target element
     * @return isPropertyDefinition
     */
    public static boolean isPropertyDefinition(PsiElement element) {
        if (element == null) {
            return false;
        }
        PsiElement parent = element.getParent();
        if (parent == null) {
            return false;
        }
        return parent.getParent() instanceof CjPropertyDefinition;
    }

    /**
     * isInitDefinition
     *
     * @param element target element
     * @return isInitDefinition
     */
    public static boolean isInitDefinition(PsiElement element) {
        if (element == null || !"init".equals(element.getText())) {
            return false;
        }
        return element.getParent() instanceof CjStructInit || element.getParent() instanceof CjClassInit;
    }

    /**
     * isInitDefinition
     *
     * @param element target element
     * @return isInitDefinition
     */
    public static boolean isPrimaryInit(PsiElement element) {
        if (element == null) {
            return false;
        }
        PsiElement parent = element.getParent();
        if (parent == null) {
            return false;
        }
        PsiElement preParent = parent.getParent();
        return preParent.getParent() instanceof CjStructPrimaryInit
                || preParent.getParent() instanceof CjClassPrimaryInit;
    }
}
