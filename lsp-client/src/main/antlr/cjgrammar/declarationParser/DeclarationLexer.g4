/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 * This source file is part of the Cangjie project, licensed under Apache-2.0
 * with Runtime Library Exception.
 *
 * See https://cangjie-lang.cn/pages/LICENSE for license information.
 */

lexer grammar DeclarationLexer;

@lexer::members {public boolean isGarbled = false;}

//import UnicodeClasses;

// Lexical Grammar

// Comments

DelimitedComment
    : '/*' ( DelimitedComment | . )*? ('*/' | EOF)
      -> channel(HIDDEN)
    ;

EditorfoldStart
    : '//''<''e''d''i''t''o''r''-''f''o''l''d' ~[\u000A\u000D]*
    | '//''r''e''g''i''o''n' ~[\u000A\u000D]*
    ;

EditorfoldEnd
    : '//''<''/''e''d''i''t''o''r''-''f''o''l''d' ~[\u000A\u000D]*
    | '//''e''n''d''r''e''g''i''o''n' ~[\u000A\u000D]*
    ;

LineComment
    : '//' ~[\u000A\u000D]*
      -> channel(HIDDEN)
    ;

// Whitespace and Newline

WS
    : [\u0020\u0009\u000C]+
      -> channel(HIDDEN)
    ;

NL: (WS? ('\u000A' | '\u000D' '\u000A'))+ ;
// Symbols

DOT: '.' ;
COMMA: ',' ;
LPAREN: '(' -> pushMode(Inside);
RPAREN: ')' { if (!_modeStack.isEmpty()) { popMode(); } else setChannel(HIDDEN); };
LSQUARE: '[' -> pushMode(Inside);
RSQUARE: ']' { if (!_modeStack.isEmpty()) { popMode(); } else setChannel(HIDDEN); };
LCURL: '{' -> pushMode(DEFAULT_MODE);
RCURL: '}' { if (!_modeStack.isEmpty()) { popMode(); } else setChannel(HIDDEN); };
EXP: '**' ;
MUL: '*' ;
MOD: '%' ;
DIV: '/' ;
ADD: '+' ;
SUB: '-' ;
TILDE: '~' ;

// flow operator
PIPELINE: '|>' ;
COMPOSITION: '~>' ;

INC: '++' ;
DEC: '--' ;
AND: '&&' ;
OR: '||' ;
NOT: '!' ;
BITAND: '&' ;
BITOR: '|' ;
BITXOR: '^' ;
LSHIFT: '<<' ;
//rshift: '>>'
COLON: ':' ;
SEMI: ';' ;
ASSIGN: '=' ;
ADD_ASSIGN: '+=' ;
SUB_ASSIGN: '-=' ;
MUL_ASSIGN: '*=' ;
EXP_ASSIGN: '**=' ;
DIV_ASSIGN: '/=' ;
MOD_ASSIGN: '%=' ;

//// new Compound Assignment Operators
AND_ASSIGN: '&&=' ;
OR_ASSIGN: '||=' ;
BITAND_ASSIGN: '&=' ;
BITOR_ASSIGN: '|=' ;
BITXOR_ASSIGN: '^=' ;
LSHIFT_ASSIGN: '<<=' ;
//RSHIFT_ASSIGN: '>>='

ARROW: '->' ;
DOUBLE_ARROW: '=>' ;
ELLIPSIS: '...' ;
CLOSEDRANGEOP: '..=' ;
RANGEOP: '..' ;
HASH: '#' ;
AT: '@' ;
QUEST: '?' ;
UPPERBOUND: '<:';
LT: '<' ;
GT: '>' ;
LE: '<=' ;
//ge: '>=' ;
NOTEQUAL: '!=' ;
EQUAL: '==' ;
WILDCARD: '_' ;
BACKSLASH: '\\' ;
QUOTESYMBOL: '`';
DOLLAR: '$';

// Keywords

// Builtin Types
INT8: 'Int8' ;
INT16: 'Int16' ;
INT32: 'Int32' ;
INT64: 'Int64' ;
INTNATIVE: 'IntNative';
UINT8: 'UInt8' ;
UINT16: 'UInt16' ;
UINT32: 'UInt32' ;
UINT64: 'UInt64' ;
UINTNATIVE: 'UIntNative';
FLOAT16: 'Float16' ;
FLOAT32: 'Float32' ;
FLOAT64: 'Float64' ;
RUNE: 'Rune' ;
BOOLEAN: 'Bool' ;
UNIT: 'Unit' ;
NOTHING: 'Nothing' ;
VARRAY: 'VArray';
STRUCT: 'struct' ;
ENUM: 'enum' ;
THISTYPE: 'This';

