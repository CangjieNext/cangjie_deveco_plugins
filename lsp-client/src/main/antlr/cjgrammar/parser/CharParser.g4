/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

parser grammar CharParser;

options { tokenVocab = CharLexer; }

// Syntactic Grammar

// Translation Unit

translationUnit
    : preamble NL* (((EditorfoldStart | EditorfoldEnd) NL*)* (mainDefinition|topLevelObject) end* ((EditorfoldStart | EditorfoldEnd) NL*)*)* NL* EOF
    ;

end: NL | SEMI;

// Package Definition and Package Import

preamble
    : NL* packageHeader? (((EditorfoldStart | EditorfoldEnd) NL*)* importList (EditorfoldEnd NL*)*)*
    ;

packageHeader
    : (MACRO)? (packageModifier)? NL* PACKAGE NL* packageNameIdentifier end*
    ;

packageNameIdentifier
    : identifier (DOT identifier)*
    ;

packageModifier
    : PUBLIC
    | PROTECTED
    | INTERNAL
    | PRIVATE
    ;

importList
    : (packageModifier)? NL* IMPORT NL* importAllOrSpecified(NL* COMMA NL* importAllOrSpecified)* end+
    ;

importAllOrSpecified
    : importAll
    | importSpecified
    | importMulti
    | importSingle
    ;

importSingle
    : identifier (NL* importAllAlias)?
    ;

importMulti
    : (identifier NL* DOT NL*)* LCURL NL* (importSingle | importSpecified | importAll) (NL* COMMA NL* (importSingle | importSpecified | importAll))* NL* RCURL
    ;

importSpecified
    : (identifier NL* DOT NL*)+ identifier (NL* importAlias)?
    ;

importAll
    : (identifier NL* DOT NL*)+ MUL (NL* importAllAlias)?
    ;

importAllAlias
    : AS NL* identifier NL* DOT NL* MUL
    ;

importAlias
    : AS NL* identifier
    ;

// Top-Level Definitions

topLevelObject
    : classDefinition
    | interfaceDefinition
    | varOrfuncDeclaration
    | enumDefinition
    | structDefinition
    | typeAlias
    | extendDefinition
    | foreignDeclaration
    | macroDefinition
    | expression
    ;

// Class Definitions

classDefinition
    : (classModifierList NL*)? CLASS NL* identifier
    (NL* typeParameters NL*)?
    (NL* UPPERBOUND NL* superClassOrInterfaces)?
    (NL* genericConstraints)?
    (NL* classBody)
    ;

superClassOrInterfaces
    : superClass (NL* BITAND NL* superInterfaces)?
    | superInterfaces
    ;

classModifierList
    : (modifier NL*)+
    ;

typeParameters
    : LT NL* (identifier|constTypeParameter|charLangTypes|expression) (NL* COMMA NL* (identifier|constTypeParameter|charLangTypes|expression))* NL* GT
    ;

constTypeParameter
    : identifier COLON type
    ;

superClass
    : classType
    ;

classType
    : (identifier QUEST? NL* DOT NL*)*  identifier (NL* typeParameters)?
    ;

typeArguments
    : LT NL* (type|constTypeArguments) (NL* COMMA NL* (type|constTypeArguments))* NL* GT
    | (LT NL* (type|constTypeArguments) NL*)+ rShift+ NL* GT*
    ;

constTypeArguments
    : DollarIdentifier
    | DOLLAR LPAREN expression RPAREN
    | DOLLAR literalConstant
    ;

superInterfaces
    : interfaceType (NL* BITAND NL* interfaceType)*
    ;

interfaceType
    : classType
    ;

genericConstraints
    : WHERE NL* ((identifier | THISTYPE) NL* UPPERBOUND NL* upperBounds | expression)
    (NL* COMMA NL* ((identifier | THISTYPE) NL* UPPERBOUND NL* upperBounds | expression))*
    ;

upperBounds
    : userType (NL* BITAND NL* userType)*
    ;

// for lsp
classBody
    : LCURL (end | (EditorfoldStart | EditorfoldEnd) NL*)*
         (classMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)*
      RCURL
    ;

//for lsp
classMemberDeclaration
    : (classPrimaryInit
    | classInit
    | variableDeclaration
    | functionDefinition
    | operatorFunctionDefinition
    | associatedTypeDefinition
    | propertyDefinition
    | classFinalizer
    | expression
    ) end*
    ;

classInit
    : (modifier+ NL*)? INIT NL* functionParameters NL* block
    ;

classPrimaryInit
    : (modifier+ NL*)?  className NL*  classPrimaryInitParamLists NL* block
    ;

className
    : identifier
    ;

