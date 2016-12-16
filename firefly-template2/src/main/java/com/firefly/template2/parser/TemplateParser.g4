grammar TemplateParser;
import TemplateLexer;

// program
program : extends? mainFunction functionDeclaration*;

templateBody :
    ( OutputString
    | OutputStringWithNewLine
    | OutputNewLine
    | set | include | selection | switch | whileLoop | forLoop | beanAccess)*;

extends : EXTENDS templatePath ';';

include : INCLUDE '(' (THIS | templatePath) methodCall? ')';

set : SET '(' Identifier '=' expression ')';

templatePath : Identifier ('.' Identifier)* ;

// function
mainFunction : MAIN templateBody END;

functionDeclaration : FUNCTION Identifier functionParameters templateBody END;

functionParameters : '(' ')' | '(' Identifier (',' Identifier)* ')';


// flow control
selection
    : IF '(' expression ')' templateBody
    (ELSE THEN_IF '(' expression ')' templateBody)*
    (ELSE templateBody)?
    END
    ;

switch
    : SWITCH '(' beanAccess ')'
    (CASE (StringLiteral | IntegerLiteral) templateBody BREAK)+
    (DEFAULT templateBody BREAK)?
    END
    ;

whileLoop : WHILE '(' expression ')' templateBody END;

forLoop : FOR '(' Identifier ':' beanAccess ')' templateBody END;


// expression
expression
    : beanAccess                                        # bean
    | IntegerLiteral                                    # integerLiteral
    | FloatingPointLiteral                              # floatingPointLiteral
    | BooleanLiteral                                    # booleanLiteral
    | StringLiteral                                     # stringLiteral
    | '(' expression ')'                                # parens
    | ('+' | '-' | '~' | '!' | '++' | '--') expression  # prefixUnary
    | expression ('++' | '--')                          # suffixUnary
    | expression ('*' | '/' | '%') expression           # mulDivMod
    | expression ('+' | '-') expression                 # addSub
    | expression ('>>>' | '>>' | '<<') expression       # shift
    | expression ('>' | '<' | '>=' | '<=') expression   # greatOrLess
    | expression ('==' | '!=') expression               # equalOrNotEqual
    | expression '&' expression                         # bitAnd
    | expression '|' expression                         # bitOr
    | expression '^' expression                         # xor
    | expression '&&' expression                        # shortCircuitAnd
    | expression '||' expression                        # shortCircuitOr
    | expression '?' expression ':' expression          # ternary
    | expression ('='
                | '+=' | '-=' | '*=' | '/=' | '%='
                | '&=' | '|=' | '^='
                | '<<=' | '>>=' | '>>>=') expression    # assignment
    ;


// bean access
beanAccess : '${' objectAccess '}';

objectAccess : Identifier (propertyAccess | arrayAccess | mapAccess | methodCall)*;

propertyAccess : '.' Identifier;

arrayAccess : '[' IntegerLiteral ']';

mapAccess : '[' StringLiteral ']';

methodCall : '.' Identifier callMethodParameters;

callMethodParameters : '(' ')' | '(' expression ( ',' expression )* ')';
