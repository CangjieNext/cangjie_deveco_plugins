/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.codefolding;

import com.huawei.ideacj.language.CangJieTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * CangJieFoldingBuilder
 *
 * @since 2021-07-20
 */
public class CangJieFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
    @NotNull
    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors, @NotNull PsiElement root,
                                            @NotNull Document document, boolean isQuick) {
        // The cases we want to fold are:
        // * block internals, between { and }
        // * multiline comments, between /* and */
        // * multiple line comments, between // and end of line
        // * multiple importlist
        FoldRecursiveElementWalkingVisitor visitor = new FoldRecursiveElementWalkingVisitor(root, descriptors);
        visitor.visitElement(root);
        visitor.addLineCommentFolding();
    }

    @Nullable
    @Override
    protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
        if (node instanceof PsiComment) {
            if (node.getElementType() == CangJieTypes.BLOCK_COMMENT) {
                return "/*...*/";
            } else {
                // For CangJieTypes.LINE_COMMENT
                return "// ...";
            }
        } else if (node.getElementType() == CangJieTypes.RULE_IMPORT_LIST) {
            return "import ...";
        } else {
            return "{...}";
        }
    }

    @Override
    public boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
        return node.findChildByType(CangJieTypes.IMPORT) != null;
    }

    @Override
    protected boolean isCustomFoldingCandidate(@NotNull ASTNode node) {
        return node.getElementType() == CangJieTypes.EDITOR_FOLD_START
                || node.getElementType() == CangJieTypes.EDITOR_FOLD_END;
    }

    private static class FoldRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private Stack<PsiElement> braceStack;
        private final PsiElement root;
        private final List<FoldingDescriptor> descriptors;
        private FoldRecord commentFoldRecord = new FoldRecord();
        private IElementType[] elementTypeArray = {CangJieTypes.NL, CangJieTypes.RULE_END,
                TokenType.WHITE_SPACE, CangJieTypes.RULE_TRANSLATION_UNIT};
        private Set<IElementType> elementTypeSet = new HashSet<>(Arrays.asList(elementTypeArray));

        /**
         * Visitor for folding.
         *
         * @param root interface of PsiElement
         * @param descriptors interface of List<FoldingDescriptor>
         */
        public FoldRecursiveElementWalkingVisitor(PsiElement root, List<FoldingDescriptor> descriptors) {
            this.root = root;
            this.descriptors = descriptors;
            braceStack = new Stack<>();
            commentFoldRecord.reset();
        }

        private static class FoldRecord {
            int start = -1;
            int end = -1;
            int count = 0;

            /**
             *  reset FoldRecord
             */
            public void reset() {
                start = -1;
                end = -1;
                count = 0;
            }
        }

        @Override
        public void visitElement(PsiElement element) {
            IElementType elementType = element.getNode().getElementType();
            handleDelimitedeComment(element, elementType);
            handleLineComment(element, elementType);
            handleDelimitedeComment(element, elementType);
            handleImportStatement(element, elementType);
            handlBlockFoldElement(element, elementType);

            super.visitElement(element);
        }

        /**
         * Handle single-line comments, especially at the end of text.
         */
        public void addLineCommentFolding() {
            if (commentFoldRecord.count == 0) {
                return;
            } else if (commentFoldRecord.count > 1) {
                PsiElement comment = root.findElementAt(commentFoldRecord.start);
                if (comment == null) {
                    throw new IllegalStateException("failed to find element.");
                }
                descriptors.add(new FoldingDescriptor(comment,
                        new TextRange(commentFoldRecord.start, commentFoldRecord.end)));

                commentFoldRecord.reset();
            } else {
                commentFoldRecord.reset();
            }
        }

        private boolean handleDelimitedeComment(PsiElement element, IElementType elementType) {
            if (elementType != CangJieTypes.BLOCK_COMMENT) {
                return false;
            }
            descriptors.add(new FoldingDescriptor(element, new TextRange(element.getTextOffset(),
                element.getTextRange().getEndOffset())));
            return true;
        }

        private void handlBlockFoldElement(PsiElement element, IElementType elementType) {
            // for "{", "${"
            if (elementType == CangJieTypes.LCURL
                || elementType == CangJieTypes.MULTI_LINE_STR_EXPR_START
                || elementType == CangJieTypes.LINE_STR_EXPR_START) {
                braceStack.add(element);
                return;
            }

            if (braceStack.isEmpty()) {
                return;
            }

            if (elementType == CangJieTypes.RCURL) {
                PsiElement leftBrace = braceStack.pop();
                if (leftBrace.getNode().getElementType() == CangJieTypes.LCURL) {
                    descriptors.add(
                            new FoldingDescriptor(element, new TextRange(leftBrace.getNode().getStartOffset(),
                                element.getNode().getStartOffset() + 1)));
                }
            }
        }

        private void handleLineComment(PsiElement element, IElementType elementType) {
            if (elementTypeSet.contains(elementType)) {
                return;
            }

            if (CangJieTypes.LINE_COMMENT.equals(elementType)) {
                if (commentFoldRecord.count == 0) {
                    commentFoldRecord.start = element.getTextOffset();
                } else {
                    commentFoldRecord.end = element.getTextRange().getEndOffset();
                }
                commentFoldRecord.count++;
                return;
            }
            addLineCommentFolding();
        }

        private void handleImportStatement(PsiElement element, IElementType elementType) {
            if (!CangJieTypes.RULE_PREAMBLE.equals(elementType)) {
                return;
            }
            PsiElement startImportList = null;
            PsiElement endImportList = null;
            PsiElement[] children = element.getChildren();
            int startOffset = -1;
            int endOffset = -1;
            for (PsiElement child : children) {
                IElementType childType = child.getNode().getElementType();
                if (!CangJieTypes.RULE_IMPORT_LIST.equals(childType)) {
                    if (endOffset > startOffset && startOffset > 0) {
                        descriptors.add(new FoldingDescriptor(startImportList,
                                new TextRange(startOffset, endOffset)));
                        startOffset = endOffset = -1;
                    }
                    continue;
                }
                if (startOffset == -1) {
                    startImportList = child;
                    startOffset = child.getTextRange().getStartOffset();
                    endOffset = child.getTextRange().getEndOffset();
                } else {
                    endImportList = child;
                    endOffset = child.getTextRange().getEndOffset();
                }
            }
            if (endImportList == null) {
                return;
            }

            // get the endOffset before comments
            PsiElement[] childrenOfEndPsiElement = endImportList.getChildren();
            for (PsiElement child : childrenOfEndPsiElement) {
                IElementType childType = child.getNode().getElementType();
                if (CangJieTypes.RULE_IMPORT_ALL_OR_SPECIFIED.equals(childType)) {
                    endOffset = child.getTextRange().getEndOffset();
                    break;
                }
            }

            if (endOffset > startOffset && startOffset > 0) {
                descriptors.add(new FoldingDescriptor(startImportList,
                        new TextRange(startOffset, endOffset)));
            }
        }
    }
}