classPrimaryInitParamLists
    : functionParameters
    | LPAREN unnamedParameterList (NL* COMMA NL* namedParameterList | classUnnamedInitParamList)? (NL* COMMA NL* classNamedInitParamList)? NL* RPAREN
    | LPAREN classUnnamedInitParamList (NL* COMMA NL* classNamedInitParamList)? NL* RPAREN
    | LPAREN namedParameterList (NL* COMMA NL* classNamedInitParamList)? NL* RPAREN
    | LPAREN classNamedInitParamList NL* RPAREN
    ;

classUnnamedInitParamList
    : classUnnamedInitParam (NL* COMMA NL* classUnnamedInitParam)*
    ;

classNamedInitParamList
    : classNamedInitParam (NL* COMMA NL* classNamedInitParam)*
    ;

classUnnamedInitParam
    : (modifier+ NL*)? (LET | VAR) NL* identifier NL* COLON NL* type
    ;

classNamedInitParam
    : (modifier+ NL*)? (LET | VAR) NL* identifier NL* NOT NL* COLON NL* type (NL* ASSIGN NL* expression)?
    ;

classFinalizer
    : TILDE INIT NL* LPAREN NL* RPAREN NL* block
    ;

associatedTypeDefinition
    : TYPE_ALIAS NL* identifier NL* ASSIGN NL* type NL*
    ;

// Interface Definitions

interfaceDefinition
    : (interfaceModifierList NL*)? INTERFACE NL* identifier
    (NL* typeParameters NL*)?
    (NL* UPPERBOUND NL* superInterfaces)?
    (NL* genericConstraints)?
    (NL* interfaceBody)
    ;

interfaceBody
    : LCURL (end | (EditorfoldStart | EditorfoldEnd) NL*)* (interfaceMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)* RCURL
    ;

interfaceMemberDeclaration
         : (functionDefinition
         | operatorFunctionDefinition
         | associatedTypeDeclaration
         | propertyDefinition
         | expression
         ) end*
         ;

interfaceModifierList
    : (modifier NL*)+
    ;

associatedTypeDeclaration
    : TYPE_ALIAS NL* identifier
    ;

// Function Definitions

functionDefinition
    :(functionModifierList NL*)? FUNC
    NL* identifier
    (NL* typeParameters)?
    NL* functionParameters
    (NL* COLON NL* type)?
    (NL* genericConstraints)?
    (NL* block)?
    ;

operatorFunctionDefinition
    : (functionModifierList NL*)? OPERATOR NL* (CONST | OVERRIDE | CONST OVERRIDE | OVERRIDE CONST)? NL* FUNC
    NL* overloadedOperators
    (NL* typeParameters)?
    NL* functionParameters
    (NL* COLON NL* type)?
    (NL* genericConstraints)?
    (NL* block)?
    ;

functionParameters
    : (LPAREN ((NL* namedParameterList NL*)? | (NL* unnamedParameterList (NL* COMMA NL* namedParameterList)?)) NL* RPAREN) NL* (LPAREN NL* nondefaultParameterList? NL* RPAREN NL*)*
    ;

nondefaultParameterList
    : unnamedParameter (NL* COMMA NL* unnamedParameter)* (NL* COMMA NL* namedParameter)*
    | namedParameter (NL* COMMA NL* namedParameter)*
    ;

namedParameterList
    : (namedParameter | defaultParameter | macroExpression) (NL* COMMA NL* (namedParameter | defaultParameter | macroExpression))*
    ;

namedParameter
    : annotationList? NL* identifier NL* NOT NL* COLON NL* type
    ;

defaultParameter
    : annotationList? NL* identifier NL* NOT NL* COLON NL* type NL* ASSIGN NL* expression
    ;

unnamedParameterList
    : unnamedParameter (NL* COMMA NL* unnamedParameter)*
    ;

unnamedParameter
    : annotationList? NL* modifier? NL* (LET | VAR | CONST)? (identifier | WILDCARD) NL* (NOT)?COLON NL* type (NL* ASSIGN NL* expression)?
    ;

functionModifierList
    : (modifier NL*)+
    ;

// Variable Definitions

variableDeclaration
    : modifier* (LET | VAR | CONST) patternsMaybeIrrefutable
    ( (NL* COLON NL* type)? (NL* ASSIGN NL* (loopExpression|expression)) | (NL* COLON NL* type))
    ;

mainDefinition
    : (MAIN
      NL* functionParameters
      (NL* COLON NL* type)?
      NL* block)
    ;

// Enum Definitions

enumDefinition
    : (modifier+ NL*)? ENUM NL* identifier (NL* typeParameters NL*)?
     (NL* UPPERBOUND NL* superInterfaces)?
     (NL* genericConstraints)? NL*  enumBody
    ;

enumBody
    : LCURL ((EditorfoldStart | EditorfoldEnd) NL*)* (BITOR NL*)? (caseBody end*)? (NL* end* BITOR NL* caseBody end*)* (NL* BITOR NL* ELLIPSIS)?
    (NL* (functionDefinition | operatorFunctionDefinition | associatedTypeDefinition | propertyDefinition | expression) end* ((EditorfoldStart | EditorfoldEnd) NL*)*)*
    NL* end* RCURL
    ;

