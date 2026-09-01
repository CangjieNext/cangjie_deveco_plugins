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

import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
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
 * a tree structure for callee item
 *
 * @since 2021-04-26
 */
public class CangjieCalleeHierarchyTreeStructure extends HierarchyTreeStructure {
    private PsiElement leafElement;

    /**
     * CangjieCalleeHierarchyTreeStructure
     *
     * @param project project
     * @param method method
     * @param leafElement leafElement
     */
    public CangjieCalleeHierarchyTreeStructure(@NotNull Project project, @NotNull PsiElement method,
                                               PsiElement leafElement) {
        super(project, new CangjieCalleeHierarchyNodeDescriptor(project, null, method, true, null, null));
        this.leafElement = leafElement;
    }

    @Override
    protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        if (!(descriptor instanceof CangjieCalleeHierarchyNodeDescriptor)) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        descriptor.setIcon(CangjieIcons.CANGJIE_FUNCTION);
        final PsiElement targetElement = ((CangjieCalleeHierarchyNodeDescriptor) descriptor).getTargetElement();
        boolean isBase = ((CangjieCalleeHierarchyNodeDescriptor) descriptor).isBase();
        CallHierarchyItem item;
        if (isBase) {
            item = sendPrepareCallHierarchy(this.leafElement != null ? this.leafElement : targetElement, myProject);
            if (item != null) {
                ((CangjieCalleeHierarchyNodeDescriptor) descriptor).setMyHighlightedText(item.getName());
            }
        } else {
            item = ((CangjieCalleeHierarchyNodeDescriptor) descriptor).getToItem();
        }
        if (item == null) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        if (StringUtil.isEmpty(((CangjieCalleeHierarchyNodeDescriptor) descriptor).getNameText())) {
            ((CangjieCalleeHierarchyNodeDescriptor) descriptor).setNameText(item.getName());
        }

        boolean isRecursionCall = isRecursionCall((CangjieCalleeHierarchyNodeDescriptor) descriptor);
        List<CallHierarchyOutgoingCall> callHierarchyOutgoingCallList = new ArrayList<>();
        if (!isRecursionCall) {
            callHierarchyOutgoingCallList =
                    sendOutgoingCall(item, this.leafElement != null ? this.leafElement : targetElement);
        }

        List<CangjieCalleeHierarchyNodeDescriptor> result = new ArrayList<>();
        for (CallHierarchyOutgoingCall outgoingItem : callHierarchyOutgoingCallList) {
            String uri = getUri((CangjieCalleeHierarchyNodeDescriptor) descriptor, targetElement, isBase);
            PsiFile targetPsifile = getTargetPsiFile(uri, myProject);
            if (targetPsifile == null) {
                continue;
            }
            CangjieCalleeHierarchyNodeDescriptor nodeDescriptor = getCangjieCalleeHierarchyNodeDescriptor(
                    descriptor, outgoingItem, uri, targetPsifile);
            result.add(nodeDescriptor);
        }
        return ArrayUtil.toObjectArray(result);
    }

    @NotNull
    private CangjieCalleeHierarchyNodeDescriptor getCangjieCalleeHierarchyNodeDescriptor(
            @NotNull HierarchyNodeDescriptor descriptor,
            CallHierarchyOutgoingCall outgoingItem,
            String uri,
            PsiFile targetPsifile) {
        Editor outgoingItemEditor = openFile(uri, myProject);
        List<Range> fromRanges = outgoingItem.getFromRanges();
        Position position = fromRanges.get(0).getStart();
        PsiElement element = leafElement;
        if (outgoingItemEditor != null) {
            element = targetPsifile.findElementAt(DocumentUtils.lspPosToOffset(outgoingItemEditor, position));
            if (element == null) {
                element = leafElement;
            }
        } else {
            Position startPos;
            Position endPos;
            if (outgoingItem.getFromRanges().isEmpty()) {
                startPos = outgoingItem.getTo().getSelectionRange().getStart();
                endPos = outgoingItem.getTo().getSelectionRange().getEnd();
            } else {
                Range fromRage = outgoingItem.getFromRanges().get(0);
                startPos = fromRage.getStart();
                endPos = fromRage.getEnd();
            }
            LSPPsiElement lspPsiElement = newLSPPsiElement(startPos, endPos, targetPsifile, myProject);
            if (lspPsiElement != null) {
                element = lspPsiElement;
            }
        }
        CallHierarchyItem fromItem = outgoingItem.getTo();
        String itemName = fromItem.getName();
        return new CangjieCalleeHierarchyNodeDescriptor(
                myProject, descriptor, element, itemName, fromItem, fromRanges.size());
    }

    private static boolean isRecursionCall(@NotNull CangjieCalleeHierarchyNodeDescriptor descriptor) {
        boolean isRecursionCall = false;
        CangjieCalleeHierarchyNodeDescriptor parentDescriptor = descriptor;
        while ((parentDescriptor.getParentDescriptor() instanceof CangjieCalleeHierarchyNodeDescriptor)
                && ((parentDescriptor =
                        (CangjieCalleeHierarchyNodeDescriptor) parentDescriptor.getParentDescriptor()) != null)) {
            if (parentDescriptor.equals(descriptor)) {
                isRecursionCall = true;
                break;
            }
        }
        return isRecursionCall;
    }

    private static String getUri(CangjieCalleeHierarchyNodeDescriptor descriptor,
                                 PsiElement targetElement, boolean isBase) {
        String uri = targetElement.getContainingFile().getVirtualFile().getUrl();
        if (descriptor.getToItem() != null
                && !descriptor.getToItem().getUri().isEmpty()) {
            uri = descriptor.getToItem().getUri();
        }
        if (isBase) {
            uri = targetElement.getContainingFile().getVirtualFile().getUrl();
        }
        return uri;
    }

    @NotNull
    private List<CallHierarchyOutgoingCall> sendOutgoingCall(CallHierarchyItem item, PsiElement targetElement) {
        String uri = targetElement.getContainingFile().getVirtualFile().getUrl();
        EditorEventManager eventManager = EditorEventManagerBase.forUri(FileUtils.sanitizeURI(uri));
        if (eventManager == null || eventManager.wrapper.getServer() == null) {
            return Collections.emptyList();
        }
        CallHierarchyOutgoingCallsParams outgoingParams = new CallHierarchyOutgoingCallsParams(item);
        CompletableFuture<List<CallHierarchyOutgoingCall>> request =
                eventManager.getRequestManager().callHierarchyOutgoingCalls(outgoingParams);
        if (request == null) {
            return Collections.emptyList();
        }
        List<CallHierarchyOutgoingCall> callHierarchyOutgoingCallList = new ArrayList<>();
        try {
            callHierarchyOutgoingCallList = request
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
        return callHierarchyOutgoingCallList == null ? Collections.emptyList() : callHierarchyOutgoingCallList;
    }
}
