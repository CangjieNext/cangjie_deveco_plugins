/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.formatter;

import static com.huawei.ideacj.lsp.utils.CommonUtils.getCodeStyleIndent;

import com.huawei.ideacj.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.ideacj.language.CjPsiUtils;
import com.huawei.ideacj.language.CangJieTypes;
import com.huawei.ideacj.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.ideacj.language.psi.operatornode.CJAdditiveOperator;
import com.huawei.ideacj.language.psi.operatornode.CJAssignmentOperator;
import com.huawei.ideacj.language.psi.operatornode.CJComparisonOperator;
import com.huawei.ideacj.language.psi.operatornode.CJConditionOperator;
import com.huawei.ideacj.language.psi.operatornode.CJEqualityOperator;
import com.huawei.ideacj.language.psi.operatornode.CJFlowOperator;
import com.huawei.ideacj.language.psi.operatornode.CJShiftingOperator;
import com.huawei.ideacj.language.psi.operatornode.CJMultiplicativeOperator;
import com.huawei.ideacj.language.psi.othersnode.CjEnd;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjForeignMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.expressionnode.CjExpression;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.FunctionDefinitionInfo;
import com.huawei.ideacj.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclarations;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroAttrDecl;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputDecl;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.ideacj.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CangjieEnterHandlerDelegateAdapter
 *
 * @since 2025/12/16
 */
public class CangjieEnterHandlerDelegateAdapter extends EnterHandlerDelegateAdapter {
    private static final String SEPERATOR = "\n";
    private static final String TRASH_CAN = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'";

    private static final String INCREASE_INDENT_PATENT = "^.*(\\{[^}]*|\\([^)]*|\\[[^]]*)$";
    private static final String DECREASE_INDENT_PATENT = "^\\s*[}\\])>].*$";
    private static final String UNINDENTED_LINE_PATENT =
            "(\\t|[ ])*[ ]\\*[^/]*\\*/\\s*$|^(\\t|[ ])*[ ]\\*/\\s*$|^(\\t|[ ])*\\*([ ]([^\\*]|\\*(?!/))*)?$";
    private static final String INDENT_NEXT_LINE_PATENT =
            "^(.*((=>)|(=)|(>)|(<))\\s*)$";
    private static final String INDENT_CUR_PATENT = "^(\\s*((=>)|(=)|(!=)|(>)|(<)))";
    private static final String CHAIN_CALL_UNINDENTED_LINE_PATENT = "^\\s*\\.\\w+";

    private static final Pattern TRASH = Pattern.compile(TRASH_CAN);
    private static final Pattern INCREASE = Pattern.compile(INCREASE_INDENT_PATENT);
    private static final Pattern DECREASE = Pattern.compile(DECREASE_INDENT_PATENT);
    private static final Pattern UNINDENTED = Pattern.compile(UNINDENTED_LINE_PATENT);
    private static final Pattern INDENT_NEXT = Pattern.compile(INDENT_NEXT_LINE_PATENT);
    private static final Pattern INDENT_CUR = Pattern.compile(INDENT_CUR_PATENT);
    private static final Pattern CHAIN_CALL_UNINDENTED = Pattern.compile(CHAIN_CALL_UNINDENTED_LINE_PATENT);

    private static final Set<Class<?>> INDENT_PSI_NODES = Set.of(CjPostfixExpression.class);
    private static final Set<Class<?>> UN_INDENT_PSI_NODES = Set.of(CjTranslationUnit.class,
            CjExpressionOrDeclarations.class, CjMacroExpression.class, CjClassMemberDeclaration.class,
            CjInterfaceMemberDeclaration.class, CjStructMemberDeclaration.class, CjExtendMemberDeclaration.class,
            CjPropertyMemberDeclaration.class, CjForeignMemberDeclaration.class);
    private static final Set<Class<?>> OPERATOR_PSI_NODES = Set.of(CJAdditiveOperator.class,
            CJAssignmentOperator.class, CJComparisonOperator.class, CJConditionOperator.class,
            CJEqualityOperator.class, CJFlowOperator.class, CJShiftingOperator.class, CJMultiplicativeOperator.class);