caseBody
    : annotationList? NL* identifier NL* LPAREN NL* unnamedEnumConstructorParameter
    (NL* COMMA NL* unnamedEnumConstructorParameter)* (NL* COMMA NL* namedEnumConstructorParameter)* NL* RPAREN
    | annotationList? NL* identifier NL* LPAREN NL* namedEnumConstructorParameter (NL* COMMA NL* namedEnumConstructorParameter)* NL* RPAREN
    | annotationList? NL* identifier
    ;

namedEnumConstructorParameter
    : identifier NL* NOT NL* COLON NL* type
    ;

unnamedEnumConstructorParameter
    : (identifier NL* COLON NL*)? type
    ;

// Struct Definitions

structDefinition
    : (modifier+ NL*)? STRUCT NL* identifier (NL* typeParameters)?
     (NL* UPPERBOUND NL* superInterfaces)?
     (NL* genericConstraints)? NL* structBody
    ;

structBody
    : LCURL (end | (EditorfoldStart | EditorfoldEnd) NL*)*
        (structMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)*
      RCURL
    ;

structMemberDeclaration
    : (structInit
    | structPrimaryInit
    | variableDeclaration
    | functionDefinition
    | operatorFunctionDefinition
    | associatedTypeDefinition
    | propertyDefinition
    | expression
    ) end*
    ;

structInit
    : (modifier+ NL*)? INIT NL* functionParameters NL* block
    ;

structPrimaryInit
    : (modifier+ NL*)?  structName NL*  structPrimaryInitParamLists NL* block
    ;

structName
    : identifier
    ;

structPrimaryInitParamLists
    : functionParameters
    | LPAREN unnamedParameterList (NL* COMMA NL* namedParameterList | structUnnamedInitParamList)? (NL* COMMA NL* structNamedInitParamList)? NL* RPAREN
    | LPAREN structUnnamedInitParamList (NL* COMMA NL* structNamedInitParamList)? NL* RPAREN
    | LPAREN namedParameterList (NL* COMMA NL* structNamedInitParamList)? NL* RPAREN
    | LPAREN structNamedInitParamList NL* RPAREN
    ;

structUnnamedInitParamList
     : structUnnamedInitParam (NL* COMMA NL*  structUnnamedInitParam)*
     ;

structNamedInitParamList
     : structNamedInitParam (NL* COMMA NL*  structNamedInitParam)*
     ;

structUnnamedInitParam
     : (modifier+ NL*)? (LET | VAR) NL* identifier NL* COLON NL* type
     ;

structNamedInitParam
     : (modifier+ NL*)? (LET | VAR) NL* identifier NL* NOT NL* COLON NL* type (NL* ASSIGN NL* expression)?
     ;

// Typealias Definitions

typeAlias
    : (modifier+ NL*)? TYPE_ALIAS NL* identifier (NL* typeParameters)? NL* ASSIGN NL* type
    ;

// Extension Definitions

extendDefinition
    : EXTEND NL* typeArguments NL* extendType
    (NL* UPPERBOUND NL* superInterfaces)?
    (NL* genericConstraints)? NL* extendBody
    ;

extendType
    :  (identifier QUEST? NL* DOT  NL*)*  identifier (NL* typeParameters)?
    | LPAREN NL* (identifier NL* COLON NL* identifier (NL* COMMA NL* identifier NL* COLON NL* identifier)* NL*)? RPAREN NL* ARROW NL* identifier
    | LPAREN NL* (identifier (NL* COMMA NL* identifier)* NL*)? RPAREN NL* ARROW NL* identifier
    | LPAREN NL* identifier NL* COLON NL* identifier (NL* COMMA NL* identifier NL* COLON NL* identifier)+ NL* RPAREN
    | LPAREN NL* identifier (NL* COMMA NL* identifier)+ NL* RPAREN
    | INT8
    | INT16
    | INT32
    | INT64
    | INTNATIVE
    | UINT8
    | UINT16
    | UINT32
    | UINT64
    | UINTNATIVE
    | FLOAT16
    | FLOAT32
    | FLOAT64
    | RUNE
    | BOOLEAN
    | NOTHING
    | UNIT
    ;

extendBody
    : LCURL (end | (EditorfoldStart | EditorfoldEnd) NL*)* (extendMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)* RCURL
    ;

extendMemberDeclaration
    : (functionDefinition
    | operatorFunctionDefinition
    | typeAlias
    | propertyDefinition
    | expression
    ) end*
    ;

// Foreign Declarations

foreignDeclaration
    : FOREIGN NL* (foreignBody | foreignMemberDeclaration)
    ;

foreignBody
    : LCURL (end | (EditorfoldStart | EditorfoldEnd) NL*)* (foreignMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)* RCURL
    ;

