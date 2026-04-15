import java.util.*;

public class Automato {
    int estadoInicial;
    Set<Integer> estadosFinais = new HashSet<>();
    Map<Integer, Map<String, List<Integer>>> transicoes = new HashMap<>();
    Map<Integer, String> anotacaoDeEstados = new HashMap<>();
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