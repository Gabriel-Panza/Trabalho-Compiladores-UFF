# Gerador de Scanners & Parser Top-Down (Racket)

Este projeto consiste em um motor de processamento de linguagens desenvolvido para a disciplina de Compiladores. Ele implementa um pipeline completo, desde a interpretação de expressões regulares até a construção de uma **Árvore Sintática Abstrata (AST)**, utilizando as técnicas clássicas da ciência da computação para garantir eficiência e precisão.

## Metodologia e Arquitetura

O projeto é dividido em dois grandes núcleos: o **Motor Léxico** (Gerador de Scanners) e o **Analisador Sintático** (Parser).

### 1. O Pipeline Léxico (Regex para AFD)
Para transformar regras textuais em um reconhecedor de tokens de alta performance, seguimos o fluxo:

* **Conversão Postfix**: Expressões regulares em formato *Infix* são convertidas para notação posfixa através do algoritmo **Shunting-yard**. Isso resolve ambiguidades de precedência e insere operadores de concatenação explícita ($\cdot$).
* **Construção de Thompson**: A expressão posfixa é transformada em um **Autômato Finito Não-Determinístico (AFN)**. Este modelo permite transições vazias ($\epsilon$) e múltiplos caminhos, facilitando a montagem estrutural da lógica.
* **Subset Construction**: O AFN é convertido em um **Autômato Finito Determinístico (AFD)**. Esse processo elimina o não-determinismo, resultando em um motor que processa cada caractere em tempo linear $O(n)$.
* **Minimização de Hopcroft**: O AFD é otimizado através do particionamento de estados equivalentes, reduzindo o autômato ao seu menor tamanho possível sem perder a fidelidade da linguagem.

### 2. Estratégia de Reconhecimento e Análise
* **Maximum Munch**: O scanner implementa a política do "maior casamento possível". Ao ler o código-fonte, ele garante que tokens mais longos tenham prioridade sobre prefixos (ex: identifica `define` como um único identificador em vez de vários menores).
* **Parser de Descida Recursiva (Top-Down)**: A análise sintática é feita através de uma gramática **LL(1)**. O parser consome a lista de tokens e reconstrói a hierarquia de parênteses e expressões da linguagem Racket, gerando uma AST visual.
* **Sincronização de Erros**: O sistema é capaz de se recuperar de falhas léxicas ou sintáticas, reportando o erro com o contexto da linha e continuando a análise das expressões subsequentes.


### Observações adicionais

O projeto agora inclui persistência do autômato final em JSON com Jackson. Ao executar o `Main`, o autômato final é salvo em `out/automato-final.json` e carregado novamente para validação.

Tambem ha uma classe `Scanner` que le um automato salvo em JSON e um arquivo texto, substituindo cada lexema reconhecido pelo tipo do estado final correspondente.

## Configuração (configs.json)

O projeto agora possui suas configurações e caminhos de arquivos externalizados no arquivo `configs.json`. Isso permite modificar as regras léxicas, a gramática sintática e os arquivos de teste a serem executados de maneira prática.

Exemplo da estrutura do `configs.json`:
```json
{
  "Scanner": {
    "LexicalRules": "casos_teste/lexico.rules",
    "OutputAutomaton": "out/automato-final.json"
  },
  "Parser": {
    "GrammarBnf": "casos_teste/racket.bnf",
    "AutomatonJson": "out/automato-final.json"
  },
  "Test": {
    "Files": [
      "casos_teste/teste_simples.txt",
      "casos_teste/teste_fibonacci.txt",
      "casos_teste/teste_erro.txt"
    ]
  }
}
```

### Entendendo os Campos
*   **Bloco Scanner**: Utilizado pelo **Gerador de Autômatos**. Ele serve apenas para transformar as regras léxicas (pares de Regex e Tipo) no autômato serializado (JSON) que será consumido pelo compilador. Se o autômato já existir, este bloco é opcional para a execução.
*   **Bloco Parser**: Utilizado pelo **Analisador Sintático**. Ele recebe a gramática em formato **BNF** para construir a tabela de análise preditiva LL(1) em memória e aponta para o autômato que deve ser usado na fase léxica.
*   **Bloco Test**: Contém a lista de arquivos de entrada. O compilador percorrerá esta lista executando a análise léxica (via autômato) e sintática (via tabela LL1) sequencialmente para cada arquivo.

## Como Executar

O projeto utiliza um **Makefile** para simplificar o ciclo de vida do desenvolvimento. Certifique-se de ter o JDK 17 ou superior instalado.

Se estiver na raiz do repositorio, entre na pasta principal antes de rodar os comandos:
```bash
cd "Trabalho 1"
```

Garanta que `java -version` e `javac -version` apontem para JDK 17 ou superior. Se precisar, sobrescreva os binarios usados pelo Makefile:
```bash
make JAVAC="/caminho/para/javac" JAVA="/caminho/para/java"
```

### Compilação

Para compilar todos os módulos do projeto e gerar os arquivos binários:
```bash
make
```

### Execução
Para iniciar o compilador e processar o arquivo de testes padrão:


```bash
make run-scanner
make run-parser
```

### Limpeza
Caso precise limpar os arquivos makefile e recriar eles:
```bash
make clean
```
