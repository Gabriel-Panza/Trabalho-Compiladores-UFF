%{
package compiler;

import compiler.DottedListExpr;
import compiler.Expr;
import compiler.ListExpr;
import compiler.PrefixExpr;
import compiler.Program;
import compiler.Token;
import compiler.TokenType;
import compiler.VectorExpr;

/*
 * Especificacao de referencia para o parser.
 * Os terminais abaixo correspondem aos TokenType retornados pelo scanner
 * JFlex descrito em scheme.flex.
 */
%}

%token LPAREN RPAREN INTEGER FLOAT STRING BOOLEAN IDENTIFIER
%token DOT QUOTE QUASIQUOTE UNQUOTE UNQUOTE_SPLICING VECTOR_START
%token CHARACTER RATIONAL HEX_INTEGER BIN_INTEGER

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
    | LPAREN expr_list DOT expr RPAREN
    | VECTOR_START expr_list RPAREN
    | QUOTE expr
    | QUASIQUOTE expr
    | UNQUOTE expr
    | UNQUOTE_SPLICING expr
    ;

atom
    : INTEGER
    | FLOAT
    | STRING
    | BOOLEAN
    | IDENTIFIER
    | CHARACTER
    | RATIONAL
    | HEX_INTEGER
    | BIN_INTEGER
    ;
%%
