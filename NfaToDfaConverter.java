import java.util.*;

public class NfaToDfaConverter {

    private static Set<Integer> epsilonClosure(Set<Integer> estados, Automato afn) {
        Stack<Integer> stack = new Stack<>();
        Set<Integer> closure = new HashSet<>(estados);

        for (int e : estados) {
            stack.push(e);
        }

        while (!stack.isEmpty()) {
            int estado = stack.pop();
            // Verifica se o estado tem transições e se alguma delas é "ε"
            if (afn.transicoes.containsKey(estado) && afn.transicoes.get(estado).containsKey("ε")) {
                for (int destino : afn.transicoes.get(estado).get("ε")) {
                    if (!closure.contains(destino)) {
                        closure.add(destino);
                        stack.push(destino);
                    }
                }
            }
        }
        return closure;
    }

    private static Set<Integer> move(Set<Integer> estados, String simbolo, Automato afn) {
        Set<Integer> alcancaveis = new HashSet<>();
        for (int estado : estados) {
            if (afn.transicoes.containsKey(estado) && afn.transicoes.get(estado).containsKey(simbolo)) {
                alcancaveis.addAll(afn.transicoes.get(estado).get(simbolo));
            }
        }
        return alcancaveis;
    }

    private static Set<String> getAlfabeto(Automato afn) {
        Set<String> alfabeto = new HashSet<>();
        for (Map<String, List<Integer>> transicoesDoEstado : afn.transicoes.values()) {
            for (String simbolo : transicoesDoEstado.keySet()) {
                if (!simbolo.equals("ε")) {
                    alfabeto.add(simbolo);
                }
            }
        }
        return alfabeto;
    }

    public static Automato convert(Automato afn) {
        Automato afd = new Automato(afn.tipo + "_AFD");
        Set<String> alfabeto = getAlfabeto(afn);

        Map<Set<Integer>, Integer> dfaStatesMap = new HashMap<>();
        Queue<Set<Integer>> fila = new LinkedList<>();

        Set<Integer> estadoInicialAfn = new HashSet<>(Collections.singletonList(afn.estadoInicial));
        Set<Integer> estadoInicialAfd = epsilonClosure(estadoInicialAfn, afn);

        int contadorEstadosAfd = 0;
        dfaStatesMap.put(estadoInicialAfd, contadorEstadosAfd);
        fila.add(estadoInicialAfd);

        afd.estadoInicial = contadorEstadosAfd;
        contadorEstadosAfd++;

        while (!fila.isEmpty()) {
            Set<Integer> estadoAtualAfd = fila.poll();
            int idAtualAfd = dfaStatesMap.get(estadoAtualAfd);

            for (int eFinal : afn.estadosFinais) {
                if (estadoAtualAfd.contains(eFinal)) {
                    afd.estadosFinais.add(idAtualAfd);
                    break;
                }
            }

            for (String simbolo : alfabeto) {
                Set<Integer> alcancaveis = move(estadoAtualAfd, simbolo, afn);

                if (!alcancaveis.isEmpty()) {
                    Set<Integer> proximoEstadoAfd = epsilonClosure(alcancaveis, afn);

                    if (!dfaStatesMap.containsKey(proximoEstadoAfd)) {
                        dfaStatesMap.put(proximoEstadoAfd, contadorEstadosAfd);
                        fila.add(proximoEstadoAfd);
                        contadorEstadosAfd++;
                    }

                    afd.addTransicao(idAtualAfd, simbolo, dfaStatesMap.get(proximoEstadoAfd));
                }
            }
        }

        return afd;
    }
}