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

```java
// 1. Defina sua Regex
String regex = "a(b|c)*";

// 2. Converta para Postfix
String postfix = RegexToPostfix.convert(regex);

// 3. Gere o AFN (Thompson)
NFAState nfa = ThompsonConstruction.build(postfix);

// 4. Converta para AFD
DFAState dfa = SubsetConstruction.convertToDFA(nfa);

// 5. Minimize o Autômato
DFAState optimizedDfa = DFAMinimizer.minimize(dfa);
```
