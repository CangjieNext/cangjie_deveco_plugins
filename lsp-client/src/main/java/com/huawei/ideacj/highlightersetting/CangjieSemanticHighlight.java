/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.ideacj.highlightersetting;

import com.huawei.ideacj.language.psi.CangjieGetID;
import com.huawei.ideacj.language.psi.importnode.CjImportAlias;
import com.huawei.ideacj.language.psi.importnode.CjImportAll;
import com.huawei.ideacj.language.psi.importnode.CjImportAllAlias;
import com.huawei.ideacj.language.psi.importnode.CjImportList;
import com.huawei.ideacj.language.psi.importnode.CjImportMulti;
import com.huawei.ideacj.language.psi.importnode.CjImportSingle;
import com.huawei.ideacj.language.psi.importnode.CjImportSpecified;
import com.huawei.ideacj.language.psi.othersnode.CjAnnotation;
import com.huawei.ideacj.language.psi.othersnode.CjArrowParameters;
import com.huawei.ideacj.language.psi.othersnode.CjAtomicExpression;
import com.huawei.ideacj.language.psi.othersnode.CjConstTypeParameter;
import com.huawei.ideacj.language.psi.othersnode.CjDiffFunc;
import com.huawei.ideacj.language.psi.othersnode.CjEnumPattern;
import com.huawei.ideacj.language.psi.othersnode.CjExceptionTypePattern;
import com.huawei.ideacj.language.psi.othersnode.CjFieldAccess;
import com.huawei.ideacj.language.psi.othersnode.CjIdentifier;
import com.huawei.ideacj.language.psi.othersnode.CjItemAfterQuest;
import com.huawei.ideacj.language.psi.othersnode.CjLambdaParameter;
import com.huawei.ideacj.language.psi.othersnode.CjLeftAuxExpression;
import com.huawei.ideacj.language.psi.othersnode.CjLeftValueExpression;
import com.huawei.ideacj.language.psi.othersnode.CjPostfixExpression;
import com.huawei.ideacj.language.psi.othersnode.CjQuoteToken;
import com.huawei.ideacj.language.psi.othersnode.CjResourceSpecification;
import com.huawei.ideacj.language.psi.othersnode.CjSynchronizedExpression;
import com.huawei.ideacj.language.psi.othersnode.CjTupleType;
import com.huawei.ideacj.language.psi.othersnode.CjTypePattern;
import com.huawei.ideacj.language.psi.othersnode.CjValueArgument;
import com.huawei.ideacj.language.psi.othersnode.CjVarBindingPattern;
import com.huawei.ideacj.language.psi.packagenode.CjPackageNameIdentifier;
import com.huawei.ideacj.language.psi.toplevel.CjDefaultParameter;
import com.huawei.ideacj.language.psi.toplevel.CjGenericConstraints;
import com.huawei.ideacj.language.psi.toplevel.CjNamedParameter;
import com.huawei.ideacj.language.psi.toplevel.CjTypeAlias;
import com.huawei.ideacj.language.psi.toplevel.CjTypeParameters;
import com.huawei.ideacj.language.psi.toplevel.CjUnnamedParameter;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjAssociatedTypeDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjAssociatedTypeDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassName;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassNamedInitParam;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassType;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjClassUnnamedInitParam;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyMemberDeclaration;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjPropertyMemberVar;
import com.huawei.ideacj.language.psi.toplevel.classnode.CjUserType;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumCaseBody;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjNamedEnumConstructorParameter;
import com.huawei.ideacj.language.psi.toplevel.enumnode.CjUnnamedEnumConstructorParameter;
import com.huawei.ideacj.language.psi.toplevel.extendnode.CjExtendType;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjLambdaDefinition;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjLambdaParam;
import com.huawei.ideacj.language.psi.toplevel.functionnode.CjOverloadedOperators;
import com.huawei.ideacj.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroAttrDecl;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroAttrExpr;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroAttrType;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputDecl;
import com.huawei.ideacj.language.psi.toplevel.macronode.CjMacroInputType;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructName;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructNamedInitParam;
import com.huawei.ideacj.language.psi.toplevel.structnode.CjStructUnnamedInitParam;
import com.huawei.ideacj.lsp.utils.LSPThreadPoolManager;
import com.huawei.ideacj.trace.TraceUtils;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.client.languageserver.ServerStatus;
import org.wso2.lsp4intellij.contributors.semantic.ServerSemanticHighlight;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CangjieAnnotator
 *
 * @since 2022-05-05
 */
