%{
/* Especificacao de referencia Bison para a AST de S-expressions.
   A versao Java em src/compiler implementa as mesmas reducoes com pilha. */
%}

%token LPAREN RPAREN INTEGER FLOAT STRING BOOLEAN IDENTIFIER

%%
program
    : expr_list
    ;

expr_list
    : /* vazio */
    | expr_list expr
    ;

expr
    : atom
    | LPAREN expr_list RPAREN
    ;

atom
    : INTEGER
    | FLOAT
    | STRING
    | BOOLEAN
    | IDENTIFIER
    ;
%%
