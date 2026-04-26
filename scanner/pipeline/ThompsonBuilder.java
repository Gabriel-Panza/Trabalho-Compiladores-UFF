package Scripts;

/*
* Lógica Implementada:
1. novoEstado
Responsável por gerar identificadores únicos para os estados do autômato.
- Utiliza um contador global (contadorEstados).
- A cada chamada, retorna um novo número e incrementa o contador.
- Garante que não existam estados duplicados.

2. build
Método principal que constrói o AFN a partir da expressão em pós-fixa.
- Inicializa uma pilha de autômatos (Stack<Automato>).
- Percorre os tokens da expressão:
  - Operando: cria um AFN básico e empilha.
  - '·': desempilha dois AFNs e concatena.
  - '|': desempilha dois AFNs e faz união.
  - '*': desempilha um AFN e aplica fecho de Kleene.
- Ao final, o único elemento da pilha é o AFN completo.
- Define o tipo (ex: FLOAT, ID) e retorna o autômato.

3. basico
Cria um AFN simples para um único símbolo.
- Cria dois estados: inicial e final.
- Adiciona uma transição do inicial para o final com o símbolo.
- Estrutura: q0 --simbolo--> q1
- É a base para construção de expressões maiores.

4. concat
Realiza a concatenação de dois autômatos (A seguido de B).
- Conecta todos os estados finais de A ao estado inicial de B usando ε.
- O estado inicial do resultado é o de A.
- Os estados finais do resultado são os de B.
- Representa: A·B

5. union
Realiza a união entre dois autômatos (A | B).
- Cria um novo estado inicial e um novo estado final.
- Adiciona ε-transições do novo início para os inícios de A e B.
- Adiciona ε-transições dos finais de A e B para o novo final.
- Permite escolher entre A ou B.

6. kleene
Aplica o fecho de Kleene (A*), permitindo repetição 0 ou mais vezes.
- Cria um novo estado inicial e um novo estado final.
- Adiciona ε-transições:
  - do início para o autômato A
  - do início direto para o final (caso vazio)
  - dos finais de A de volta para o início de A (loop)
  - dos finais de A para o estado final
- Permite repetição indefinida ou nenhuma ocorrência.

7. merge
Responsável por copiar todas as transições de um autômato para outro.
- Percorre todos os estados e suas transições.
- Adiciona cada transição no autômato destino.
- Usado para unir os grafos dos autômatos ao criar concatenação, união e kleene.
*/

import java.util.List;
import java.util.Stack;

class ThompsonBuilder {
    private static int contadorEstados = 0;

    private static int novoEstado() {
        return contadorEstados++;
    }

    public static Automato build(List<String> postfix, String tipo) {
        contadorEstados = 0;
        Stack<Automato> stack = new Stack<>();

        for (String token : postfix) {
            switch (token) {
                case "|": {
                    Automato b = stack.pop();
                    Automato a = stack.pop();
                    stack.push(union(a, b));
                    break;
                }
                case "·": {
                    Automato b = stack.pop();
                    Automato a = stack.pop();
                    stack.push(concat(a, b));
                    break;
                }
                case "*": {
                    stack.push(kleene(stack.pop()));
                    break;
                }
                case "+": {
                    stack.push(plus(stack.pop()));
                    break;
                }
                case "?": {
                    stack.push(optional(stack.pop()));
                    break;
                }
                default: {
                    stack.push(basico(token));
                }
            }
        }
        Automato resultado = stack.pop();
        resultado.tipo = tipo;
        return resultado;
    }

    // A+ = A seguido de A* (A·A*)
    private static Automato plus(Automato a) {
        Automato r = new Automato(null);
        int inicio = novoEstado();
        int fim = novoEstado();
        r.estadoInicial = inicio;
        r.estadosFinais.add(fim);

        r.addTransicao(inicio, "ε", a.estadoInicial);
        for (int f : a.estadosFinais) {
            r.addTransicao(f, "ε", a.estadoInicial);
            r.addTransicao(f, "ε", fim);
        }
        merge(r, a);
        return r;
    }

    // A? = A ou vazio (A | ε)
    private static Automato optional(Automato a) {
        Automato r = new Automato(null);
        int inicio = novoEstado();
        int fim = novoEstado();
        r.estadoInicial = inicio;
        r.estadosFinais.add(fim);

        r.addTransicao(inicio, "ε", a.estadoInicial);
        r.addTransicao(inicio, "ε", fim);
        for (int f : a.estadosFinais) {
            r.addTransicao(f, "ε", fim);
        }
        merge(r, a);
        return r;
    }

    // 🔹 AFN básico
    private static Automato basico(String simbolo) {
        Automato a = new Automato(null);

        int inicio = novoEstado();
        int fim = novoEstado();

        a.estadoInicial = inicio;
        a.estadosFinais.add(fim);
        a.addTransicao(inicio, simbolo, fim);

        return a;
    }

    // 🔹 Concatenação
    private static Automato concat(Automato a, Automato b) {
        Automato r = new Automato(null);

        r.estadoInicial = a.estadoInicial;

        for (int f : a.estadosFinais) {
            r.addTransicao(f, "ε", b.estadoInicial);
        }

        r.estadosFinais = b.estadosFinais;

        merge(r, a);
        merge(r, b);

        return r;
    }

    // 🔹 União
    private static Automato union(Automato a, Automato b) {
        Automato r = new Automato(null);

        int inicio = novoEstado();
        int fim = novoEstado();

        r.estadoInicial = inicio;
        r.estadosFinais.add(fim);

        r.addTransicao(inicio, "ε", a.estadoInicial);
        r.addTransicao(inicio, "ε", b.estadoInicial);

        for (int f : a.estadosFinais) {
            r.addTransicao(f, "ε", fim);
        }

        for (int f : b.estadosFinais) {
            r.addTransicao(f, "ε", fim);
        }

        merge(r, a);
        merge(r, b);

        return r;
    }

    // 🔹 Kleene
    private static Automato kleene(Automato a) {
        Automato r = new Automato(null);

        int inicio = novoEstado();
        int fim = novoEstado();

        r.estadoInicial = inicio;
        r.estadosFinais.add(fim);

        r.addTransicao(inicio, "ε", a.estadoInicial);
        r.addTransicao(inicio, "ε", fim);

        for (int f : a.estadosFinais) {
            r.addTransicao(f, "ε", a.estadoInicial);
            r.addTransicao(f, "ε", fim);
        }

        merge(r, a);

        return r;
    }

    // 🔹 Merge de transições
    private static void merge(Automato destino, Automato origem) {
        for (var entry : origem.transicoes.entrySet()) {
            int estado = entry.getKey();

            for (var trans : entry.getValue().entrySet()) {
                for (int prox : trans.getValue()) {
                    destino.addTransicao(estado, trans.getKey(), prox);
                }
            }
        }
    }

    public static Automato unify(List<Automato> automatos) {
    Automato superAfn = new Automato("RACKET_LEXER");
    int novoInicio = novoEstado();
    superAfn.estadoInicial = novoInicio;

    for (Automato afn : automatos) {
        superAfn.addTransicao(novoInicio, "ε", afn.estadoInicial);
        merge(superAfn, afn);
        for (int f : afn.estadosFinais) {
            superAfn.estadosFinais.add(f);
            superAfn.mapeamentoTokens.put(f, afn.tipo);
        }
    }
    return superAfn;
}
}