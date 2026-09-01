/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.cjlsp.capabilities.lsp;

import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * this is extension of Lsp GotoDeclaration with language type check
 *
 * @since 2020-07-09
 */
public class CangjieGotoDeclarationHandler extends CangjieGotoDeclaration {
    private static final Logger LOG = Logger.getInstance(CangjieGotoDeclarationHandler.class);

    // 防抖延迟时间
    private static final long DEBOUNCE_DELAY = 1000L;

    private final int invalidOffset = -1;

    private long lastTraceTime = 0L;

    /**
     * override lsp go to declaration with language check
     *
     * @param sourceElement the sourceElement
     * @param editor        the editor
     * @return the declaration target element
     */
    @Nullable
    @Override
    public PsiElement getGotoDeclarationTarget(@Nullable PsiElement sourceElement, Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        if (sourceElement.getLanguage() != CangJieLanguage.INSTANCE) {
            return null;
        }
        if (sourceElement instanceof PsiWhiteSpace) {
            return null;
        }
        // 用户使用ctrl + 鼠标悬浮在identifier上底座会多次调用该方法，造成多次打点，所以使用防抖机制进行规避
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTraceTime > DEBOUNCE_DELAY) {
            TraceUtils.trace(TraceUtils.Action.GOTO_DECLARE_OR_USAGES);
            lastTraceTime = currentTime;
        }
        if (isDefinition(sourceElement, editor)) {
            return null;
        }
        return super.getGotoDeclarationTarget(sourceElement, editor);
    }

    private boolean isDefinition(@Nullable PsiElement sourceElement, Editor editor) {
        if (sourceElement == null) {
            return false;
        }
        int offset = sourceElement.getTextRange().getStartOffset();
        PsiFile file = sourceElement.getContainingFile();
        if (offset > 1) {
            PsiElement preElement = file.findElementAt(offset - 1);
            if (preElement != null && preElement.getParent() instanceof CjIdentifier) {
                return false;
            }
        }
        // determine whether is definition by definition request
        Optional<Location> locationOpt = getDefinition(offset, editor);
        if (locationOpt.isEmpty()) {
            return false;
        }
        Location location = locationOpt.get();
        if (location.getRange() == null) {
            return false;
        }
        int textOffsetByLocation = getTextOffsetByLocation(location, editor);
        if (textOffsetByLocation == invalidOffset) {
            return false;
        }
        return textOffsetByLocation == sourceElement.getTextOffset();
    }

    private int getTextOffsetByLocation(Location location, Editor editor) {
        String uri = location.getUri();
        try {
            String filePath = (new URI(uri)).getPath();
            if (filePath == null) {
                return invalidOffset;
            }
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (virtualFile == null) {
                return invalidOffset;
            }
            Project project = editor.getProject();
            if (project == null) {
                return invalidOffset;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null || psiFile == null || !psiFile.isValid() || !isLocationValid(location, document)) {
                return invalidOffset;
            }
            Position start = location.getRange().getStart();
            return document.getLineStartOffset(start.getLine()) + start.getCharacter();
        } catch (URISyntaxException e) {
            LOG.warn("URI syntax exception");
            return invalidOffset;
        }
    }
}