foreignMemberDeclaration
    : (classDefinition
    | interfaceDefinition
    | functionDefinition
    | variableDeclaration
    | expression) end*
    ;

// Macro Declarations

macroDefinition
    : annotationList? NL* PUBLIC NL* MACRO NL* identifier NL*
    (macroWithoutAttrParam | macroWithAttrParam) NL*
    (COLON NL* userType NL*)?
    block
    ;

macroWithoutAttrParam
    : LPAREN NL* macroInputDecl NL* RPAREN
    ;

macroWithAttrParam
    : LPAREN NL* macroAttrDecl NL* COMMA NL* macroInputDecl NL* RPAREN
    ;

macroInputDecl
    : identifier NL* COLON NL* macroInputType
    ;

macroInputType
    : identifier
    ;

macroAttrDecl
    : identifier NL* COLON NL* macroAttrType
    ;

macroAttrType
    : identifier
    ;

// Properties Definitions

propertyDefinition
    : modifier* NL* PROP NL* identifier NL* COLON NL* type (NL* propertyBody | end+)
    | modifier* NL* MUT PROP NL* identifier NL* COLON NL* type (NL* propertyBody | end+)
    ;

propertyBody
    : LCURL ((EditorfoldStart | EditorfoldEnd) NL*)* end* (propertyMemberDeclaration ((EditorfoldStart | EditorfoldEnd) NL*)*)+ end* RCURL
    ;

propertyMemberDeclaration
    : identifier NL* LPAREN propertyMemberVar? RPAREN NL* block end+
    ;

propertyMemberVar
    :identifier;

// Types

type
    : arrowType
    | tupleType
    | prefixType
    | atomicType
    | varrayType
    ;

varrayType
    : VARRAY LT type COMMA DOLLAR IntegerLiteral GT
    ;

arrowType
    : arrowParameters NL* ARROW NL* type
    ;

arrowParameters
    : LPAREN NL* (type (NL* COMMA NL* type)* NL*)? RPAREN
    | LPAREN NL* (identifier NL* COLON NL* type (NL* COMMA NL* identifier NL* COLON NL* type)* NL*)? RPAREN
    ;

tupleType
    : LPAREN NL* type (NL* COMMA NL* type)+ NL* RPAREN
    | LPAREN NL* identifier NL* COLON NL* type (NL* COMMA NL* identifier NL* COLON NL* type)+ NL* RPAREN
    ;

prefixType
    : prefixTypeOperator type
    ;

prefixTypeOperator
    : QUEST
    ;

atomicType
    : charLangTypes
    | userType
    | parenthesizedType
    ;

charLangTypes
    : INT8
    | INT16
    | INT32
    | INT64
    | INTNATIVE
    | UINT8
    | UINT16
    | UINT32
    | UINT64
    | UINTNATIVE
    | FLOAT16
    | FLOAT32
    | FLOAT64
    | RUNE
    | BOOLEAN
    | NOTHING
    | UNIT
    | THISTYPE
    ;

userType
    : (identifier QUEST? NL* DOT NL*)* identifier (typeArguments)?
    ;

parenthesizedType
    : LPAREN NL* type NL* RPAREN
    ;

// Expressions Syntax


//for lsp
expression
    : leftValueExpression NL* assignmentOperator NL* expression // 可以写 a = b = c 而且是右结合
    | tupleLeftValueExpression NL* ASSIGN NL* expression
    | LET NL* deconstructPattern NL* LT SUB NL* expression
    | operatorOperand expressionSuffix*
    ;

expressionSuffix
    : NL* (AS | IS) NL* type
    | NL* shiftingOperator NL* operatorOperand
    | NL* comparisonOperator NL* operatorOperand
    | NL* equalityOperator NL* operatorOperand
    | NL* conditionOperator NL* operatorOperand
    | NL* (CLOSEDRANGEOP | RANGEOP) NL* operatorOperand (NL* COLON NL* operatorOperand)?
    | NL* QUEST QUEST NL* operatorOperand
    | NL* flowOperator NL* operatorOperand
    | NL* assignmentOperator NL* expression
    ;

operatorOperand
    : additiveExpression
    | multiplicativeExpression
    | exponentExpression
    | unaryPostfixExpression
    ;

additiveExpression
    : multiplicativeOperand (NL* additiveOperator NL* multiplicativeOperand)*
    ;

multiplicativeOperand
    : multiplicativeExpression
    | exponentExpression
    | unaryPostfixExpression
    ;

multiplicativeExpression
    : exponentOperand (NL* multiplicativeOperator NL* exponentOperand)+
    ;

exponentOperand
    : exponentExpression
    | unaryPostfixExpression
    ;

exponentExpression
    : unaryPostfixExpression (NL* exponentOperator NL* unaryPostfixExpression)+
    ;

unaryPostfixExpression
    : prefixUnaryOperator* postfixExpression (INC | DEC)?
    ;

