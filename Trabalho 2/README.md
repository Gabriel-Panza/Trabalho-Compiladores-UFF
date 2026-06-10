# Trabalho 2 - Compilador Scheme para Python

Compilador em Java para um subconjunto de Scheme, com scanner, parser bottom-up, verificacao semantica e geracao de Python.

## Estrutura

- `grammar/`: especificacoes de referencia no estilo Flex/Bison (`scheme.flex` e `scheme.y`).
- `src/compiler/`: implementacao Java do pipeline.
- `exemplos/`: programas Scheme usados para testar sucesso e erros.
- `out/`: arquivos Python gerados.
- `build/`: classes Java compiladas.
- `pipeline_trabalho2.svg`: diagrama do pipeline do Trabalho 2.
- `makefile`: comandos de compilacao e execucao.

## Como executar

Na pasta `Trabalho 2`:

```sh
make
make run
```

`make run` executa todos os casos exemplos e mostra erros com trecho do codigo, linha, coluna e mensagem para o usuario.

Para compilar um arquivo especifico:

```sh
make run-file FILE=exemplos/fatorial.scm OUT=out/fatorial.py
python out/fatorial.py
```

## Pipeline

`.scm` -> scanner -> tokens -> parser bottom-up -> AST + tabela de simbolos + verificacao semantica -> gerador Python -> `.py`.

Durante a execucao, o compilador imprime esse passo a passo de forma indentada. Em caso de erro, ele mostra o que conseguiu fazer ate a etapa que falhou e depois imprime a mensagem de erro com linha e coluna.

A tabela de simbolos e a verificacao de tipos/contexto sao acionadas pelo parser enquanto as expressoes completas sao reduzidas e adicionadas ao programa.
