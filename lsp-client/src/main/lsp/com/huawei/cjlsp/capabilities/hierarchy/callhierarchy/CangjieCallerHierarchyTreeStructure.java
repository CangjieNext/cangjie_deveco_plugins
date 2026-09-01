/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.hierarchy.callhierarchy;

import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.getTargetPsiFile;
import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.newLSPPsiElement;
import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.openFile;
import static com.huawei.cjlsp.capabilities.hierarchy.callhierarchy.CangjieCallHierarchyUtils.sendPrepareCallHierarchy;
import static com.huawei.ideacj.lsp.utils.CommonUtils.timeoutCrashCheck;

import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;

import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.contributors.psi.LSPPsiElement;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.requests.Timeout;
import org.wso2.lsp4intellij.requests.Timeouts;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * a tree structure for caller item
 *
 * @since 2021-04-26
 */
public class CangjieCallerHierarchyTreeStructure extends HierarchyTreeStructure {
    private PsiElement leafElement;

    /**
     * Instantiates a new Cangjie caller hierarchy tree structure.
     *
     * @param project     the project
     * @param method      the method
     * @param leafElement the leaf element
     */
    public CangjieCallerHierarchyTreeStructure(@NotNull Project project, @NotNull PsiElement method,
                                               PsiElement leafElement) {
        super(project, new CangjieCallerHierarchyNodeDescriptor(project, null, method, true, null, null));
        this.leafElement = leafElement;
    }

    @Override
    protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        if (!(descriptor instanceof CangjieCallerHierarchyNodeDescriptor)) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        descriptor.setIcon(CangjieIcons.CANGJIE_FUNCTION);
        final PsiElement targetElement = ((CangjieCallerHierarchyNodeDescriptor) descriptor).getTargetElement();
        boolean isBase = ((CangjieCallerHierarchyNodeDescriptor) descriptor).isBase();
        CallHierarchyItem item;
        if (isBase) {
            item = sendPrepareCallHierarchy(this.leafElement != null ? this.leafElement : targetElement, myProject);
            if (item != null) {
                ((CangjieCallerHierarchyNodeDescriptor) descriptor).setMyHighlightedText(item.getName());
            }
        } else {
            item = ((CangjieCallerHierarchyNodeDescriptor) descriptor).getFromItem();
        }
        if (item == null) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        if (StringUtil.isEmpty(((CangjieCallerHierarchyNodeDescriptor) descriptor).getNameText())) {
            ((CangjieCallerHierarchyNodeDescriptor) descriptor).setNameText(item.getName());
        }

        boolean isRecursionCall = isRecursionCall((CangjieCallerHierarchyNodeDescriptor) descriptor);
        List<CallHierarchyIncomingCall> callHierarchyIncomingCallList = new ArrayList<>();
        if (!isRecursionCall) {
            callHierarchyIncomingCallList =
                    sendIncomingCall(item, this.leafElement != null ? this.leafElement : targetElement);
        }

        List<CangjieCallerHierarchyNodeDescriptor> result = new ArrayList<>();
        for (CallHierarchyIncomingCall incomingItem : callHierarchyIncomingCallList) {
            String uri = incomingItem.getFrom().getUri();
            PsiFile targetPsifile = getTargetPsiFile(uri, myProject);
            if (targetPsifile == null) {
                continue;
            }
            CangjieCallerHierarchyNodeDescriptor nodeDescriptor = getCangjieCallerHierarchyNodeDescriptor(
                    descriptor, incomingItem, uri, targetPsifile);
            result.add(nodeDescriptor);
        }
        return ArrayUtil.toObjectArray(result);
    }

    @NotNull
    private CangjieCallerHierarchyNodeDescriptor getCangjieCallerHierarchyNodeDescriptor(
            @NotNull HierarchyNodeDescriptor descriptor,
            CallHierarchyIncomingCall incomingItem,
            String uri,
            PsiFile targetPsifile) {
        Editor incomingItemEditor = openFile(uri, myProject);
        PsiElement element = leafElement;
        List<Range> fromRanges = incomingItem.getFromRanges();
        Position position = fromRanges.get(0).getStart();
        if (incomingItemEditor != null) {
            element = targetPsifile.findElementAt(DocumentUtils.lspPosToOffset(incomingItemEditor, position));
            if (element == null) {
                element = leafElement;
            }
        } else {
            Position startPos;
            Position endPos;
            if (incomingItem.getFromRanges().isEmpty()) {
                startPos = incomingItem.getFrom().getSelectionRange().getStart();
                endPos = incomingItem.getFrom().getSelectionRange().getEnd();
            } else {
                Range fromRage = incomingItem.getFromRanges().get(0);
                startPos = fromRage.getStart();
                endPos = fromRage.getEnd();
            }
            LSPPsiElement lspPsiElement = newLSPPsiElement(startPos, endPos, targetPsifile, myProject);
            if (lspPsiElement != null) {
                element = lspPsiElement;
            }
        }
        CallHierarchyItem fromItem = incomingItem.getFrom();
        String itemName = fromItem.getName();
        return new CangjieCallerHierarchyNodeDescriptor(
                myProject, descriptor, element, itemName, fromItem, fromRanges.size());
    }

    private static boolean isRecursionCall(@NotNull CangjieCallerHierarchyNodeDescriptor descriptor) {
        boolean isRecursionCall = false;
        CangjieCallerHierarchyNodeDescriptor parentDescriptor = descriptor;
        while ((parentDescriptor.getParentDescriptor() instanceof CangjieCallerHierarchyNodeDescriptor)
                && ((parentDescriptor =
                        (CangjieCallerHierarchyNodeDescriptor) parentDescriptor.getParentDescriptor()) != null)) {
            if (parentDescriptor.equals(descriptor)) {
                isRecursionCall = true;
                break;
            }
        }
        return isRecursionCall;
    }

    @NotNull
    private List<CallHierarchyIncomingCall> sendIncomingCall(CallHierarchyItem item, PsiElement targetElement) {
        String uri = targetElement.getContainingFile().getVirtualFile().getUrl();
        EditorEventManager eventManager = EditorEventManagerBase.forUri(FileUtils.sanitizeURI(uri));
        if (eventManager == null || eventManager.wrapper.getServer() == null) {
            return Collections.emptyList();
        }
        CallHierarchyIncomingCallsParams incomingParams = new CallHierarchyIncomingCallsParams(item);
        CompletableFuture<List<CallHierarchyIncomingCall>> request =
            eventManager.getRequestManager().callHierarchyIncomingCalls(incomingParams);
        if (request == null) {
            return Collections.emptyList();
        }
        List<CallHierarchyIncomingCall> callHierarchyIncomingCallList = new ArrayList<>();
        try {
            callHierarchyIncomingCallList = request
                    .get(Timeout.getTimeout(Timeouts.CJ_CALL_HIERARCHY), TimeUnit.MILLISECONDS);
            eventManager.wrapper.notifySuccess(Timeouts.CJ_CALL_HIERARCHY);
        } catch (TimeoutException e) {
            timeoutCrashCheck(eventManager.getRequestManager());
            eventManager.wrapper.notifyFailure(Timeouts.CJ_CALL_HIERARCHY);
        } catch (JsonRpcException | ExecutionException | InterruptedException e) {
            eventManager.wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return callHierarchyIncomingCallList == null ? Collections.emptyList() : callHierarchyIncomingCallList;
    }

    @Override
    public String toString() {
        return "CangjieCallerHierarchyTreeStructure{}";
    }
}