// Keywords
PACKAGE: 'package' ;
IMPORT: 'import' ;
CLASS: 'class' ;
INTERFACE: 'interface' ;
FUNC: 'func';
MAIN: 'main';
LET: 'let' ;
VAR: 'var' ;
CONST: 'const' ;
TYPE_ALIAS: 'type' ;
INIT: 'init'  ;
THIS: 'this' ;
SUPER: 'super' ;
IF: 'if' ;
ELSE: 'else' ;
CASE: 'case' ;
TRY: 'try' ;
CATCH: 'catch' ;
FINALLY: 'finally' ;
FOR: 'for' ;
DO: 'do' ;
WHILE: 'while' ;
THROW: 'throw' ;
RETURN: 'return' ;
CONTINUE: 'continue' ;
BREAK: 'break' ;
IS: 'is' ;
AS: 'as' ;
IN: 'in' ;
MATCH: 'match' ;
WHERE: 'where';
EXTEND: 'extend';
SPAWN: 'spawn';
SYNCHRONIZED: 'synchronized';
MACRO: 'macro';
QUOTE: 'quote';
TRUE: 'true';
FALSE: 'false';
PROP: 'prop';
MUT: 'mut';
INOUT: 'inout';

// Modifiers
STATIC: 'static';
PUBLIC: 'public' ;
PRIVATE: 'private' ;
PROTECTED: 'protected' ;
INTERNAL: 'internal';
OVERRIDE: 'override' ;
ABSTRACT: 'abstract' ;
OPEN: 'open' ;
SEALED: 'sealed' ;
OPERATOR: 'operator' ;
FOREIGN: 'foreign';
REDEF: 'redef';
UNSAFE: 'unsafe';

// ad
GRAD: '@grad';
VAlWITHGRAD: '@valWithGrad';
ADJOINTOF: '@adjointOf';
DIFFERENTIABLE: '@differentiable';
ADJOINT: '@adjoint';
VJP: '@vjp';
INCLUDE: 'include';
EXCEPT: 'except';
PRIMAL: 'primal';
STAGE: 'stage';

// Literals
fragment FloatLiteralSuffix
    : 'f16' | 'f32' | 'f64'
    ;

FloatLiteral
    : (DecimalLiteral DecimalExponent | DotDecimalFraction DecimalExponent? | (DecimalLiteral DecimalFraction) DecimalExponent?)  FloatLiteralSuffix?
    | ( Hexadecimalprefix (HexadecimalDigits | DotHexadecimalFraction | (HexadecimalDigits HexadecimalFraction)) HexadecimalExponent)  FloatLiteralSuffix?
    ;
fragment DotDecimalFraction : '.' DecimalLiteral ;
fragment DecimalFraction : '.' DecimalLiteral ;
fragment DecimalExponent : FloatE Sign? DecimalLiteral  ;
fragment DotHexadecimalFraction : '.' HexadecimalDigits ;
fragment HexadecimalFraction : '.' HexadecimalDigits? ;
fragment HexadecimalExponent : FloatP Sign? DecimalLiteral ;
fragment FloatE : [eE] ;
fragment FloatP : [pP] ;
fragment Sign : [-] ;
fragment Hexadecimalprefix : '0' [xX] ;

fragment IntegerLiteralSuffix
   : 'i8' |'i16' |'i32' |'i64' |'u8' |'u16' |'u32' | 'u64'
   ;

IntegerLiteral
   : BinaryLiteral IntegerLiteralSuffix?
   | OctalLiteral IntegerLiteralSuffix?
   | DecimalLiteral IntegerLiteralSuffix?
   | HexadecimalLiteral IntegerLiteralSuffix?
   ;

BinaryLiteral
    : '0' [bB] BinDigit (BinDigit | '_')*
    ;
fragment BinDigit
    : [01]
    ;
OctalLiteral
    : '0' [oO] OctalDigit (OctalDigit | '_')*
    ;
fragment OctalDigit
    : [0-7]
    ;
DecimalLiteral
    : DecimalDigit (DecimalDigit | '_')*
    ;