lamdaDefinition
    : identifier callSuffix NL* lambdaExpression (QUEST? NL* DOT NL* lamdaParam)*
    | identifier callSuffix (QUEST? NL* DOT NL* lamdaParam)*
    | identifier  NL* lambdaExpression (QUEST? NL* DOT NL* lamdaParam)*;

lamdaParam
    : identifier callSuffix NL* lambdaExpression
    | identifier callSuffix
    | identifier NL* lambdaExpression;

conditionOperator
    : BITAND | BITXOR | BITOR | AND | OR
    ;

tupleLeftValueExpression
    : LPAREN NL* (identifier | leftValueExpression | tupleLeftValueExpression) (NL* COMMA NL* (identifier | leftValueExpression | tupleLeftValueExpression))+ NL* RPAREN
    ;

leftValueExpression
    : leftAuxExpression QUEST? assignableSuffix
    | WILDCARD
    ;

leftAuxExpression
    : identifier typeArguments
    | leftAuxExpression QUEST? callSuffix
    | leftAuxExpression QUEST? NL* DOT NL* identifier (typeArguments)?
    | leftAuxExpression QUEST? indexAccess
    ;

assignableSuffix
    : fieldAccess
    | indexAccess
    ;

fieldAccess
    : NL* DOT NL* identifier
    ;

postfixExpression
    : (GRAD|VAlWITHGRAD|VJP) adCallSuffix?
    | ADJOINTOF (LPAREN NL* diffFunc NL* RPAREN)?
    | (identifier QUEST? NL* DOT NL*)* identifier NL* (LT NL* type NL*)+ rShift+ NL* GT*
    | atomicExpression lambdaExpression?
    | postfixExpression QUEST? NL* DOT NL* identifier (typeArguments | (callSuffix? lambdaExpression))
    | postfixExpression QUEST? NL* DOT NL* identifier?
    | postfixExpression NOT
    | postfixExpression callSuffix lambdaExpression?
    | postfixExpression callSuffix lambdaExpression? (QUEST? NL* DOT NL* lamdaParam)+
    | postfixExpression indexAccess
    | postfixExpression (QUEST questSeperatedItems)+ // optional chaining expression
    ;

questSeperatedItems
    : questSeperatedItem+
    ;

questSeperatedItem
    : itemAfterQuest (callSuffix | callSuffix? lambdaExpression | indexAccess)?
    ;

itemAfterQuest
    : DOT identifier (typeArguments)?
    | callSuffix
    | indexAccess
    | lambdaExpression
    ;

callSuffix
    : LPAREN NL* COMMA* (INOUT? valueArgument (NL* COMMA NL* INOUT? valueArgument?)* NL*)? RPAREN
    ;

adCallSuffix
    : LPAREN NL* diffFunc (NL* COMMA NL* valueArgument?)* NL* RPAREN
    ;

valueArgument
    : identifier NL* COLON NL* ( expression | type )?
    | expression
    | dumbArgument
    ;

dumbArgument
    : (DOT | COMMA)+;

diffFunc
    : identifier;

indexAccess
    : LSQUARE NL* (expression | rangeElement) (NL* COMMA NL* (expression | rangeElement))* NL* RSQUARE
    ;

rangeElement
    :  RANGEOP
    | ( CLOSEDRANGEOP | RANGEOP ) NL* expression
    | expression NL* RANGEOP
    ;

atomicExpression
    : literalConstant
    | collectionLiteral
    | tupleLiteral
    | identifier (typeArguments)?
    | ifExpression
    | matchExpression
    | tryExpression
    | jumpExpression
    | typeConvExpr
    | thisSuperExpression
    | spawnExpression
    | synchronizedExpression
    | parenthesizedExpression
    | macroExpression
    | quoteExpression
    | lambdaExpression
    | unsafeExpression
    | charLangTypes
    | VARRAY (typeArguments)?
    ;

literalConstant
    : IntegerLiteral
    | FloatLiteral
    | CharacterByteLiteral
    | charLiteral
    | booleanLiteral
    | ByteStringLiteral
    | stringLiteral
    | unitLiteral
    ;

booleanLiteral
    : TRUE
    | FALSE
    ;

charLiteral
    : SINGLE_QUOTE_OPEN (characterExpression | charContent | escapeContent)* SINGLE_QUOTE_CLOSE
    ;

characterExpression
    : CharacterStrExprStart SEMI* (expressionOrDeclaration (SEMI+ expressionOrDeclaration?)*) RCURL
    | CharacterStrExprStart NL* RCURL
    ;

charContent
    : CharacterLiteral
    ;

stringLiteral
    : lineStringLiteral
    | multiLineStringLiteral
    | javaStringLiteral
    | MultiLineRawStringLiteral
    ;

lineStringContent
    : LineStrText
    ;

escapeContent
    : EscapeSeq
    ;