public class CangjieSemanticHighlight extends ServerSemanticHighlight {
    static HashMap<String, Class<? extends CangjieGetID>> listClass = new HashMap<>(Map.ofEntries(
            // CLASS
            Map.entry(CjClassDefinition.class.getName(), CjClassDefinition.class),
            Map.entry(CjStructDefinition.class.getName(), CjStructDefinition.class),
            Map.entry(CjInterfaceDefinition.class.getName(), CjInterfaceDefinition.class),
            Map.entry(CjExtendType.class.getName(), CjExtendType.class),
            Map.entry(CjAssociatedTypeDeclaration.class.getName(), CjAssociatedTypeDeclaration.class),
            Map.entry(CjEnumDefinition.class.getName(), CjEnumDefinition.class),
            Map.entry(CjClassType.class.getName(), CjClassType.class),
            Map.entry(CjTypeParameters.class.getName(), CjTypeParameters.class),
            Map.entry(CjConstTypeParameter.class.getName(), CjConstTypeParameter.class),
            Map.entry(CjGenericConstraints.class.getName(), CjGenericConstraints.class),
            Map.entry(CjMacroInputType.class.getName(), CjMacroInputType.class),
            Map.entry(CjTypeAlias.class.getName(), CjTypeAlias.class),
            Map.entry(CjAssociatedTypeDefinition.class.getName(), CjAssociatedTypeDefinition.class),
            Map.entry(CjMacroAttrType.class.getName(), CjMacroAttrType.class)
    ));

    static HashMap<String, Class<? extends CangjieGetID>> listFunc = new HashMap<>(Map.ofEntries(
            // FUNC
            Map.entry(CjOverloadedOperators.class.getName(), CjOverloadedOperators.class),
            Map.entry(CjMacroDefinition.class.getName(), CjMacroDefinition.class),
            Map.entry(CjFunctionDefinition.class.getName(), CjFunctionDefinition.class),
            Map.entry(CjPropertyMemberDeclaration.class.getName(), CjPropertyMemberDeclaration.class),
            Map.entry(CjClassName.class.getName(), CjClassName.class),
            Map.entry(CjStructName.class.getName(), CjStructName.class),
            Map.entry(CjDiffFunc.class.getName(), CjDiffFunc.class),
            Map.entry(CjLambdaDefinition.class.getName(), CjLambdaDefinition.class),
            Map.entry(CjLambdaParam.class.getName(), CjLambdaParam.class),
            Map.entry(CjAnnotation.class.getName(), CjAnnotation.class)
    ));

    static HashMap<String, Class<? extends CangjieGetID>> listImportOrPackage = new HashMap<>(Map.ofEntries(
            // PACKAGE
            Map.entry(CjPackageNameIdentifier.class.getName(), CjPackageNameIdentifier.class),
            Map.entry(CjImportAll.class.getName(), CjImportAll.class),
            Map.entry(CjImportAlias.class.getName(), CjImportAlias.class),
            Map.entry(CjImportAllAlias.class.getName(), CjImportAllAlias.class),
            Map.entry(CjImportSpecified.class.getName(), CjImportSpecified.class),
            Map.entry(CjImportList.class.getName(), CjImportList.class),
            Map.entry(CjImportMulti.class.getName(), CjImportMulti.class),
            Map.entry(CjImportSingle.class.getName(), CjImportSingle.class)
    ));

    static HashMap<String, Class<? extends CangjieGetID>> listVar = new HashMap<>(Map.ofEntries(
            // VAR
            Map.entry(CjStructUnnamedInitParam.class.getName(), CjStructUnnamedInitParam.class),
            Map.entry(CjExceptionTypePattern.class.getName(), CjExceptionTypePattern.class),
            Map.entry(CjLambdaParameter.class.getName(), CjLambdaParameter.class),
            Map.entry(CjLeftValueExpression.class.getName(), CjLeftValueExpression.class),
            Map.entry(CjResourceSpecification.class.getName(), CjResourceSpecification.class),
            Map.entry(CjSynchronizedExpression.class.getName(), CjSynchronizedExpression.class),
            Map.entry(CjTypePattern.class.getName(), CjTypePattern.class),
            Map.entry(CjValueArgument.class.getName(), CjValueArgument.class),
            Map.entry(CjUnnamedEnumConstructorParameter.class.getName(), CjUnnamedEnumConstructorParameter.class),
            Map.entry(CjNamedEnumConstructorParameter.class.getName(), CjNamedEnumConstructorParameter.class),
            Map.entry(CjArrowParameters.class.getName(), CjArrowParameters.class),
            Map.entry(CjVarBindingPattern.class.getName(), CjVarBindingPattern.class),
            Map.entry(CjClassUnnamedInitParam.class.getName(), CjClassUnnamedInitParam.class),
            Map.entry(CjClassNamedInitParam.class.getName(), CjClassNamedInitParam.class),
            Map.entry(CjNamedParameter.class.getName(), CjNamedParameter.class),
            Map.entry(CjDefaultParameter.class.getName(), CjDefaultParameter.class),
            Map.entry(CjUnnamedParameter.class.getName(), CjUnnamedParameter.class),
            Map.entry(CjPropertyDefinition.class.getName(), CjPropertyDefinition.class),
            Map.entry(CjStructNamedInitParam.class.getName(), CjStructNamedInitParam.class),
            Map.entry(CjTupleType.class.getName(), CjTupleType.class),
            Map.entry(CjMacroInputDecl.class.getName(), CjMacroInputDecl.class),
            Map.entry(CjMacroAttrDecl.class.getName(), CjMacroAttrDecl.class),
            Map.entry(CjLeftAuxExpression.class.getName(), CjLeftAuxExpression.class),
            Map.entry(CjPropertyMemberVar.class.getName(), CjPropertyMemberVar.class),
            Map.entry(CjMacroAttrExpr.class.getName(), CjMacroAttrExpr.class)
    ));