fragment DecimalDigit
    : [0-9]
    ;
HexadecimalLiteral
    : '0' [xX] HexadecimalDigits
    ;
fragment HexadecimalDigits
    : HexadecimalDigit (HexadecimalDigit | '_')*
    ;
fragment HexadecimalDigit
    : [0-9a-fA-F]
    ;

fragment SingleChar
    : ~['\\\r\n]
    ;

EscapeSeq
    : UniCharacterLiteral
    | EscapedIdentifier
    ;

fragment UniCharacterLiteral
    : '\\' 'u' '{' HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit '}'
    ;

fragment EscapedIdentifier
    : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | 'f' | 'v' | '0' | '$')
    ;

CharacterByteLiteral
    : 'b' '\'' (SingleCharByte | ByteEscapeSeq) '\''
    ;

ByteEscapeSeq
    : HexCharByte
    | ByteEscapedIdentifier
    ;

fragment SingleCharByte
    // ASCII 0x00~0x7F without \n \r \' \" \\
    // +-------+-----+-----+
    // | Char  | Hex | Dec |
    // +-------+-----+-----+
    // | \n    |  0A |  10 |
    // | \r    |  0D |  13 |
    // | \"    |  22 |  34 |
    // | \'    |  27 |  39 |
    // | \\    |  5C |  92 |
    // +-------+-----+-----+
    : [\u0000-\u0009\u000B\u000C\u000E-\u0021\u0023-\u0026\u0028-\u005B\u005D-\u007F]
    ;

fragment ByteEscapedIdentifier
    : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | 'f' | 'v' | '0')
    ;

fragment HexCharByte
    : '\\' 'u' '{' HexadecimalDigit '}'
    | '\\' 'u' '{' HexadecimalDigit HexadecimalDigit '}'
    ;

ByteStringLiteral
    : 'b' '"' (SingleCharByte | ByteEscapeSeq | '\'')* '"'
    ;

MultiLineRawStringLiteral
    : MultiLineRawStringContent
    ;

fragment MultiLineRawStringContent
    : HASH MultiLineRawStringContent HASH
    | HASH '"' .*? '"' HASH
    | HASH '\'' .*? '\'' HASH
    ;

// Identifiers

Identifier
    : (Letter | '_' | [\p{XID_Start}]) (Letter | '_' | DecimalDigit | [\p{XID_Continue}])*
    | '`' (Letter | '_' | [\p{XID_Start}]) (Letter | '_' | DecimalDigit | [\p{XID_Continue}])* '`'
    ;

DollarIdentifier
    : '$' Identifier
    ;

QUOTE_OPEN: 'r' ? '"' -> pushMode(LineString);

TRIPLE_QUOTE_OPEN: '"""' NL -> pushMode(MultiLineString);

TRIPLE_SINGLE_QUOTE_OPEN: '\'\'\'' NL -> pushMode(MultiLineString);

SINGLE_QUOTE_OPEN: 'r' ? '\'' -> pushMode(CharContent);

Java_QUOTE_OPEN: 'J"' -> pushMode(LineString);

mode CharContent;

SINGLE_QUOTE_CLOSE
    : '\'' -> popMode
    ;