lineStringLiteral
    : QUOTE_OPEN (lineStringExpression | lineStringContent | escapeContent)* QUOTE_CLOSE
    ;

lineStringExpression
    : LineStrExprStart SEMI* (expressionOrDeclaration (SEMI+ expressionOrDeclaration?)*) RCURL
    | LineStrExprStart NL* RCURL
    ;

multiLineStringContent
    : MultiLineStrText
    ;

multiLineStringLiteral
    : TRIPLE_QUOTE_OPEN (multiLineStringExpression | multiLineStringContent | MultiLineStringQuote | MultiLineStringSingleQuote | escapeContent)* TRIPLE_QUOTE_CLOSE
    | TRIPLE_SINGLE_QUOTE_OPEN (multiLineStringExpression | multiLineStringContent | MultiLineStringQuote | MultiLineStringSingleQuote | escapeContent)* TRIPLE_SINGLE_QUOTE_CLOSE
    ;

multiLineStringExpression
    : MultiLineStrExprStart NL* ((expressionOrDeclaration end+)* expressionOrDeclaration?) RCURL
    ;

javaStringLiteral
    : Java_QUOTE_OPEN (lineStringExpression | lineStringContent | escapeContent)* QUOTE_CLOSE
    ;

collectionLiteral
    : arrayLiteral
    ;

arrayLiteral
    : LSQUARE (NL* elements)? NL* RSQUARE
    ;

elements
    : element ( NL* COMMA NL* element? )*
    ;

element
    : spreadElement
    | expression
    | dumbArgument
    ;

spreadElement
    : MUL expression
    ;

tupleLiteral
    : LPAREN NL* expression (NL* COMMA NL* expression?)+ NL* RPAREN
    ;

unitLiteral
    : LPAREN NL* RPAREN
    ;

ifExpression
    : IF NL* LPAREN NL* expression NL* RPAREN NL* block
    (NL* ELSE (NL* ifExpression | NL* block))?
    ;

deconstructPattern
    : constantPattern
    | wildcardPattern
    | varBindingPattern
    | tuplePattern
    | enumPattern
    ;

matchExpression
    : MATCH NL* LPAREN NL* expression NL* RPAREN NL* LCURL end* ((EditorfoldStart | EditorfoldEnd) NL*)* (matchCase ((EditorfoldStart | EditorfoldEnd) NL*)*)* RCURL
    | MATCH NL* LCURL end* ((EditorfoldStart | EditorfoldEnd) NL*)* (CASE NL* (expression | WILDCARD) NL* DOUBLE_ARROW NL* (expressionOrDeclaration end+)* expressionOrDeclaration? ((EditorfoldStart | EditorfoldEnd) NL*)*)+ RCURL
    ;

matchCase
    : CASE NL* pattern ( NL* BITOR NL* pattern)* NL* patternGuard? NL* DOUBLE_ARROW NL* (expressionOrDeclaration end+)* expressionOrDeclaration?
    ;

patternGuard
    : WHERE NL* expression
    ;

pattern
   : constantPattern
   | wildcardPattern
   | varBindingPattern
   | tuplePattern
   | sequencePattern
   | typePattern
   | enumPattern
   ;

constantPattern
   : literalConstant
   ;

wildcardPattern
   : WILDCARD
   ;

varBindingPattern
   : identifier
   ;

tuplePattern
   : LPAREN NL* pattern (NL* COMMA NL* pattern)+ NL* RPAREN
   ;
sequencePattern
   : LSQUARE NL* (ELLIPSIS (NL* COMMA NL* pattern)* NL*)? RSQUARE
   | LSQUARE NL* pattern (NL* COMMA NL* pattern)* (NL* COMMA NL* ELLIPSIS)? (NL* COMMA NL* pattern)* NL* RSQUARE
   ;

typePattern
   : (WILDCARD | identifier) NL* COLON NL* type
   ;

enumPattern
   : NL* enumPatternConstructor patternCallSuffix
   | NL* enumPatternConstructor callSuffix
   | NL* enumPatternConstructor
   ;

enumPatternConstructor
    : enumPatternQualifier ? identifier
    ;

enumPatternQualifier
    : identifier (typeArguments)? QUEST? NL* DOT NL*
      (identifier (typeArguments)? QUEST? NL* DOT NL*)*
    ;

patternCallSuffix
    : LPAREN NL* (pattern (NL* COMMA NL* pattern)* NL*)? RPAREN
    ;

loopExpression
    : forInExpression
    | whileExpression
    | doWhileExpression
    ;

forInExpression
    : FOR NL* LPAREN NL* patternsMaybeIrrefutable NL* IN NL* expression NL* (patternGuard NL*)? RPAREN NL* block
    ;

patternsMaybeIrrefutable
    : wildcardPattern
    | varBindingPattern
    | tuplePattern
    | enumPattern
    ;

