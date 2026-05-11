# Trabalho 2 - Compilador Scheme para Python

Este diretorio contem uma segunda entrega independente: um compilador em Java para um subconjunto de Scheme, com scanner, parser bottom-up, verificacao semantica de tipos/contexto e geracao de codigo Python.

## Subconjunto aceito

- Literais: inteiros, decimais, strings e booleanos `#t` / `#f`.
- Identificadores Scheme com conversao segura para nomes Python.
- Definicoes: `(define x expr)` e `(define (f a b) corpo...)`.
- Expressoes especiais: `if`, `begin`, `lambda`, `let`, `set!`.
- Operadores: `+`, `-`, `*`, `/`, `<`, `<=`, `>`, `>=`, `=`, `and`, `or`, `not`.
- Saida simples: `display` e `newline`.
- Chamadas de funcoes definidas pelo usuario.

## Relacao com Flex/Bison

A pasta `grammar/` contem especificacoes de referencia para scanner e parser no estilo Flex/Bison. A implementacao entregue em Java usa as mesmas categorias lexicas e uma estrategia bottom-up explicita por deslocamento/reducao de S-expressions, para manter o projeto executavel sem depender de ferramentas externas que nao estejam instaladas.
