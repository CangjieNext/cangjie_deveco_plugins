/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.typehierarchy;

import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.requests.Timeouts;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Method of sending a message to the server by type hierarchy.
 *
 * @since 2022-01-19
 */
public class TypeHierarchyUtils {
    // Mapping between typeHierarchyItem and psiElement
    static Map<String, PsiElement> corrElement = new HashMap<>();

    private static final Logger LOG = Logger.getInstance(TypeHierarchyUtils.class);

    @Nullable
    private static LanguageServerWrapper getWrapper(Project project) {
        final Set<LanguageServerWrapper> serverWrappers = IntellijLanguageClient.getProjectToLanguageWrappers()
            .getOrDefault(FileUtils.projectToUri(project), Collections.emptySet());
        for (LanguageServerWrapper wrapper : serverWrappers) {
            if (wrapper.getStatus() == ServerStatus.INITIALIZED
                    && isCangjieWrapper(wrapper.getServerDefinition().ext)) {
                return wrapper;
            }
        }
        return null;
    }

    @Nullable
    private static ExtendRequestManager getExtendRequestManager(LanguageServerWrapper wrapper) {
        if (wrapper == null || wrapper.getRequestManager() == null) {
            return null;
        }
        if (wrapper.getRequestManager() instanceof ExtendRequestManager) {
            return (ExtendRequestManager) wrapper.getRequestManager();
        }
        return null;
    }

    private static boolean isCangjieWrapper(String extention) {
        String[] split = extention.split(",");
        for (String s : split) {
            if ("cj".equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turn location 2 psi element psi element.
     *
     * @param location the location
     * @param project  the project
     * @param baseItemLocation the baseItemLocation
     * @return the psi element
     */
    @Nullable
    public static PsiElement turnLocation2PsiElement(Location location, Project project, Location baseItemLocation) {
        if (project == null) {
            return null;
        }
        String uri = location.getUri();
        Location finalLocation = location;
        try {
            String uriPath = (new URI(uri)).getPath();
            if (StringUtil.isEmpty(uriPath)) {
                uriPath = (new URI(baseItemLocation.getUri())).getPath();
                finalLocation = baseItemLocation;
            }
            if (StringUtil.isEmpty(uriPath)) {
                return null;
            }
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath((new URI(uriPath)).getPath());
            if (virtualFile == null) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null || psiFile == null || !psiFile.isValid()) {
                return null;
            }
            Position start = finalLocation.getRange().getStart();
            int elementOffset = document.getLineStartOffset(start.getLine()) + start.getCharacter();
            return psiFile.findElementAt(elementOffset);
        } catch (URISyntaxException e) {
            LOG.warn(e.getMessage());
            return null;
        }
    }

    /**
     * Send type hierarchy request.
     *
     * @param psiElement the psi element
     * @return the type hierarchy items
     */
    @Nullable
    public static List<TypeHierarchyItem> sendPrepareTypeHierarchy(PsiElement psiElement) {
        List<TypeHierarchyItem> list = new ArrayList<>();
        LanguageServerWrapper wrapper = getWrapper(psiElement.getProject());
        ExtendRequestManager extendRequestManager = getExtendRequestManager(wrapper);
        if (extendRequestManager == null) {
            return list;
        }

        PsiFile psiFile =
                PsiUtilCore.getPsiFile(psiElement.getProject(), psiElement.getContainingFile().getVirtualFile());
        Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(psiFile);
        if (document == null) {
            return list;
        }
        Position position = DocumentUtils.offsetToLSPPos(document, psiElement.getTextOffset());

        // new params for type hierarchy and set new value to params
        TypeHierarchyPrepareParams params = new TypeHierarchyPrepareParams();
        params.setPosition(position);
        String uri = psiElement.getContainingFile().getVirtualFile().getUrl();
        params.setTextDocument(new TextDocumentIdentifier(FileUtils.sanitizeURI(uri)));
        CompletableFuture<List<TypeHierarchyItem>> request = extendRequestManager.prepareTypeHierarchy(params);
        if (request == null) {
            return list;
        }
        TraceUtils.trace(TraceUtils.Action.TYPE_HIERARCHY);
        return sendRequest(request, wrapper, extendRequestManager);
    }

    private static List<TypeHierarchyItem> sendRequest(CompletableFuture<List<TypeHierarchyItem>> request,
                                                       LanguageServerWrapper wrapper,
                                                       ExtendRequestManager requestManager) {
        try {
            List<TypeHierarchyItem> typeHierarchyItem =
                    request.get(20000, TimeUnit.MILLISECONDS);
            wrapper.notifySuccess(Timeouts.CJ_TYPE_HIERARCHY);
            if (typeHierarchyItem != null) {
                return typeHierarchyItem;
            }
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            requestManager.checkStatus();
            wrapper.notifyFailure(Timeouts.CJ_TYPE_HIERARCHY);
        } catch (JsonRpcException e) {
            wrapper.crashed(e);
            TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
            CrashLogPackager.packageLogFiles();
        }
        return new ArrayList<>();
    }

    /**
     * Send sub/super type hierarchy request.
     *
     * @param item       the item
     * @param psiElement the psi element
     * @param isSub      is sub or super
     * @return the type hierarchy item
     */
    @Nullable
    public static List<TypeHierarchyItem> sendChildTypes(TypeHierarchyItem item, PsiElement psiElement, boolean isSub) {
        List<TypeHierarchyItem> list = new ArrayList<>();
        if (Objects.isNull(item)) {
            return list;
        }
        LanguageServerWrapper wrapper = getWrapper(psiElement.getProject());
        ExtendRequestManager extendRequestManager = getExtendRequestManager(wrapper);
        if (extendRequestManager == null) {
            return list;
        }

        // new params for resolve type hierarchy and set new value to params
        CompletableFuture<List<TypeHierarchyItem>> request = null;
        if (isSub) {
            TypeHierarchySubtypesParams params = new TypeHierarchySubtypesParams();
            params.setItem(item);
            request = extendRequestManager.typeHierarchySubtypes(params);
        } else {
            TypeHierarchySupertypesParams params = new TypeHierarchySupertypesParams();
            params.setItem(item);
            request = extendRequestManager.typeHierarchySupertypes(params);
        }

        if (Objects.isNull(request)) {
            return list;
        }
        return sendRequest(request, wrapper, extendRequestManager);
    }

    /**
     * initNodeDescriptorIcon
     *
     * @param element element
     * @param descriptor descriptor
     */
    public static void initNodeDescriptorIcon(PsiElement element, HierarchyNodeDescriptor descriptor) {
        if (element == null) {
            return;
        }
        if (element.getParent() == null) {
            return;
        }
        PsiElement targetElement = element.getParent().getParent();
        if (targetElement == null) {
            return;
        }
        descriptor.setIcon(targetElement.getIcon(0));
    }
}
