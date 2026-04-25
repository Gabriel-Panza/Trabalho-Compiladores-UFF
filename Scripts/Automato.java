package Scripts;

/**
 * ESTRUTURA FUNDAMENTAL DO AUTÔMATO (AFN/AFD)
 * Esta classe serve como a representação de grafo para os autômatos, suportando 
 * tanto características de não-determinismo (AFN) quanto determinismo (AFD).
 * * Principais Funcionalidades:
 * 1. Representação de Transições: Utiliza mapas aninhados para armazenar transições 
 * entre estados, permitindo múltiplos destinos para um mesmo símbolo (essencial para AFNs).
 * 2. ε-closure (Fecho-épsilon): Implementa a busca em profundidade (usando Pilha) para 
 * encontrar todos os estados alcançáveis através de transições vazias, base para 
 * o algoritmo de Subset Construction.
 * 3. Função Move: Computa o conjunto de estados alcançáveis a partir de um conjunto 
 * de estados atual dado um símbolo específico do alfabeto.
 */

import java.util.*;

public class Automato {
    int estadoInicial;
    Set<Integer> estadosFinais = new HashSet<>();
    Map<Integer, String> mapeamentoTokens = new HashMap<>(); 
    Map<Integer, Map<String, List<Integer>>> transicoes = new HashMap<>();
    Map<Integer, String> anotacaoDeEstados = new HashMap<>();
    Map<Integer, List<String>> finalStateTipos = new HashMap<>();
    Map<String, Integer> tipoPriority = new HashMap<>();
    String tipo;

    public Automato(String tipo) {
        this.tipo = tipo;
    }

    void addTransicao(int origem, String simbolo, int destino) {
        transicoes
                .computeIfAbsent(origem, k -> new HashMap<>())
                .computeIfAbsent(simbolo, k -> new ArrayList<>())
                .add(destino);
    }

    void addFinalStateTipo(int estado, String tipo) {
        List<String> tipos = finalStateTipos.computeIfAbsent(estado, k -> new ArrayList<>());
        if (!tipos.contains(tipo)) {
            tipos.add(tipo);
        }
    }

    void setTipoPriority(String tipo, int priority) {
        tipoPriority.put(tipo, priority);
    }

    String getPreferredTipoForState(int estado) {
        List<String> tipos = finalStateTipos.get(estado);
        if (tipos == null || tipos.isEmpty()) {
            return null;
        }

        String preferred = tipos.get(0);
        int preferredPriority = tipoPriority.getOrDefault(preferred, Integer.MAX_VALUE);
        for (String current : tipos) {
            int currentPriority = tipoPriority.getOrDefault(current, Integer.MAX_VALUE);
            if (currentPriority < preferredPriority) {
                preferred = current;
                preferredPriority = currentPriority;
            }
        }
        return preferred;
    }

    public Set<String> getAlfabeto() {
        Set<String> alfabeto = new HashSet<>();
        for (Map<String, List<Integer>> transicoesDoEstado : transicoes.values()) {
            for (String simbolo : transicoesDoEstado.keySet()) {
                if (!simbolo.equals("ε")) {
                    alfabeto.add(simbolo);
                }
            }
        }
        return alfabeto;
    }

    public Set<Integer> getTodosEstados() {
        Set<Integer> estados = new HashSet<>();
        estados.add(estadoInicial);
        estados.addAll(estadosFinais);
        estados.addAll(transicoes.keySet());
        for (Map<String, List<Integer>> transicoesDoEstado : transicoes.values()) {
            for (List<Integer> destinos : transicoesDoEstado.values()) {
                estados.addAll(destinos);
            }
        }
        return estados;
    }

    public Set<Integer> epsilonClosure(Set<Integer> estados) {
        Stack<Integer> stack = new Stack<>();
        Set<Integer> closure = new HashSet<>(estados);

        for (int e : estados) {
            stack.push(e);
        }

        while (!stack.isEmpty()) {
            int estado = stack.pop();
            // Verifica se o estado tem transições e se alguma delas é "ε"
            if (transicoes.containsKey(estado) && transicoes.get(estado).containsKey("ε")) {
                for (int destino : transicoes.get(estado).get("ε")) {
                    if (!closure.contains(destino)) {
                        closure.add(destino);
                        stack.push(destino);
                    }
                }
            }
        }
        return closure;
    }

    public Set<Integer> move(Set<Integer> estados, String simbolo) {
        Set<Integer> alcancaveis = new HashSet<>();
        for (int estado : estados) {
            if (transicoes.containsKey(estado) && transicoes.get(estado).containsKey(simbolo)) {
                alcancaveis.addAll(transicoes.get(estado).get(simbolo));
            }
        }
        return alcancaveis;
    }
}