    /** PSI node types that can carry a generated doc comment template. */
    private static final List<Class<? extends PsiElement>> DOC_TARGET_TYPES = List.of(
            CjFunctionDefinition.class,
            CjOperatorFunctionDefinition.class,
            CjClassInit.class,
            CjClassPrimaryInit.class,
            CjStructInit.class,
            CjStructPrimaryInit.class,
            CjMacroDefinition.class);

    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        if (!(file.getLanguage() instanceof CangJieLanguage)) {
            return super.postProcessEnter(file, editor, dataContext);
        }
        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(offset);
        if (line <= 0) {
            return null;
        }

        PsiDocumentManager.getInstance(file.getProject()).commitDocument(doc);

        String prevTextA = getLineText(doc, line - 1);
        if ("/**".equals(prevTextA.stripLeading())) {
            return handleDocCommentStart(file, doc, editor, line - 1, offset);
        }

        PsiElement atCaret = file.findElementAt(offset);
        if (atCaret == null && offset > 0) {
            atCaret = file.findElementAt(offset - 1);
        }
        if (atCaret instanceof PsiComment pc
                && pc.getText().startsWith("/**")
                && !pc.getText().contains("*/")) {
            int commentLine = doc.getLineNumber(pc.getTextOffset());
            int indentI = countLeadingSpaces(getLineText(doc, commentLine), doc, file.getProject());
            doc.insertString(offset, " ".repeat(indentI) + "* ");
            editor.getCaretModel().moveToOffset(offset + indentI + 2);
            return Result.Continue;
        }

        String currLineC = getLineText(doc, line);
        if (currLineC.stripLeading().startsWith("//")) {
            String afterCaret = doc.getText().substring(offset, doc.getLineEndOffset(line));
            if (!afterCaret.trim().isEmpty()) {
                return Result.Continue;
            }
        }

        int currLineStartOffset = doc.getLineStartOffset(line);
        String prevTextD = getLineText(doc, line - 1);
        if (prevTextD.stripLeading().startsWith("//") && getLineText(doc, line).strip().isEmpty()) {
            // Enter at the end of a line comment: keep the new line aligned with
            // the comment's indent but do NOT continue the "//" prefix, matching
            // common editor behaviour. Mid-comment Enter is handled by the
            // platform line-comment handler, which carries the prefix itself.
            int indentI = countLeadingSpaces(prevTextD, doc, file.getProject());
            doc.replaceString(currLineStartOffset, offset, " ".repeat(indentI));
            editor.getCaretModel().moveToOffset(currLineStartOffset + indentI);
            return Result.Continue;
        }

