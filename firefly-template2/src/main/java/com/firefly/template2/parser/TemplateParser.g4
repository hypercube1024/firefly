grammar TemplateParser;
import TemplateLexer;

// function
mainFunction : MAIN templateBody END;

templateBody : (OUT_STRING | selection | switch | whileLoop | forLoop | beanAccess)*;


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
    : beanAccess                                                                    # bean
    | IntegerLiteral                                                                # integerLiteral
    | FloatingPointLiteral                                                          # floatingPointLiteral
    | BooleanLiteral                                                                # booleanLiteral
    | '(' expression ')'                                                            # parens
    | (ADD | SUB | TILDE | BANG | INC | DEC) expression                             # prefixUnary
    | expression (INC | DEC)                                                        # suffixUnary
    | expression (MUL | DIV | MOD) expression                                       # mulDivMod
    | expression (ADD | SUB) expression                                             # addSub
    | expression (URSHIFT | RSHIFT | LSHIFT) expression                             # shift
    | expression (GT | LT | GE | LE) expression                                     # greatOrLess
    | expression (EQUAL | NOTEQUAL) expression                                      # equalOrNotEqual
    | expression BITAND expression                                                  # bitAnd
    | expression BITOR expression                                                   # bitOr
    | expression CARET expression                                                   # xor
    | expression AND expression                                                     # shortCircuitAnd
    | expression OR expression                                                      # shortCircuitOr
    | expression QUESTION expression COLON expression                               # ternary
    | expression (ASSIGN
                | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN
                | AND_ASSIGN | OR_ASSIGN | XOR_ASSIGN
                | LSHIFT_ASSIGN | RSHIFT_ASSIGN | URSHIFT_ASSIGN) expression        # assignment
    ;


// bean access
beanAccess : '${' objectAccess '}';

objectAccess : Identifier (propertyAccess | arrayAccess | mapAccess | methodCall)*;

propertyAccess : DOT Identifier;

arrayAccess : '[' IntegerLiteral ']';

mapAccess : '[' StringLiteral ']';

methodCall : DOT Identifier callMethodParameters;

callMethodParameters : '(' ')' | '(' expression ( ',' expression )* ')';
