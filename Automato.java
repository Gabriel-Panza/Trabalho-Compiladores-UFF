import java.util.*;

class Automato {
    int estadoInicial;
    Set<Integer> estadosFinais = new HashSet<>();
    Map<Integer, Map<String, List<Integer>>> transicoes = new HashMap<>();
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
}