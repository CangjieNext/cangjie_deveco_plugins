/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.generation.override;

import static com.huawei.ideacj.lsp.utils.CommonUtils.getCodeStyleIndent;

import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassBody;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjAssociatedTypeDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassFinalizer;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumCaseBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendBody;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceBody;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructBody;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Cangjie chooser  member creator
 *
 * @param <T>
 * @since 2025-07-01
 */
public class CangjieCreateMember<T> {
    private PsiElement anchor;

    private int insertOffset = -1;

    private Document document;

    /**
     * default indent, 4 spaces
     */
    private String indentText = "    ";

    private final Set<T> elementsToProcess = new LinkedHashSet<>();

    @NotNull
    private final PsiElement outerClass;

    public CangjieCreateMember(@NotNull PsiElement outer) {
        this.outerClass = outer;
    }

    /**
     * Add choose element
     *
     * @param element T
     */
    public void addElementToProcess(T element) {
        this.elementsToProcess.add(element);
    }

    /**
     * Create all elements
     *
     * @param editor Editor
     * @param file PsiFile
     * @throws IncorrectOperationException IncorrectOperationException
     */
    public void invoke(@NotNull Editor editor, @NotNull PsiFile file)
        throws IncorrectOperationException {
        indentText = " ".repeat(getCodeStyleIndent(file));
        document = editor.getDocument();
        evalAnchor(editor, file);
        processElements();
    }

    private void evalAnchor(@Nullable Editor editor, @NotNull PsiFile file) {
        if (editor == null || !PsiTreeUtil.instanceOf(outerClass, CjClassDefinition.class, CjInterfaceDefinition.class,
                CjStructDefinition.class, CjEnumDefinition.class, CjExtendDefinition.class)) {
            return;
        }

        PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
        anchor = at;

        while (anchor != null) {
            // carat position is in member decl
            if (isMemberDeclaration(anchor)) {
                PsiElement parent = anchor.getParent();
                // element must be a kind of decl while it's parent node must be a kind of member declaration
                // we treat such element as the real member decl
                boolean isMemberDecl = parent != null && PsiTreeUtil.instanceOf(parent, CjClassMemberDeclaration.class,
                        CjInterfaceMemberDeclaration.class, CjStructMemberDeclaration.class, CjEnumBody.class,
                        CjExtendMemberDeclaration.class);
                if (isMemberDecl) {
                    break;
                } else {
                    anchor = anchor.getParent();
                    continue;
                }
            }

            // carat position is in class body but not in any member, '[|]{'/ '[|]}' should be treated as out of body
            boolean isBody = isBodyElement(anchor) && anchor.getTextOffset() != at.getTextOffset()
                    && anchor.getTextRange().getEndOffset() != at.getTextRange().getEndOffset();
            if (isBody && (!(outerClass instanceof CjEnumDefinition)
                    || PsiTreeUtil.getParentOfType(at, CjEnumCaseBody.class) == null && !isInEnumCaseBody(at))) {
                    anchor = at;
                    break;
            }

            // carat position is not in class body
            if (isInheritableElement(anchor)) {
                PsiElement body = PsiTreeUtil.getChildOfAnyType(anchor, CjClassBody.class, CjInterfaceBody.class,
                        CjStructBody.class, CjEnumBody.class, CjExtendBody.class);
                if (body != null) {
                    anchor = body.getLastChild().getPrevSibling();
                } else {
                    anchor = null;
                }
                break;
            }
            anchor = anchor.getParent();
        }
        if (anchor == null) {
            anchor = at;
        }

        deleteSpaceLine();
    }

    private void deleteSpaceLine() {
        int endOffset = anchor.getTextRange().getEndOffset();
        int lineStartOff = document.getLineStartOffset(document.getLineNumber(endOffset));
        String lineStartStr = document.getText(new TextRange(lineStartOff, endOffset));
        if (lineStartStr.trim().isEmpty()) {
            document.insertString(lineStartOff, "\n");
        } else {
            document.insertString(endOffset, "\n");
        }
        int anchorLine = document.getLineNumber(endOffset + 1);

        // no space char between endOffSet and line tail
        // delete space line up to down
        if (isLineEmpty(anchorLine)) {
            int line = anchorLine + 1;
            while (isLineEmpty(line)) {
                int lineStartOffset = document.getLineStartOffset(line);
                int nextLineStartOffset = document.getLineStartOffset(line + 1);
                document.deleteString(lineStartOffset, nextLineStartOffset);
            }
        }

        if (isLineEmpty(anchorLine)) {
            anchorLine++;
        }

        // delete space line down to up
        int line = anchorLine - 1;
        while (isLineEmpty(line)) {
            int lineStartOffset = document.getLineStartOffset(line);
            int nextLineStartOffset = document.getLineStartOffset(line + 1);
            document.deleteString(lineStartOffset, nextLineStartOffset);
            line--;
            anchorLine--;
        }
        insertOffset = document.getLineStartOffset(anchorLine);
    }

    private boolean isLineEmpty(int line) {
        if (line < 0 || line >= document.getLineCount()) {
            return false;
        }

        int lineStartOffset = document.getLineStartOffset(line);
        int lineEndOffset = document.getLineEndOffset(line);

        String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset));
        return lineText.trim().isEmpty();
    }

    private void processElements() throws IncorrectOperationException {
        ArrayList<T> elementPointers = new ArrayList<>(elementsToProcess);
        elementsToProcess.clear();
        elementPointers.forEach(element -> {
            if (element instanceof CangjieChooserElementNode cjElement) {
                addMethodToClass(cjElement);
            }
        });
    }

    private void addMethodToClass(CangjieChooserElementNode element) throws IncorrectOperationException {
        if (element == null || (element.getInsertText().isEmpty() && insertOffset != - 1)) {
            return;
        }

        String insertText = element.getInsertText();
        String[] insertTexts = insertText.split("\\R");
        for (String text : insertTexts) {
            addIndent();
            document.insertString(insertOffset, text);
            insertOffset += text.length();
            addLine(insertOffset);
            insertOffset++;
        }
    }

    private void addIndent() {
        document.insertString(insertOffset, indentText);
        insertOffset += indentText.length();
    }

    private boolean isMemberDeclaration(PsiElement element) {
        return PsiTreeUtil.instanceOf(element, CjClassPrimaryInit.class, CjClassInit.class, CjVariableDeclaration.class,
                CjFunctionDefinition.class, CjOperatorFunctionDefinition.class, CjAssociatedTypeDeclaration.class,
                CjPropertyDefinition.class, CjClassFinalizer.class);
    }

    private boolean isBodyElement(PsiElement element) {
        return PsiTreeUtil.instanceOf(element, CjClassBody.class, CjInterfaceBody.class,
                CjStructBody.class, CjEnumBody.class, CjExtendBody.class);
    }

    private boolean isInheritableElement(PsiElement element) {
        return PsiTreeUtil.instanceOf(element, CjClassDefinition.class, CjInterfaceDefinition.class,
                CjStructDefinition.class, CjEnumDefinition.class, CjExtendDefinition.class);
    }

    private void addLine(int offset) {
        if (offset < 0 || offset > document.getLineEndOffset(document.getLineCount() - 1)) {
            return;
        }
        document.insertString(offset, "\n");
    }

    private boolean isInEnumCaseBody(PsiElement element) {
        if (element == null) {
            return false;
        }

        PsiElement e = element;
        while (e != null) {
            if (e instanceof CjEnumCaseBody || e.getText().equals("...")) {
                return true;
            }
            e = e.getNextSibling();
        }
        return false;
    }
}
