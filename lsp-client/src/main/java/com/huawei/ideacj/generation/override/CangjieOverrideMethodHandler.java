/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.generation.override;

import com.huawei.ace.language.JavaScriptBundle;
import com.huawei.cjlsp.capabilities.lsp.CangjieGotoDeclarationHandler;
import com.huawei.ideacj.language.CangjieIcons;
import com.huawei.ideacj.language.psi.CangJiePsiFileRoot;
import com.huawei.ideacj.language.psi.CangjieBaseNode;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassBody;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjSuperInterfaces;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendBody;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceBody;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructBody;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.lsp.extend.ExtendRequestManager;
import com.huawei.ideacj.lsp.extend.params.OverridableMethodInfo;
import com.huawei.ideacj.lsp.extend.params.OverridableMethods;
import com.huawei.ideacj.lsp.extend.params.OverrideMethodsParams;
import com.huawei.ideacj.lsp.utils.CrashLogPackager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.concurrency.EdtExecutorService;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.ApplicationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Cangjie Override Methods Handler
 *
 * @since 2025-07-01
 */
public final class CangjieOverrideMethodHandler implements LanguageCodeInsightActionHandler {
    private static final Logger LOG = Logger.getInstance(CangjieGotoDeclarationHandler.class);

    private final EdtExecutorService myEdtExecutorService = EdtExecutorService.getInstance();

