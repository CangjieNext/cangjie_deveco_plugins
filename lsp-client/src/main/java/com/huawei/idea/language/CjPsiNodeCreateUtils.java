/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

package com.huawei.idea.language;

import com.huawei.idea.extend.cjpsi.adaptor.lexer.RuleIElementType;
import com.huawei.idea.extend.cjpsi.adaptor.psi.CJPsiNode;
import com.huawei.idea.language.psi.compileunitnode.CjPreamble;
import com.huawei.idea.language.psi.compileunitnode.CjTranslationUnit;
import com.huawei.idea.language.psi.importnode.CjImportAlias;
import com.huawei.idea.language.psi.importnode.CjImportAll;
import com.huawei.idea.language.psi.importnode.CjImportAllAlias;
import com.huawei.idea.language.psi.importnode.CjImportList;
import com.huawei.idea.language.psi.importnode.CjImportMulti;
import com.huawei.idea.language.psi.importnode.CjImportSingle;
import com.huawei.idea.language.psi.importnode.CjImportSpecified;
import com.huawei.idea.language.psi.operatornode.CJAdditiveOperator;
import com.huawei.idea.language.psi.operatornode.CJAssignmentOperator;
import com.huawei.idea.language.psi.operatornode.CJComparisonOperator;
import com.huawei.idea.language.psi.operatornode.CJConditionOperator;
import com.huawei.idea.language.psi.operatornode.CJEqualityOperator;
import com.huawei.idea.language.psi.operatornode.CJFlowOperator;
import com.huawei.idea.language.psi.operatornode.CJShiftingOperator;
import com.huawei.idea.language.psi.operatornode.CJMultiplicativeOperator;
import com.huawei.idea.language.psi.othersnode.CjAdCallSuffix;
import com.huawei.idea.language.psi.othersnode.CjArrowParameters;
import com.huawei.idea.language.psi.othersnode.CjAtomicExpression;
import com.huawei.idea.language.psi.othersnode.CjCallSuffix;
import com.huawei.idea.language.psi.othersnode.CjCharContent;
import com.huawei.idea.language.psi.othersnode.CjCollectionLiteral;
import com.huawei.idea.language.psi.othersnode.CjConstTypeParameter;
import com.huawei.idea.language.psi.othersnode.CjDiffFunc;
import com.huawei.idea.language.psi.othersnode.CjEnd;
import com.huawei.idea.language.psi.othersnode.CjEnumPattern;
import com.huawei.idea.language.psi.othersnode.CjExceptionTypePattern;
import com.huawei.idea.language.psi.othersnode.CjFieldAccess;
import com.huawei.idea.language.psi.othersnode.CjIdentifier;
import com.huawei.idea.language.psi.othersnode.CjIndexAccess;
import com.huawei.idea.language.psi.othersnode.CjItemAfterQuest;
import com.huawei.idea.language.psi.othersnode.CjLambdaExpression;
import com.huawei.idea.language.psi.othersnode.CjLambdaParameter;
import com.huawei.idea.language.psi.othersnode.CjLeftAuxExpression;
import com.huawei.idea.language.psi.othersnode.CjLeftValueExpression;
import com.huawei.idea.language.psi.othersnode.CjLineStringContent;
import com.huawei.idea.language.psi.othersnode.CjLineStringLiteral;
import com.huawei.idea.language.psi.othersnode.CjLiteralConstant;
import com.huawei.idea.language.psi.othersnode.CjModifier;
import com.huawei.idea.language.psi.othersnode.CjPostfixExpression;
import com.huawei.idea.language.psi.othersnode.CjQuoteToken;
import com.huawei.idea.language.psi.othersnode.CjResourceSpecification;
import com.huawei.idea.language.psi.othersnode.CjStringLiteral;
import com.huawei.idea.language.psi.othersnode.CjSynchronizedExpression;
import com.huawei.idea.language.psi.othersnode.CjTupleType;
import com.huawei.idea.language.psi.othersnode.CjTypePattern;
import com.huawei.idea.language.psi.othersnode.CjValueArgument;
import com.huawei.idea.language.psi.othersnode.CjVarBindingPattern;
import com.huawei.idea.language.psi.othersnode.CjAnnotation;
import com.huawei.idea.language.psi.packagenode.CjPackageHeader;
import com.huawei.idea.language.psi.packagenode.CjPackageNameIdentifier;
import com.huawei.idea.language.psi.toplevel.CjDefaultParameter;
import com.huawei.idea.language.psi.toplevel.CjGenericConstraints;
import com.huawei.idea.language.psi.toplevel.CjNamedParameter;
import com.huawei.idea.language.psi.toplevel.CjNamedParameterList;
import com.huawei.idea.language.psi.toplevel.CjTopLevelObject;
import com.huawei.idea.language.psi.toplevel.CjType;
import com.huawei.idea.language.psi.toplevel.CjTypeAlias;
import com.huawei.idea.language.psi.toplevel.CjTypeArguments;
import com.huawei.idea.language.psi.toplevel.CjTypeParameters;
import com.huawei.idea.language.psi.toplevel.CjUnnamedParameter;
import com.huawei.idea.language.psi.toplevel.CjUnnamedParameterList;
import com.huawei.idea.language.psi.toplevel.classnode.CjAssociatedTypeDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjAssociatedTypeDefinition;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassBody;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassDefinition;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassInit;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassModifierList;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassName;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassNamedInitParam;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassNamedInitParamList;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassPrimaryInit;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassPrimaryInitParamLists;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassType;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassUnnamedInitParam;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassUnnamedInitParamList;
import com.huawei.idea.language.psi.toplevel.classnode.CjForeignBody;
import com.huawei.idea.language.psi.toplevel.classnode.CjForeignMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjInterfaceType;
import com.huawei.idea.language.psi.toplevel.classnode.CjMatchExpression;
import com.huawei.idea.language.psi.toplevel.classnode.CjPropertyBody;
import com.huawei.idea.language.psi.toplevel.classnode.CjPropertyDefinition;
import com.huawei.idea.language.psi.toplevel.classnode.CjPropertyMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.classnode.CjPropertyMemberVar;
import com.huawei.idea.language.psi.toplevel.classnode.CjSuperClass;
import com.huawei.idea.language.psi.toplevel.classnode.CjSuperClassOrInterfaces;
import com.huawei.idea.language.psi.toplevel.classnode.CjSuperInterfaces;
import com.huawei.idea.language.psi.toplevel.classnode.CjUpperBounds;
import com.huawei.idea.language.psi.toplevel.classnode.CjUserType;
import com.huawei.idea.language.psi.toplevel.classnode.CjClassFinalizer;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumBody;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumCaseBody;
import com.huawei.idea.language.psi.toplevel.enumnode.CjEnumDefinition;
import com.huawei.idea.language.psi.toplevel.enumnode.CjNamedEnumConstructorParameter;
import com.huawei.idea.language.psi.toplevel.enumnode.CjUnnamedEnumConstructorParameter;
import com.huawei.idea.language.psi.toplevel.expressionnode.CjExpression;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendBody;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendDefinition;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.extendnode.CjExtendType;
import com.huawei.idea.language.psi.toplevel.functionnode.CjBlock;
import com.huawei.idea.language.psi.toplevel.functionnode.CjFunctionDefinition;
import com.huawei.idea.language.psi.toplevel.functionnode.CjFunctionModifierList;
import com.huawei.idea.language.psi.toplevel.functionnode.CjFunctionParameters;
import com.huawei.idea.language.psi.toplevel.functionnode.CjLambdaDefinition;
import com.huawei.idea.language.psi.toplevel.functionnode.CjLambdaParam;
import com.huawei.idea.language.psi.toplevel.functionnode.CjMainDefinition;
import com.huawei.idea.language.psi.toplevel.functionnode.CjNondefaultParameterList;
import com.huawei.idea.language.psi.toplevel.functionnode.CjOperatorFunctionDefinition;
import com.huawei.idea.language.psi.toplevel.functionnode.CjOverloadedOperators;
import com.huawei.idea.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclaration;
import com.huawei.idea.language.psi.toplevel.functionnode.blocknode.CjExpressionOrDeclarations;
import com.huawei.idea.language.psi.toplevel.functionnode.blocknode.CjVarOrFuncDeclaration;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceBody;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceDefinition;
import com.huawei.idea.language.psi.toplevel.interfacenode.CjInterfaceMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroAttrDecl;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroAttrExpr;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroAttrType;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroDefinition;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroExpression;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroInputDecl;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroInputExprWithParens;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroInputExprWithoutParens;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroInputType;
import com.huawei.idea.language.psi.toplevel.macronode.CjMacroTokens;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructBody;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructDefinition;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructInit;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructMemberDeclaration;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructName;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructNamedInitParam;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructPrimaryInit;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructUnnamedInitParam;
import com.huawei.idea.language.psi.toplevel.structnode.CjStructPrimaryInitParamLists;
import com.huawei.idea.language.psi.toplevel.variabledeclaration.CjVariableDeclaration;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