    static HashMap<String, Class<? extends CangjieGetID>> ambiguityList = new HashMap<>(Map.ofEntries(
            // ambiguity Class
            Map.entry(CjAtomicExpression.class.getName(), CjAtomicExpression.class),
            Map.entry(CjEnumPattern.class.getName(), CjEnumPattern.class),
            Map.entry(CjEnumCaseBody.class.getName(), CjEnumCaseBody.class),
            Map.entry(CjPostfixExpression.class.getName(), CjPostfixExpression.class),
            Map.entry(CjFieldAccess.class.getName(), CjFieldAccess.class),
            Map.entry(CjItemAfterQuest.class.getName(), CjItemAfterQuest.class),
            Map.entry(CjUserType.class.getName(), CjUserType.class),
            Map.entry(CjMacroExpression.class.getName(), CjMacroExpression.class),
            Map.entry(CjQuoteToken.class.getName(), CjQuoteToken.class)
    ));

    static HashMap<String, Class<? extends CangjieGetID>> verifyTypes = new HashMap<>(Map.ofEntries(
            Map.entry(CjClassDefinition.class.getName(), CjClassDefinition.class),
            Map.entry(CjStructDefinition.class.getName(), CjStructDefinition.class),
            Map.entry(CjInterfaceDefinition.class.getName(), CjInterfaceDefinition.class),
            Map.entry(CjTypeAlias.class.getName(), CjTypeAlias.class),
            Map.entry(CjEnumDefinition.class.getName(), CjEnumDefinition.class),
            Map.entry(CjFunctionDefinition.class.getName(), CjFunctionDefinition.class)
    ));

    private static Map<String, List<RangeHighlighter>> highlighterMap = new HashMap<>();
    private static final int TIME_SEMANTICTOKEN_WAIT = 100;

    List<PsiElement> highLightPsiArray = null;
    TextAttributesKey elementColor = null;
    Editor editor = null;

    private int traceCount = TraceUtils.ActionThreshold.SYNTAX_HIGHLIGHT;

    @Nullable
    private Class<? extends CangjieGetID> isContain(HashMap<String, Class<? extends CangjieGetID>> myList,
                                                    PsiElement element) {
        var matchClass = myList.get(element.getClass().getName());
        if (matchClass != null) {
            highLightPsiArray = matchClass.cast(element).getIDPsi(element);
        }
        return matchClass;
    }

    private boolean isInvalidType(PsiElement element) {
        if (isContain(verifyTypes, element) == null) {
            return false;
        }
        @NotNull PsiElement[] children = element.getChildren();
        return children.length == 3 && Arrays.stream(children)
                .anyMatch(child -> child instanceof CjIdentifier && child.getText().isEmpty());
    }

    /**
     * according to element set elementColor
     *
     * @param element PsiElement
     */
    private void setColor(PsiElement element) {
        highLightPsiArray = new ArrayList<>();
        if (isInvalidType(element)) {
            elementColor = CangjieSemanticTokenHighlighter.KEYWORD;
        } else if (isContain(listClass, element) != null) {
            elementColor = CangjieSemanticTokenHighlighter.CLASS;
        } else if (isContain(listImportOrPackage, element) != null) {
            elementColor = CangjieSemanticTokenHighlighter.EVENT;
        } else if (isContain(listFunc, element) != null) {
            elementColor = CangjieSemanticTokenHighlighter.FUNCTION;
        } else if (isContain(listVar, element) != null) {
            elementColor = CangjieSemanticTokenHighlighter.VARIABLE;
        } else if (isContain(ambiguityList, element) != null) {
            Class<? extends CangjieGetID> ambiguityClass = isContain(ambiguityList, element);
            elementColor = CangjieSemanticTokenHighlighter.NUMBER;
            if (ambiguityClass == null) {
                return;
            }
            var newElement = ambiguityClass.cast(element);
            if (newElement == null) {
                return;
            }
            elementColor = newElement.getColor();
        } else {
            elementColor = CangjieSemanticTokenHighlighter.NUMBER;
        }
    }

