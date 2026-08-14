/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.formatter;

import static com.huawei.idea.lsp.utils.CommonUtils.getCodeStyleIndent;

import com.huawei.idea.extend.cjpsi.adaptor.psi.CJPsiLeafNode;
import com.huawei.idea.language.CangJieTypes;
import com.huawei.idea.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.idea.language.psi.operatornode.CJAdditiveOperator;
import com.huawei.idea.language.psi.operatornode.CJAssignmentOperator;
import com.huawei.idea.language.psi.operatornode.CJComparisonOperator;
import com.huawei.idea.language.psi.operatornode.CJConditionOperator;
import com.huawei.idea.language.psi.operatornode.CJEqualityOperator;
import com.huawei.idea.language.psi.operatornode.CJFlowOperator;
import com.huawei.idea.language.psi.operatornode.CJShiftingOperator;
import com.huawei.idea.language.psi.operatornode.CJMultiplicativeOperator;
import com.huawei.idea.language.psi.othersnode.CjEnd;
import com.huawei.idea.language.psi.othersnode.CjPostfixExpression;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjForeignMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjPropertyMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.expressionnode.CjExpression;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclarations;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructMemberDeclaration;
import com.huawei.idea.lsp.utils.CangJieLanguage;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        if (!(file.getLanguage() instanceof CangJieLanguage)) {
            return super.postProcessEnter(file, editor, dataContext);
        }
        int indentSize = getCodeStyleIndent(file);
        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(offset);
        int currLineStartOffset = doc.getLineStartOffset(line);
        if (line <= 0) {
            return null;
        }
        int prevLine = line - 1;
        prevLine = getRealTokenPreLine(file, doc, prevLine);
        if (prevLine < 0) {
            return Result.Continue;
        }
        // current line text
        String curr = getLineText(doc, line);
        // previous line text
        String prev = getLineText(doc, prevLine);

        String text = doc.getText(new TextRange(currLineStartOffset, offset));
        if (text.contains("\t")) {
            int tabSize = CodeStyle.getIndentOptions(file.getProject(), doc).TAB_SIZE;
            int tabCount = StringUtil.countChars(text, '\t');
            text = text.replaceAll("\t", " ".repeat(tabSize));
            doc.replaceString(currLineStartOffset, offset, text);
            offset = currLineStartOffset + tabCount * tabSize;
            editor.getCaretModel().moveToOffset(offset);
        }

        if (UNINDENTED.matcher(curr).find()) {
            return Result.Continue;
        }

        // base indent: same as previous line indent
        int baseIndentPrev = countLeadingSpaces(prev, doc, file.getProject());
        int baseIndentCurr = countLeadingSpaces(curr, doc, file.getProject());
        int redundantSpaceNum = baseIndentCurr - (offset - currLineStartOffset);

        // enter inside empty ()、[]、{}
        if ((matchesExcludingStrings(prev, INCREASE)
                || !checkIsClose(prev, file, doc.getLineStartOffset(line - 1))) && DECREASE.matcher(curr).find()) {
            editor.getDocument().insertString(offset,
                    " ".repeat(baseIndentPrev + indentSize)
                            + SEPERATOR
                            + " ".repeat(Math.max(0, baseIndentPrev)));

            editor.getCaretModel().moveToOffset(offset + indentSize);
            return Result.Continue;
        }

        // e.g.  evt => xxxxx <enter at this position> }
        if (matchesExcludingStrings(prev, INDENT_NEXT) && DECREASE.matcher(curr).find()) {
            editor.getDocument().insertString(offset,
                    " ".repeat(baseIndentPrev + indentSize)
                            + SEPERATOR
                            + " ".repeat(Math.max(0, baseIndentPrev - indentSize)));
            editor.getCaretModel().moveToOffset(offset + indentSize);
            return Result.Continue;
        }

        PsiElement curEle = file.findElementAt(offset);
        if (curEle == null && doc.getTextLength() == offset) {
            // is in doc end position
            curEle = file.findElementAt(offset - 1);
        }
        // check top-level node
        if (checkTopLevelNode(editor, curEle, curr, line, offset)) {
            return Result.Continue;
        }

        // check increase
        int increaseIndent = checkCodeIdentIncrease(indentSize, new Pair<>(prev, curr), doc, curEle, line);
        // check decrease
        increaseIndent = increaseIndent - checkCodeIdentDecrease(indentSize, prev, curr, curEle);

        // insert indent
        increaseIndent = increaseIndent - redundantSpaceNum;
        if (increaseIndent >= 0) {
            editor.getDocument().insertString(offset, " ".repeat(increaseIndent));
            editor.getCaretModel().moveToOffset(Math.max(currLineStartOffset, offset + increaseIndent));
            return Result.Continue;
        }
        if (offset - currLineStartOffset >= 0 && offset - currLineStartOffset <= curr.length()
                && curr.substring(0, offset - currLineStartOffset).trim().isEmpty()) {
            // delete indent when current line string between start to offset is empty
            editor.getDocument().deleteString(Math.max(currLineStartOffset, offset + increaseIndent), offset);
        }
        // move to proper position
        editor.getCaretModel().moveToOffset(Math.max(currLineStartOffset, offset + increaseIndent));
        return Result.Continue;
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
}