import cjgrammar.parser.CharParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils for create Psi node, Using singleton mode, table-driven
 *
 * @since 2020-07-31
 */
public class CjPsiNodeCreateUtils {
    /**
     * Instance for create PsiNode
     */
    public static final CjPsiNodeCreateUtils INSTANCE = new CjPsiNodeCreateUtils();

    private final Map<Integer, InitPsiNode> nodeMap = new HashMap<>();

    private CjPsiNodeCreateUtils() {
        // init all psi node according to CangJie Spec Chapter17.2 and g4
        initCompileNode();
        initPackageNode();
        initTopLevelNode();
        initOperatorNode();
        initOtherNode();
    }

    /**
     * Convert the AST tree converted by ANTLR into PSI node.
     *
     * @param node ASTNode
     * @return The custom node inherited from ANTLRPsiNode, generally the same name as the AST node
     */
    public PsiElement create(ASTNode node) {
        IElementType elType = node.getElementType();
        if (elType instanceof RuleIElementType) {
            RuleIElementType ruleElType = (RuleIElementType) elType;
            InitPsiNode method = nodeMap.get(ruleElType.getRuleIndex());
            if (method != null) {
                return method.init(node);
            }
        }
        return new CJPsiNode(node);
    }

    private interface InitPsiNode {
        /**
         * Init psi element.
         *
         * @param node the node
         * @return the psi element
         */
        PsiElement init(ASTNode node);
    }

