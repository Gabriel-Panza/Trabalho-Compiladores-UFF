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

O projeto inclui persistência do autómato final em JSON com Jackson. Ao executar o `Main`, o autômato final é salvo em `out/automato-final.json` e carregado novamente para validação.

Também ha uma classe `Scanner` que le um autómato salvo em JSON e um arquivo texto, substituindo cada lexema reconhecido pelo tipo do estado final correspondente.
O arquivo do Scanner.java estava sendo gerado a partir de uma String ao executar 'make run-scanner', substituindo o nome do arquivo do autômato para um literal,
porém após reorganizar o código para melhor compreensão, a geração parou de funcionar e achamos melhor
manter apenas o Scanner já gerado acessando sempre o mesmo caminho de autómato.

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

* **Scanner.LexicalRules**: Caminho para o arquivo que contém as regras léxicas (`regex -> TIPO_TOKEN`).
* **Parser.GrammarBnf**: Caminho para o arquivo que define a gramática em formato BNF.
* **Test.Files**: Lista de arquivos de entrada que serão avaliados pelo compilador.

## Como Executar

O projeto utiliza um **Makefile** para simplificar o ciclo de vida do desenvolvimento. Certifique-se de ter o JDK 17 ou superior instalado.

### Compilação

Para compilar todos os módulos do projeto e gerar os arquivos binários:
```bash
make
```

### Execução
Para iniciar o compilador e processar o arquivo de testes padrão:
```bash
make run-scanner
make run-scanner-file
make run-parser
```

### Limpeza
Caso precise limpar os arquivos makefile e recriar eles:
```bash
make clean
```