whileExpression
    : WHILE NL* LPAREN NL* (LET)? (NL* deconstructPattern NL* ASSIGN NL*)? expression NL* RPAREN NL* block
    ;

doWhileExpression
    : DO NL* block NL* WHILE NL* LPAREN NL* expression NL* RPAREN
    ;

tryExpression
    : TRY NL* block NL* FINALLY NL* block
    | TRY NL* block (NL* CATCH NL* LPAREN NL* catchPattern NL* RPAREN NL* block)+ (NL* FINALLY NL* block)?
    | TRY NL* LPAREN NL* resourceSpecifications NL* RPAREN NL* block
    (NL* CATCH NL* LPAREN NL* catchPattern NL* RPAREN NL* block)* (NL* FINALLY NL* block)?
    | TRY NL* block NL*
    ;

catchPattern
    : wildcardPattern
    | exceptionTypePattern
    ;

exceptionTypePattern
    : (WILDCARD | identifier) NL* COLON NL* type (NL* BITOR NL* type)*
    ;

resourceSpecifications
    : resourceSpecification (NL* COMMA NL* resourceSpecification)*
    ;

resourceSpecification
    : identifier (NL* COLON NL* classType)? NL* ASSIGN NL* expression
    ;

jumpExpression
    : throwExpression
    | RETURN (NL* expression)?
    | CONTINUE
    | BREAK
    ;

throwExpression
    : THROW NL* expression
    ;

typeConvExpr
    :  (INT8 | INT16 | INT32 | INT64 | INTNATIVE | UINT8 | UINT16 | UINT32 | UINT64 | UINTNATIVE | FLOAT16 | FLOAT32 | FLOAT64| RUNE) LPAREN NL* expression NL* RPAREN
    ;

thisSuperExpression
    : THIS
    | SUPER
    ;

lambdaExpression
        : LCURL end* ((EditorfoldStart | EditorfoldEnd) NL*)* (lambdaParameters? NL* DOUBLE_ARROW NL*)? expressionOrDeclarations (LPAREN RPAREN)? ((EditorfoldStart | EditorfoldEnd) NL*)* RCURL
    ;

lambdaParameters
    : lambdaParameter (NL* COMMA NL* lambdaParameter)*
    ;

lambdaParameter
    : (identifier | WILDCARD) (NL* COLON NL* type)?
    ;

spawnExpression
    : SPAWN NL* lambdaExpression
    ;

unsafeExpression
    : UNSAFE NL* block
    ;

synchronizedExpression
    : SYNCHRONIZED LPAREN NL* identifier NL* RPAREN NL* block
    ;

parenthesizedExpression
    : LPAREN NL* expression NL* RPAREN
    ;

block
    : LCURL end* ((EditorfoldStart | EditorfoldEnd) NL*)* RCURL
    | LCURL end* expressionOrDeclarations RCURL
    ;

expressionOrDeclarations
    :  (((EditorfoldStart | EditorfoldEnd) NL*)* expressionOrDeclaration end+ ((EditorfoldStart | EditorfoldEnd) NL*)*)* expressionOrDeclaration?
    ;

expressionOrDeclaration
    : varOrfuncDeclaration
    | jumpExpression
    | loopExpression
    | expression
    ;

varOrfuncDeclaration
    : functionDefinition
    | variableDeclaration
    ;

quoteExpression
    : QUOTE quoteExpr
    ;

quoteExpr
    : LPAREN NL* quoteParameters NL* RPAREN
    ;

quoteParameters
    : (NL* quoteInterpolate | NL* macroExpression |NL* quoteToken)+
    ;

modifier
    : PUBLIC
    | PROTECTED
    | INTERNAL
    | PRIVATE
    | STATIC
    | OPEN
    | OVERRIDE
    | ABSTRACT
    | REDEF
    | SEALED
    | CONST
    | MUT
    | UNSAFE
    ;

keywords
    : PUBLIC | PRIVATE | PROTECTED | OVERRIDE | ABSTRACT | OPEN | REDEF | INTERNAL
    ;

identifier
    : keywords | Identifier | WILDCARD
    ;