    private void initCompileNode() {
        nodeMap.put(CharParser.RULE_translationUnit, CjTranslationUnit::new);
        nodeMap.put(CharParser.RULE_topLevelObject, CjTopLevelObject::new);
    }

    private void initPackageNode() {
        nodeMap.put(CharParser.RULE_preamble, CjPreamble::new);
        nodeMap.put(CharParser.RULE_packageHeader, CjPackageHeader::new);
        nodeMap.put(CharParser.RULE_packageNameIdentifier, CjPackageNameIdentifier::new);
        nodeMap.put(CharParser.RULE_importAll, CjImportAll::new);
        nodeMap.put(CharParser.RULE_importSpecified, CjImportSpecified::new);
        nodeMap.put(CharParser.RULE_importAlias, CjImportAlias::new);
        nodeMap.put(CharParser.RULE_importAllAlias, CjImportAllAlias::new);
        nodeMap.put(CharParser.RULE_importList, CjImportList::new);
        nodeMap.put(CharParser.RULE_importMulti, CjImportMulti::new);
        nodeMap.put(CharParser.RULE_importSingle, CjImportSingle::new);
    }

    private void initOperatorNode() {
        nodeMap.put(CharParser.RULE_additiveOperator, CJAdditiveOperator::new);
        nodeMap.put(CharParser.RULE_assignmentOperator, CJAssignmentOperator::new);
        nodeMap.put(CharParser.RULE_comparisonOperator, CJComparisonOperator::new);
        nodeMap.put(CharParser.RULE_conditionOperator, CJConditionOperator::new);
        nodeMap.put(CharParser.RULE_equalityOperator, CJEqualityOperator::new);
        nodeMap.put(CharParser.RULE_flowOperator, CJFlowOperator::new);
        nodeMap.put(CharParser.RULE_shiftingOperator, CJShiftingOperator::new);
        nodeMap.put(CharParser.RULE_multiplicativeOperator, CJMultiplicativeOperator::new);
    }

