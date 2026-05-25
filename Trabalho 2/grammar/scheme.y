%{
#include <stdio.h>
#include <stdlib.h>
/* #include "ast.h" -> Descomente quando criar as estruturas da Árvore Sintática */

/* Assinaturas das funções requeridas pelo Bison */
int yylex(void);
void yyerror(const char *s);

/* Funções mockadas para a árvore (você deve implementá-las no seu .c) */
extern int verificar_contexto_e_tipos(void* ast);
extern void gerar_codigo_python(void* ast);

/* Usando void* temporariamente para evitar erro de compilação sem o ast.h */
#define ASTNode void* ASTNode criar_lista_vazia() { return NULL; }
ASTNode adicionar_na_lista(ASTNode lista, ASTNode item) { return NULL; }
ASTNode criar_no_define(char* id, ASTNode exp) { return NULL; }
ASTNode criar_no_if(ASTNode cond, ASTNode then_branch, ASTNode else_branch) { return NULL; }
ASTNode criar_no_chamada_funcao(ASTNode args) { return NULL; }
ASTNode criar_no_inteiro(int val) { return NULL; }
ASTNode criar_no_float(float val) { return NULL; }
ASTNode criar_no_string(char* val) { return NULL; }
ASTNode criar_no_boolean(int val) { return NULL; }
ASTNode criar_no_identificador(char* val) { return NULL; }
%}

/* Fornece mensagens de erro mais descritivas (ex: "syntax error, unexpected IDENTIFIER, expecting RPAREN") */
%define parse.error verbose

/* Definição dos tipos de dados que trafegam do Flex para o Bison e entre as regras */
%union {
    int int_val;
    float float_val;
    char* str_val;
    void* node; /* Substitua void* por struct ASTNode* após criar sua árvore */
}

/* Mapeamento de Tokens -> Tipos na Union */
%token <int_val> INTEGER BOOLEAN HEX_INTEGER BIN_INTEGER
%token <float_val> FLOAT
%token <str_val> STRING IDENTIFIER
%token ERROR RATIONAL CHARACTER

/* Palavras-chave e Símbolos (Sem valor atrelado na union, apenas estruturais) */
%token LPAREN RPAREN VECTOR_START
%token QUOTE QUASIQUOTE UNQUOTE UNQUOTE_SPLICING DOT
%token DEFINE LAMBDA IF COND ELSE LET QUOTE_KW SET_BANG BEGIN_KW AND OR

/* Declaração de retorno dos Não-Terminais */
%type <node> program expr_list expr atom

%%

program
    : expr_list { 
        if (verificar_contexto_e_tipos($1)) {
            gerar_codigo_python($1);
            printf("Compilação e tradução finalizadas com sucesso.\n");
        } else {
            fprintf(stderr, "Erro de tipo ou contexto detectado na AST.\n");
        }
    }
    ;

expr_list
    : /* vazio */ 
        { $$ = criar_lista_vazia(); }
    | expr_list expr 
        { $$ = adicionar_na_lista($1, $2); }
    ;

expr
    : atom 
        { $$ = $1; }
    
    /* Estrutura: (define x 10) */
    | LPAREN DEFINE IDENTIFIER expr RPAREN 
        { $$ = criar_no_define($3, $4); }
    
    /* Estrutura: (if (> a b) a b) */
    | LPAREN IF expr expr expr RPAREN 
        { $$ = criar_no_if($3, $4, $5); }
    
    /* Chamada genérica de função ou operador. Ex: (+ 1 2) */
    | LPAREN expr_list RPAREN 
        { $$ = criar_no_chamada_funcao($2); }
    ;

atom
    : INTEGER    { $$ = criar_no_inteiro($1); }
    | FLOAT      { $$ = criar_no_float($1); }
    | STRING     { $$ = criar_no_string($1); }
    | BOOLEAN    { $$ = criar_no_boolean($1); }
    | IDENTIFIER { $$ = criar_no_identificador($1); }
    ;

%%

/* Função de tratamento de erros sintáticos */
void yyerror(const char *s) {
    /* yytext vem do Flex, precisa ser declarado externamente se usado aqui */
    extern int yylineno;
    extern char* yytext;
    fprintf(stderr, "Erro sintático na linha %d (token '%s'): %s\n", yylineno, yytext, s);
}

/* O main padrão para iniciar o parser */
int main(void) {
    return yyparse();
}
