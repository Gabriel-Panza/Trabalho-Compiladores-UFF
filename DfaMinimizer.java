import java.util.*;
// https://www.geeksforgeeks.org/theory-of-computation/minimization-of-dfa/
public class DfaMinimizer {

    private static Set<String> getAlfabeto(Automato afd) {
        Set<String> alfabeto = new HashSet<>();
        for (Map<String, List<Integer>> transicoesDoEstado : afd.transicoes.values()) {
            for (String simbolo : transicoesDoEstado.keySet()) {
                if (!simbolo.equals("ε")) {
                    alfabeto.add(simbolo);
                }
            }
        }
        return alfabeto;
    }

    private static Set<Integer> getTodosEstados(Automato afd) {
        Set<Integer> estados = new HashSet<>();
        estados.add(afd.estadoInicial);
        estados.addAll(afd.estadosFinais);
        estados.addAll(afd.transicoes.keySet());
        for (Map<String, List<Integer>> transicoes : afd.transicoes.values()) {
            for (List<Integer> destinos : transicoes.values()) {
                estados.addAll(destinos);
            }
        }
        return estados;
    }

    public static Automato minimize(Automato afd) {
        Set<Integer> todosEstados = getTodosEstados(afd);
        Set<String> alfabeto = getAlfabeto(afd);

        Set<Integer> finais = new HashSet<>(afd.estadosFinais);
        Set<Integer> naoFinais = new HashSet<>(todosEstados);
        naoFinais.removeAll(finais);

        List<Set<Integer>> particoes = new ArrayList<>();
        if (!naoFinais.isEmpty()) particoes.add(naoFinais);
        if (!finais.isEmpty()) particoes.add(finais);

        boolean mudou = true;

        while (mudou) {
            mudou = false;
            List<Set<Integer>> novasParticoes = new ArrayList<>();

            for (Set<Integer> particao : particoes) {
                if (particao.size() <= 1) {
                    novasParticoes.add(particao);
                    continue;
                }

                Map<List<Integer>, Set<Integer>> divisoes = new HashMap<>();

                for (int estado : particao) {
                    List<Integer> assinatura = new ArrayList<>();

                    for (String simbolo : alfabeto) {
                        int destino = -1;

                        if (afd.transicoes.containsKey(estado) && afd.transicoes.get(estado).containsKey(simbolo)) {
                            List<Integer> dests = afd.transicoes.get(estado).get(simbolo);
                            if (!dests.isEmpty()) {
                                destino = dests.get(0);
                            }
                        }

                        int idParticaoDestino = -1;
                        if (destino != -1) {
                            for (int i = 0; i < particoes.size(); i++) {
                                if (particoes.get(i).contains(destino)) {
                                    idParticaoDestino = i;
                                    break;
                                }
                            }
                        }
                        assinatura.add(idParticaoDestino);
                    }

                    divisoes.computeIfAbsent(assinatura, k -> new HashSet<>()).add(estado);
                }

                novasParticoes.addAll(divisoes.values());

                if (divisoes.size() > 1) {
                    mudou = true;
                }
            }
            particoes = novasParticoes;
        }

        Automato afdMinimizado = new Automato(afd.tipo + "_MINIMIZADO");

        Map<Integer, Integer> mapaNovosEstados = new HashMap<>();
        for (int i = 0; i < particoes.size(); i++) {
            for (int estado : particoes.get(i)) {
                mapaNovosEstados.put(estado, i);
            }
        }

        afdMinimizado.estadoInicial = mapaNovosEstados.get(afd.estadoInicial);

        for (int estadoFinal : afd.estadosFinais) {
            if (mapaNovosEstados.containsKey(estadoFinal)) {
                afdMinimizado.estadosFinais.add(mapaNovosEstados.get(estadoFinal));
            }
        }

        for (int i = 0; i < particoes.size(); i++) {
            Set<Integer> particao = particoes.get(i);
            int estadoRepresentante = particao.iterator().next();

            if (afd.transicoes.containsKey(estadoRepresentante)) {
                for (String simbolo : alfabeto) {
                    if (afd.transicoes.get(estadoRepresentante).containsKey(simbolo)) {
                        List<Integer> destinos = afd.transicoes.get(estadoRepresentante).get(simbolo);
                        if (!destinos.isEmpty()) {
                            int destinoAntigo = destinos.get(0);
                            int novoDestino = mapaNovosEstados.get(destinoAntigo);
                            afdMinimizado.addTransicao(i, simbolo, novoDestino);
                        }
                    }
                }
            }
        }

        return afdMinimizado;
    }
}