    private void initOtherNode() {
        nodeMap.put(CharParser.RULE_atomicExpression, CjAtomicExpression::new);
        nodeMap.put(CharParser.RULE_diffFunc, CjDiffFunc::new);
        nodeMap.put(CharParser.RULE_enumPattern, CjEnumPattern::new);
        nodeMap.put(CharParser.RULE_exceptionTypePattern, CjExceptionTypePattern::new);
        nodeMap.put(CharParser.RULE_lambdaParameter, CjLambdaParameter::new);
        nodeMap.put(CharParser.RULE_leftValueExpression, CjLeftValueExpression::new);
        nodeMap.put(CharParser.RULE_postfixExpression, CjPostfixExpression::new);
        nodeMap.put(CharParser.RULE_callSuffix, CjCallSuffix::new);
        nodeMap.put(CharParser.RULE_adCallSuffix, CjAdCallSuffix::new);
        nodeMap.put(CharParser.RULE_collectionLiteral, CjCollectionLiteral::new);
        nodeMap.put(CharParser.RULE_resourceSpecification, CjResourceSpecification::new);
        nodeMap.put(CharParser.RULE_synchronizedExpression, CjSynchronizedExpression::new);
        nodeMap.put(CharParser.RULE_typePattern, CjTypePattern::new);
        nodeMap.put(CharParser.RULE_valueArgument, CjValueArgument::new);
        nodeMap.put(CharParser.RULE_constTypeParameter, CjConstTypeParameter::new);
        nodeMap.put(CharParser.RULE_unnamedEnumConstructorParameter, CjUnnamedEnumConstructorParameter::new);
        nodeMap.put(CharParser.RULE_namedEnumConstructorParameter, CjNamedEnumConstructorParameter::new);
        nodeMap.put(CharParser.RULE_arrowParameters, CjArrowParameters::new);
        nodeMap.put(CharParser.RULE_overloadedOperators, CjOverloadedOperators::new);
        nodeMap.put(CharParser.RULE_tupleType, CjTupleType::new);
        nodeMap.put(CharParser.RULE_userType, CjUserType::new);
        nodeMap.put(CharParser.RULE_leftAuxExpression, CjLeftAuxExpression::new);
        nodeMap.put(CharParser.RULE_fieldAccess, CjFieldAccess::new);
        nodeMap.put(CharParser.RULE_indexAccess, CjIndexAccess::new);
        nodeMap.put(CharParser.RULE_itemAfterQuest, CjItemAfterQuest::new);
        nodeMap.put(CharParser.RULE_lambdaExpression, CjLambdaExpression::new);
        nodeMap.put(CharParser.RULE_varBindingPattern, CjVarBindingPattern::new);
        nodeMap.put(CharParser.RULE_quoteToken, CjQuoteToken::new);
        nodeMap.put(CharParser.RULE_identifier, CjIdentifier::new);
        nodeMap.put(CharParser.RULE_modifier, CjModifier::new);
        nodeMap.put(CharParser.RULE_literalConstant, CjLiteralConstant::new);
        nodeMap.put(CharParser.RULE_stringLiteral, CjStringLiteral::new);
        nodeMap.put(CharParser.RULE_lineStringLiteral, CjLineStringLiteral::new);
        nodeMap.put(CharParser.RULE_lineStringContent, CjLineStringContent::new);
        nodeMap.put(CharParser.RULE_charContent, CjCharContent::new);
        nodeMap.put(CharParser.RULE_annotation, CjAnnotation::new);
        nodeMap.put(CharParser.RULE_end, CjEnd::new);
    }

    private void initTopLevelNode() {
        initClassNode();
        initFunctionNode();
        initVariableDeclarationNode();
        initEnumNode();
        initStructNode();
        initExtendNode();
        initInterfaceNode();
        initMacroNode();
    }