    private Pair<List<PsiElement>, TextAttributesKey> getElementColor(PsiElement element) {
        setColor(element);
        return new Pair<>(highLightPsiArray, elementColor);
    }

    @Nullable
    @Override
    public AnnotationData collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        ApplicationManager.getApplication().invokeLater(() -> {
            long startTime = System.currentTimeMillis();
            this.editor = editor;
            List<RangeHighlighter> highlighterList = highlighterMap
                    .get(FileUtils.pathToUri(file.getVirtualFile().getPath()));
            if (highlighterList == null) {
                highlighterList = new ArrayList<>();
                highlighterMap.put(FileUtils.pathToUri(file.getVirtualFile().getPath()), highlighterList);
            }
            MarkupModel markupModelEx = editor.getMarkupModel();
            markupModelEx.removeAllHighlighters();
            highlighterList.clear();
            buildHighlighter(file, editor, highlighterList);
            if (traceCount >= TraceUtils.ActionThreshold.SYNTAX_HIGHLIGHT) {
                TraceUtils.trace(TraceUtils.Action.SYNTAX_HIGHLIGHT, TraceUtils.Cause.DEFAULT,
                        -1, System.currentTimeMillis() - startTime);
                traceCount = 0;
            } else {
                traceCount++;
            }
        });
        return new AnnotationData(file, editor);
    }

    private void buildHighlighter(@NotNull PsiElement element, @NotNull Editor editor,
                                  List<RangeHighlighter> highlighterList) {
        var colorResult = getElementColor(element);
        if (!colorResult.first.isEmpty()) {
            TextAttributesKey textAttributesKey = colorResult.second;
            MarkupModel markupModelEx = editor.getMarkupModel();
            int textLength = editor.getDocument().getTextLength();
            for (PsiElement paintEle : colorResult.first) {
                int startOffset = paintEle.getTextRange().getStartOffset();
                int endOffset = paintEle.getTextRange().getEndOffset();
                if (endOffset > textLength || startOffset > endOffset) {
                    continue;
                }
                RangeHighlighter highlighter = markupModelEx.addRangeHighlighter(textAttributesKey, startOffset,
                        endOffset, HighlighterLayer.FIRST, HighlighterTargetArea.EXACT_RANGE);
                highlighterList.add(highlighter);
            }
        }
        PsiElement[] children = element.getChildren();
        for (PsiElement child : children) {
            buildHighlighter(child, editor, highlighterList);
        }
    }

    @Override
    public void apply(@NotNull PsiFile psiFile, @NotNull AnnotationData annotationData,
        @NotNull AnnotationHolder holder) {
        if (this.editor != null) {
            EditorEventManager manager = EditorEventManagerBase.forEditor(this.editor);
            if (manager != null) {
                manager.setAnonHolder(holder);
            }
        }
        super.apply(psiFile, annotationData, holder);
    }

    /**
     * get all highlighter from map by uri when document is highlighted
     *
     * @param uri Sting, uri of the current document
     * @return List<RangeHighlighter>, rangeHighlighters of the current document
     */
    public static List<RangeHighlighter> getAllRangHighlighter(String uri) {
        return highlighterMap.get(uri);
    }

    /**
     * remove highlighter list form map by uri when document is closed
     *
     * @param uri String, uri of the closed file
     */
    public static void removeHighlighter(String uri) {
        highlighterMap.remove(uri);
    }

    /**
     * toString override
     *
     * @return String
     */
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    /**
     * equals override
     *
     * @param obj compare object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CangjieSemanticHighlight that = (CangjieSemanticHighlight) obj;

        return new EqualsBuilder().append(highLightPsiArray, that.highLightPsiArray)
                .append(elementColor, that.elementColor).append(listClass, that.listClass)
                .append(listFunc, that.listFunc).append(listImportOrPackage, that.listImportOrPackage)
                .append(listVar, that.listVar).append(ambiguityList, that.ambiguityList).isEquals();
    }

    /**
     * hashCode override
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(highLightPsiArray).append(elementColor).append(listClass)
                .append(listFunc).append(listImportOrPackage).append(listVar)
                .append(ambiguityList).toHashCode();
    }

    @Nullable
    @Override
    public AnnotationData doAnnotate(AnnotationData annotationData) {
        Future<AnnotationData> future = LSPThreadPoolManager.pool(() -> super.doAnnotate(annotationData));
        while (true) {
            try {
                return future.get(TIME_SEMANTICTOKEN_WAIT, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
                if (manager == null || manager.wrapper == null) {
                    future.cancel(true);
                    return null;
                }
                if (!ServerStatus.INITIALIZED.equals(manager.wrapper.getStatus())) {
                    future.cancel(true);
                    return null;
                }
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }
    }
}