        // Generic indent handling for non-comment lines; every path continues
        return handleGeneralIndent(file, doc, editor, line, offset);
    }

    /**
     * Generic (non-comment) Enter handling: adjusts the caret line indent
     * according to the previous line, bracket state and code style rules.
     * All paths in this method return Result.Continue.
     *
     * @param file the PSI file
     * @param doc the document
     * @param editor the editor
     * @param line the zero-based line index of the caret line
     * @param offset the caret offset
     * @return Result.Continue
     */
    private Result handleGeneralIndent(PsiFile file, Document doc, Editor editor, int line, int offset) {
        int prevLine = line - 1;
        prevLine = getRealTokenPreLine(file, doc, prevLine);
        if (prevLine < 0) {
            return Result.Continue;
        }

        // current line text, kept close to first usage (G.DCL.02)
        String curr = getLineText(doc, line);
        if (UNINDENTED.matcher(curr).find()) {
            return Result.Continue;
        }

        int currLineStartOffset = doc.getLineStartOffset(line);
        int caretOffset = offset;
        String text = doc.getText(new TextRange(currLineStartOffset, caretOffset));
        if (text.contains("\t")) {
            int tabSize = CodeStyle.getIndentOptions(file.getProject(), doc).TAB_SIZE;
            int tabCount = StringUtil.countChars(text, '\t');
            text = text.replaceAll("\t", " ".repeat(tabSize));
            doc.replaceString(currLineStartOffset, caretOffset, text);
            caretOffset = currLineStartOffset + tabCount * tabSize;
            editor.getCaretModel().moveToOffset(caretOffset);
        }

        // previous line text, kept close to first usage (G.DCL.02)
        String prev = getLineText(doc, prevLine);
        int indentSize = getCodeStyleIndent(file);
        int baseIndentPrev = countLeadingSpaces(prev, doc, file.getProject());

        // enter inside empty ()、[]、{}
        if ((matchesExcludingStrings(prev, INCREASE)
                || !checkIsClose(prev, file, doc.getLineStartOffset(line - 1))) && DECREASE.matcher(curr).find()) {
            return insertSeparatorIndent(editor, caretOffset, indentSize, baseIndentPrev, 0);
        }

        // e.g.  evt => xxxxx <enter at this position> }
        if (matchesExcludingStrings(prev, INDENT_NEXT) && DECREASE.matcher(curr).find()) {
            return insertSeparatorIndent(editor, caretOffset, indentSize, baseIndentPrev, indentSize);
        }

        return adjustFinalIndent(file, doc, editor, line, caretOffset);
    }

    /**
     * Inserts a separator line with the computed base indent when Enter is
     * pressed inside empty brackets or right after an arrow/assignment line.
     *
     * @param editor the editor
     * @param offset the caret offset (after tab normalization)
     * @param indentSize the code style indent size
     * @param baseIndentPrev the indent of the previous line
     * @param extraUnindent extra unindent for the second line (0 for brackets,
     *        indentSize for arrow/assignment lines)
     * @return Result.Continue
     */
    private Result insertSeparatorIndent(Editor editor, int offset, int indentSize, int baseIndentPrev,
        int extraUnindent) {
        editor.getDocument().insertString(offset,
                " ".repeat(baseIndentPrev + indentSize)
                        + SEPERATOR
                        + " ".repeat(Math.max(0, baseIndentPrev - extraUnindent)));
        editor.getCaretModel().moveToOffset(offset + indentSize);
        return Result.Continue;
    }

    /**
     * Applies the final indent adjustment for the caret line: honors the
     * top-level-node check and the increase/decrease indent rules derived from
     * the previous and current line, then inserts, deletes or moves the caret
     * to the proper indent position.
     *
     * @param file the PSI file
     * @param doc the document
     * @param editor the editor
     * @param line the zero-based line index of the caret line
     * @param caretOffset the caret offset (after tab normalization)
     * @return Result.Continue
     */
    private Result adjustFinalIndent(PsiFile file, Document doc, Editor editor, int line, int caretOffset) {
        int prevLine = getRealTokenPreLine(file, doc, line - 1);
        if (prevLine < 0) {
            return Result.Continue;
        }

        PsiElement curEle = file.findElementAt(caretOffset);
        if (curEle == null && doc.getTextLength() == caretOffset) {
            // is in doc end position
            curEle = file.findElementAt(caretOffset - 1);
        }
        // current line text, kept close to first usage (G.DCL.02)
        String curr = getLineText(doc, line);
        if (checkTopLevelNode(editor, curEle, curr, line, caretOffset)) {
            return Result.Continue;
        }

        // previous line text, kept close to first usage (G.DCL.02)
        String prev = getLineText(doc, prevLine);
        int indentSize = getCodeStyleIndent(file);
        int increaseIndent = checkCodeIdentIncrease(indentSize, new Pair<>(prev, curr), doc, curEle, line);
        increaseIndent = increaseIndent - checkCodeIdentDecrease(indentSize, prev, curr, curEle);

        int currLineStartOffset = doc.getLineStartOffset(line);
        int baseIndentCurr = countLeadingSpaces(curr, doc, file.getProject());
        int redundantSpaceNum = baseIndentCurr - (caretOffset - currLineStartOffset);
        increaseIndent = increaseIndent - redundantSpaceNum;
        if (increaseIndent >= 0) {
            editor.getDocument().insertString(caretOffset, " ".repeat(increaseIndent));
            editor.getCaretModel().moveToOffset(Math.max(currLineStartOffset, caretOffset + increaseIndent));
            return Result.Continue;
        }
        if (caretOffset - currLineStartOffset >= 0 && caretOffset - currLineStartOffset <= curr.length()
                && curr.substring(0, caretOffset - currLineStartOffset).trim().isEmpty()) {
            // delete indent when current line string between start to offset is empty
            editor.getDocument().deleteString(Math.max(currLineStartOffset, caretOffset + increaseIndent), caretOffset);
        }
        editor.getCaretModel().moveToOffset(Math.max(currLineStartOffset, caretOffset + increaseIndent));
        return Result.Continue;
    }

    /**
     * Handles Enter pressed right after typing the "/**" doc-comment start:
     * aligns the caret line with the comment start and, when the doc comment is
     * directly followed by a function declaration, inserts a generated
     * &#64;param/&#64;return template; otherwise falls back to a plain
     * " * ... * /" skeleton.
     *
     * @param file the PSI file
     * @param doc the document
     * @param editor the editor
     * @param commentLine the zero-based line index of the "/**" start line
     * @param offset the caret offset
     * @return Result.Continue
     */
    private Result handleDocCommentStart(PsiFile file, Document doc, Editor editor, int commentLine, int offset) {
        String commentText = getLineText(doc, commentLine);
        int indentI = countLeadingSpaces(commentText, doc, file.getProject());
        int currLineStartOffset = doc.getLineStartOffset(commentLine + 1);
        doc.replaceString(currLineStartOffset, offset, " ".repeat(indentI));
        int caretOffset = currLineStartOffset + indentI;
        editor.getCaretModel().moveToOffset(caretOffset);

        Optional<FunctionDefinitionInfo> infoOpt = findDocTargetInfoAfterDocComment(file, doc, commentLine);
        if (infoOpt.isPresent()) {
            String template = buildDocTemplate(infoOpt.get(), indentI);
            doc.insertString(caretOffset, template);
            removeExtraCommentClose(doc, caretOffset + template.length());
            editor.getCaretModel().moveToOffset(caretOffset + 3);
            return Result.Continue;
        }
        // Plain skeleton when no function signature is attached to this comment
        String skeleton = " * \n" + " ".repeat(indentI) + " */";
        doc.insertString(caretOffset, skeleton);
        removeExtraCommentClose(doc, caretOffset + skeleton.length());
        editor.getCaretModel().moveToOffset(caretOffset + 3);
        return Result.Continue;
    }

    /**
     * Finds the signature info of the declaration attached to the doc comment at
     * the given line. Covers regular functions, constructors (class/struct init
     * and primary constructors), operator functions and macro definitions.
     *
     * @param file the PSI file
     * @param doc the document
     * @param commentLine the zero-based line index of the doc-comment start line
     * @return the signature info of the nearest supported declaration after the
     *         comment, or empty when no supported declaration follows the comment
     *         or the PSI index is temporarily unavailable
     */
    private Optional<FunctionDefinitionInfo> findDocTargetInfoAfterDocComment(
            PsiFile file, Document doc, int commentLine) {
        try {
            int commentOffset = doc.getLineStartOffset(commentLine)
                    + getLineText(doc, commentLine).indexOf("/**");
            PsiElement commentEl = file.findElementAt(commentOffset);
            if (commentEl == null) {
                commentEl = file.findElementAt(commentOffset - 1);
            }
            if (commentEl instanceof PsiComment docComment) {
                int commentEnd = docComment.getTextRange().getEndOffset();
                PsiElement target = findNearestDocTargetAfter(file, commentEnd);
                // Accept only when nothing but blank / comment-decoration / annotation
                // lines sits between the doc comment and the target — a
                // class/struct/interface between us and the nearest declaration must
                // not leak its signature.
                if (target != null && isOnlyCommentDecorationBetween(doc, commentLine,
                        target.getTextRange().getStartOffset())) {
                    return getFunctionDefinitionInfo(target);
                }
            }
            return Optional.empty();
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (IndexNotReadyException e) {
            // Index not ready yet — fall back to the plain skeleton
            return Optional.empty();
        }
    }

    private static PsiElement findNearestDocTargetAfter(PsiFile file, int commentEnd) {
        // The doc comment and the declaration may be nested/wrapped differently
        // depending on the PSI structure, so pick the FIRST declaration whose
        // start offset is after the comment instead of walking siblings.
        PsiElement nearest = null;
        int nearestStart = Integer.MAX_VALUE;
        for (Class<? extends PsiElement> type : DOC_TARGET_TYPES) {
            for (PsiElement element : PsiTreeUtil.findChildrenOfType(file, type)) {
                int start = element.getTextRange().getStartOffset();
                if (start >= commentEnd && start < nearestStart) {
                    nearest = element;
                    nearestStart = start;
                }
            }
        }
        return nearest;
    }

    /**
     * Extracts the signature info from any supported documentable declaration.
     *
     * @param element the PSI declaration node
     * @return the signature info, or empty when the node carries none
     */
    private static Optional<FunctionDefinitionInfo> getFunctionDefinitionInfo(PsiElement element) {
        if (element instanceof CjFunctionDefinition func) {
            return Optional.ofNullable(func.getFunctionDefinitionInfo());
        }
        if (element instanceof CjOperatorFunctionDefinition operatorFunc) {
            return Optional.ofNullable(operatorFunc.getFunctionDefinitionInfo());
        }
        if (element instanceof CjClassInit classInit) {
            return Optional.ofNullable(classInit.getFunctionDefinitionInfo());
        }
        if (element instanceof CjClassPrimaryInit classPrimaryInit) {
            return Optional.ofNullable(classPrimaryInit.getFunctionDefinitionInfo());
        }
        if (element instanceof CjStructInit structInit) {
            return Optional.ofNullable(structInit.getFunctionDefinitionInfo());
        }
        if (element instanceof CjStructPrimaryInit structPrimaryInit) {
            return Optional.ofNullable(structPrimaryInit.getFunctionDefinitionInfo());
        }
        if (element instanceof CjMacroDefinition macroDef) {
            return getMacroDefinitionInfo(macroDef);
        }
        return Optional.empty();
    }

    /**
     * Builds signature info for a macro definition. Macro parameters use the
     * dedicated macroAttrDecl / macroInputDecl rules which the shared
     * CjFunctionInfoHandlers does not cover, so they are extracted here; the
     * return type reuses the same helper as the usage view.
     *
     * @param macroDef the macro definition node
     * @return the signature info, or empty when unavailable
     */
    private static Optional<FunctionDefinitionInfo> getMacroDefinitionInfo(CjMacroDefinition macroDef) {
        FunctionDefinitionInfo info = macroDef.getFunctionDefinitionInfo();
        if (info == null) {
            return Optional.empty();
        }
        LinkedHashMap<String, String> args = new LinkedHashMap<>();
        collectMacroParams(macroDef, CjMacroAttrDecl.class, args);
        collectMacroParams(macroDef, CjMacroInputDecl.class, args);
        info.setUnNamedArgs(args);
        String returnType = CjPsiUtils.getMacroReturnType(macroDef);
        if (returnType != null && !returnType.isEmpty()) {
            info.setReturnType(returnType);
        }
        return Optional.of(info);
    }

    /**
     * Collects macro parameters of the given declaration type into an
     * insertion-ordered map. Each declaration's text is "name: Type", the name
     * comes from the first identifier child, the type from the text after the
     * first colon.
     *
     * @param macroDef the macro definition node
     * @param declType CjMacroAttrDecl or CjMacroInputDecl
     * @param args the target ordered map
     */
    private static void collectMacroParams(CjMacroDefinition macroDef,
            Class<? extends PsiElement> declType, LinkedHashMap<String, String> args) {
        for (PsiElement decl : PsiTreeUtil.findChildrenOfType(macroDef, declType)) {
            String name = null;
            for (PsiElement child : decl.getChildren()) {
                if (CjPsiUtils.INSTANCE.isIdentifier(child)) {
                    name = child.getText().trim();
                    break;
                }
            }
            if (name == null || name.isEmpty()) {
                continue;
            }
            String flat = decl.getText().replaceAll("\\s+", " ").trim();
            int colon = flat.indexOf(':');
            String type = colon >= 0 ? flat.substring(colon + 1).trim() : "";
            args.put(name, type);
        }
    }

    private static String buildDocTemplate(FunctionDefinitionInfo info, int indentI) {
        StringBuilder template = new StringBuilder(" * ");
        if (info.getUnNamedArgs() != null) {
            for (String p : info.getUnNamedArgs().keySet()) {
                template.append("\n").append(" ".repeat(indentI)).append(" * @param ").append(p).append(" ");
            }
        }
        if (info.getNamedArgs() != null) {
            for (String p : info.getNamedArgs().keySet()) {
                template.append("\n").append(" ".repeat(indentI)).append(" * @param ").append(p).append(" ");
            }
        }
        String retType = info.getReturnType();
        if (retType != null && !retType.isEmpty()) {
            template.append("\n").append(" ".repeat(indentI)).append(" * @return ");
        }
        template.append("\n").append(" ".repeat(indentI)).append(" */");
        return template.toString();
    }

    private boolean matchesExcludingStrings(String text, Pattern pattern) {
        if (StringUtils.isEmpty(text) || pattern == null) {
            return false;
        }
        String cleanText = text.replaceAll(TRASH_CAN, "");
        return pattern.matcher(cleanText).find();
    }

    private boolean checkTopLevelNode(Editor editor, PsiElement curEle, String curr, int line, int offset) {
        if (curEle instanceof PsiErrorElement) {
            return false;
        }
        if (!(curEle instanceof CJPsiLeafNode leafNode) || !leafNode.getTokenType().equals(CangJieTypes.NL)) {
            return false;
        }
        Document doc = editor.getDocument();
        if (line < 0 || line >= doc.getLineCount()) {
            return false;
        }
        int baseIndentCurr = countLeadingSpaces(curr, doc, curEle.getProject());
        if (baseIndentCurr != offset - doc.getLineStartOffset(line)) {
            baseIndentCurr = offset - doc.getLineStartOffset(line);
        }
        int currLineStartOffset = doc.getLineStartOffset(line);
        PsiElement parent = curEle.getParent();
        if (parent != null && (UN_INDENT_PSI_NODES.contains(parent.getClass())
                || (parent.getParent() != null && UN_INDENT_PSI_NODES.contains(parent.getParent().getClass())))) {
            PsiElement targetEle = parent.getParent();
            int targetLine = doc.getLineNumber(targetEle.getTextOffset());
            targetLine = getRealTokenLine(targetEle.getContainingFile(), doc, targetLine);
            String targetText = getLineText(doc, targetLine);
            int baseIdentTarget = countLeadingSpaces(targetText, doc, curEle.getProject());
            // Calculate indent change relative to current line start
            int indentChange = baseIdentTarget - baseIndentCurr;
            if (indentChange > 0) {
                // Only insert spaces when increasing indent
                editor.getDocument().insertString(offset, " ".repeat(indentChange));
            }
            // Ensure cursor position is within valid bounds
            int targetOffset = offset + indentChange;
            int safeOffset = Math.max(currLineStartOffset, Math.min(targetOffset, doc.getTextLength()));
            editor.getCaretModel().moveToOffset(safeOffset);
            return true;
        }
        return false;
    }

    private int checkCodeIdentIncrease(int indentSize, Pair<String, String> preAndCurText,
                                       Document doc, PsiElement curEle, int curLine) {
        String prev = preAndCurText.first;
        String curr = preAndCurText.second;
        int increaseIndent = 0;
        if (curEle == null || doc == null) {
            return increaseIndent;
        }
        int baseIndentCurr = countLeadingSpaces(curr, doc, curEle.getProject());

        // match INCREASE, indent + 1
        if (matchesExcludingStrings(prev, INCREASE)
                || !checkIsClose(prev, curEle.getContainingFile(), doc.getLineStartOffset(curLine - 1))) {
            increaseIndent += indentSize;
            baseIndentCurr += indentSize;

            // match DECREASE, indent - 1
            if (DECREASE.matcher(curr).find()) {
                increaseIndent -= indentSize;
                baseIndentCurr -= indentSize;
            }
            return increaseIndent;
        }

        // => <enter at this position>
        if (matchesExcludingStrings(prev, INDENT_NEXT)
                && !matchesExcludingStrings(prev, INCREASE) && !(curEle.getParent() instanceof CjEnd)) {
            increaseIndent += indentSize;
            baseIndentCurr += indentSize;
            return increaseIndent;
        }

        // <enter at this position> = / !=/ > / < / >= / <= / ==
        if (INDENT_CUR.matcher(curr).find()) {
            increaseIndent += indentSize;
            baseIndentCurr += indentSize;
            return increaseIndent;
        }

        // check chain call
        return checkChainCallExpr(indentSize, doc, curEle, curr);
    }

    private int checkChainCallExpr(int indentSize, Document doc, PsiElement curEle, String curr) {
        int increaseIndent = 0;
        if (curEle == null) {
            return increaseIndent;
        }
        int baseIndentCurr = countLeadingSpaces(curr, doc, curEle.getProject());
        PsiElement parent = curEle.getParent();
        if (parent == null) {
            return increaseIndent;
        }
        if (curEle instanceof CJPsiLeafNode leafNode && leafNode.getTokenType().equals(CangJieTypes.IDENTIFIER)) {
            parent = parent.getParent();
        }
        if (parent == null) {
            return increaseIndent;
        }
        int offset = curEle.getTextOffset();
        int parentStartOffset = parent.getTextOffset();
        int parentEndOffset = parent.getTextOffset() + parent.getTextLength();
        if (offset <= parentStartOffset || offset >= parentEndOffset || !INDENT_PSI_NODES.contains(parent.getClass())) {
            return increaseIndent;
        }
        while (!(parent instanceof CjTranslationUnit) && parent != null) {
            parent = parent.getParent();
            if (parent instanceof CjExpression) {
                break;
            }
        }
        if (!(parent instanceof CjExpression)) {
            return increaseIndent;
        }
        int parentLine = doc.getLineNumber(parent.getTextOffset());
        String parentText = getLineText(doc, parentLine);
        if (CHAIN_CALL_UNINDENTED.matcher(parentText).find()) {
            return increaseIndent;
        }
        int baseIdentParent = countLeadingSpaces(parentText, doc, curEle.getProject());
        if (baseIdentParent + indentSize == baseIndentCurr) {
            return increaseIndent;
        }
        if (baseIdentParent + indentSize < baseIndentCurr) {
            return baseIdentParent + indentSize - baseIndentCurr;
        }
        increaseIndent += indentSize;
        baseIndentCurr += indentSize;
        return increaseIndent;
    }

    private int checkCodeIdentDecrease(int indentSize, String prev, String curr, PsiElement curEle) {
        int decreaseIndent = 0;
        if (curEle == null) {
            return decreaseIndent;
        }
        // operator do not check
        if (curEle.getParent() != null) {
            PsiElement parent = curEle.getParent();
            if (OPERATOR_PSI_NODES.contains(parent.getClass())) {
                return decreaseIndent;
            }
            if (parent.getParent() != null && OPERATOR_PSI_NODES.contains(parent.getParent().getClass())) {
                return decreaseIndent;
            }
        }

        // } <enter at this position>
        if (matchesExcludingStrings(prev, DECREASE) && DECREASE.matcher(curr).find()) {
            decreaseIndent += indentSize;
            return decreaseIndent;
        }

        // xxxxxxxxx <enter at this position>}
        if (!prev.trim().isEmpty() && DECREASE.matcher(curr).find()) {
            decreaseIndent += indentSize;
            return decreaseIndent;
        }

        return decreaseIndent;
    }

    private int getRealTokenPreLine(PsiFile file, Document doc, int preLine) {
        int realLine = preLine;
        while (realLine >= 0) {
            int lineStartOffset = doc.getLineStartOffset(realLine);
            // check is comment
            if (!(file.findElementAt(lineStartOffset) instanceof PsiComment)) {
                break;
            }
            realLine--;
        }
        return realLine;
    }

    private int getRealTokenLine(PsiFile file, Document doc, int line) {
        int realLine = line;
        int lineStartOffset = doc.getLineStartOffset(realLine);
        while (realLine < doc.getLineCount()) {
            // check is comment
            if (!(file.findElementAt(lineStartOffset) instanceof PsiComment)) {
                break;
            }
            realLine++;
            lineStartOffset = doc.getLineStartOffset(realLine);
        }
        return realLine;
    }

    private boolean checkIsClose(String str, PsiFile psiFile, int lineStartOffset) {
        Stack<Character> stack = new Stack<>();
        Matcher matcher = TRASH.matcher(str);
        boolean hasLShiftAssign = false;

        int i = 0;
        while (i < str.length()) {
            // search is into trash text such as string, skip
            if (matcher.find(i) && matcher.start() == i) {
                i = matcher.end();
                continue;
            }

            char c = str.charAt(i);
            switch (c) {
                case '(':
                case '{':
                case '[':
                    stack.push(c);
                    break;
                case '<':
                    if (!checkIsRealLeftAngleBracket(i, str, psiFile, lineStartOffset)) {
                        break;
                    }
                    stack.push(c);
                    hasLShiftAssign = true;
                    break;
                case ')':
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (stack.pop() != '(') {
                        return false;
                    }
                    break;
                case '}':
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (stack.pop() != '{') {
                        return false;
                    }
                    break;
                case ']':
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (stack.pop() != '[') {
                        return false;
                    }
                    break;
                case '>':
                    if (!hasLShiftAssign) {
                        // no left shift assign, used to be operator
                        break;
                    }
                    if (!checkIsRealRightAngleBracket(i, str, psiFile, lineStartOffset)) {
                        break;
                    }
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (stack.pop() != '<') {
                        return false;
                    }
                    hasLShiftAssign = false;
                    break;
                default:
                    break;
            }
            i++;
        }
        return stack.isEmpty();
    }

    private boolean checkIsRealLeftAngleBracket(int index, String str, PsiFile psiFile, int lineStartOffset) {
        if (index + 1 < str.length() && (str.charAt(index + 1) == '-' || str.charAt(index + 1) == '=')) {
            return false;
        }
        if (psiFile == null) {
            return true;
        }
        PsiElement element = psiFile.findElementAt(lineStartOffset + index);
        return element == null || !(element.getParent() instanceof CJComparisonOperator);
    }

    private boolean checkIsRealRightAngleBracket(int index, String str, PsiFile psiFile, int lineStartOffset) {
        if (index + 1 < str.length() && str.charAt(index + 1) == '=') {
            return false;
        }
        if (index - 1 >= 0 && (str.charAt(index - 1) == '-' || str.charAt(index - 1) == '=')) {
            return false;
        }

        if (psiFile == null) {
            return true;
        }
        PsiElement element = psiFile.findElementAt(lineStartOffset + index);
        return element == null || !(element.getParent() instanceof CJComparisonOperator);
    }

    /**
     * get line text
     *
     * @param doc doc
     * @param line line
     * @return line text
     */
    public static String getLineText(@NotNull Document doc, int line) {
        if (line < 0 || line >= doc.getLineCount()) {
            return StringUtils.EMPTY;
        }
        int start = doc.getLineStartOffset(line);
        int end = doc.getLineEndOffset(line);
        if (start < 0 || end < start || end > doc.getText().length()) {
            return StringUtils.EMPTY;
        }
        return doc.getText().substring(start, end);
    }

    /**
     * count leading spaces
     *
     * @param s s
     * @param document document
     * @param project project
     * @return spaces count
     */
    public static int countLeadingSpaces(@NotNull String s, @NotNull Document document, Project project) {
        int tabSize = CodeStyle.getIndentOptions(project, document).TAB_SIZE;
        int count = 0;
        int index = 0;
        int len = s.length();
        while (index < len && (s.charAt(index) == ' ' || s.charAt(index) == '\t')) {
            if (s.charAt(index) == '\t') {
                count += tabSize;
            }
            if (s.charAt(index) == ' ') {
                count++;
            }
            index++;
        }
        return count;
    }

    /**
     * Check the lines strictly between the doc comment line and the function start
     * line: blank lines, comment-decoration lines (leading asterisk) and
     * annotation lines (leading @, e.g. "@Frozen" above the declaration) are
     * tolerated; any other real code line means the function does not directly
     * follow the comment.
     *
     * @param doc doc
     * @param commentLine line index of the doc comment
     * @param funcStartOffset start offset of the candidate function
     * @return true when only blanks, comment decorations or annotations sit in between
     */
    private static boolean isOnlyCommentDecorationBetween(Document doc, int commentLine, int funcStartOffset) {
        int funcLine = doc.getLineNumber(funcStartOffset);
        for (int l = commentLine + 1; l < funcLine; l++) {
            String t = getLineText(doc, l).strip();
            if (!t.isEmpty() && !t.startsWith("*") && !t.startsWith("@")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove an extra block comment close marker that the platform
     * EnterInBlockCommentHandler may have inserted right after our own
     * doc-comment template, to avoid a duplicated close.
     *
     * @param doc document
     * @param insertEnd offset right after the text we inserted
     */
    private static void removeExtraCommentClose(Document doc, int insertEnd) {
        CharSequence text = doc.getCharsSequence();
        int i = insertEnd;
        // A stray close inserted by other handlers sits on its own line: "\n" + indent + "*/"
        if (i < text.length() && text.charAt(i) == '\n') {
            i++;
        }
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        if (i + 2 <= text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
            int end = i + 2;
            // Also swallow one blank line left behind after the stray close
            if (end + 1 < text.length() && text.charAt(end) == '\n' && text.charAt(end + 1) == '\n') {
                end++;
            }
            doc.deleteString(insertEnd, end);
        }
    }
}