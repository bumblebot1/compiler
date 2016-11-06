// COMS22201: Lexical analyser

lexer grammar Lex;

//---------------------------------------------------------------------------
// KEYWORDS
//---------------------------------------------------------------------------
DO         : 'do' ;
ELSE       : 'else' ;
FALSE      : 'false' ;
IF         : 'if' ;
READ       : 'read' ;
READR      : 'readr';
SKIP       : 'skip' ;
THEN       : 'then' ;
TRUE       : 'true' ;
WHILE      : 'while' ;
WRITE      : 'write' ;
WRITELN    : 'writeln' ;
SYMBOL     : ('int'|'real') ;

//---------------------------------------------------------------------------
// OPERATORS
//---------------------------------------------------------------------------
SEMICOLON    : ';' ;
OPENPAREN    : '(' ;
CLOSEPAREN   : ')' ;
ASSIGN       : ':=';
AND          : '&' ;
NOT          : '!' ;
EQUALS       : '=' ;
LOWERTHAN    : '<=';
PLUS         : '+' ;
TIMES        : '*' ;
MINUS        : '-' ;
OR           : '||';
DIV          : '/' ;


INTNUM       : ('0'..'9')+;
REALNUM      : ('0'..'9')+ '.' ('0'..'9')* ;

STRING       : '\'' ('\'' '\'' | ~'\'')* '\'';


IDENTIFIER   : ( 'A'..'Z' | 'a'..'z' ) ('A'..'Z' | 'a'..'z'| '0'..'9')*
          ;

COMMENT      : '{' (~'}')* '}' {skip();} ;

WS           : (' ' | '\t' | '\r' | '\n' )+ {skip();} ;