    @Override
    public boolean isValidFor(Editor editor, PsiFile file) {
        PsiElement targetElement = getInheritableElement(editor, file);
        return isValidForTarget(targetElement);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof CangJiePsiFileRoot)) {
            return;
        }

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            LOG.warn("the file is not prepare for write");
            return;
        }
        // 1.get the class definition
        PsiElement inheritableElement = getInheritableElement(editor, file);
        if (inheritableElement == null) {
            return;
        }

        PsiElement targetElement = getIdentifier(inheritableElement);
        if (targetElement == null) {
            return;
        }

        // 2. send request to lsp server
        int offset = targetElement.getTextOffset();
        Position pos = DocumentUtils.offsetToLSPPos(editor, offset);

        EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
        if (eventManager == null) {
            return;
        }
        OverrideMethodsParams params = new OverrideMethodsParams(eventManager.getIdentifier(),
                pos, inheritableElement instanceof CjExtendDefinition);
        RequestManager requestManager = eventManager.getRequestManager();

        List<OverridableMethods> overrideMethods = request(requestManager, params);
        List<CangjieChooserElementNode> memberList = convertOverridableMethods2ChooserList(overrideMethods);

        // 4. dialog overrideable methods
        myEdtExecutorService.execute(() -> {
            List<CangjieChooserElementNode> selectedElements;
            if (memberList.isEmpty()) {
                HintManager.getInstance().showErrorHint(editor, this.getNoCandidatesMessage());
                return;
            } else {
                MemberChooser<CangjieChooserElementNode> chooser = showMemberChooserDialog(project, memberList);
                if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
                    return;
                }
                if (chooser instanceof CangjieMemberChooser cangjieChooser) {
                    if (cangjieChooser.isMyCustomOptionSelected()) {
                        cangjieChooser.addOverrideModifier();
                    }
                }
                selectedElements = chooser.getSelectedElements();
            }

            // 5. generate selected override methods
            ApplicationUtils.invokeLater(() -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                doInvoke(project, editor, file, selectedElements, inheritableElement);
            });
        });
    }

    private List<OverridableMethods> request(RequestManager requestManager, OverrideMethodsParams params) {
        List<OverridableMethods> overrideMethods = null;

        if (requestManager instanceof ExtendRequestManager extendRequestManager) {
            CompletableFuture<List<OverridableMethods>> request = extendRequestManager.requestOverrideMethods(params);

            // 3. wait result returned by lsp server
            try {
                overrideMethods = request.get(20000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | ExecutionException | InterruptedException e) {
                extendRequestManager.checkStatus();
                LOG.warn("get overridable methods failed");
            } catch (JsonRpcException e) {
                extendRequestManager.getWrapper().crashed(e);
                TraceUtils.trace(TraceUtils.Action.LSP_CRASH);
                CrashLogPackager.packageLogFiles();
            }
        }

        return overrideMethods;
    }

    private void doInvoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file,
                          @Nullable List<CangjieChooserElementNode> selectedElements,
                          @NotNull PsiElement caretElement) {
        if (selectedElements == null || selectedElements.isEmpty()) {
            return;
        }

        CangjieCreateMember<CangjieChooserElementNode> createMember = new CangjieCreateMember<>(caretElement);
        Runnable runnable = () -> {
            selectedElements.forEach(elementNode -> {
                if (elementNode == null) {
                    LOG.warn("selected element is null.");
                    return;
                }
                createMember.addElementToProcess(elementNode);
            });
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    createMember.invoke(editor, file);
                } catch (IncorrectOperationException exception) {
                    LOG.warn("override method failed: add element fail");
                }
            });
        };
        if (CommandProcessor.getInstance().getCurrentCommand() == null) {
            CommandProcessor.getInstance().executeCommand(project, runnable, "Override Functions...", null);
        } else {
            runnable.run();
        }
    }

    @Nullable
    private PsiElement getInheritableElement(Editor editor, PsiFile file) {
        if (editor == null || !(file instanceof CangJiePsiFileRoot)) {
            return null;
        }

        if (editor.isDisposed()) {
            return null;
        }

        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        return PsiTreeUtil.getNonStrictParentOfType(elementAtCaret, CjClassDefinition.class,
                CjInterfaceDefinition.class, CjStructDefinition.class,
                CjEnumDefinition.class, CjExtendDefinition.class);
    }

    @Nullable
    private PsiElement getIdentifier(PsiElement element) {
        if (PsiTreeUtil.instanceOf(element, CjClassDefinition.class, CjInterfaceDefinition.class,
                CjStructDefinition.class, CjEnumDefinition.class, CjExtendDefinition.class)) {
            if (element instanceof CangjieBaseNode baseNode) {
                return baseNode.getIdentifier();
            }
        }
        return null;
    }

    @NotNull
    private MemberChooser<CangjieChooserElementNode> showMemberChooserDialog(@NotNull Project project,
        @NotNull List<CangjieChooserElementNode> candidates) {
        MemberChooser<CangjieChooserElementNode> chooser = new CangjieMemberChooser(candidates, project, false);
        chooser.setTitle(getTitle());
        chooser.setCopyJavadocVisible(false);
        chooser.show();
        return chooser;
    }

    private boolean isValidForTarget(PsiElement element) {
        if (element == null) {
            return false;
        }
        boolean valid = element.isValid() && element.isWritable()
                && PsiTreeUtil.instanceOf(element, CjClassDefinition.class, CjInterfaceDefinition.class,
                        CjStructDefinition.class, CjEnumDefinition.class, CjExtendDefinition.class)
                && getIdentifier(element) != null
                && PsiTreeUtil.getChildOfAnyType(element, CjClassBody.class, CjInterfaceBody.class,
                        CjStructBody.class, CjEnumBody.class, CjExtendBody.class) != null;

        if (element instanceof CjExtendDefinition) {
            PsiElement child = element.getFirstChild();
            boolean isInterfaceExtend = false;
            while (child != null) {
                if (child instanceof CjSuperInterfaces) {
                    isInterfaceExtend = true;
                    break;
                }
                child = child.getNextSibling();
            }
            valid &= isInterfaceExtend;
        }
        return valid;
    }

    private String getTitle() {
        return JavaScriptBundle.message("members.to.override.chooser.title");
    }

    private String getNoCandidatesMessage() {
        return JavaScriptBundle.message("no.members.to.override");
    }

    private List<CangjieChooserElementNode> convertOverridableMethods2ChooserList(List<OverridableMethods> in) {
        ArrayList<CangjieChooserElementNode> out = new ArrayList<>();
        if (in == null) {
            return out;
        }
        for (OverridableMethods item : in) {
            String fullPkgName = item.getFullPackageName();
            String parentClass = item.getIdentifier();
            String kind = item.getKind();
            for (OverridableMethodInfo info : item.getData()) {
                out.add(new CangjieChooserElementNode(fullPkgName, parentClass, kind, info,
                        info.getIsProp() ? CangjieIcons.CANGJIE_PROPERTY : CangjieIcons.CANGJIE_FUNCTION));
            }
        }
        return out;
    }
}