CharacterLiteral
    : (~['\\\r\n$])+
    ;

CharContent_EscapeSeq
    : EscapeSeq -> type(EscapeSeq)
    ;

CharacterStrExprStart
    : '${' -> pushMode(Inside)
    ;

CharacterStrDollar : '$' -> type(CharacterLiteral);

ErrorCharacter: . -> channel(HIDDEN);

mode LineString;

QUOTE_CLOSE
    : '"' -> popMode
    ;

LineStrText
    : (~["\\\r\n$])+
    ;

LineStr_EscapeSeq
    : EscapeSeq -> type(EscapeSeq)
    ;

LineStrExprStart
    : '${' -> pushMode(Inside)
    ;

LineStrDollar : '$' -> type(LineStrText);

ErrorCharacterStr: . -> channel(HIDDEN);

mode MultiLineString;

TRIPLE_QUOTE_CLOSE
    : MultiLineStringQuote? '"""' -> popMode
    ;

TRIPLE_SINGLE_QUOTE_CLOSE
    : MultiLineStringQuote? '\'\'\'' -> popMode
    ;

MultiLineStrText
    : (~[\\$"'])+
    | MultiLineDollar
    ;

MultiLineDollar: '$' -> type(LineStrText);

MultiLineStringQuote
    : '"'+
    ;

MultiLineStringSingleQuote
    : '\''+
    ;

MulStr_EscapeSeq
    : EscapeSeq -> type(EscapeSeq)
    ;

MultiLineStrExprStart
    : '${' -> pushMode(Inside)
    ;

fragment Letter
    : [a-zA-Z]
    | ~[\u0000-\u00FF\uD800-\uDBFF]
    | [\uD800-\uDBFF] [\uDC00-\uDFFF]
    | '·'
    ;

ErrorCharacterMulStr: . -> channel(HIDDEN);

mode Inside ;

Inside_LPAREN: LPAREN -> pushMode(Inside), type(LPAREN) ;
Inside_LSQUARE: LSQUARE -> pushMode(Inside), type(LSQUARE) ;
Inside_LCURL: LCURL -> pushMode(Inside), type(LCURL) ;
Inside_RPAREN: RPAREN -> popMode, type(RPAREN);
Inside_RSQUARE: RSQUARE -> popMode, type(RSQUARE);
Inside_RCURL: RCURL -> popMode, type(RCURL);

Inside_DOT: DOT -> type(DOT) ;
Inside_COMMA: COMMA  -> type(COMMA) ;
Inside_EXP: EXP -> type(EXP) ;
Inside_MUL: MUL -> type(MUL) ;
Inside_MOD: MOD  -> type(MOD) ;
Inside_DIV: DIV -> type(DIV) ;
Inside_ADD: ADD  -> type(ADD) ;
Inside_SUB: SUB  -> type(SUB) ;

// flow operator
Inside_PIPELINE: PIPELINE -> type(PIPELINE) ;
Inside_COMPOSITION: COMPOSITION -> type(COMPOSITION) ;

Inside_INC: INC  -> type(INC) ;
Inside_DEC: DEC  -> type(DEC) ;
Inside_AND: AND  -> type(AND) ;
Inside_OR: OR  -> type(OR) ;
Inside_NOT: NOT  -> type(NOT) ;
Inside_BITAND: BITAND  -> type(BITAND) ;
Inside_BITOR: BITOR  -> type(BITOR) ;
Inside_BITXOR: BITXOR -> type(BITXOR) ;
Inside_LSHIFT: LSHIFT  -> type(LSHIFT) ;
//Inside_RSHIFT: rshift  -> type(rshift)
Inside_COLON: COLON  -> type(COLON) ;
Inside_SEMI: SEMI  -> type(SEMI) ;
Inside_ASSIGN: ASSIGN -> type(ASSIGN) ;
Inside_ADD_ASSIGN: ADD_ASSIGN  -> type(ADD_ASSIGN) ;
Inside_SUB_ASSIGN: SUB_ASSIGN  -> type(SUB_ASSIGN) ;
Inside_EXP_ASSIGN: EXP_ASSIGN  -> type(EXP_ASSIGN) ;
Inside_MUL_ASSIGN: MUL_ASSIGN  -> type(MUL_ASSIGN) ;
Inside_DIV_ASSIGN: DIV_ASSIGN  -> type(DIV_ASSIGN) ;
Inside_MOD_ASSIGN: MOD_ASSIGN  -> type(MOD_ASSIGN) ;

// new Compound Assignment Operators
//Inside_AND_ASSIGN: AND_ASSIGN  -> type(AND_ASSIGN)
//Inside_OR_ASSIGN: OR_ASSIGN  -> type(OR_ASSIGN)
//Inside_BITAND_ASSIGN: BITAND_ASSIGN  -> type(BITAND_ASSIGN)
//Inside_BITOR_ASSIGN: BITOR_ASSIGN  -> type(BITOR_ASSIGN)
//Inside_BITXOR_ASSIGN: BITXOR_ASSIGN -> type(BITXOR_ASSIGN)
//Inside_LSHIFT_ASSIGN: LSHIFT_ASSIGN  -> type(LSHIFT_ASSIGN)
//Inside_RSHIFT_ASSIGN: RSHIFT_ASSIGN  -> type(RSHIFT_ASSIGN)

Inside_ARROW: ARROW  -> type(ARROW) ;
Inside_DOUBLE_ARROW: DOUBLE_ARROW  -> type(DOUBLE_ARROW) ;
Inside_ELLIPSIS: ELLIPSIS  -> type(ELLIPSIS) ;
Inside_CLOSEDRANGEOP: CLOSEDRANGEOP  -> type(CLOSEDRANGEOP) ;
Inside_RANGEOP: RANGEOP  -> type(RANGEOP) ;
Inside_HASH: HASH  -> type(HASH) ;
Inside_AT: AT  -> type(AT) ;
Inside_QUEST: QUEST  -> type(QUEST) ;
Inside_UPPERBOUND: UPPERBOUND -> type(UPPERBOUND) ;
Inside_LT: LT  -> type(LT) ;
Inside_GT: GT  -> type(GT) ;
Inside_LE: LE  -> type(LE) ;
//Inside_GE: ge  -> type(ge)
Inside_NOTEQUAL: NOTEQUAL  -> type(NOTEQUAL) ;
Inside_EQUAL: EQUAL -> type(EQUAL) ;
Inside_WILDCARD: WILDCARD  -> type(WILDCARD) ;
Inside_BACKSLASH: BACKSLASH -> type(BACKSLASH) ;
Inside_QUOTESYMBOL: QUOTESYMBOL -> type(QUOTESYMBOL) ;
Inside_DOLLAR: DOLLAR -> type(DOLLAR) ;

Inside_INT8: INT8 -> type(INT8) ;
Inside_INT16: INT16 -> type(INT16) ;
Inside_INT32: INT32 -> type(INT32) ;
Inside_INT64: INT64 -> type(INT64) ;
Inside_INTNATIVE: INTNATIVE -> type(INTNATIVE) ;
Inside_UINT8: UINT8 -> type(UINT8) ;
Inside_UINT16: UINT16 -> type(UINT16) ;
Inside_UINT32: UINT32 -> type(UINT32) ;
Inside_UINT64: UINT64 -> type(UINT64) ;
Inside_UINTNATIVE: UINTNATIVE -> type(UINTNATIVE) ;
Inside_FLOAT16: FLOAT16 -> type(FLOAT16) ;
Inside_FLOAT32: FLOAT32 -> type(FLOAT32) ;
Inside_FLOAT64: FLOAT64 -> type(FLOAT64) ;
Inside_CHAR: RUNE -> type(RUNE) ;
Inside_BOOLEAN: BOOLEAN -> type(BOOLEAN) ;
Inside_UNIT: UNIT -> type(UNIT) ;
Inside_NOTHING: NOTHING -> type(NOTHING) ;
Inside_STRUCT: STRUCT -> type(STRUCT) ;
Inside_ENUM: ENUM -> type(ENUM) ;
Inside_THISTYPE: THISTYPE -> type(THISTYPE) ;

// KEYWORDS
Inside_PACKAGE: PACKAGE -> type(PACKAGE) ;
Inside_IMPORT: IMPORT -> type(IMPORT) ;
Inside_CLASS: CLASS -> type(CLASS) ;
Inside_INTERFACE: INTERFACE -> type(INTERFACE) ;
Inside_FUNC: FUNC -> type(FUNC) ;
Inside_MAIN: MAIN -> type(MAIN) ;
Inside_LET: LET -> type(LET) ;
Inside_VAR: VAR -> type(VAR) ;
Inside_PRIMAL: PRIMAL -> type(PRIMAL) ;
Inside_INCLUDE: INCLUDE -> type(INCLUDE) ;
Inside_EXCEPT: EXCEPT -> type(EXCEPT) ;
Inside_STAGE: STAGE -> type(STAGE) ;
Inside_CONST: CONST -> type(CONST) ;
Inside_TYPE_ALIAS: TYPE_ALIAS -> type(TYPE_ALIAS) ;
Inside_INIT: INIT -> type(INIT) ;
Inside_THIS: THIS -> type(THIS) ;
Inside_SUPER: SUPER -> type(SUPER) ;
Inside_IF: IF -> type(IF) ;
Inside_ELSE: ELSE -> type(ELSE) ;
Inside_CASE: CASE -> type(CASE) ;
Inside_TRY: TRY -> type(TRY) ;
Inside_CATCH: CATCH -> type(CATCH) ;
Inside_FINALLY: FINALLY -> type(FINALLY) ;
Inside_FOR: FOR -> type(FOR) ;
Inside_DO: DO -> type(DO) ;
Inside_WHILE: WHILE -> type(WHILE) ;
Inside_THROW: THROW -> type(THROW) ;
Inside_RETURN: RETURN -> type(RETURN) ;
Inside_CONTINUE: CONTINUE -> type(CONTINUE) ;
Inside_BREAK: BREAK -> type(BREAK) ;
Inside_IS: IS -> type(IS) ;
Inside_AS: AS -> type(AS) ;
Inside_IN: IN -> type(IN) ;
Inside_MATCH: MATCH -> type(MATCH) ;
Inside_WHERE: WHERE -> type(WHERE) ;
Inside_EXTEND: EXTEND -> type(EXTEND) ;
Inside_SPAWN: SPAWN -> type(SPAWN) ;
Inside_SYNCHRONIZED: SYNCHRONIZED -> type(SYNCHRONIZED);
Inside_MACRO: MACRO -> type(MACRO);
Inside_QUOTE: QUOTE -> type(QUOTE);
Inside_TRUE: TRUE -> type(TRUE);
Inside_FALSE: FALSE -> type(FALSE);
Inside_PROP: PROP -> type(PROP);
Inside_MUT: MUT -> type(MUT);
Inside_INOUT: INOUT -> type(INOUT);

Inside_GRAD: GRAD -> type(GRAD);
Inside_VAlWITHGRAD: VAlWITHGRAD -> type(VAlWITHGRAD);
Inside_VJP: VJP -> type(VJP);
Inside_ADJOINTOF : ADJOINTOF -> type(ADJOINTOF);

// Modifiers
Inside_STATIC: STATIC -> type(STATIC) ;
Inside_PUBLIC: PUBLIC -> type(PUBLIC) ;
Inside_PRIVATE: PRIVATE -> type(PRIVATE) ;
Inside_PROTECTED: PROTECTED -> type(PROTECTED) ;
Inside_OVERRIDE: OVERRIDE -> type(OVERRIDE) ;
Inside_ABSTRACT: ABSTRACT -> type(ABSTRACT) ;
Inside_OPEN: OPEN -> type(OPEN) ;
Inside_SEALED: SEALED -> type(SEALED) ;
Inside_OPERATOR: OPERATOR -> type(OPERATOR) ;
Inside_FOREIGN: FOREIGN -> type(FOREIGN) ;
Inside_REDEF: REDEF -> type(REDEF) ;

Inside_CharacterByteLiteral: CharacterByteLiteral -> type(CharacterByteLiteral);
Inside_FloatLiteral: FloatLiteral -> type(FloatLiteral) ;
Inside_IntegerLiteral: IntegerLiteral -> type(IntegerLiteral) ;
Inside_BinaryLiteral: BinaryLiteral -> type(BinaryLiteral) ;
Inside_OctalLiteral: OctalLiteral -> type(OctalLiteral) ;
Inside_DecimalLiteral: DecimalLiteral -> type(DecimalLiteral) ;
Inside_HexadecimalLiteral: HexadecimalLiteral -> type(HexadecimalLiteral) ;
Inside_Identifier: Identifier -> type(Identifier) ;
Inside_DollarIdentifier: DollarIdentifier -> type(DollarIdentifier) ;
Inside_EscapeSeq: EscapeSeq -> type(EscapeSeq) ;
//Inside_LineStringLiteral: LineStringLiteral -> type(LineStringLiteral) ;
//Inside_MultiLineStringLiteral: MultiLineStringLiteral -> type(MultiLineStringLiteral) ;
Inside_MultiLineRawStringLiteral: MultiLineRawStringLiteral -> type(MultiLineRawStringLiteral) ;
Inside_QUOTE_OPEN: QUOTE_OPEN -> pushMode(LineString), type(QUOTE_OPEN);
Inside_TRIPLE_QUOTE_OPEN: TRIPLE_QUOTE_OPEN -> pushMode(MultiLineString), type(TRIPLE_QUOTE_OPEN);
Inside_SINGLE_QUOTE_OPEN: SINGLE_QUOTE_OPEN -> pushMode(CharContent),type(SINGLE_QUOTE_OPEN);

Inside_LineComment: LineComment -> type(LineComment), channel(HIDDEN);
Inside_DelimitedComment: DelimitedComment -> type(DelimitedComment), channel(HIDDEN) ;

//For Lsp
Inside_NL: NL -> type(NL) ;
Inside_WS: WS -> type(WS), channel(HIDDEN) ;

ErrorCharacterInside: . -> channel(HIDDEN);