quoteToken
    : DOT | COMMA | LCURL | RCURL | EXP | MUL | MOD | DIV | ADD | SUB
    | PIPELINE | COMPOSITION
    | INC | DEC | AND | OR | BITXOR | NOT | BITAND | BITOR | LSHIFT | rShift | COLON | SEMI | NL
    | ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | EXP_ASSIGN | DIV_ASSIGN | MOD_ASSIGN
    | AND_ASSIGN | OR_ASSIGN | BITXOR_ASSIGN | BITAND_ASSIGN | BITOR_ASSIGN | LSHIFT_ASSIGN
    | ARROW | DOUBLE_ARROW | ELLIPSIS | CLOSEDRANGEOP | RANGEOP | HASH | AT | QUEST | UPPERBOUND | LT | GT | LE | ge
    | NOTEQUAL | EQUAL | WILDCARD | BACKSLASH | QUOTESYMBOL | DOLLAR
    | INT8 | INT16 | INT32 | INT64 | UINT8 | UINT16 | UINT32 | UINT64 | FLOAT16 | INTNATIVE | UINTNATIVE
    | FLOAT32 | FLOAT64 | RUNE | BOOLEAN | UNIT | NOTHING | STRUCT | ENUM | THIS
    | PACKAGE | IMPORT | CLASS | INTERFACE | FUNC | LET | VAR | CONST | TYPE_ALIAS
    | INIT | THIS | SUPER | IF | ELSE | CASE | TRY | CATCH | FINALLY
    | FOR | DO | WHILE | THROW | RETURN | CONTINUE | BREAK | AS | IN | IS
    | MATCH  | WHERE | EXTEND | SPAWN | SYNCHRONIZED | MACRO | QUOTE | TRUE | FALSE
    | SEALED | STATIC | PUBLIC | PRIVATE | PROTECTED | PUBLIC
    | OVERRIDE | ABSTRACT | OPEN | OPERATOR | FOREIGN
    | Identifier | DollarIdentifier
    | LPAREN NL* quoteToken* NL* RPAREN
    | LSQUARE NL* quoteToken* NL* RSQUARE
    | literalConstant
    ;

quoteInterpolate
    : DOLLAR LPAREN NL* expression NL* RPAREN
    ;

annotationList: annotation+;

annotation
    : AT NOT? (identifier QUEST? NL* DOT)* identifier (LSQUARE NL* annotationArgumentList NL* RSQUARE)?
    ;

annotationArgumentList
    : annotationArgument (NL* COMMA NL* annotationArgument)* NL* COMMA?
    ;

annotationArgument
    : identifier NL* COLON NL* expression
    | macroExpression
    | expression
    ;

macroExpression
    : ((macroKeywords | AT NOT? identifier) macroAttrExpr? NL*)+ (macroInputExprWithoutParens | macroInputExprWithParens)
    ;

macroKeywords
    : DIFFERENTIABLE | ADJOINT
    ;

macroAttrExpr
    : LSQUARE NL* (macroAttrParam (COMMA macroAttrParam)*)? NL* RSQUARE
    | LSQUARE NL* PRIMAL NL* COLON NL* (identifier)? NL* RSQUARE
    | LSQUARE NL* quoteToken*? NL* RSQUARE
    ;

macroAttrParam
    : (INCLUDE|EXCEPT|STAGE)* NL* COLON NL* ((LSQUARE NL* identifier (( NL* COMMA NL* identifier)* NL* RSQUARE)) | literalConstant);

macroInputExprWithoutParens
    : functionDefinition
    | operatorFunctionDefinition
    | structDefinition
    | structPrimaryInit
    | structInit
    | enumDefinition
    | classDefinition
    | classPrimaryInit
    | classInit
    | classFinalizer
    | interfaceDefinition
    | variableDeclaration
    | propertyDefinition
    | propertyMemberDeclaration
    | extendDefinition
    | foreignDeclaration
    | lambdaExpression
    | defaultParameter
    | typeAlias
    ;

macroInputExprWithParens
    : LPAREN NL* macroTokens NL* RPAREN
    ;

macroTokens
    : (lambdaExpression | quoteToken | macroExpression)*
    ;

assignmentOperator
    : ASSIGN
    | ADD_ASSIGN
    | SUB_ASSIGN
    | EXP_ASSIGN
    | MUL_ASSIGN
    | DIV_ASSIGN
    | MOD_ASSIGN
    | AND_ASSIGN
    | OR_ASSIGN
    | BITXOR_ASSIGN
    | BITAND_ASSIGN
    | BITOR_ASSIGN
    | LSHIFT_ASSIGN
    | rSHIFT_ASSIGN
    ;

equalityOperator
    : NOTEQUAL
    | EQUAL
    ;

comparisonOperator
    : LT
    | GT
    | LE
    | ge
    ;

ge:GT ASSIGN;

shiftingOperator
    : LSHIFT | rShift
    ;

rShift:GT GT;

rSHIFT_ASSIGN:rShift ASSIGN;

flowOperator
    : PIPELINE | COMPOSITION
    ;

additiveOperator
    : ADD | SUB
    ;

exponentOperator
    : EXP
    ;

multiplicativeOperator
    : MUL
    | DIV
    | MOD
    ;

prefixUnaryOperator
    : SUB
    | NOT
    ;

overloadedOperators
    : LSQUARE RSQUARE
    | LPAREN RPAREN
    | INC
    | DEC
    | NOT
    | ADD
    | SUB
    | EXP
    | MUL
    | DIV
    | MOD
    | LSHIFT
    | rShift
    | LT
    | GT
    | LE
    | ge
    | EQUAL
    | NOTEQUAL
    | BITAND
    | BITOR
    | BITXOR
    | AND
    | OR
    ;
