# Trabalho-Compiladores-UFF

Este projeto é um gerador de analisadores léxicos (scanners) genérico, desenvolvido com base nos princípios fundamentais da **Teoria da Computação** e **Linguagens Formais**. O motor é capaz de transformar expressões regulares em Autômatos Finitos Determinísticos (AFD) otimizados para o reconhecimento de tokens.

## O Pipeline de Compilação

O gerador segue rigorosamente o fluxo clássico de construção de scanners:

1.  **Regex to Postfix**: Converte expressões regulares (Infix) para notação posfixa usando o algoritmo *Shunting-yard*, facilitando o tratamento de precedência e a inserção de concatenações explícitas.
2.  **Algoritmo de Thompson**: Transforma a expressão posfixa em um **AFN** (Autômato Finito Não-Determinístico) com transições-ε.
3.  **Subset Construction**: Converte o AFN em um **AFD** (Autômato Finito Determinístico), eliminando o não-determinismo e garantindo performance de tempo linear $O(n)$ na análise.
4.  **Minimização de Hopcroft**: Otimiza o AFD final, fundindo estados equivalentes para reduzir a ocupação de memória e o número de transições.

## Tecnologias e Conceitos

* **Linguagem:** Java 17+
* **Estruturas de Dados:** Grafos direcionados, Pilhas de fragmentos, Mapas de transição.
* **Conceitos:** ε-closure, Particionamento de estados, Alfabeto de entrada, Maximum Munch.

## Como utilizar

O projeto agora inclui persistência do autômato final em JSON com Jackson.

```bash
mvn compile
mvn exec:java -Dexec.mainClass=Main
```

Ao executar o `Main`, o autômato final é salvo em `out/automato-final.json` e carregado novamente para validação.

## Scanner via JSON

Tambem ha uma classe `Scanner` que le um automato salvo em JSON e um arquivo texto, substituindo cada lexema reconhecido pelo tipo do estado final correspondente.

```bash
mvn compile
mvn exec:java -Dexec.mainClass=Scanner -Dexec.args="out/automato-final.json caminho/entrada.txt caminho/saida.txt"
```

O terceiro argumento (`caminho/saida.txt`) e opcional. Sem ele, a saida padrao fica em `out/<nome-da-entrada>.scanned.txt`.