    private void initClassNode() {
        // classDefinition
        nodeMap.put(CharParser.RULE_classDefinition, CjClassDefinition::new);
        nodeMap.put(CharParser.RULE_upperBounds, CjUpperBounds::new);
        nodeMap.put(CharParser.RULE_superClassOrInterfaces, CjSuperClassOrInterfaces::new);
        nodeMap.put(CharParser.RULE_classBody, CjClassBody::new);
        // superClassOrInterfaces
        nodeMap.put(CharParser.RULE_superClass, CjSuperClass::new);
        nodeMap.put(CharParser.RULE_superInterfaces, CjSuperInterfaces::new);
        // classModifierList
        nodeMap.put(CharParser.RULE_classModifierList, CjClassModifierList::new);
        // typeParameters
        nodeMap.put(CharParser.RULE_typeParameters, CjTypeParameters::new);
        // superClass
        nodeMap.put(CharParser.RULE_classType, CjClassType::new);
        // classType
        nodeMap.put(CharParser.RULE_typeArguments, CjTypeArguments::new);
        // typeArguments
        nodeMap.put(CharParser.RULE_type, CjType::new);
        // superInterfaces interfaceType
        nodeMap.put(CharParser.RULE_interfaceType, CjInterfaceType::new);
        // genericConstraints
        nodeMap.put(CharParser.RULE_genericConstraints, CjGenericConstraints::new);
        // upperBounds
        nodeMap.put(CharParser.RULE_userType, CjUserType::new);
        // classBody
        nodeMap.put(CharParser.RULE_classMemberDeclaration, CjClassMemberDeclaration::new);
        nodeMap.put(CharParser.RULE_classPrimaryInit, CjClassPrimaryInit::new);
        // classMemberDeclaration
        nodeMap.put(CharParser.RULE_classInit, CjClassInit::new);
        nodeMap.put(CharParser.RULE_propertyDefinition, CjPropertyDefinition::new);
        nodeMap.put(CharParser.RULE_propertyMemberDeclaration, CjPropertyMemberDeclaration::new);
        nodeMap.put(CharParser.RULE_propertyBody, CjPropertyBody::new);
        nodeMap.put(CharParser.RULE_foreignBody, CjForeignBody::new);
        nodeMap.put(CharParser.RULE_foreignMemberDeclaration, CjForeignMemberDeclaration::new);
        nodeMap.put(CharParser.RULE_matchExpression, CjMatchExpression::new);
        nodeMap.put(CharParser.RULE_propertyMemberVar, CjPropertyMemberVar::new);
        // classInit classPrimaryInit className
        nodeMap.put(CharParser.RULE_className, CjClassName::new);
        // classPrimaryInitParamLists
        nodeMap.put(CharParser.RULE_classPrimaryInitParamLists, CjClassPrimaryInitParamLists::new);
        nodeMap.put(CharParser.RULE_unnamedParameterList, CjUnnamedParameterList::new);
        nodeMap.put(CharParser.RULE_namedParameterList, CjNamedParameterList::new);
        // classUnnamedInitParamList
        nodeMap.put(CharParser.RULE_classUnnamedInitParamList, CjClassUnnamedInitParamList::new);
        // classNamedInitParamList
        nodeMap.put(CharParser.RULE_classNamedInitParamList, CjClassNamedInitParamList::new);
        nodeMap.put(CharParser.RULE_classNamedInitParam, CjClassNamedInitParam::new);
        // classUnnamedInitParam
        nodeMap.put(CharParser.RULE_classUnnamedInitParam, CjClassUnnamedInitParam::new);
        // classNamedInitParam classNonStaticMemberModifier associatedTypeDefinition
        nodeMap.put(CharParser.RULE_associatedTypeDeclaration, CjAssociatedTypeDeclaration::new);
        nodeMap.put(CharParser.RULE_associatedTypeDefinition, CjAssociatedTypeDefinition::new);
        nodeMap.put(CharParser.RULE_typeAlias, CjTypeAlias::new);
        nodeMap.put(CharParser.RULE_classFinalizer, CjClassFinalizer::new);
    }

    private void initFunctionNode() {
        // functionDefinition
        nodeMap.put(CharParser.RULE_functionDefinition, CjFunctionDefinition::new);
        nodeMap.put(CharParser.RULE_functionModifierList, CjFunctionModifierList::new);
        nodeMap.put(CharParser.RULE_functionParameters, CjFunctionParameters::new);
        nodeMap.put(CharParser.RULE_block, CjBlock::new);

        // main
        nodeMap.put(CharParser.RULE_mainDefinition, CjMainDefinition::new);

        // lamdaDefinition
        nodeMap.put(CharParser.RULE_lamdaDefinition, CjLambdaDefinition::new);
        nodeMap.put(CharParser.RULE_lamdaParam, CjLambdaParam::new);

        // block
        nodeMap.put(CharParser.RULE_expressionOrDeclarations, CjExpressionOrDeclarations::new);
        nodeMap.put(CharParser.RULE_expressionOrDeclaration, CjExpressionOrDeclaration::new);
        nodeMap.put(CharParser.RULE_varOrfuncDeclaration, CjVarOrFuncDeclaration::new);

        // operatorFunctionDefinition
        nodeMap.put(CharParser.RULE_operatorFunctionDefinition, CjOperatorFunctionDefinition::new);
        nodeMap.put(CharParser.RULE_overloadedOperators, CjOverloadedOperators::new);

        // functionParameters nondefaultParameterList unnamedParameterList unnamedParameter namedParameterList
        nodeMap.put(CharParser.RULE_nondefaultParameterList, CjNondefaultParameterList::new);
        nodeMap.put(CharParser.RULE_unnamedParameter, CjUnnamedParameter::new);
        nodeMap.put(CharParser.RULE_namedParameter, CjNamedParameter::new);
        nodeMap.put(CharParser.RULE_defaultParameter, CjDefaultParameter::new);

        // namedParameter defaultParameter functionModifierList
        nodeMap.put(CharParser.RULE_expression, CjExpression::new);
    }

    private void initVariableDeclarationNode() {
        nodeMap.put(CharParser.RULE_variableDeclaration, CjVariableDeclaration::new);
    }

    private void initMacroNode() {
        nodeMap.put(CharParser.RULE_macroAttrDecl, CjMacroAttrDecl::new);
        nodeMap.put(CharParser.RULE_macroDefinition, CjMacroDefinition::new);
        nodeMap.put(CharParser.RULE_macroInputDecl, CjMacroInputDecl::new);
        nodeMap.put(CharParser.RULE_macroInputType, CjMacroInputType::new);
        nodeMap.put(CharParser.RULE_macroAttrType, CjMacroAttrType::new);
        nodeMap.put(CharParser.RULE_macroAttrExpr, CjMacroAttrExpr::new);
        nodeMap.put(CharParser.RULE_macroExpression, CjMacroExpression::new);
        nodeMap.put(CharParser.RULE_macroInputExprWithParens, CjMacroInputExprWithParens::new);
        nodeMap.put(CharParser.RULE_macroInputExprWithoutParens, CjMacroInputExprWithoutParens::new);
        nodeMap.put(CharParser.RULE_macroTokens, CjMacroTokens::new);
    }

    private void initEnumNode() {
        nodeMap.put(CharParser.RULE_enumDefinition, CjEnumDefinition::new);
        nodeMap.put(CharParser.RULE_enumBody, CjEnumBody::new);
        nodeMap.put(CharParser.RULE_caseBody, CjEnumCaseBody::new);
    }

    private void initStructNode() {
        nodeMap.put(CharParser.RULE_structDefinition, CjStructDefinition::new);
        nodeMap.put(CharParser.RULE_structBody, CjStructBody::new);
        nodeMap.put(CharParser.RULE_structPrimaryInit, CjStructPrimaryInit::new);
        nodeMap.put(CharParser.RULE_structInit, CjStructInit::new);
        nodeMap.put(CharParser.RULE_structMemberDeclaration, CjStructMemberDeclaration::new);
        nodeMap.put(CharParser.RULE_structName, CjStructName::new);
        nodeMap.put(CharParser.RULE_structUnnamedInitParam, CjStructUnnamedInitParam::new);
        nodeMap.put(CharParser.RULE_structNamedInitParam, CjStructNamedInitParam::new);
        nodeMap.put(CharParser.RULE_structPrimaryInitParamLists, CjStructPrimaryInitParamLists::new);
    }

    private void initExtendNode() {
        nodeMap.put(CharParser.RULE_extendDefinition, CjExtendDefinition::new);
        nodeMap.put(CharParser.RULE_extendType, CjExtendType::new);
        nodeMap.put(CharParser.RULE_extendBody, CjExtendBody::new);
        nodeMap.put(CharParser.RULE_extendMemberDeclaration, CjExtendMemberDeclaration::new);
    }

    private void initInterfaceNode() {
        nodeMap.put(CharParser.RULE_interfaceDefinition, CjInterfaceDefinition::new);
        nodeMap.put(CharParser.RULE_interfaceBody, CjInterfaceBody::new);
        nodeMap.put(CharParser.RULE_interfaceMemberDeclaration, CjInterfaceMemberDeclaration::new);
    }
}
